package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.AiChatTurn
import com.example.data.GeminiPharmacyService
import com.example.data.OrderAndChatRepository
import com.example.data.PharmacyRepository
import com.example.model.Advertisement
import com.example.model.BusinessSetting
import com.example.model.ChatMessage
import com.example.model.Conversation
import com.example.model.CustomerAddress
import com.example.model.CustomerProfile
import com.example.model.PharmacyOrder
import com.example.model.UiState
import com.example.util.PreferencesStore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class PendingLocalMessage(
    val localId: String,
    val text: String,
    val messageType: String,
    val failed: Boolean = false,
    val error: String? = null,
    val mediaBase64: String = "",
    val mediaUrl: String = "",
    val mediaFileName: String = "",
    val mediaMimeType: String = "",
    val mediaDurationSec: Long = 0L
)

class CustomerViewModel(
    private val customerId: String,
    private val pharmacyRepo: PharmacyRepository,
    private val orderRepo: OrderAndChatRepository,
    private val geminiService: GeminiPharmacyService,
    private val prefs: PreferencesStore
) : ViewModel() {

    val profile: StateFlow<CustomerProfile?> = pharmacyRepo.observeCustomerProfile(customerId)
        .catch { if (it is CancellationException) throw it; emit(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val addresses: StateFlow<List<CustomerAddress>> = pharmacyRepo.observeCustomerAddresses(customerId)
        .catch { if (it is CancellationException) throw it; emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val conversation: StateFlow<Conversation?> = orderRepo.observeCustomerConversation(customerId)
        .catch { if (it is CancellationException) throw it; emit(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val messagesState: StateFlow<UiState<List<ChatMessage>>> = orderRepo.observeMessages(customerId)
        .map<List<ChatMessage>, UiState<List<ChatMessage>>> { UiState.Success(it) }
        .catch { error ->
            val ffe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
            if (ffe == null && error is CancellationException) throw error
            emit(UiState.Error((ffe ?: error).message ?: "Failed to load chat"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val ordersState: StateFlow<UiState<List<PharmacyOrder>>> = orderRepo.observeCustomerOrders(customerId)
        .map<List<PharmacyOrder>, UiState<List<PharmacyOrder>>> { UiState.Success(it) }
        .catch { error ->
            val ffe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
            if (ffe == null && error is CancellationException) throw error
            emit(UiState.Error((ffe ?: error).message ?: "Failed to load orders"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val ads: StateFlow<List<Advertisement>> = orderRepo.observePublicActiveAdvertisements()
        .catch { if (it is CancellationException) throw it; emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val shopSettings: StateFlow<BusinessSetting?> = pharmacyRepo.observeShopPublicSettings()
        .catch { if (it is CancellationException) throw it; emit(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    private val _pendingMessages = MutableStateFlow<List<PendingLocalMessage>>(emptyList())
    val pendingMessages: StateFlow<List<PendingLocalMessage>> = _pendingMessages.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    // Multi-turn Gemini Pharmacist Chatbot state
    private val _aiTurns = MutableStateFlow<List<AiChatTurn>>(emptyList())
    val aiTurns: StateFlow<List<AiChatTurn>> = _aiTurns.asStateFlow()

    private val _aiStreamingText = MutableStateFlow<String?>(null)
    val aiStreamingText: StateFlow<String?> = _aiStreamingText.asStateFlow()

    private val _aiError = MutableStateFlow<String?>(null)
    val aiError: StateFlow<String?> = _aiError.asStateFlow()

    val draftText = prefs.draftChatFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), "")

    fun saveDraft(text: String) {
        viewModelScope.launch { prefs.saveDraftChat(text) }
    }

    fun clearBanner() {
        _statusBanner.value = null
    }

    fun markInboxRead() {
        viewModelScope.launch { orderRepo.markConversationRead(customerId, byManager = false) }
    }

    fun sendChatMessage(
        text: String,
        messageType: String = "text",
        mediaBase64: String = "",
        mediaUrl: String = "",
        mediaFileName: String = "",
        mediaMimeType: String = "",
        mediaDurationSec: Long = 0L
    ) {
        val p = profile.value ?: CustomerProfile(userId = customerId, name = "Customer")
        val pendingId = UUID.randomUUID().toString()
        val pending = PendingLocalMessage(
            localId = pendingId,
            text = text,
            messageType = messageType,
            mediaBase64 = mediaBase64,
            mediaUrl = mediaUrl,
            mediaFileName = mediaFileName,
            mediaMimeType = mediaMimeType,
            mediaDurationSec = mediaDurationSec
        )
        _pendingMessages.value = _pendingMessages.value + pending

        viewModelScope.launch {
            val res = orderRepo.sendMessage(
                conversationId = customerId,
                customerName = p.name.ifBlank { "Customer" },
                customerPhone = p.phone,
                customerArea = p.defaultArea,
                senderRole = "customer",
                senderName = p.name.ifBlank { "Customer" },
                messageType = messageType,
                text = text,
                mediaBase64 = mediaBase64,
                mediaUrl = mediaUrl,
                mediaFileName = mediaFileName,
                mediaMimeType = mediaMimeType,
                mediaDurationSec = mediaDurationSec
            )
            res.onSuccess {
                _pendingMessages.value = _pendingMessages.value.filterNot { it.localId == pendingId }
                prefs.saveDraftChat("")
            }.onFailure { err ->
                _pendingMessages.value = _pendingMessages.value.map {
                    if (it.localId == pendingId) it.copy(failed = true, error = err.localizedMessage) else it
                }
            }
        }
    }

    fun retryPendingMessage(localId: String) {
        val target = _pendingMessages.value.firstOrNull { it.localId == localId } ?: return
        _pendingMessages.value = _pendingMessages.value.filterNot { it.localId == localId }
        sendChatMessage(
            text = target.text,
            messageType = target.messageType,
            mediaBase64 = target.mediaBase64,
            mediaUrl = target.mediaUrl,
            mediaFileName = target.mediaFileName,
            mediaMimeType = target.mediaMimeType,
            mediaDurationSec = target.mediaDurationSec
        )
    }

    fun placeOrder(
        address: CustomerAddress,
        itemsSummary: String,
        prescriptionRequired: Boolean,
        idempotencyKey: String
    ) {
        val p = profile.value ?: return
        viewModelScope.launch {
            val res = orderRepo.createOrder(
                customerId = customerId,
                customerName = p.name,
                customerPhone = p.phone,
                deliveryAddress = address.fullAddress,
                deliveryArea = address.villageOrArea,
                deliveryLandmark = address.landmark,
                lat = address.lat,
                lng = address.lng,
                accuracyMeters = address.accuracyMeters,
                itemsSummary = itemsSummary,
                prescriptionRequired = prescriptionRequired,
                idempotencyKey = idempotencyKey
            )
            res.onSuccess {
                _statusBanner.value = "Order confirmed & sent to Prayag Deen & Sons!"
                sendChatMessage(
                    text = "Order confirmed for: $itemsSummary\nAddress: ${address.fullAddress}",
                    messageType = "order_update"
                )
            }.onFailure {
                _statusBanner.value = "Order failed: ${it.localizedMessage}"
            }
        }
    }

    fun saveAddress(address: CustomerAddress, setAsDefault: Boolean) {
        viewModelScope.launch {
            pharmacyRepo.saveCustomerAddress(address, setAsDefault)
                .onSuccess { _statusBanner.value = "Delivery address saved." }
                .onFailure { _statusBanner.value = "Failed to save address: ${it.localizedMessage}" }
        }
    }

    fun setDefaultAddress(address: CustomerAddress) {
        viewModelScope.launch {
            pharmacyRepo.setDefaultCustomerAddress(address)
                .onSuccess { _statusBanner.value = "Default delivery address updated." }
                .onFailure { _statusBanner.value = "Update failed: ${it.localizedMessage}" }
        }
    }

    fun deleteAddress(addressId: String) {
        viewModelScope.launch {
            pharmacyRepo.deleteCustomerAddress(addressId)
                .onSuccess { _statusBanner.value = "Address removed." }
                .onFailure { _statusBanner.value = "Delete failed: ${it.localizedMessage}" }
        }
    }

    fun updateProfile(name: String, phone: String) {
        viewModelScope.launch {
            pharmacyRepo.updateCustomerProfile(name, phone)
                .onSuccess { _statusBanner.value = "Profile updated." }
                .onFailure { _statusBanner.value = "Update failed: ${it.localizedMessage}" }
        }
    }

    fun showStatusMessage(msg: String) {
        _statusBanner.value = msg
    }

    fun toggleLiveLocation(orderId: String, enabled: Boolean, lat: Double, lng: Double, accuracy: Double) {
        viewModelScope.launch {
            orderRepo.toggleLiveLocationForOrder(orderId, enabled, lat, lng, accuracy)
                .onSuccess {
                    _statusBanner.value = if (enabled) "Live location shared for active delivery." else "Live location sharing stopped."
                }
        }
    }

    fun submitRating(orderId: String, rating: Long, feedback: String) {
        viewModelScope.launch {
            orderRepo.submitCustomerRating(orderId, rating, feedback)
                .onSuccess { _statusBanner.value = "Thank you for rating your delivery!" }
        }
    }

    fun replyToAnnouncementInChat(title: String, replyText: String) {
        sendChatMessage(
            text = "Re: [$title] — $replyText",
            messageType = "announcement_reply"
        )
    }

    fun askPharmacistAi(prompt: String, useHighThinking: Boolean) {
        if (prompt.isBlank()) return
        val previousTurns = _aiTurns.value
        _aiTurns.value = previousTurns + AiChatTurn(isUser = true, text = prompt, usedHighThinking = useHighThinking)
        _aiStreamingText.value = ""
        _aiError.value = null

        viewModelScope.launch {
            try {
                var finalAnswer = ""
                geminiService.streamPharmacistChat(previousTurns, prompt, useHighThinking).collect { partial ->
                    finalAnswer = partial
                    _aiStreamingText.value = partial
                }
                _aiStreamingText.value = null
                _aiTurns.value = _aiTurns.value + AiChatTurn(
                    isUser = false,
                    text = finalAnswer,
                    usedHighThinking = useHighThinking,
                    groundingNote = if (useHighThinking) "Deep Reasoning (gemini-4-carbon)" else "Search & Maps Grounded (gemini-4-hydrogen)"
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _aiStreamingText.value = null
                _aiError.value = e.localizedMessage ?: "AI Assistant request failed."
            }
        }
    }

    companion object {
        fun provideFactory(
            customerId: String,
            pharmacyRepo: PharmacyRepository,
            orderRepo: OrderAndChatRepository,
            prefs: PreferencesStore
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                CustomerViewModel(
                    customerId = customerId,
                    pharmacyRepo = pharmacyRepo,
                    orderRepo = orderRepo,
                    geminiService = GeminiPharmacyService(),
                    prefs = prefs
                )
            }
        }
    }
}
