package com.example.model

enum class DateRangePreset(val labelEn: String, val labelHi: String) {
    TODAY("Today", "आज"),
    YESTERDAY("Yesterday", "कल"),
    LAST_7_DAYS("Last 7 Days", "पिछले 7 दिन"),
    THIS_WEEK("This Week", "इस सप्ताह"),
    THIS_MONTH("This Month", "इस महीने"),
    LAST_MONTH("Last Month", "पिछला महीना"),
    THIS_QUARTER("This Quarter", "इस तिमाही"),
    THIS_YEAR("This Year", "इस वर्ष"),
    CUSTOM("Custom Range", "कस्टम तिथि")
}

data class FinancialOverview(
    val totalGrossSales: Double = 0.0,
    val totalDiscounts: Double = 0.0,
    val totalRefunds: Double = 0.0,
    val totalNetSales: Double = 0.0,
    val totalCostValue: Double = 0.0,
    val totalProductGrossProfit: Double = 0.0,
    val totalDeliveryChargesCollected: Double = 0.0,
    val totalDeliveryExpensesPaid: Double = 0.0,
    val totalOtherBusinessExpenses: Double = 0.0,
    val totalNetBusinessProfit: Double = 0.0,
    val completedOrderCount: Int = 0,
    val cancelledOrderCount: Int = 0,
    val averageOrderValue: Double = 0.0,
    val cancellationRatePercent: Double = 0.0
)

data class SalesTrendPoint(
    val label: String,
    val netSales: Double,
    val netProfit: Double,
    val orderCount: Int
)

data class CustomerAnalyticsStat(
    val customerId: String,
    val customerName: String,
    val customerPhone: String,
    val area: String,
    val totalOrders: Int,
    val totalSales: Double,
    val averageOrderValue: Double,
    val isReturning: Boolean
)

data class AreaAnalyticsStat(
    val areaName: String,
    val orderCount: Int,
    val totalSales: Double,
    val totalDeliveryCharges: Double,
    val totalDistanceKm: Double,
    val avgDistanceKm: Double,
    val totalNetProfit: Double,
    val centerLat: Double,
    val centerLng: Double
)

data class DeliveryPersonAnalyticsStat(
    val deliveryPersonId: String,
    val deliveryPersonName: String,
    val totalAssigned: Int,
    val completedDeliveries: Int,
    val pendingDeliveries: Int,
    val cancelledOrFailed: Int,
    val totalDistanceKm: Double,
    val avgDistanceKm: Double,
    val avgDurationMinutes: Double,
    val totalDeliveryChargesCollected: Double,
    val totalDeliveryExpensesPaid: Double
)

data class MedicineProductStat(
    val medicineName: String,
    val requestCount: Int,
    val estimatedSales: Double,
    val unavailableMentions: Int
)

data class BiDashboardSnapshot(
    val dateRangeLabel: String = "",
    val generatedAtIst: String = "",
    val accountingMethod: String = "ACCRUAL",
    val overview: FinancialOverview = FinancialOverview(),
    val salesTrend: List<SalesTrendPoint> = emptyList(),
    val totalCustomers: Int = 0,
    val newCustomers: Int = 0,
    val returningCustomers: Int = 0,
    val topCustomers: List<CustomerAnalyticsStat> = emptyList(),
    val areaStats: List<AreaAnalyticsStat> = emptyList(),
    val deliveryStats: List<DeliveryPersonAnalyticsStat> = emptyList(),
    val productStats: List<MedicineProductStat> = emptyList()
)
