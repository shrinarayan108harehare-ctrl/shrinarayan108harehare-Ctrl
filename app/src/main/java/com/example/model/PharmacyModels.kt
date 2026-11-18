package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class UserRole(val wireValue: String) {
    CUSTOMER("customer"),
    MANAGER("manager"),
    OWNER("owner"),
    DELIVERY("delivery");

    companion object {
        fun fromRaw(raw: String?): UserRole =
            entries.firstOrNull { it.wireValue == raw } ?: CUSTOMER
    }
}

enum class OrderStatus(val wireValue: String, val labelEn: String, val labelHi: String) {
    NEW("NEW", "New", "नया"),
    PENDING("PENDING", "Pending", "लंबित"),
    PROCESSING("PROCESSING", "Processing", "प्रक्रिया में"),
    READY("READY", "Ready for Delivery", "डिलीवरी के लिए तैयार"),
    ASSIGNED("ASSIGNED", "Assigned", "सौंपा गया"),
    PICKED_UP("PICKED_UP", "Picked Up", "पिकअप हो गया"),
    OUT_FOR_DELIVERY("OUT_FOR_DELIVERY", "Out for Delivery", "डिलीवरी के लिए निकला"),
    DELIVERED("DELIVERED", "Delivered", "डिलीवर हुआ"),
    CANCELLED("CANCELLED", "Cancelled", "रद्द");

    companion object {
        fun fromRaw(raw: String?): OrderStatus =
            entries.firstOrNull { it.wireValue == raw } ?: NEW

        val activePipeline = listOf(NEW, PENDING, PROCESSING, READY, ASSIGNED, PICKED_UP, OUT_FOR_DELIVERY)
    }
}

enum class NotificationStatus(val wireValue: String) {
    PENDING("PENDING"),
    SENT_TO_FCM("SENT_TO_FCM"),
    FAILED("FAILED"),
    OPENED("OPENED"),
    READ("READ");

    companion object {
        fun fromRaw(raw: String?): NotificationStatus =
            entries.firstOrNull { it.wireValue == raw } ?: PENDING
    }
}

@Keep
@IgnoreExtraProperties
data class UserAccount(
    @DocumentId val id: String = "",
    val userId: String = "",
    val role: String = "customer",
    val displayName: String = "",
    val phone: String = "",
    val email: String = "",
    val linkedManagerId: String = "",
    val linkedDeliveryPersonId: String = "",
    val active: Boolean = true,
    val language: String = "en",
    val notificationsEnabled: Boolean = true,
    val fcmToken: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CustomerProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val phone: String = "",
    val phoneVerified: Boolean = false,
    val defaultAddressId: String = "",
    val defaultAddressText: String = "",
    val defaultLandmark: String = "",
    val defaultArea: String = "",
    val defaultLat: Double = 26.8467,
    val defaultLng: Double = 80.9462,
    val totalOrders: Long = 0L,
    val totalSpent: Double = 0.0,
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CustomerAddress(
    @DocumentId val id: String = "",
    val userId: String = "",
    val label: String = "Home",
    val houseNo: String = "",
    val street: String = "",
    val villageOrArea: String = "",
    val landmark: String = "",
    val fullAddress: String = "",
    val lat: Double = 26.8467,
    val lng: Double = 80.9462,
    val accuracyMeters: Double = 15.0,
    val primaryAddress: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ManagerAccount(
    @DocumentId val id: String = "",
    val name: String = "",
    val username: String = "",
    val mobile: String = "",
    val roleType: String = "family_manager",
    val passwordHash: String = "",
    val passwordSalt: String = "",
    val active: Boolean = true,
    val linkedUid: String = "",
    @ServerTimestamp val lastLoginAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DeliveryPerson(
    @DocumentId val id: String = "",
    val name: String = "",
    val mobile: String = "",
    val username: String = "",
    val passwordHash: String = "",
    val passwordSalt: String = "",
    val active: Boolean = true,
    val linkedUid: String = "",
    val onShift: Boolean = false,
    val trackingConsentGranted: Boolean = false,
    @ServerTimestamp val lastLoginAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Conversation(
    @DocumentId val id: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val customerArea: String = "",
    val lastMessage: String = "",
    val lastMessageType: String = "text",
    @ServerTimestamp val lastMessageAt: Timestamp? = null,
    val unreadByManager: Long = 0L,
    val unreadByCustomer: Long = 0L,
    val activeOrderId: String = "",
    val activeOrderStatus: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val conversationId: String = "",
    val customerId: String = "",
    val senderUid: String = "",
    val senderRole: String = "customer",
    val senderName: String = "",
    val messageType: String = "text",
    val text: String = "",
    val mediaBase64: String = "",
    val mediaUrl: String = "",
    val mediaFileName: String = "",
    val mediaMimeType: String = "",
    val mediaDurationSec: Long = 0L,
    val orderId: String = "",
    val deliveryStatus: String = "sent",
    val prescriptionVerified: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PharmacyOrder(
    @DocumentId val id: String = "",
    val orderNumber: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val deliveryAddress: String = "",
    val deliveryArea: String = "",
    val deliveryLandmark: String = "",
    val deliveryLat: Double = 26.8467,
    val deliveryLng: Double = 80.9462,
    val locationAccuracyMeters: Double = 20.0,
    val liveLocationShared: Boolean = false,
    val itemsSummary: String = "",
    val prescriptionRequired: Boolean = false,
    val prescriptionVerified: Boolean = false,
    val verifiedByManagerName: String = "",
    val quotedBillAmount: Double = 0.0,
    val quotedDeliveryCharge: Double = 0.0,
    val status: String = "NEW",
    val assignedDeliveryPersonId: String = "",
    val assignedDeliveryUid: String = "",
    val assignedDeliveryPersonName: String = "",
    val assignedAt: Timestamp? = null,
    val pickedUpAt: Timestamp? = null,
    val deliveredAt: Timestamp? = null,
    val cancelledAt: Timestamp? = null,
    val cancellationReason: String = "",
    val deliveryIssue: String = "",
    val estimatedDistanceKm: Double = 0.0,
    val actualDistanceKm: Double = 0.0,
    val deliveryDurationMinutes: Long = 0L,
    val customerRating: Long = 0L,
    val customerFeedback: String = "",
    val lastUpdatedByManagerId: String = "",
    val lastUpdatedByManagerName: String = "",
    val idempotencyKey: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class OrderFinancial(
    @DocumentId val id: String = "",
    val orderId: String = "",
    val orderNumber: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val deliveryArea: String = "",
    val orderStatus: String = "NEW",
    val billValue: Double = 0.0,
    val discountAmount: Double = 0.0,
    val refundAmount: Double = 0.0,
    val netProductSales: Double = 0.0,
    val costValue: Double = 0.0,
    val productGrossProfit: Double = 0.0,
    val deliveryDistanceKm: Double = 0.0,
    val deliveryChargeCollected: Double = 0.0,
    val deliveryExpensePaid: Double = 0.0,
    val netBusinessProfit: Double = 0.0,
    val deliveryPersonId: String = "",
    val deliveryPersonName: String = "",
    val internalNotes: String = "",
    val accountingMethod: String = "ACCRUAL",
    val recordedByManagerId: String = "",
    val recordedByManagerName: String = "",
    val orderDateKey: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DeliveryAssignment(
    @DocumentId val id: String = "",
    val orderId: String = "",
    val orderNumber: String = "",
    val deliveryPersonId: String = "",
    val deliveryPersonName: String = "",
    val assignedByManagerId: String = "",
    val assignedByManagerName: String = "",
    val status: String = "ASSIGNED",
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DeliveryLocation(
    @DocumentId val id: String = "",
    val deliveryPersonId: String = "",
    val deliveryPersonName: String = "",
    val linkedUid: String = "",
    val lat: Double = 26.8467,
    val lng: Double = 80.9462,
    val accuracyMeters: Double = 20.0,
    val trackingActive: Boolean = false,
    val currentStatus: String = "Off Shift",
    val activeOrderCount: Long = 0L,
    @ServerTimestamp val lastUpdatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class RoutePlan(
    @DocumentId val id: String = "",
    val name: String = "",
    val deliveryPersonId: String = "",
    val deliveryPersonName: String = "",
    val stopOrderIds: List<String> = emptyList(),
    val stopLabels: List<String> = emptyList(),
    val stopLats: List<Double> = emptyList(),
    val stopLngs: List<Double> = emptyList(),
    val totalEstimatedDistanceKm: Double = 0.0,
    val totalEstimatedMinutes: Long = 0L,
    val optimized: Boolean = true,
    val createdByManagerId: String = "",
    val createdByManagerName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Announcement(
    @DocumentId val id: String = "",
    val title: String = "",
    val message: String = "",
    val category: String = "GENERAL",
    val targetAudience: String = "ALL_CUSTOMERS",
    val targetUids: List<String> = emptyList(),
    val createdByManagerId: String = "",
    val createdByManagerName: String = "",
    val recipientCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class RecipientNotification(
    @DocumentId val id: String = "",
    val announcementId: String = "",
    val recipientUid: String = "",
    val recipientName: String = "",
    val recipientRole: String = "customer",
    val title: String = "",
    val message: String = "",
    val category: String = "GENERAL",
    val status: String = "PENDING",
    val failureReason: String = "",
    val retryCount: Long = 0L,
    val createdByManagerName: String = "",
    val openedAt: Timestamp? = null,
    val readAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Advertisement(
    @DocumentId val id: String = "",
    val title: String = "",
    val subtitle: String = "",
    val category: String = "OFFER",
    val imagePreset: String = "hero_pharmacy",
    val imageBase64: String = "",
    val linkUrl: String = "",
    val active: Boolean = true,
    val priority: Long = 1L,
    val createdByManagerId: String = "",
    val createdByManagerName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class BusinessExpense(
    @DocumentId val id: String = "",
    val title: String = "",
    val category: String = "OTHER",
    val amount: Double = 0.0,
    val expenseDateKey: String = "",
    val notes: String = "",
    val recordedByManagerId: String = "",
    val recordedByManagerName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class BusinessSetting(
    @DocumentId val id: String = "",
    val shopName: String = "PRAYAG DEEN & SONS",
    val phone: String = "+91 98765 43210",
    val address: String = "Main Medical Market, Sadar Bazar",
    val hours: String = "8:00 AM - 10:30 PM",
    val baseDeliveryCharge: Double = 20.0,
    val freeDeliveryAbove: Double = 500.0,
    val shopLat: Double = 26.8467,
    val shopLng: Double = 80.9462,
    val accountingMethod: String = "ACCRUAL",
    val backupRetentionDays: Long = 365L,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AuditLog(
    @DocumentId val id: String = "",
    val managerId: String = "",
    val managerName: String = "",
    val actorUid: String = "",
    val actionPerformed: String = "",
    val recordType: String = "",
    val recordId: String = "",
    val fieldChanged: String = "",
    val previousValue: String = "",
    val newValue: String = "",
    val dateKey: String = "",
    val timeDisplay: String = "",
    val actionStatus: String = "SUCCESS",
    val modificationReason: String = "",
    val prevHash: String = "GENESIS",
    val entryHash: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)
