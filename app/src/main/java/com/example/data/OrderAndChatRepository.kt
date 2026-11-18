package com.example.data

import com.example.firebase.OperationType
import com.example.firebase.handleFirestoreError
import com.example.model.Advertisement
import com.example.model.Announcement
import com.example.model.AuditLog
import com.example.model.BusinessExpense
import com.example.model.BusinessSetting
import com.example.model.ChatMessage
import com.example.model.Conversation
import com.example.model.DeliveryAssignment
import com.example.model.DeliveryLocation
import com.example.model.DeliveryPerson
import com.example.model.ManagerAccount
import com.example.model.OrderFinancial
import com.example.model.OrderStatus
import com.example.model.PharmacyOrder
import com.example.model.RecipientNotification
import com.example.model.RoutePlan
import com.example.model.UserAccount
import com.example.util.SecurityCrypto
import com.example.util.TimezoneUtils
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class OrderAndChatRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    private fun requireUid(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("Authentication required.")

    // --- Real-time Streams ---
    fun observeCustomerConversation(customerId: String): Flow<Conversation?> =
        db.collection("conversations").document(customerId)
            .snapshots()
            .map { if (it.exists()) it.toObject(Conversation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) else null }

    fun observeAllConversations(): Flow<List<Conversation>> =
        db.collection("conversations")
            .orderBy("lastMessageAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { it.toObjects(Conversation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }

    fun observeMessages(conversationId: String): Flow<List<ChatMessage>> =
        db.collection("conversations").document(conversationId).collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .limitToLast(200)
            .snapshots()
            .map { it.toObjects(ChatMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }

    fun observeCustomerOrders(customerId: String): Flow<List<PharmacyOrder>> =
        db.collection("orders")
            .whereEqualTo("customerId", customerId)
            .snapshots()
            .map { snap ->
                snap.toObjects(PharmacyOrder::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }

    fun observeAllOrders(): Flow<List<PharmacyOrder>> =
        db.collection("orders")
            .snapshots()
            .map { snap ->
                snap.toObjects(PharmacyOrder::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }

    fun observeDeliveryAssignedOrders(deliveryUid: String): Flow<List<PharmacyOrder>> =
        db.collection("orders")
            .whereEqualTo("assignedDeliveryUid", deliveryUid)
            .snapshots()
            .map { snap ->
                snap.toObjects(PharmacyOrder::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }

    fun observeOrderFinancials(): Flow<List<OrderFinancial>> =
        db.collection("orderFinancials")
            .snapshots()
            .map { it.toObjects(OrderFinancial::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }

    fun observeDeliveryLocations(): Flow<List<DeliveryLocation>> =
        db.collection("deliveryLocations")
            .snapshots()
            .map { it.toObjects(DeliveryLocation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }

    fun observeOwnDeliveryLocation(dpId: String): Flow<DeliveryLocation?> =
        db.collection("deliveryLocations").document(dpId)
            .snapshots()
            .map { if (it.exists()) it.toObject(DeliveryLocation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) else null }

    fun observeRoutePlans(): Flow<List<RoutePlan>> =
        db.collection("routePlans")
            .snapshots()
            .map { snap ->
                snap.toObjects(RoutePlan::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }

    fun observeDeliveryRoutePlans(dpId: String): Flow<List<RoutePlan>> =
        db.collection("routePlans")
            .whereEqualTo("deliveryPersonId", dpId)
            .snapshots()
            .map { snap ->
                snap.toObjects(RoutePlan::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }

    fun observeAdvertisements(): Flow<List<Advertisement>> =
        db.collection("advertisements")
            .snapshots()
            .map { snap ->
                snap.toObjects(Advertisement::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.priority }
            }

    fun observePublicActiveAdvertisements(): Flow<List<Advertisement>> =
        db.collection("advertisements")
            .whereEqualTo("active", true)
            .snapshots()
            .map { snap ->
                snap.toObjects(Advertisement::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.priority }
            }

    fun observeAnnouncements(): Flow<List<Announcement>> =
        db.collection("announcements")
            .snapshots()
            .map { snap ->
                snap.toObjects(Announcement::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }

    fun observeMyNotifications(uid: String): Flow<List<RecipientNotification>> =
        db.collection("notifications")
            .whereEqualTo("recipientUid", uid)
            .snapshots()
            .map { snap ->
                snap.toObjects(RecipientNotification::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }

    fun observeAllNotifications(): Flow<List<RecipientNotification>> =
        db.collection("notifications")
            .snapshots()
            .map { snap ->
                snap.toObjects(RecipientNotification::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }

    fun observeExpenses(): Flow<List<BusinessExpense>> =
        db.collection("expenses")
            .snapshots()
            .map { snap ->
                snap.toObjects(BusinessExpense::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }

    fun observeAuditLogs(): Flow<List<AuditLog>> =
        db.collection("auditLogs")
            .snapshots()
            .map { snap ->
                snap.toObjects(AuditLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }

    // --- Audit Log Append-Only Writer ---
    suspend fun recordAuditLog(
        managerId: String,
        managerName: String,
        actionPerformed: String,
        recordType: String,
        recordId: String,
        fieldChanged: String = "",
        previousValue: String = "",
        newValue: String = "",
        modificationReason: String = "",
        actionStatus: String = "SUCCESS"
    ) {
        try {
            val uid = requireUid()
            val latest = db.collection("auditLogs")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()
            val prevHash = latest.documents.firstOrNull()?.getString("entryHash") ?: "GENESIS"
            val dateKey = TimezoneUtils.todayDateKey()
            val timeDisplay = TimezoneUtils.formatTimeDisplay()
            val entryHash = SecurityCrypto.computeAuditHash(
                prevHash = prevHash,
                managerId = managerId,
                actionPerformed = actionPerformed,
                recordType = recordType,
                recordId = recordId,
                previousValue = previousValue,
                newValue = newValue,
                dateKey = dateKey,
                timeDisplay = timeDisplay
            )
            db.collection("auditLogs").document().set(
                mapOf(
                    "managerId" to managerId,
                    "managerName" to managerName,
                    "actorUid" to uid,
                    "actionPerformed" to actionPerformed,
                    "recordType" to recordType,
                    "recordId" to recordId,
                    "fieldChanged" to fieldChanged.take(80),
                    "previousValue" to previousValue.take(500),
                    "newValue" to newValue.take(500),
                    "dateKey" to dateKey,
                    "timeDisplay" to timeDisplay,
                    "actionStatus" to actionStatus,
                    "modificationReason" to modificationReason.take(300),
                    "prevHash" to prevHash,
                    "entryHash" to entryHash,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "auditLogs")
        }
    }

    // --- Chat Messaging ---
    suspend fun sendMessage(
        conversationId: String,
        customerName: String,
        customerPhone: String,
        customerArea: String,
        senderRole: String,
        senderName: String,
        messageType: String,
        text: String,
        mediaBase64: String = "",
        mediaUrl: String = "",
        mediaFileName: String = "",
        mediaMimeType: String = "",
        mediaDurationSec: Long = 0L,
        orderId: String = ""
    ): Result<Unit> {
        return try {
            val uid = requireUid()
            val convRef = db.collection("conversations").document(conversationId)
            val msgRef = convRef.collection("messages").document()
            val convSnap = convRef.get().await()
            val batch = db.batch()
            val previewText = when (messageType) {
                "prescription" -> "Prescription attached: ${text.ifBlank { mediaFileName }}"
                "image" -> "Photo: ${text.ifBlank { mediaFileName }}"
                "document" -> "Document: $mediaFileName"
                "video" -> "Video: $mediaFileName"
                "audio" -> "Voice message (${mediaDurationSec}s)"
                else -> text
            }.take(450)

            if (!convSnap.exists()) {
                batch.set(
                    convRef,
                    mapOf(
                        "customerId" to conversationId,
                        "customerName" to customerName.ifBlank { "Customer" },
                        "customerPhone" to customerPhone,
                        "customerArea" to customerArea,
                        "lastMessage" to previewText,
                        "lastMessageType" to messageType,
                        "lastMessageAt" to FieldValue.serverTimestamp(),
                        "unreadByManager" to if (senderRole == "customer") 1L else 0L,
                        "unreadByCustomer" to if (senderRole == "manager") 1L else 0L,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                val updates = mutableMapOf<String, Any>(
                    "customerName" to customerName.ifBlank { convSnap.getString("customerName") ?: "Customer" },
                    "lastMessage" to previewText,
                    "lastMessageType" to messageType,
                    "lastMessageAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (senderRole == "customer") {
                    updates["unreadByManager"] = FieldValue.increment(1L)
                } else {
                    updates["unreadByCustomer"] = FieldValue.increment(1L)
                }
                batch.update(convRef, updates)
            }

            val msgPayload = mutableMapOf<String, Any>(
                "conversationId" to conversationId,
                "customerId" to conversationId,
                "senderUid" to uid,
                "senderRole" to senderRole,
                "senderName" to senderName,
                "messageType" to messageType,
                "text" to text.take(4000),
                "deliveryStatus" to "sent",
                "prescriptionVerified" to false,
                "createdAt" to FieldValue.serverTimestamp()
            )
            if (mediaBase64.isNotBlank()) msgPayload["mediaBase64"] = mediaBase64
            if (mediaUrl.isNotBlank()) msgPayload["mediaUrl"] = mediaUrl.take(1500)
            if (mediaFileName.isNotBlank()) msgPayload["mediaFileName"] = mediaFileName.take(200)
            if (mediaMimeType.isNotBlank()) msgPayload["mediaMimeType"] = mediaMimeType.take(100)
            if (mediaDurationSec > 0L) msgPayload["mediaDurationSec"] = mediaDurationSec
            if (orderId.isNotBlank()) msgPayload["orderId"] = orderId
            batch.set(msgRef, msgPayload)
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "conversations/$conversationId/messages")
            Result.failure(e)
        }
    }

    suspend fun markConversationRead(conversationId: String, byManager: Boolean) {
        try {
            requireUid()
            val ref = db.collection("conversations").document(conversationId)
            if (ref.get().await().exists()) {
                val field = if (byManager) "unreadByManager" else "unreadByCustomer"
                val batch = db.batch()
                batch.update(ref, mapOf(field to 0L, "updatedAt" to FieldValue.serverTimestamp()))
                val senderToMarkRead = if (byManager) "customer" else "manager"
                val unreadMsgs = ref.collection("messages")
                    .whereEqualTo("senderRole", senderToMarkRead)
                    .whereEqualTo("deliveryStatus", "sent")
                    .limit(30)
                    .get()
                    .await()
                unreadMsgs.documents.forEach { doc ->
                    batch.update(doc.reference, mapOf("deliveryStatus" to "read"))
                }
                batch.commit().await()
            }
        } catch (_: Exception) {
        }
    }

    suspend fun verifyPrescriptionMessage(
        conversationId: String,
        messageId: String,
        managerId: String,
        managerName: String
    ): Result<Unit> {
        return try {
            requireUid()
            db.collection("conversations").document(conversationId)
                .collection("messages").document(messageId)
                .update(mapOf("prescriptionVerified" to true)).await()
            recordAuditLog(
                managerId = managerId,
                managerName = managerName,
                actionPerformed = "PRESCRIPTION_VERIFIED",
                recordType = "message",
                recordId = messageId,
                fieldChanged = "prescriptionVerified",
                previousValue = "false",
                newValue = "true",
                modificationReason = "Pharmacist verification"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Orders & Confidential Financials ---
    suspend fun createOrder(
        customerId: String,
        customerName: String,
        customerPhone: String,
        deliveryAddress: String,
        deliveryArea: String,
        deliveryLandmark: String,
        lat: Double,
        lng: Double,
        accuracyMeters: Double,
        itemsSummary: String,
        prescriptionRequired: Boolean,
        idempotencyKey: String,
        createdByManagerId: String? = null,
        createdByManagerName: String? = null
    ): Result<String> {
        return try {
            requireUid()
            // Prevent duplicate orders from double-clicks or retries
            val existing = db.collection("orders")
                .whereEqualTo("customerId", customerId)
                .whereEqualTo("idempotencyKey", idempotencyKey)
                .limit(1)
                .get()
                .await()
            if (!existing.isEmpty) {
                return Result.success(existing.documents.first().id)
            }

            val orderNumber = TimezoneUtils.generateOrderNumber()
            val orderRef = db.collection("orders").document()
            val batch = db.batch()
            val orderPayload = mutableMapOf<String, Any>(
                "orderNumber" to orderNumber,
                "customerId" to customerId,
                "customerName" to customerName,
                "customerPhone" to customerPhone,
                "deliveryAddress" to deliveryAddress,
                "deliveryArea" to deliveryArea.ifBlank { "Local Area" },
                "deliveryLandmark" to deliveryLandmark,
                "deliveryLat" to lat,
                "deliveryLng" to lng,
                "locationAccuracyMeters" to accuracyMeters,
                "liveLocationShared" to false,
                "itemsSummary" to itemsSummary.take(2000),
                "prescriptionRequired" to prescriptionRequired,
                "prescriptionVerified" to false,
                "quotedBillAmount" to 0.0,
                "quotedDeliveryCharge" to 0.0,
                "status" to "NEW",
                "estimatedDistanceKm" to 0.0,
                "actualDistanceKm" to 0.0,
                "deliveryDurationMinutes" to 0L,
                "idempotencyKey" to idempotencyKey,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!createdByManagerId.isNullOrBlank()) {
                orderPayload["lastUpdatedByManagerId"] = createdByManagerId
                orderPayload["lastUpdatedByManagerName"] = createdByManagerName.orEmpty()
            }
            batch.set(orderRef, orderPayload)

            val convRef = db.collection("conversations").document(customerId)
            val convExists = convRef.get().await().exists()
            if (convExists) {
                batch.update(
                    convRef,
                    mapOf(
                        "activeOrderId" to orderRef.id,
                        "activeOrderStatus" to "NEW",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                batch.set(
                    convRef,
                    mapOf(
                        "customerId" to customerId,
                        "customerName" to customerName,
                        "customerPhone" to customerPhone,
                        "customerArea" to deliveryArea,
                        "lastMessage" to "New order placed: $orderNumber",
                        "lastMessageType" to "order_update",
                        "lastMessageAt" to FieldValue.serverTimestamp(),
                        "unreadByManager" to 1L,
                        "unreadByCustomer" to 0L,
                        "activeOrderId" to orderRef.id,
                        "activeOrderStatus" to "NEW",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()

            if (!createdByManagerId.isNullOrBlank()) {
                recordAuditLog(
                    managerId = createdByManagerId,
                    managerName = createdByManagerName.orEmpty(),
                    actionPerformed = "MANUAL_ORDER_CREATED",
                    recordType = "orders",
                    recordId = orderRef.id,
                    newValue = "$orderNumber ($customerName)",
                    modificationReason = "Manager manual order entry"
                )
            }
            Result.success(orderRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "orders")
            Result.failure(e)
        }
    }

    suspend fun saveOrderFinancials(
        order: PharmacyOrder,
        existingFinancial: OrderFinancial?,
        billValue: Double,
        discountAmount: Double,
        refundAmount: Double,
        costValue: Double,
        deliveryDistanceKm: Double,
        deliveryChargeCollected: Double,
        deliveryExpensePaid: Double,
        internalNotes: String,
        accountingMethod: String,
        managerId: String,
        managerName: String,
        reason: String
    ): Result<Unit> {
        return try {
            requireUid()
            val netProductSales = (billValue - discountAmount - refundAmount).coerceAtLeast(0.0)
            val productGrossProfit = netProductSales - costValue
            val netBusinessProfit = productGrossProfit + deliveryChargeCollected - deliveryExpensePaid
            val dateKey = TimezoneUtils.formatDateKey(order.createdAt)

            val finRef = db.collection("orderFinancials").document(order.id)
            val payload = mutableMapOf<String, Any>(
                "orderId" to order.id,
                "orderNumber" to order.orderNumber,
                "customerId" to order.customerId,
                "customerName" to order.customerName,
                "deliveryArea" to order.deliveryArea,
                "orderStatus" to order.status,
                "billValue" to billValue,
                "discountAmount" to discountAmount,
                "refundAmount" to refundAmount,
                "netProductSales" to netProductSales,
                "costValue" to costValue,
                "productGrossProfit" to productGrossProfit,
                "deliveryDistanceKm" to deliveryDistanceKm,
                "deliveryChargeCollected" to deliveryChargeCollected,
                "deliveryExpensePaid" to deliveryExpensePaid,
                "netBusinessProfit" to netBusinessProfit,
                "deliveryPersonId" to order.assignedDeliveryPersonId,
                "deliveryPersonName" to order.assignedDeliveryPersonName,
                "internalNotes" to internalNotes.take(1000),
                "accountingMethod" to accountingMethod,
                "recordedByManagerId" to managerId,
                "recordedByManagerName" to managerName,
                "orderDateKey" to dateKey,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            val batch = db.batch()
            if (existingFinancial == null) {
                payload["createdAt"] = FieldValue.serverTimestamp()
                batch.set(finRef, payload)
            } else {
                batch.update(finRef, payload)
            }
            // Customer only sees quoted bill amount & delivery charge, NEVER cost or profit!
            batch.update(
                db.collection("orders").document(order.id),
                mapOf(
                    "quotedBillAmount" to (billValue - discountAmount).coerceAtLeast(0.0),
                    "quotedDeliveryCharge" to deliveryChargeCollected,
                    "estimatedDistanceKm" to deliveryDistanceKm,
                    "lastUpdatedByManagerId" to managerId,
                    "lastUpdatedByManagerName" to managerName,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()

            val prevSummary = if (existingFinancial == null) "None"
            else "Bill ₹${existingFinancial.billValue}, Cost ₹${existingFinancial.costValue}, DelChg ₹${existingFinancial.deliveryChargeCollected}"
            val newSummary = "Bill ₹$billValue, Cost ₹$costValue, DelChg ₹$deliveryChargeCollected, NetProfit ₹$netBusinessProfit"
            recordAuditLog(
                managerId = managerId,
                managerName = managerName,
                actionPerformed = "ORDER_FINANCIAL_SAVED",
                recordType = "orderFinancials",
                recordId = order.orderNumber,
                fieldChanged = "bill_cost_delivery",
                previousValue = prevSummary,
                newValue = newSummary,
                modificationReason = reason.ifBlank { "Order financial entry" }
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "orderFinancials/${order.id}")
            Result.failure(e)
        }
    }

    suspend fun assignDeliveryPerson(
        order: PharmacyOrder,
        dp: DeliveryPerson,
        managerId: String,
        managerName: String,
        reason: String = "Order delivery assignment"
    ): Result<Unit> {
        return try {
            requireUid()
            if (order.status == OrderStatus.CANCELLED.wireValue || order.status == OrderStatus.DELIVERED.wireValue) {
                return Result.failure(IllegalStateException("Cannot assign a ${order.status} order."))
            }
            val isReassign = order.assignedDeliveryPersonId.isNotBlank()
            val batch = db.batch()
            batch.update(
                db.collection("orders").document(order.id),
                mapOf(
                    "status" to OrderStatus.ASSIGNED.wireValue,
                    "assignedDeliveryPersonId" to dp.id,
                    "assignedDeliveryUid" to dp.linkedUid,
                    "assignedDeliveryPersonName" to dp.name,
                    "assignedAt" to FieldValue.serverTimestamp(),
                    "lastUpdatedByManagerId" to managerId,
                    "lastUpdatedByManagerName" to managerName,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                db.collection("deliveryAssignments").document(),
                mapOf(
                    "orderId" to order.id,
                    "orderNumber" to order.orderNumber,
                    "deliveryPersonId" to dp.id,
                    "deliveryPersonName" to dp.name,
                    "assignedByManagerId" to managerId,
                    "assignedByManagerName" to managerName,
                    "status" to if (isReassign) "REASSIGNED" else "ASSIGNED",
                    "notes" to reason,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            val finRef = db.collection("orderFinancials").document(order.id)
            if (finRef.get().await().exists()) {
                batch.update(
                    finRef,
                    mapOf(
                        "orderStatus" to OrderStatus.ASSIGNED.wireValue,
                        "deliveryPersonId" to dp.id,
                        "deliveryPersonName" to dp.name,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()

            recordAuditLog(
                managerId = managerId,
                managerName = managerName,
                actionPerformed = if (isReassign) "DELIVERY_REASSIGNED" else "DELIVERY_ASSIGNED",
                recordType = "orders",
                recordId = order.orderNumber,
                fieldChanged = "assignedDeliveryPerson",
                previousValue = order.assignedDeliveryPersonName.ifBlank { "Unassigned" },
                newValue = dp.name,
                modificationReason = reason
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "orders/${order.id}")
            Result.failure(e)
        }
    }

    suspend fun updateOrderStatusByManager(
        order: PharmacyOrder,
        newStatus: OrderStatus,
        managerId: String,
        managerName: String,
        deliveredByDp: DeliveryPerson? = null,
        cancellationReason: String = "",
        reason: String = "Status updated"
    ): Result<Unit> {
        return try {
            requireUid()
            val cur = OrderStatus.fromRaw(order.status)
            if (cur == OrderStatus.DELIVERED && newStatus == OrderStatus.DELIVERED) {
                return Result.failure(IllegalStateException("Order ${order.orderNumber} is already Delivered."))
            }
            if (cur == OrderStatus.CANCELLED && newStatus == OrderStatus.DELIVERED) {
                return Result.failure(IllegalStateException("Cannot deliver an already Cancelled order."))
            }
            if (newStatus == OrderStatus.DELIVERED && order.prescriptionRequired && !order.prescriptionVerified) {
                return Result.failure(IllegalStateException("Prescription must be verified by manager before marking Delivered."))
            }

            val updates = mutableMapOf<String, Any>(
                "status" to newStatus.wireValue,
                "lastUpdatedByManagerId" to managerId,
                "lastUpdatedByManagerName" to managerName,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (newStatus == OrderStatus.DELIVERED) {
                updates["deliveredAt"] = FieldValue.serverTimestamp()
                if (deliveredByDp != null) {
                    updates["assignedDeliveryPersonId"] = deliveredByDp.id
                    updates["assignedDeliveryUid"] = deliveredByDp.linkedUid
                    updates["assignedDeliveryPersonName"] = deliveredByDp.name
                }
            }
            if (newStatus == OrderStatus.CANCELLED) {
                updates["cancelledAt"] = FieldValue.serverTimestamp()
                updates["cancellationReason"] = cancellationReason.ifBlank { "Cancelled by manager" }
            }

            val batch = db.batch()
            batch.update(db.collection("orders").document(order.id), updates)

            val convRef = db.collection("conversations").document(order.customerId)
            if (convRef.get().await().exists()) {
                batch.update(
                    convRef,
                    mapOf("activeOrderStatus" to newStatus.wireValue, "updatedAt" to FieldValue.serverTimestamp())
                )
            }
            val finRef = db.collection("orderFinancials").document(order.id)
            if (finRef.get().await().exists()) {
                val finUpdates = mutableMapOf<String, Any>(
                    "orderStatus" to newStatus.wireValue,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (deliveredByDp != null) {
                    finUpdates["deliveryPersonId"] = deliveredByDp.id
                    finUpdates["deliveryPersonName"] = deliveredByDp.name
                }
                batch.update(finRef, finUpdates)
            }
            batch.commit().await()

            recordAuditLog(
                managerId = managerId,
                managerName = managerName,
                actionPerformed = "ORDER_${newStatus.wireValue}",
                recordType = "orders",
                recordId = order.orderNumber,
                fieldChanged = "status",
                previousValue = order.status,
                newValue = newStatus.wireValue,
                modificationReason = cancellationReason.ifBlank { reason }
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "orders/${order.id}")
            Result.failure(e)
        }
    }

    suspend fun verifyOrderPrescription(
        order: PharmacyOrder,
        managerId: String,
        managerName: String
    ): Result<Unit> {
        return try {
            requireUid()
            db.collection("orders").document(order.id).update(
                mapOf(
                    "prescriptionVerified" to true,
                    "verifiedByManagerName" to managerName,
                    "lastUpdatedByManagerId" to managerId,
                    "lastUpdatedByManagerName" to managerName,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            recordAuditLog(
                managerId = managerId,
                managerName = managerName,
                actionPerformed = "ORDER_RX_VERIFIED",
                recordType = "orders",
                recordId = order.orderNumber,
                fieldChanged = "prescriptionVerified",
                previousValue = order.prescriptionVerified.toString(),
                newValue = "true",
                modificationReason = "Pharmacist verified prescription"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateOrderStatusByDelivery(
        order: PharmacyOrder,
        newStatus: OrderStatus,
        issueReport: String = "",
        actualDistanceKm: Double? = null
    ): Result<Unit> {
        return try {
            requireUid()
            if (order.status == OrderStatus.CANCELLED.wireValue || order.status == OrderStatus.DELIVERED.wireValue) {
                return Result.failure(IllegalStateException("Order is already ${order.status}."))
            }
            val updates = mutableMapOf<String, Any>(
                "status" to newStatus.wireValue,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (newStatus == OrderStatus.PICKED_UP) updates["pickedUpAt"] = FieldValue.serverTimestamp()
            if (newStatus == OrderStatus.DELIVERED) {
                updates["deliveredAt"] = FieldValue.serverTimestamp()
                val assignedSec = order.assignedAt?.seconds ?: order.createdAt?.seconds ?: 0L
                if (assignedSec > 0L) {
                    val mins = ((System.currentTimeMillis() / 1000L) - assignedSec) / 60L
                    updates["deliveryDurationMinutes"] = mins.coerceAtLeast(1L)
                }
            }
            if (issueReport.isNotBlank()) updates["deliveryIssue"] = issueReport.take(400)
            if (actualDistanceKm != null && actualDistanceKm > 0.0) updates["actualDistanceKm"] = actualDistanceKm
            db.collection("orders").document(order.id).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "orders/${order.id}")
            Result.failure(e)
        }
    }

    suspend fun toggleLiveLocationForOrder(
        orderId: String,
        enabled: Boolean,
        lat: Double,
        lng: Double,
        accuracyMeters: Double
    ): Result<Unit> {
        return try {
            requireUid()
            val updates = mutableMapOf<String, Any>(
                "liveLocationShared" to enabled,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (enabled) {
                updates["deliveryLat"] = lat
                updates["deliveryLng"] = lng
                updates["locationAccuracyMeters"] = accuracyMeters
            }
            db.collection("orders").document(orderId).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun submitCustomerRating(orderId: String, rating: Long, feedback: String): Result<Unit> {
        return try {
            requireUid()
            db.collection("orders").document(orderId).update(
                mapOf(
                    "customerRating" to rating.coerceIn(1L, 5L),
                    "customerFeedback" to feedback.take(500),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Delivery Shift Tracking & Route Plans ---
    suspend fun updateDeliveryShiftLocation(
        dpId: String,
        dpName: String,
        lat: Double,
        lng: Double,
        accuracyMeters: Double,
        trackingActive: Boolean,
        statusLabel: String,
        activeOrderCount: Long
    ): Result<Unit> {
        return try {
            val uid = requireUid()
            val batch = db.batch()
            batch.set(
                db.collection("deliveryLocations").document(dpId),
                mapOf(
                    "deliveryPersonId" to dpId,
                    "deliveryPersonName" to dpName,
                    "linkedUid" to uid,
                    "lat" to lat,
                    "lng" to lng,
                    "accuracyMeters" to accuracyMeters,
                    "trackingActive" to trackingActive,
                    "currentStatus" to statusLabel,
                    "activeOrderCount" to activeOrderCount,
                    "lastUpdatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            )
            batch.update(
                db.collection("deliveryPersonnel").document(dpId),
                mapOf(
                    "onShift" to trackingActive,
                    "trackingConsentGranted" to trackingActive,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "deliveryLocations/$dpId")
            Result.failure(e)
        }
    }

    suspend fun saveRoutePlan(
        name: String,
        dp: DeliveryPerson?,
        stops: List<PharmacyOrder>,
        totalDistanceKm: Double,
        estimatedMinutes: Long,
        optimized: Boolean,
        managerId: String,
        managerName: String
    ): Result<Unit> {
        return try {
            requireUid()
            val ref = db.collection("routePlans").document()
            ref.set(
                mapOf(
                    "name" to name.ifBlank { "Route ${TimezoneUtils.todayDateKey()}" },
                    "deliveryPersonId" to (dp?.id ?: ""),
                    "deliveryPersonName" to (dp?.name ?: ""),
                    "stopOrderIds" to stops.map { it.id },
                    "stopLabels" to stops.map { "${it.orderNumber}: ${it.customerName} (${it.deliveryArea})" },
                    "stopLats" to stops.map { it.deliveryLat },
                    "stopLngs" to stops.map { it.deliveryLng },
                    "totalEstimatedDistanceKm" to totalDistanceKm,
                    "totalEstimatedMinutes" to estimatedMinutes,
                    "optimized" to optimized,
                    "createdByManagerId" to managerId,
                    "createdByManagerName" to managerName,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            recordAuditLog(
                managerId = managerId,
                managerName = managerName,
                actionPerformed = "ROUTE_PLAN_SAVED",
                recordType = "routePlans",
                recordId = ref.id,
                newValue = "${stops.size} stops, ${"%.1f".format(totalDistanceKm)} km -> ${dp?.name ?: "Unassigned"}",
                modificationReason = "Multi-stop delivery route plan"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Announcements & Reliable Recipient Notifications ---
    suspend fun sendAnnouncement(
        title: String,
        message: String,
        category: String,
        targetAudience: String,
        selectedTargetUids: List<String>,
        managerId: String,
        managerName: String
    ): Result<Int> {
        return try {
            requireUid()
            val allUsers = db.collection("users").get().await()
                .toObjects(UserAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                .filter { it.active }

            val recipients = when (targetAudience) {
                "ALL_CUSTOMERS" -> allUsers.filter { it.role == "customer" }
                "ALL_DELIVERY" -> allUsers.filter { it.role == "delivery" }
                "ALL_USERS" -> allUsers.filter { it.role == "customer" || it.role == "delivery" }
                else -> allUsers.filter { it.userId in selectedTargetUids }
            }

            val annRef = db.collection("announcements").document()
            val batch = db.batch()
            batch.set(
                annRef,
                mapOf(
                    "title" to title.trim(),
                    "message" to message.trim(),
                    "category" to category,
                    "targetAudience" to targetAudience,
                    "targetUids" to recipients.map { it.userId }.take(100),
                    "createdByManagerId" to managerId,
                    "createdByManagerName" to managerName,
                    "recipientCount" to recipients.size.toLong(),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            recipients.forEach { user ->
                val notifId = "${annRef.id}_${user.userId}"
                val initialStatus = if (!user.notificationsEnabled) "FAILED" else "PENDING"
                val failReason = if (!user.notificationsEnabled) "User disabled notifications; saved to Announcement Inbox" else ""
                batch.set(
                    db.collection("notifications").document(notifId),
                    mapOf(
                        "announcementId" to annRef.id,
                        "recipientUid" to user.userId,
                        "recipientName" to user.displayName,
                        "recipientRole" to user.role,
                        "title" to title.trim(),
                        "message" to message.trim(),
                        "category" to category,
                        "status" to initialStatus,
                        "failureReason" to failReason,
                        "retryCount" to 0L,
                        "createdByManagerName" to managerName,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            recordAuditLog(
                managerId = managerId,
                managerName = managerName,
                actionPerformed = "ANNOUNCEMENT_SENT",
                recordType = "announcements",
                recordId = annRef.id,
                newValue = "$title ($targetAudience, ${recipients.size} recipients)",
                modificationReason = "Shop broadcast announcement"
            )
            Result.success(recipients.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "announcements")
            Result.failure(e)
        }
    }

    suspend fun markNotificationState(notificationId: String, newStatus: String) {
        try {
            requireUid()
            val updates = mutableMapOf<String, Any>(
                "status" to newStatus,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (newStatus == "OPENED") updates["openedAt"] = FieldValue.serverTimestamp()
            if (newStatus == "READ") updates["readAt"] = FieldValue.serverTimestamp()
            db.collection("notifications").document(notificationId).update(updates).await()
        } catch (_: Exception) {
        }
    }

    // --- Advertisements, Expenses, Settings, Managers & Delivery Personnel Management ---
    suspend fun saveAdvertisement(
        adId: String?,
        title: String,
        subtitle: String,
        category: String,
        imagePreset: String,
        imageBase64: String,
        linkUrl: String,
        active: Boolean,
        priority: Long,
        managerId: String,
        managerName: String
    ): Result<Unit> {
        return try {
            requireUid()
            val ref = if (adId.isNullOrBlank()) db.collection("advertisements").document()
            else db.collection("advertisements").document(adId)
            val payload = mutableMapOf<String, Any>(
                "title" to title.trim(),
                "subtitle" to subtitle.trim(),
                "category" to category,
                "imagePreset" to imagePreset,
                "imageBase64" to imageBase64,
                "linkUrl" to linkUrl.trim(),
                "active" to active,
                "priority" to priority,
                "createdByManagerId" to managerId,
                "createdByManagerName" to managerName,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (adId.isNullOrBlank()) {
                payload["createdAt"] = FieldValue.serverTimestamp()
                ref.set(payload).await()
            } else {
                ref.update(payload).await()
            }
            recordAuditLog(
                managerId = managerId,
                managerName = managerName,
                actionPerformed = if (adId.isNullOrBlank()) "AD_CREATED" else "AD_UPDATED",
                recordType = "advertisements",
                recordId = ref.id,
                newValue = "$title (active=$active)",
                modificationReason = "Advertisement management"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteAdvertisement(adId: String, title: String, managerId: String, managerName: String): Result<Unit> {
        return try {
            requireUid()
            db.collection("advertisements").document(adId).delete().await()
            recordAuditLog(managerId, managerName, "AD_DELETED", "advertisements", adId, "title", title, "Deleted", "Advertisement removed")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addBusinessExpense(
        title: String,
        category: String,
        amount: Double,
        dateKey: String,
        notes: String,
        managerId: String,
        managerName: String
    ): Result<Unit> {
        return try {
            requireUid()
            val ref = db.collection("expenses").document()
            ref.set(
                mapOf(
                    "title" to title.trim(),
                    "category" to category,
                    "amount" to amount,
                    "expenseDateKey" to dateKey,
                    "notes" to notes.trim(),
                    "recordedByManagerId" to managerId,
                    "recordedByManagerName" to managerName,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            recordAuditLog(managerId, managerName, "EXPENSE_ADDED", "expenses", ref.id, category, "0", "₹$amount ($title)", "Operating expense recorded")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveBusinessSettings(
        setting: BusinessSetting,
        managerId: String,
        managerName: String
    ): Result<Unit> {
        return try {
            requireUid()
            val batch = db.batch()
            batch.set(
                db.collection("businessSettings").document("shop_public"),
                mapOf(
                    "shopName" to setting.shopName,
                    "phone" to setting.phone,
                    "address" to setting.address,
                    "hours" to setting.hours,
                    "baseDeliveryCharge" to setting.baseDeliveryCharge,
                    "freeDeliveryAbove" to setting.freeDeliveryAbove,
                    "shopLat" to setting.shopLat,
                    "shopLng" to setting.shopLng,
                    "accountingMethod" to setting.accountingMethod,
                    "backupRetentionDays" to setting.backupRetentionDays,
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            )
            batch.set(
                db.collection("businessSettings").document("financial_config"),
                mapOf(
                    "shopName" to setting.shopName,
                    "accountingMethod" to setting.accountingMethod,
                    "backupRetentionDays" to setting.backupRetentionDays,
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            )
            batch.commit().await()
            recordAuditLog(managerId, managerName, "SETTINGS_UPDATED", "businessSettings", "shop_public", "config", "", "Method=${setting.accountingMethod}, DelFee=₹${setting.baseDeliveryCharge}", "Shop settings updated")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun upsertDeliveryPerson(
        existing: DeliveryPerson?,
        name: String,
        mobile: String,
        username: String,
        newPassword: String?,
        active: Boolean,
        managerId: String,
        managerName: String
    ): Result<Unit> {
        return try {
            requireUid()
            val cleanUser = username.trim().lowercase()
            val ref = if (existing == null) db.collection("deliveryPersonnel").document()
            else db.collection("deliveryPersonnel").document(existing.id)
            val batch = db.batch()

            val salt = if (!newPassword.isNullOrBlank() || existing == null) SecurityCrypto.generateSalt() else existing.passwordSalt
            val hash = if (!newPassword.isNullOrBlank()) SecurityCrypto.hashPassword(salt, newPassword) else existing?.passwordHash.orEmpty()

            if (existing == null) {
                batch.set(
                    ref,
                    mapOf(
                        "name" to name.trim(),
                        "mobile" to mobile.trim(),
                        "username" to cleanUser,
                        "passwordHash" to hash,
                        "passwordSalt" to salt,
                        "active" to active,
                        "onShift" to false,
                        "trackingConsentGranted" to false,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                batch.update(
                    ref,
                    mapOf(
                        "name" to name.trim(),
                        "mobile" to mobile.trim(),
                        "username" to cleanUser,
                        "passwordHash" to hash,
                        "passwordSalt" to salt,
                        "active" to active,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.set(
                db.collection("deliveryDirectory").document(cleanUser),
                mapOf(
                    "accountId" to ref.id,
                    "username" to cleanUser,
                    "salt" to salt,
                    "roleType" to "delivery",
                    "active" to active
                )
            )
            // If deactivated, unassign pending orders cleanly
            if (existing != null && existing.active && !active) {
                val pendingAssigned = db.collection("orders")
                    .whereEqualTo("assignedDeliveryPersonId", existing.id)
                    .get()
                    .await()
                    .toObjects(PharmacyOrder::class.java)
                    .filter { it.status in listOf("ASSIGNED", "PICKED_UP", "OUT_FOR_DELIVERY") }
                pendingAssigned.forEach { ord ->
                    batch.update(
                        db.collection("orders").document(ord.id),
                        mapOf(
                            "status" to "READY",
                            "assignedDeliveryPersonId" to "",
                            "assignedDeliveryUid" to "",
                            "assignedDeliveryPersonName" to "",
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }
            batch.commit().await()
            recordAuditLog(
                managerId = managerId,
                managerName = managerName,
                actionPerformed = if (existing == null) "DELIVERY_PERSON_ADDED" else "DELIVERY_PERSON_UPDATED",
                recordType = "deliveryPersonnel",
                recordId = ref.id,
                previousValue = existing?.let { "${it.name} (active=${it.active})" } ?: "None",
                newValue = "$name (active=$active)",
                modificationReason = "Delivery staff management"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Owner-only Manager Management ---
    suspend fun updateManagerAccountByOwner(
        target: ManagerAccount,
        newName: String,
        newMobile: String,
        newPassword: String?,
        active: Boolean,
        actorManagerId: String,
        actorManagerName: String
    ): Result<Unit> {
        return try {
            requireUid()
            val salt = if (!newPassword.isNullOrBlank()) SecurityCrypto.generateSalt() else target.passwordSalt
            val hash = if (!newPassword.isNullOrBlank()) SecurityCrypto.hashPassword(salt, newPassword) else target.passwordHash
            val batch = db.batch()
            batch.update(
                db.collection("managers").document(target.id),
                mapOf(
                    "name" to newName.trim(),
                    "mobile" to newMobile.trim(),
                    "passwordSalt" to salt,
                    "passwordHash" to hash,
                    "active" to active,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                db.collection("managerDirectory").document(target.username),
                mapOf(
                    "accountId" to target.id,
                    "username" to target.username,
                    "salt" to salt,
                    "roleType" to target.roleType,
                    "active" to active
                )
            )
            batch.commit().await()
            recordAuditLog(
                managerId = actorManagerId,
                managerName = actorManagerName,
                actionPerformed = if (!newPassword.isNullOrBlank()) "MANAGER_PASSWORD_RESET" else "MANAGER_ACCOUNT_UPDATED",
                recordType = "managers",
                recordId = target.id,
                previousValue = "${target.name} (active=${target.active})",
                newValue = "$newName (active=$active)",
                modificationReason = "Owner manager administration"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "managers/${target.id}")
            Result.failure(e)
        }
    }

    suspend fun changeOwnManagerPassword(
        target: ManagerAccount,
        newPassword: String
    ): Result<Unit> {
        return try {
            requireUid()
            val salt = SecurityCrypto.generateSalt()
            val hash = SecurityCrypto.hashPassword(salt, newPassword)
            val batch = db.batch()
            batch.update(
                db.collection("managers").document(target.id),
                mapOf(
                    "passwordSalt" to salt,
                    "passwordHash" to hash,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                db.collection("managerDirectory").document(target.username),
                mapOf(
                    "accountId" to target.id,
                    "username" to target.username,
                    "salt" to salt,
                    "roleType" to target.roleType,
                    "active" to target.active
                )
            )
            batch.commit().await()
            recordAuditLog(
                managerId = target.id,
                managerName = target.name,
                actionPerformed = "MANAGER_SELF_PASSWORD_CHANGED",
                recordType = "managers",
                recordId = target.id,
                previousValue = "********",
                newValue = "********",
                modificationReason = "Self password rotation"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createManagerAccountByOwner(
        name: String,
        mobile: String,
        username: String,
        password: String,
        actorManagerId: String,
        actorManagerName: String
    ): Result<Unit> {
        return try {
            requireUid()
            val cleanUser = username.trim().lowercase().replace(Regex("[^a-z0-9_\\-]"), "")
            if (cleanUser.length < 2) {
                return Result.failure(IllegalArgumentException("Username must be at least 2 alphanumeric characters."))
            }
            val existingDir = db.collection("managerDirectory").document(cleanUser).get().await()
            if (existingDir.exists()) {
                return Result.failure(IllegalStateException("Manager username '$cleanUser' already exists."))
            }
            val salt = SecurityCrypto.generateSalt()
            val hash = SecurityCrypto.hashPassword(salt, password)
            val mgrRef = db.collection("managers").document("mgr_$cleanUser")
            val batch = db.batch()
            batch.set(
                mgrRef,
                mapOf(
                    "name" to name.trim(),
                    "username" to cleanUser,
                    "mobile" to mobile.trim(),
                    "roleType" to "family_manager",
                    "passwordHash" to hash,
                    "passwordSalt" to salt,
                    "active" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                db.collection("managerDirectory").document(cleanUser),
                mapOf(
                    "accountId" to mgrRef.id,
                    "username" to cleanUser,
                    "salt" to salt,
                    "roleType" to "family_manager",
                    "active" to true
                )
            )
            batch.commit().await()
            recordAuditLog(
                managerId = actorManagerId,
                managerName = actorManagerName,
                actionPerformed = "MANAGER_ACCOUNT_CREATED",
                recordType = "managers",
                recordId = mgrRef.id,
                newValue = "${name.trim()} ($cleanUser)",
                modificationReason = "Owner added family manager"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "managers")
            Result.failure(e)
        }
    }
}
