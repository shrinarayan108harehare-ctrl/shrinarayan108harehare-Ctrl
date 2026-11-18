package com.example

import com.example.data.BiAnalyticsCalculator
import com.example.model.BusinessExpense
import com.example.model.CustomerProfile
import com.example.model.DateRangePreset
import com.example.model.DeliveryPerson
import com.example.model.OrderFinancial
import com.example.model.PharmacyOrder
import com.example.util.LocationUtils
import com.example.util.SecurityCrypto
import com.example.util.TimezoneUtils
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class BiAnalyticsAndSecurityTest {

    @Test
    fun `bi analytics exact financial formulas compute profit accurately`() {
        val today = TimezoneUtils.todayDateKey()
        val nowTs = Timestamp(Date())

        val orders = listOf(
            PharmacyOrder(
                id = "o1",
                orderNumber = "ORD-1001",
                customerId = "c1",
                customerName = "Ramesh",
                customerPhone = "9876543210",
                deliveryArea = "Sadar Bazar",
                deliveryLat = 26.85,
                deliveryLng = 80.95,
                itemsSummary = "Dolo 650 x2, Pan-D x1",
                status = "DELIVERED",
                assignedDeliveryPersonId = "dp1",
                assignedDeliveryPersonName = "Rahul",
                deliveryDurationMinutes = 25L,
                createdAt = nowTs
            ),
            PharmacyOrder(
                id = "o2",
                orderNumber = "ORD-1002",
                customerId = "c2",
                customerName = "Sita",
                customerPhone = "9876543211",
                deliveryArea = "Chowk",
                deliveryLat = 26.86,
                deliveryLng = 80.91,
                itemsSummary = "Azithromycin 500",
                status = "CANCELLED",
                assignedDeliveryPersonId = "dp1",
                assignedDeliveryPersonName = "Rahul",
                createdAt = nowTs
            )
        )

        val financials = listOf(
            OrderFinancial(
                id = "o1",
                orderId = "o1",
                orderNumber = "ORD-1001",
                customerId = "c1",
                customerName = "Ramesh",
                deliveryArea = "Sadar Bazar",
                orderStatus = "DELIVERED",
                billValue = 1000.0,
                discountAmount = 100.0,
                refundAmount = 50.0,
                netProductSales = 850.0,
                costValue = 600.0,
                productGrossProfit = 250.0,
                deliveryDistanceKm = 4.5,
                deliveryChargeCollected = 30.0,
                deliveryExpensePaid = 20.0,
                netBusinessProfit = 260.0,
                deliveryPersonId = "dp1",
                deliveryPersonName = "Rahul",
                orderDateKey = today,
                createdAt = nowTs
            )
        )

        val expenses = listOf(
            BusinessExpense(
                id = "e1",
                title = "Shop Electricity",
                category = "UTILITIES",
                amount = 40.0,
                expenseDateKey = today,
                createdAt = nowTs
            )
        )

        val customers = listOf(
            CustomerProfile(userId = "c1", name = "Ramesh", createdAt = nowTs),
            CustomerProfile(userId = "c2", name = "Sita", createdAt = nowTs)
        )

        val staff = listOf(DeliveryPerson(id = "dp1", name = "Rahul"))

        val snap = BiAnalyticsCalculator.compute(
            preset = DateRangePreset.TODAY,
            customStartKey = today,
            customEndKey = today,
            accountingMethod = "ACCRUAL",
            orders = orders,
            financials = financials,
            expenses = expenses,
            customers = customers,
            deliveryPersonnel = staff
        )

        // Net Product Sales = 1000 - 100 - 50 = 850
        assertEquals(850.0, snap.overview.totalNetSales, 0.01)
        // Product Gross Profit = 850 - 600 = 250
        assertEquals(250.0, snap.overview.totalProductGrossProfit, 0.01)
        // Net Business Profit = 850 + 30 - 600 - 20 - 40 = 220
        assertEquals(220.0, snap.overview.totalNetBusinessProfit, 0.01)
        assertEquals(1, snap.overview.completedOrderCount)
        assertEquals(1, snap.overview.cancelledOrderCount)
        assertEquals(50.0, snap.overview.cancellationRatePercent, 0.01)
        assertTrue(snap.areaStats.any { it.areaName == "Sadar Bazar" })
        assertTrue(snap.productStats.any { it.medicineName.contains("Dolo 650") })
    }

    @Test
    fun `salted sha256 and audit hash chain are deterministic and tamper evident`() {
        val h1 = SecurityCrypto.hashPassword("salt1", "1234")
        val h2 = SecurityCrypto.hashPassword("salt2", "1234")
        assertEquals(h1, SecurityCrypto.hashPassword("salt1", "1234"))
        assertNotEquals(h1, h2)

        val chain1 = SecurityCrypto.computeAuditHash("GENESIS", "m1", "ORDER_DELIVERED", "orders", "ORD-1", "ASSIGNED", "DELIVERED", "2026-01-01", "10:00 AM")
        val chain2 = SecurityCrypto.computeAuditHash("GENESIS", "m1", "ORDER_DELIVERED", "orders", "ORD-1", "ASSIGNED", "CANCELLED", "2026-01-01", "10:00 AM")
        assertNotEquals(chain1, chain2)
    }

    @Test
    fun `haversine distance calculates positive road distance`() {
        val d = LocationUtils.haversineKm(26.8467, 80.9462, 26.8867, 80.9862)
        assertTrue(d in 4.0..8.0)
    }
}
