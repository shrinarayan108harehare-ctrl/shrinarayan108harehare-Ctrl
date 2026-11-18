package com.example.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.AiImageResolution
import com.example.data.BiAnalyticsCalculator
import com.example.data.GeminiPharmacyService
import com.example.data.OrderAndChatRepository
import com.example.data.PharmacyRepository
import com.example.model.Advertisement
import com.example.model.Announcement
import com.example.model.AuditLog
import com.example.model.BiDashboardSnapshot
import com.example.model.BusinessExpense
import com.example.model.BusinessSetting
import com.example.model.ChatMessage
import com.example.model.Conversation
import com.example.model.CustomerProfile
import com.example.model.DateRangePreset
import com.example.model.DeliveryLocation
import com.example.model.DeliveryPerson
import com.example.model.ManagerAccount
import com.example.model.OrderFinancial
import com.example.model.OrderStatus
import com.example.model.PharmacyOrder
import com.example.model.RecipientNotification
import com.example.model.RoutePlan
import com.example.util.LocationUtils
import com.example.util.MediaUtils
import com.example.util.RouteOptimizationResult
import com.example.util.TimezoneUtils
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class ManagerViewModel(
    private val manager: ManagerAccount,
    private val pharmacyRepo: PharmacyRepository,
    private val orderRepo: OrderAndChatRepository,
    private val geminiService: GeminiPharmacyService
) : ViewModel() {

    val conversations: StateFlow<List<Conversation>> = orderRepo.observeAllConversations()
        .catch { if (it is CancellationException) throw it; emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val orders: StateFlow<List<PharmacyOrder>> = orderRepo.observeAllOrders()
        .catch { if (it is CancellationException) throw it; emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val financials: StateFlow<List<OrderFinancial>> = orderRepo.observeOrderFinancials()
        .catch { if (it is CancellationException) throw it; emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val customers: StateFlow<List<CustomerProfile>> = pharmacyRepo.observeAllCustomers()
        .catch { if (it is CancellationException) throw it; emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val managers: StateFlow<List<ManagerAccount>> = pharmacyRepo.observeManagers()
        .catch { if (it is CancellationException) throw it; emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val deliveryPersonnel: StateFlow<List<DeliveryPerson>> = pharmacyRepo.observeDeliveryPersonnel()
        .catch { if (it is CancellationException) throw it; emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val deliveryLocations: StateFlow<List<DeliveryLocation>> = orderRepo.observeDeliveryLocations()
        .catch { if (it is CancellationException) throw it; emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val routePlans: StateFlow<List<RoutePlan>> = orderRepo.observeRoutePlans()
        .catch { if (it is CancellationException) throw it; emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val advertisements: StateFlow<List<Advertisement>> = orderRepo.observeAdvertisements()
        .catch { if (it is CancellationException) throw it; emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val announcements: StateFlow<List<Announcement>> = orderRepo.observeAnnouncements()
        .catch { if (it is CancellationException) throw it; emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val allNotifications: StateFlow<List<RecipientNotification>> = orderRepo.observeAllNotifications()
        .catch { if (it is CancellationException) throw it; emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val expenses: StateFlow<List<BusinessExpense>> = orderRepo.observeExpenses()
        .catch { if (it is CancellationException) throw it; emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val auditLogs: StateFlow<List<AuditLog>> = orderRepo.observeAuditLogs()
        .catch { if (it is CancellationException) throw it; emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val shopSettings: StateFlow<BusinessSetting?> = pharmacyRepo.observeShopPublicSettings()
        .catch { if (it is CancellationException) throw it; emit(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    private val _selectedConversationId = MutableStateFlow<String?>(null)
    val selectedConversationId: StateFlow<String?> = _selectedConversationId.asStateFlow()

    val selectedMessages: StateFlow<List<ChatMessage>> = _selectedConversationId
        .flatMapLatest { cid ->
            if (cid.isNullOrBlank()) flowOf(emptyList())
            else orderRepo.observeMessages(cid).catch {
                val ffe = generateSequence(it) { c -> c.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                if (ffe == null && it is CancellationException) throw it
                emit(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    // BI Date Range State
    private val _datePreset = MutableStateFlow(DateRangePreset.TODAY)
    val datePreset: StateFlow<DateRangePreset> = _datePreset.asStateFlow()

    private val _customStartKey = MutableStateFlow(TimezoneUtils.todayDateKey())
    val customStartKey: StateFlow<String> = _customStartKey.asStateFlow()

    private val _customEndKey = MutableStateFlow(TimezoneUtils.todayDateKey())
    val customEndKey: StateFlow<String> = _customEndKey.asStateFlow()

    val biSnapshot: StateFlow<BiDashboardSnapshot> = combine(
        _datePreset,
        _customStartKey,
        _customEndKey,
        shopSettings,
        orders,
        financials,
        expenses,
        customers,
        deliveryPersonnel
    ) { args: Array<Any?> ->
        val preset = args[0] as DateRangePreset
        val start = args[1] as String
        val end = args[2] as String
        val settings = args[3] as BusinessSetting?
        @Suppress("UNCHECKED_CAST")
        val allOrders = args[4] as List<PharmacyOrder>
        @Suppress("UNCHECKED_CAST")
        val allFin = args[5] as List<OrderFinancial>
        @Suppress("UNCHECKED_CAST")
        val allExp = args[6] as List<BusinessExpense>
        @Suppress("UNCHECKED_CAST")
        val allCust = args[7] as List<CustomerProfile>
        @Suppress("UNCHECKED_CAST")
        val allDp = args[8] as List<DeliveryPerson>

        BiAnalyticsCalculator.compute(
            preset = preset,
            customStartKey = start,
            customEndKey = end,
            accountingMethod = settings?.accountingMethod ?: "ACCRUAL",
            orders = allOrders,
            financials = allFin,
            expenses = allExp,
            customers = allCust,
            deliveryPersonnel = allDp
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), BiDashboardSnapshot())

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    // AI Assistant States for Manager
    private val _rxExtractionResult = MutableStateFlow<String?>(null)
    val rxExtractionResult: StateFlow<String?> = _rxExtractionResult.asStateFlow()

    private val _aiReplyDraft = MutableStateFlow<String?>(null)
    val aiReplyDraft: StateFlow<String?> = _aiReplyDraft.asStateFlow()

    private val _aiBiInsights = MutableStateFlow<String?>(null)
    val aiBiInsights: StateFlow<String?> = _aiBiInsights.asStateFlow()

    private val _aiBannerBitmap = MutableStateFlow<Bitmap?>(null)
    val aiBannerBitmap: StateFlow<Bitmap?> = _aiBannerBitmap.asStateFlow()

    private val _aiBusy = MutableStateFlow(false)
    val aiBusy: StateFlow<Boolean> = _aiBusy.asStateFlow()

    private val _routeResult = MutableStateFlow<RouteOptimizationResult?>(null)
    val routeResult: StateFlow<RouteOptimizationResult?> = _routeResult.asStateFlow()

    fun clearFeedback() { _feedbackMessage.value = null }

    fun openConversation(conversationId: String?) {
        _selectedConversationId.value = conversationId
        _rxExtractionResult.value = null
        _aiReplyDraft.value = null
        if (!conversationId.isNullOrBlank()) {
            viewModelScope.launch { orderRepo.markConversationRead(conversationId, byManager = true) }
        }
    }

    fun sendManagerReply(
        conversation: Conversation,
        text: String,
        messageType: String = "text",
        mediaBase64: String = "",
        mediaUrl: String = "",
        mediaFileName: String = "",
        mediaMimeType: String = "",
        mediaDurationSec: Long = 0L
    ) {
        viewModelScope.launch {
            orderRepo.sendMessage(
                conversationId = conversation.customerId,
                customerName = conversation.customerName,
                customerPhone = conversation.customerPhone,
                customerArea = conversation.customerArea,
                senderRole = "manager",
                senderName = manager.name,
                messageType = messageType,
                text = text,
                mediaBase64 = mediaBase64,
                mediaUrl = mediaUrl,
                mediaFileName = mediaFileName,
                mediaMimeType = mediaMimeType,
                mediaDurationSec = mediaDurationSec
            ).onFailure { _feedbackMessage.value = "Send failed: ${it.localizedMessage}" }
        }
    }

    fun verifyPrescriptionMessage(conversationId: String, messageId: String) {
        viewModelScope.launch {
            orderRepo.verifyPrescriptionMessage(conversationId, messageId, manager.id, manager.name)
                .onSuccess { _feedbackMessage.value = "Prescription verified & logged." }
        }
    }

    fun verifyOrderPrescription(order: PharmacyOrder) {
        viewModelScope.launch {
            orderRepo.verifyOrderPrescription(order, manager.id, manager.name)
                .onSuccess { _feedbackMessage.value = "Order ${order.orderNumber} Rx verified." }
        }
    }

    fun createManualOrder(
        customer: CustomerProfile,
        itemsSummary: String,
        prescriptionRequired: Boolean
    ) {
        viewModelScope.launch {
            orderRepo.createOrder(
                customerId = customer.userId,
                customerName = customer.name,
                customerPhone = customer.phone,
                deliveryAddress = customer.defaultAddressText.ifBlank { "Counter / Phone Order" },
                deliveryArea = customer.defaultArea.ifBlank { "Local Area" },
                deliveryLandmark = customer.defaultLandmark,
                lat = customer.defaultLat,
                lng = customer.defaultLng,
                accuracyMeters = 15.0,
                itemsSummary = itemsSummary,
                prescriptionRequired = prescriptionRequired,
                idempotencyKey = UUID.randomUUID().toString(),
                createdByManagerId = manager.id,
                createdByManagerName = manager.name
            ).onSuccess { _feedbackMessage.value = "Manual order created for ${customer.name}." }
                .onFailure { _feedbackMessage.value = it.localizedMessage }
        }
    }

    fun saveOrderFinancials(
        order: PharmacyOrder,
        existing: OrderFinancial?,
        billValue: Double,
        discountAmount: Double,
        refundAmount: Double,
        costValue: Double,
        deliveryDistanceKm: Double,
        deliveryChargeCollected: Double,
        deliveryExpensePaid: Double,
        internalNotes: String,
        reason: String
    ) {
        viewModelScope.launch {
            orderRepo.saveOrderFinancials(
                order = order,
                existingFinancial = existing,
                billValue = billValue,
                discountAmount = discountAmount,
                refundAmount = refundAmount,
                costValue = costValue,
                deliveryDistanceKm = deliveryDistanceKm,
                deliveryChargeCollected = deliveryChargeCollected,
                deliveryExpensePaid = deliveryExpensePaid,
                internalNotes = internalNotes,
                accountingMethod = shopSettings.value?.accountingMethod ?: "ACCRUAL",
                managerId = manager.id,
                managerName = manager.name,
                reason = reason
            ).onSuccess { _feedbackMessage.value = "Confidential sale report saved & audited." }
                .onFailure { _feedbackMessage.value = "Save failed: ${it.localizedMessage}" }
        }
    }

    fun assignDelivery(order: PharmacyOrder, dp: DeliveryPerson, reason: String = "Assigned by manager") {
        viewModelScope.launch {
            orderRepo.assignDeliveryPerson(order, dp, manager.id, manager.name, reason)
                .onSuccess { _feedbackMessage.value = "${order.orderNumber} assigned to ${dp.name}." }
                .onFailure { _feedbackMessage.value = it.localizedMessage }
        }
    }

    fun updateOrderStatus(
        order: PharmacyOrder,
        newStatus: OrderStatus,
        deliveredBy: DeliveryPerson? = null,
        cancelReason: String = "",
        reason: String = "Status updated by manager"
    ) {
        viewModelScope.launch {
            orderRepo.updateOrderStatusByManager(
                order = order,
                newStatus = newStatus,
                managerId = manager.id,
                managerName = manager.name,
                deliveredByDp = deliveredBy,
                cancellationReason = cancelReason,
                reason = reason
            ).onSuccess { _feedbackMessage.value = "${order.orderNumber} marked ${newStatus.labelEn}." }
                .onFailure { _feedbackMessage.value = it.localizedMessage }
        }
    }

    fun optimizeRoute(selectedOrders: List<PharmacyOrder>) {
        val originLat = shopSettings.value?.shopLat ?: 26.8467
        val originLng = shopSettings.value?.shopLng ?: 80.9462
        viewModelScope.launch {
            _aiBusy.value = true
            _routeResult.value = LocationUtils.optimizeMultiStopRoute(originLat, originLng, selectedOrders)
            _aiBusy.value = false
        }
    }

    fun reorderRouteStops(newOrder: List<PharmacyOrder>) {
        val cur = _routeResult.value ?: return
        val originLat = shopSettings.value?.shopLat ?: 26.8467
        val originLng = shopSettings.value?.shopLng ?: 80.9462
        var prevLat = originLat
        var prevLng = originLng
        var dist = 0.0
        newOrder.forEach { stop ->
            dist += LocationUtils.haversineKm(prevLat, prevLng, stop.deliveryLat, stop.deliveryLng) * 1.28
            prevLat = stop.deliveryLat
            prevLng = stop.deliveryLng
        }
        _routeResult.value = cur.copy(
            orderedStops = newOrder,
            totalDistanceKm = dist,
            estimatedMinutes = ((dist / 24.0) * 60).toLong() + newOrder.size * 4L
        )
    }

    fun saveAndShareRoute(name: String, dp: DeliveryPerson?) {
        val res = _routeResult.value ?: return
        viewModelScope.launch {
            orderRepo.saveRoutePlan(
                name = name,
                dp = dp,
                stops = res.orderedStops,
                totalDistanceKm = res.totalDistanceKm,
                estimatedMinutes = res.estimatedMinutes,
                optimized = true,
                managerId = manager.id,
                managerName = manager.name
            ).onSuccess { _feedbackMessage.value = "Route plan saved & shared with ${dp?.name ?: "team"}." }
                .onFailure { _feedbackMessage.value = it.localizedMessage }
        }
    }

    fun sendAnnouncement(title: String, message: String, category: String, audience: String, selectedUids: List<String>) {
        viewModelScope.launch {
            orderRepo.sendAnnouncement(title, message, category, audience, selectedUids, manager.id, manager.name)
                .onSuccess { count -> _feedbackMessage.value = "Announcement saved & queued for $count recipients." }
                .onFailure { _feedbackMessage.value = it.localizedMessage }
        }
    }

    fun saveAd(
        adId: String?,
        title: String,
        subtitle: String,
        category: String,
        imagePreset: String,
        imageBase64: String,
        linkUrl: String,
        active: Boolean,
        priority: Long
    ) {
        viewModelScope.launch {
            orderRepo.saveAdvertisement(adId, title, subtitle, category, imagePreset, imageBase64, linkUrl, active, priority, manager.id, manager.name)
                .onSuccess {
                    _aiBannerBitmap.value = null
                    _feedbackMessage.value = "Advertisement saved."
                }
                .onFailure { _feedbackMessage.value = it.localizedMessage }
        }
    }

    fun deleteAd(ad: Advertisement) {
        viewModelScope.launch {
            orderRepo.deleteAdvertisement(ad.id, ad.title, manager.id, manager.name)
                .onSuccess { _feedbackMessage.value = "Advertisement deleted." }
        }
    }

    fun addExpense(title: String, category: String, amount: Double, dateKey: String, notes: String) {
        viewModelScope.launch {
            orderRepo.addBusinessExpense(title, category, amount, dateKey, notes, manager.id, manager.name)
                .onSuccess { _feedbackMessage.value = "Expense recorded." }
                .onFailure { _feedbackMessage.value = it.localizedMessage }
        }
    }

    fun saveSettings(setting: BusinessSetting) {
        viewModelScope.launch {
            orderRepo.saveBusinessSettings(setting, manager.id, manager.name)
                .onSuccess { _feedbackMessage.value = "Shop settings updated." }
                .onFailure { _feedbackMessage.value = it.localizedMessage }
        }
    }

    fun upsertDeliveryPerson(existing: DeliveryPerson?, name: String, mobile: String, username: String, password: String?, active: Boolean) {
        viewModelScope.launch {
            orderRepo.upsertDeliveryPerson(existing, name, mobile, username, password, active, manager.id, manager.name)
                .onSuccess { _feedbackMessage.value = "Delivery account saved." }
                .onFailure { _feedbackMessage.value = it.localizedMessage }
        }
    }

    fun updateManagerByOwner(target: ManagerAccount, newName: String, newMobile: String, newPassword: String?, active: Boolean) {
        viewModelScope.launch {
            orderRepo.updateManagerAccountByOwner(target, newName, newMobile, newPassword, active, manager.id, manager.name)
                .onSuccess { _feedbackMessage.value = "Manager account updated." }
                .onFailure { _feedbackMessage.value = it.localizedMessage }
        }
    }

    fun createManagerByOwner(name: String, mobile: String, username: String, password: String) {
        viewModelScope.launch {
            orderRepo.createManagerAccountByOwner(name, mobile, username, password, manager.id, manager.name)
                .onSuccess { _feedbackMessage.value = "Family manager account created." }
                .onFailure { _feedbackMessage.value = it.localizedMessage }
        }
    }

    fun showFeedback(msg: String) {
        _feedbackMessage.value = msg
    }

    fun changeOwnPassword(newPassword: String) {
        viewModelScope.launch {
            orderRepo.changeOwnManagerPassword(manager, newPassword)
                .onSuccess { _feedbackMessage.value = "Your manager password has been updated." }
                .onFailure { _feedbackMessage.value = it.localizedMessage }
        }
    }

    fun setDatePreset(preset: DateRangePreset) { _datePreset.value = preset }
    fun setCustomDates(start: String, end: String) {
        _customStartKey.value = start
        _customEndKey.value = end
        _datePreset.value = DateRangePreset.CUSTOM
    }

    // Gemini AI Copilot Methods
    fun extractPrescription(bitmap: Bitmap) {
        viewModelScope.launch {
            _aiBusy.value = true
            _rxExtractionResult.value = null
            geminiService.extractPrescriptionDraft(bitmap)
                .onSuccess { _rxExtractionResult.value = it }
                .onFailure { _feedbackMessage.value = "Rx AI Error: ${it.localizedMessage}" }
            _aiBusy.value = false
        }
    }

    fun draftReply(customerText: String, targetLang: String) {
        viewModelScope.launch {
            _aiBusy.value = true
            geminiService.translateOrSuggestReply(customerText, targetLang)
                .onSuccess { _aiReplyDraft.value = it }
                .onFailure { _feedbackMessage.value = "Reply AI Error: ${it.localizedMessage}" }
            _aiBusy.value = false
        }
    }

    fun runDeepBiAnalysis() {
        val snap = biSnapshot.value
        val o = snap.overview
        val prompt = """
            Period: ${snap.dateRangeLabel} (${snap.accountingMethod})
            Gross Sales: ₹${o.totalGrossSales}, Discounts: ₹${o.totalDiscounts}, Refunds: ₹${o.totalRefunds}
            Net Product Sales: ₹${o.totalNetSales}, Cost Value: ₹${o.totalCostValue}, Product Gross Profit: ₹${o.totalProductGrossProfit}
            Delivery Charges Collected: ₹${o.totalDeliveryChargesCollected}, Delivery Expenses: ₹${o.totalDeliveryExpensesPaid}, Other Expenses: ₹${o.totalOtherBusinessExpenses}
            Net Business Profit: ₹${o.totalNetBusinessProfit}
            Completed Orders: ${o.completedOrderCount}, Cancelled: ${o.cancelledOrderCount}, AOV: ₹${o.averageOrderValue}
            Top Areas: ${snap.areaStats.take(5).joinToString { "${it.areaName}(${it.orderCount} orders, ₹${it.totalNetProfit} profit)" }}
            Delivery Staff: ${snap.deliveryStats.joinToString { "${it.deliveryPersonName}(${it.completedDeliveries} done, ${"%.1f".format(it.totalDistanceKm)} km)" }}
        """.trimIndent()
        viewModelScope.launch {
            _aiBusy.value = true
            geminiService.analyzeBusinessIntelligence(prompt)
                .onSuccess { _aiBiInsights.value = it }
                .onFailure { _feedbackMessage.value = "BI AI Error: ${it.localizedMessage}" }
            _aiBusy.value = false
        }
    }

    fun generateAiPromoBanner(prompt: String, resolution: AiImageResolution) {
        viewModelScope.launch {
            _aiBusy.value = true
            geminiService.generatePromoBanner(prompt, resolution)
                .onSuccess { _aiBannerBitmap.value = it }
                .onFailure { _feedbackMessage.value = "Banner Gen Error: ${it.localizedMessage}" }
            _aiBusy.value = false
        }
    }

    fun encodeBannerBitmap(bitmap: Bitmap): String = MediaUtils.compressBitmapToBase64(bitmap, maxDimension = 1024)

    fun exportBackupJson(onReady: (String) -> Unit) {
        viewModelScope.launch {
            pharmacyRepo.buildBusinessBackupJson()
                .onSuccess {
                    orderRepo.recordAuditLog(manager.id, manager.name, "BACKUP_EXPORTED", "business", "full_backup", modificationReason = "Manual backup snapshot")
                    onReady(it)
                }
                .onFailure { _feedbackMessage.value = it.localizedMessage }
        }
    }

    fun logReportExport(format: String) {
        viewModelScope.launch {
            orderRepo.recordAuditLog(
                managerId = manager.id,
                managerName = manager.name,
                actionPerformed = "FINANCIAL_REPORT_EXPORTED",
                recordType = "biReport",
                recordId = biSnapshot.value.dateRangeLabel,
                newValue = format,
                modificationReason = "Exported BI report"
            )
        }
    }

    companion object {
        fun provideFactory(
            manager: ManagerAccount,
            pharmacyRepo: PharmacyRepository,
            orderRepo: OrderAndChatRepository
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ManagerViewModel(
                    manager = manager,
                    pharmacyRepo = pharmacyRepo,
                    orderRepo = orderRepo,
                    geminiService = GeminiPharmacyService()
                )
            }
        }
    }
}
