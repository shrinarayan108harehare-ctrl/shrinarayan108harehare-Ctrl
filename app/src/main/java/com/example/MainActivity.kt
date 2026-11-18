package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.firebase.attemptAutoSignIn
import com.example.firebase.authStateFlow
import com.example.firebase.configureAppCheck
import com.example.firebase.signOutUser
import com.example.localization.AppLanguage
import com.example.localization.stringsFor
import com.example.model.UserRole
import com.example.ui.auth.GoogleAuthGateScreen
import com.example.ui.auth.RoleSelectionAndPortalHost
import com.example.ui.customer.CustomerDashboardScreen
import com.example.ui.delivery.DeliveryDashboardScreen
import com.example.ui.manager.ManagerDashboardScreen
import com.example.ui.theme.PrayagDeenTheme
import com.example.util.LocalNotifier
import com.example.util.PreferencesStore
import com.example.data.OrderAndChatRepository
import com.example.data.PharmacyRepository
import com.example.viewmodel.AppSessionViewModel
import com.example.viewmodel.CustomerViewModel
import com.example.viewmodel.DeliveryViewModel
import com.example.viewmodel.ManagerViewModel
import com.example.viewmodel.RolePortalSelection
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Must configure Firebase App Check first before any Firebase AI Logic or Firestore calls
        configureAppCheck(this, intent)
        LocalNotifier.ensureChannels(this)
        enableEdgeToEdge()

        setContent {
            PrayagDeenTheme {
                PrayagDeenAppRoot()
            }
        }
    }
}

@Composable
private fun PrayagDeenAppRoot() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    val prefs = remember { PreferencesStore(context.applicationContext) }
    val language by prefs.languageFlow.collectAsStateWithLifecycle(initialValue = AppLanguage.ENGLISH)
    val strings = stringsFor(language)

    val currentUser by Firebase.auth.authStateFlow().collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)
    var checkingAutoSignIn by remember { mutableStateOf(Firebase.auth.currentUser == null) }
    var pendingPortal by remember { mutableStateOf<RolePortalSelection?>(null) }

    val notifPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* handled gracefully; in-app Announcement Inbox remains primary source of truth */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (Firebase.auth.currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = { checkingAutoSignIn = false },
                onUnauthenticated = { checkingAutoSignIn = false },
                scope = scope
            )
        } else {
            checkingAutoSignIn = false
        }
    }

    if (checkingAutoSignIn) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val user = currentUser
    if (user == null) {
        val publicRepo = remember { PharmacyRepository(context.applicationContext) }
        val publicOrderRepo = remember(publicRepo) { OrderAndChatRepository(publicRepo.db) }
        val publicAds by remember(publicOrderRepo) {
            publicOrderRepo.observePublicActiveAdvertisements().catch { emit(emptyList()) }
        }.collectAsStateWithLifecycle(initialValue = emptyList())
        val publicShop by remember(publicRepo) {
            publicRepo.observeShopPublicSettings().catch { emit(null) }
        }.collectAsStateWithLifecycle(initialValue = null)

        GoogleAuthGateScreen(
            strings = strings,
            language = language,
            publicAds = publicAds,
            shopSettings = publicShop,
            onToggleLanguage = {
                scope.launch {
                    val next = if (language == AppLanguage.ENGLISH) AppLanguage.HINDI else AppLanguage.ENGLISH
                    prefs.setLanguage(next)
                }
            },
            onAuthSuccess = { chosenPortal ->
                pendingPortal = chosenPortal
                checkingAutoSignIn = false
            }
        )
        return
    }

    // Scope all repositories and ViewModels cleanly to authenticated Firebase UID
    key(user.uid) {
        val sessionVm: AppSessionViewModel = viewModel(
            key = "session_${user.uid}",
            factory = AppSessionViewModel.provideFactory(context.applicationContext, user.uid)
        )
        LaunchedEffect(pendingPortal) {
            pendingPortal?.let {
                sessionVm.selectPortal(it)
                pendingPortal = null
            }
        }
        val userAccount by sessionVm.userAccount.collectAsStateWithLifecycle()
        val portalSelection by sessionVm.portalSelection.collectAsStateWithLifecycle()
        val isBusy by sessionVm.isBusy.collectAsStateWithLifecycle()
        val authError by sessionVm.authError.collectAsStateWithLifecycle()
        val activeManager by sessionVm.activeManager.collectAsStateWithLifecycle()
        val activeDelivery by sessionVm.activeDelivery.collectAsStateWithLifecycle()
        val isOnline by sessionVm.isOnline.collectAsStateWithLifecycle()
        val myNotifications by sessionVm.myNotifications.collectAsStateWithLifecycle()
        val notificationsEnabled by sessionVm.notificationsEnabled.collectAsStateWithLifecycle()
        val sessionLang by sessionVm.language.collectAsStateWithLifecycle()
        val sessionStrings = stringsFor(sessionLang)

        val handleSignOut: () -> Unit = {
            signOutUser(credentialManager, scope)
        }

        val acc = userAccount
        when {
            activeManager != null && (acc == null || UserRole.fromRaw(acc.role) == UserRole.MANAGER || UserRole.fromRaw(acc.role) == UserRole.OWNER) -> {
                val mgr = activeManager!!
                val managerVm: ManagerViewModel = viewModel(
                    key = "mgr_${user.uid}_${mgr.id}",
                    factory = ManagerViewModel.provideFactory(mgr, sessionVm.pharmacyRepo, sessionVm.orderRepo)
                )
                ManagerDashboardScreen(
                    vm = managerVm,
                    manager = mgr,
                    strings = sessionStrings,
                    language = sessionLang,
                    isOnline = isOnline,
                    notifications = myNotifications,
                    notificationsEnabled = notificationsEnabled,
                    onToggleNotifications = { sessionVm.updateNotificationSetting(it) },
                    onToggleLanguage = { sessionVm.toggleLanguage() },
                    onMarkNotificationOpened = { sessionVm.markNotificationOpened(it) },
                    onMarkNotificationRead = { sessionVm.markNotificationRead(it) },
                    onSignOut = handleSignOut
                )
            }

            activeDelivery != null && (acc == null || UserRole.fromRaw(acc.role) == UserRole.DELIVERY) -> {
                val dp = activeDelivery!!
                val deliveryVm: DeliveryViewModel = viewModel(
                    key = "dp_${user.uid}_${dp.id}",
                    factory = DeliveryViewModel.provideFactory(context.applicationContext, dp, user.uid, sessionVm.orderRepo)
                )
                DeliveryDashboardScreen(
                    vm = deliveryVm,
                    deliveryPerson = dp,
                    strings = sessionStrings,
                    language = sessionLang,
                    isOnline = isOnline,
                    notifications = myNotifications,
                    notificationsEnabled = notificationsEnabled,
                    onToggleNotifications = { sessionVm.updateNotificationSetting(it) },
                    onToggleLanguage = { sessionVm.toggleLanguage() },
                    onMarkNotificationOpened = { sessionVm.markNotificationOpened(it) },
                    onMarkNotificationRead = { sessionVm.markNotificationRead(it) },
                    onSignOut = handleSignOut
                )
            }

            acc != null && acc.active && UserRole.fromRaw(acc.role) == UserRole.CUSTOMER -> {
                val customerVm: CustomerViewModel = viewModel(
                    key = "cust_${user.uid}",
                    factory = CustomerViewModel.provideFactory(user.uid, sessionVm.pharmacyRepo, sessionVm.orderRepo, prefs)
                )
                CustomerDashboardScreen(
                    vm = customerVm,
                    strings = sessionStrings,
                    language = sessionLang,
                    isOnline = isOnline,
                    notifications = myNotifications,
                    notificationsEnabled = notificationsEnabled,
                    onToggleNotifications = { sessionVm.updateNotificationSetting(it) },
                    onToggleLanguage = { sessionVm.toggleLanguage() },
                    onMarkNotificationOpened = { sessionVm.markNotificationOpened(it) },
                    onMarkNotificationRead = { sessionVm.markNotificationRead(it) },
                    onSignOut = handleSignOut
                )
            }

            else -> {
                RoleSelectionAndPortalHost(
                    strings = sessionStrings,
                    language = sessionLang,
                    portal = portalSelection,
                    isBusy = isBusy,
                    errorText = authError,
                    onToggleLanguage = { sessionVm.toggleLanguage() },
                    onSelectPortal = { sessionVm.selectPortal(it) },
                    onRegisterCustomer = { name, phone, otpVerified, label, house, street, area, landmark, lat, lng, accMeters ->
                        sessionVm.registerCustomer(name, phone, otpVerified, label, house, street, area, landmark, lat, lng, accMeters)
                    },
                    onLoginManager = { u, p -> sessionVm.loginManager(u, p) },
                    onLoginDelivery = { u, p -> sessionVm.loginDelivery(u, p) },
                    onSignOut = handleSignOut
                )
            }
        }
    }
}
