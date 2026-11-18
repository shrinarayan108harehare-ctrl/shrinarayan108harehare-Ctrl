package com.example.viewmodel

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.OrderAndChatRepository
import com.example.data.PharmacyRepository
import com.example.localization.AppLanguage
import com.example.model.DeliveryPerson
import com.example.model.ManagerAccount
import com.example.model.RecipientNotification
import com.example.model.UserAccount
import com.example.model.UserRole
import com.example.util.LocalNotifier
import com.example.util.PreferencesStore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface RolePortalSelection {
    data object RolePicker : RolePortalSelection
    data object CustomerOnboarding : RolePortalSelection
    data object ManagerLogin : RolePortalSelection
    data object DeliveryLogin : RolePortalSelection
}

class AppSessionViewModel(
    private val appContext: Context,
    val pharmacyRepo: PharmacyRepository,
    val orderRepo: OrderAndChatRepository,
    private val prefs: PreferencesStore,
    private val currentUserId: String
) : ViewModel() {

    private val _portalSelection = MutableStateFlow<RolePortalSelection>(RolePortalSelection.RolePicker)
    val portalSelection: StateFlow<RolePortalSelection> = _portalSelection.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    val language: StateFlow<AppLanguage> = prefs.languageFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), AppLanguage.ENGLISH)

    val notificationsEnabled: StateFlow<Boolean> = prefs.notificationsEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), true)

    val userAccount: StateFlow<UserAccount?> = pharmacyRepo.observeUserAccount(currentUserId)
        .catch { error ->
            val ffe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
            if (ffe == null && error is CancellationException) throw error
            emit(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val myNotifications: StateFlow<List<RecipientNotification>> = orderRepo.observeMyNotifications(currentUserId)
        .catch { error ->
            val ffe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
            if (ffe == null && error is CancellationException) throw error
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _activeManager = MutableStateFlow<ManagerAccount?>(null)
    val activeManager: StateFlow<ManagerAccount?> = _activeManager.asStateFlow()

    private val _activeDelivery = MutableStateFlow<DeliveryPerson?>(null)
    val activeDelivery: StateFlow<DeliveryPerson?> = _activeDelivery.asStateFlow()

    private val notifiedIds = mutableSetOf<String>()
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    init {
        observeConnectivity()
        viewModelScope.launch {
            pharmacyRepo.ensureSeededAccountsIfFresh()
        }
        viewModelScope.launch {
            myNotifications.collect { list ->
                list.filter { it.status == "PENDING" || it.status == "FAILED" }
                    .forEach { n ->
                        if (notifiedIds.add(n.id)) {
                            LocalNotifier.showAnnouncementNotification(appContext, n.id, n.title, n.message)
                        }
                    }
            }
        }
        viewModelScope.launch {
            userAccount.collect { acc ->
                if (acc != null && acc.active) {
                    when (UserRole.fromRaw(acc.role)) {
                        UserRole.MANAGER, UserRole.OWNER -> {
                            if (acc.linkedManagerId.isNotBlank() && _activeManager.value == null) {
                                _activeManager.value = ManagerAccount(
                                    id = acc.linkedManagerId,
                                    name = acc.displayName,
                                    roleType = if (acc.role == "owner") "owner" else "family_manager",
                                    active = acc.active,
                                    linkedUid = currentUserId
                                )
                            }
                        }
                        UserRole.DELIVERY -> {
                            if (acc.linkedDeliveryPersonId.isNotBlank() && _activeDelivery.value == null) {
                                _activeDelivery.value = DeliveryPerson(
                                    id = acc.linkedDeliveryPersonId,
                                    name = acc.displayName,
                                    active = acc.active,
                                    linkedUid = currentUserId
                                )
                            }
                        }
                        UserRole.CUSTOMER -> Unit
                    }
                    pharmacyRepo.syncFcmToken()
                }
            }
        }
    }

    private fun observeConnectivity() {
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeCap = cm.getNetworkCapabilities(cm.activeNetwork)
        _isOnline.value = activeCap?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                _isOnline.value = true
            }
            override fun onLost(network: Network) {
                _isOnline.value = false
            }
        }
        networkCallback = cb
        runCatching { cm.registerDefaultNetworkCallback(cb) }
    }

    override fun onCleared() {
        super.onCleared()
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        networkCallback?.let { runCatching { cm.unregisterNetworkCallback(it) } }
    }

    fun selectPortal(selection: RolePortalSelection) {
        _authError.value = null
        _portalSelection.value = selection
    }

    fun toggleLanguage() {
        viewModelScope.launch {
            val next = if (language.value == AppLanguage.ENGLISH) AppLanguage.HINDI else AppLanguage.ENGLISH
            prefs.setLanguage(next)
            pharmacyRepo.updateLanguagePreference(next.code)
        }
    }

    fun clearError() {
        _authError.value = null
    }

    fun registerCustomer(
        name: String,
        phone: String,
        otpVerified: Boolean,
        addressLabel: String,
        houseNo: String,
        street: String,
        villageOrArea: String,
        landmark: String,
        lat: Double,
        lng: Double,
        accuracyMeters: Double
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            _authError.value = null
            val res = pharmacyRepo.registerCustomer(
                name = name,
                phone = phone,
                phoneVerified = otpVerified,
                addressLabel = addressLabel,
                houseNo = houseNo,
                street = street,
                villageOrArea = villageOrArea,
                landmark = landmark,
                lat = lat,
                lng = lng,
                accuracyMeters = accuracyMeters,
                language = language.value.code
            )
            _isBusy.value = false
            res.onFailure { _authError.value = it.localizedMessage ?: "Customer registration failed." }
        }
    }

    fun loginManager(username: String, password: String) {
        viewModelScope.launch {
            _isBusy.value = true
            _authError.value = null
            val res = pharmacyRepo.loginAsManager(username, password, language.value.code)
            _isBusy.value = false
            res.onSuccess { _activeManager.value = it }
                .onFailure { _authError.value = it.localizedMessage ?: "Invalid credentials." }
        }
    }

    fun loginDelivery(username: String, password: String) {
        viewModelScope.launch {
            _isBusy.value = true
            _authError.value = null
            val res = pharmacyRepo.loginAsDeliveryPerson(username, password, language.value.code)
            _isBusy.value = false
            res.onSuccess { _activeDelivery.value = it }
                .onFailure { _authError.value = it.localizedMessage ?: "Invalid delivery credentials." }
        }
    }

    fun markNotificationOpened(id: String) {
        viewModelScope.launch { orderRepo.markNotificationState(id, "OPENED") }
    }

    fun markNotificationRead(id: String) {
        viewModelScope.launch { orderRepo.markNotificationState(id, "READ") }
    }

    fun updateNotificationSetting(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setNotificationsEnabled(enabled)
            pharmacyRepo.saveNotificationSettings(enabled)
        }
    }

    companion object {
        fun provideFactory(appContext: Context, currentUserId: String): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    val repo = PharmacyRepository(appContext)
                    val orderRepo = OrderAndChatRepository(repo.db)
                    AppSessionViewModel(
                        appContext = appContext.applicationContext,
                        pharmacyRepo = repo,
                        orderRepo = orderRepo,
                        prefs = PreferencesStore(appContext.applicationContext),
                        currentUserId = currentUserId
                    )
                }
            }
    }
}
