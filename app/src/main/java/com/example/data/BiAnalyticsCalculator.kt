package com.example.data

import com.example.model.AreaAnalyticsStat
import com.example.model.BiDashboardSnapshot
import com.example.model.BusinessExpense
import com.example.model.CustomerAnalyticsStat
import com.example.model.CustomerProfile
import com.example.model.DateRangePreset
import com.example.model.DeliveryPerson
import com.example.model.DeliveryPersonAnalyticsStat
import com.example.model.FinancialOverview
import com.example.model.MedicineProductStat
import com.example.model.OrderFinancial
import com.example.model.OrderStatus
import com.example.model.PharmacyOrder
import com.example.model.SalesTrendPoint
import com.example.util.TimezoneUtils

object BiAnalyticsCalculator {

    fun compute(
        preset: DateRangePreset,
        customStartKey: String,
        customEndKey: String,
        accountingMethod: String,
        orders: List<PharmacyOrder>,
        financials: List<OrderFinancial>,
        expenses: List<BusinessExpense>,
        customers: List<CustomerProfile>,
        deliveryPersonnel: List<DeliveryPerson>
    ): BiDashboardSnapshot {
        val (startKey, endKey) = TimezoneUtils.rangeForPreset(preset, customStartKey, customEndKey)
        val finByOrderId = financials.associateBy { it.orderId }

        val ordersInRange = orders.filter { ord ->
            val key = TimezoneUtils.formatDateKey(ord.createdAt)
            key in startKey..endKey
        }
        val cancelledOrders = ordersInRange.filter { it.status == OrderStatus.CANCELLED.wireValue }
        val nonCancelledOrders = ordersInRange.filter { it.status != OrderStatus.CANCELLED.wireValue }
        val completedOrders = if (accountingMethod == "CASH") {
            nonCancelledOrders.filter { it.status == OrderStatus.DELIVERED.wireValue }
        } else {
            nonCancelledOrders.filter { it.status == OrderStatus.DELIVERED.wireValue || finByOrderId[it.id] != null }
        }

        var grossSales = 0.0
        var discounts = 0.0
        var refunds = 0.0
        var costValue = 0.0
        var deliveryCharges = 0.0
        var deliveryExpensesOrder = 0.0

        val trendMap = linkedMapOf<String, MutableList<Double>>() // [sales, profit, count]
        val areaMap = linkedMapOf<String, MutableList<Double>>() // [count, sales, delChg, dist, profit, latSum, lngSum]
        val custOrderCountAllTime = orders.filter { it.status != OrderStatus.CANCELLED.wireValue }.groupingBy { it.customerId }.eachCount()
        val custMap = linkedMapOf<String, Pair<Int, Double>>()

        completedOrders.forEach { ord ->
            val fin = finByOrderId[ord.id]
            val bill = fin?.billValue ?: ord.quotedBillAmount
            val disc = fin?.discountAmount ?: 0.0
            val ref = fin?.refundAmount ?: 0.0
            val netProd = (bill - disc - ref).coerceAtLeast(0.0)
            val cost = fin?.costValue ?: 0.0
            val prodGrossProfit = netProd - cost
            val delChg = fin?.deliveryChargeCollected ?: ord.quotedDeliveryCharge
            val delExp = fin?.deliveryExpensePaid ?: 0.0
            val orderNetProfit = prodGrossProfit + delChg - delExp
            val dist = if (ord.actualDistanceKm > 0.0) ord.actualDistanceKm else (fin?.deliveryDistanceKm ?: ord.estimatedDistanceKm)

            grossSales += bill
            discounts += disc
            refunds += ref
            costValue += cost
            deliveryCharges += delChg
            deliveryExpensesOrder += delExp

            val dateKey = fin?.orderDateKey?.ifBlank { TimezoneUtils.formatDateKey(ord.createdAt) }
                ?: TimezoneUtils.formatDateKey(ord.createdAt)
            val t = trendMap.getOrPut(dateKey) { mutableListOf(0.0, 0.0, 0.0) }
            t[0] += netProd
            t[1] += orderNetProfit
            t[2] += 1.0

            val areaName = ord.deliveryArea.ifBlank { "Unspecified" }
            val a = areaMap.getOrPut(areaName) { mutableListOf(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0) }
            a[0] += 1.0
            a[1] += netProd
            a[2] += delChg
            a[3] += dist
            a[4] += orderNetProfit
            a[5] += ord.deliveryLat
            a[6] += ord.deliveryLng

            val prevCust = custMap[ord.customerId] ?: (0 to 0.0)
            custMap[ord.customerId] = (prevCust.first + 1) to (prevCust.second + netProd)
        }

        val expensesInRange = expenses.filter { it.expenseDateKey in startKey..endKey }
        val extraDeliveryExpenses = expensesInRange.filter { it.category == "DELIVERY" }.sumOf { it.amount }
        val otherExpenses = expensesInRange.filter { it.category != "DELIVERY" }.sumOf { it.amount }

        val netProductSales = (grossSales - discounts - refunds).coerceAtLeast(0.0)
        val productGrossProfit = netProductSales - costValue
        val totalDeliveryExpenses = deliveryExpensesOrder + extraDeliveryExpenses
        val netBusinessProfit = productGrossProfit + deliveryCharges - totalDeliveryExpenses - otherExpenses
        val completedCount = completedOrders.size
        val aov = if (completedCount > 0) netProductSales / completedCount else 0.0
        val totalOrdersWithCancelled = ordersInRange.size
        val cancelRate = if (totalOrdersWithCancelled > 0) {
            (cancelledOrders.size.toDouble() / totalOrdersWithCancelled.toDouble()) * 100.0
        } else 0.0

        val overview = FinancialOverview(
            totalGrossSales = grossSales,
            totalDiscounts = discounts,
            totalRefunds = refunds,
            totalNetSales = netProductSales,
            totalCostValue = costValue,
            totalProductGrossProfit = productGrossProfit,
            totalDeliveryChargesCollected = deliveryCharges,
            totalDeliveryExpensesPaid = totalDeliveryExpenses,
            totalOtherBusinessExpenses = otherExpenses,
            totalNetBusinessProfit = netBusinessProfit,
            completedOrderCount = completedCount,
            cancelledOrderCount = cancelledOrders.size,
            averageOrderValue = aov,
            cancellationRatePercent = cancelRate
        )

        val salesTrend = trendMap.entries.sortedBy { it.key }.map { (k, v) ->
            SalesTrendPoint(
                label = k.takeLast(5),
                netSales = v[0],
                netProfit = v[1],
                orderCount = v[2].toInt()
            )
        }

        val custById = customers.associateBy { it.userId }
        val topCustomers = custMap.entries.map { (cid, pair) ->
            val profile = custById[cid]
            val orderSample = completedOrders.firstOrNull { it.customerId == cid }
            val allTimeCount = custOrderCountAllTime[cid] ?: pair.first
            CustomerAnalyticsStat(
                customerId = cid,
                customerName = profile?.name ?: orderSample?.customerName ?: "Customer",
                customerPhone = profile?.phone ?: orderSample?.customerPhone.orEmpty(),
                area = profile?.defaultArea ?: orderSample?.deliveryArea.orEmpty(),
                totalOrders = pair.first,
                totalSales = pair.second,
                averageOrderValue = if (pair.first > 0) pair.second / pair.first else 0.0,
                isReturning = allTimeCount > 1
            )
        }.sortedByDescending { it.totalSales }

        val areaStats = areaMap.entries.map { (name, v) ->
            val cnt = v[0].toInt().coerceAtLeast(1)
            AreaAnalyticsStat(
                areaName = name,
                orderCount = v[0].toInt(),
                totalSales = v[1],
                totalDeliveryCharges = v[2],
                totalDistanceKm = v[3],
                avgDistanceKm = v[3] / cnt,
                totalNetProfit = v[4],
                centerLat = v[5] / cnt,
                centerLng = v[6] / cnt
            )
        }.sortedByDescending { it.orderCount }

        val dpStats = deliveryPersonnel.map { dp ->
            val dpOrders = ordersInRange.filter { it.assignedDeliveryPersonId == dp.id }
            val dpDelivered = dpOrders.filter { it.status == OrderStatus.DELIVERED.wireValue }
            val dpPending = dpOrders.filter { it.status in listOf("ASSIGNED", "PICKED_UP", "OUT_FOR_DELIVERY") }
            val dpCancelled = dpOrders.filter { it.status == OrderStatus.CANCELLED.wireValue || it.deliveryIssue.isNotBlank() }
            val totalKm = dpDelivered.sumOf { ord ->
                val fin = finByOrderId[ord.id]
                if (ord.actualDistanceKm > 0.0) ord.actualDistanceKm else (fin?.deliveryDistanceKm ?: ord.estimatedDistanceKm)
            }
            val avgKm = if (dpDelivered.isNotEmpty()) totalKm / dpDelivered.size else 0.0
            val avgDur = if (dpDelivered.isNotEmpty()) dpDelivered.map { it.deliveryDurationMinutes.toDouble() }.average() else 0.0
            val chgCol = dpDelivered.sumOf { finByOrderId[it.id]?.deliveryChargeCollected ?: it.quotedDeliveryCharge }
            val expPaid = dpDelivered.sumOf { finByOrderId[it.id]?.deliveryExpensePaid ?: 0.0 }
            DeliveryPersonAnalyticsStat(
                deliveryPersonId = dp.id,
                deliveryPersonName = dp.name,
                totalAssigned = dpOrders.size,
                completedDeliveries = dpDelivered.size,
                pendingDeliveries = dpPending.size,
                cancelledOrFailed = dpCancelled.size,
                totalDistanceKm = totalKm,
                avgDistanceKm = avgKm,
                avgDurationMinutes = avgDur,
                totalDeliveryChargesCollected = chgCol,
                totalDeliveryExpensesPaid = expPaid
            )
        }.sortedByDescending { it.completedDeliveries }

        // Product mention breakdown from order item summaries
        val prodCounts = linkedMapOf<String, Pair<Int, Double>>()
        val unavailableCounts = linkedMapOf<String, Int>()
        ordersInRange.forEach { ord ->
            val tokens = ord.itemsSummary.split(",", "\n", ";")
                .map { it.trim() }
                .filter { it.length >= 3 }
            if (ord.status == OrderStatus.CANCELLED.wireValue && ord.cancellationReason.contains("stock", ignoreCase = true)) {
                tokens.forEach { item -> unavailableCounts[item] = (unavailableCounts[item] ?: 0) + 1 }
            } else if (ord.status != OrderStatus.CANCELLED.wireValue) {
                val perItemShare = if (tokens.isNotEmpty()) (finByOrderId[ord.id]?.billValue ?: ord.quotedBillAmount) / tokens.size else 0.0
                tokens.forEach { item ->
                    val prev = prodCounts[item] ?: (0 to 0.0)
                    prodCounts[item] = (prev.first + 1) to (prev.second + perItemShare)
                }
            }
        }
        val productStats = prodCounts.entries.map { (name, pair) ->
            MedicineProductStat(
                medicineName = name,
                requestCount = pair.first,
                estimatedSales = pair.second,
                unavailableMentions = unavailableCounts[name] ?: 0
            )
        }.sortedByDescending { it.requestCount }.take(15)

        val returningCount = topCustomers.count { it.isReturning }
        val newCount = topCustomers.size - returningCount

        return BiDashboardSnapshot(
            dateRangeLabel = if (startKey == endKey) startKey else "$startKey to $endKey",
            generatedAtIst = TimezoneUtils.formatDateTimeDisplay(null),
            accountingMethod = accountingMethod,
            overview = overview,
            salesTrend = salesTrend,
            totalCustomers = customers.size.coerceAtLeast(topCustomers.size),
            newCustomers = newCount,
            returningCustomers = returningCount,
            topCustomers = topCustomers,
            areaStats = areaStats,
            deliveryStats = dpStats,
            productStats = productStats
        )
    }
}
