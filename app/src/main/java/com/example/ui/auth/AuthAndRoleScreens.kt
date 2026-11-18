package com.example.ui.auth

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import android.app.Activity
import com.example.R
import com.example.firebase.onGoogleSignInClicked
import com.example.firebase.startPhoneNumberVerification
import com.example.firebase.verifyAndUpdatePhoneCredential
import com.example.firebase.verifyPhoneOtpCode
import com.example.localization.AppLanguage
import com.example.localization.AppStrings
import com.example.ui.components.LanguageToggleChip
import com.example.ui.components.MapPinData
import com.example.ui.components.PharmacyInteractiveMap
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.EmeraldSecondary
import com.example.ui.theme.StatusWarning
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextOverflow
import com.example.model.Advertisement
import com.example.model.BusinessSetting
import com.example.ui.theme.SaffronAccent
import com.example.util.LocationUtils
import com.example.util.MediaUtils
import com.example.viewmodel.RolePortalSelection
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoogleAuthGateScreen(
    strings: AppStrings,
    language: AppLanguage,
    publicAds: List<Advertisement> = emptyList(),
    shopSettings: BusinessSetting? = null,
    onToggleLanguage: () -> Unit,
    onAuthSuccess: (RolePortalSelection?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val isHindi = language == AppLanguage.HINDI

    fun triggerGoogleSignIn(targetPortal: RolePortalSelection? = null) {
        isLoading = true
        errorMsg = null
        onGoogleSignInClicked(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {
                isLoading = false
                onAuthSuccess(targetPortal)
            },
            onAuthError = { msg ->
                isLoading = false
                errorMsg = msg
            },
            scope = scope,
            onAuthCancelled = { isLoading = false }
        )
    }

    val displayAds = if (publicAds.isNotEmpty()) publicAds else listOf(
        Advertisement(
            id = "public_default_1",
            title = if (isHindi) "प्रयाग दीन एंड संस मेडिकल — तेज़ होम डिलीवरी" else "Prayag Deen & Sons — Fast Doorstep Delivery",
            subtitle = if (isHindi) "साइन-इन करें और अपना पर्चा WhatsApp चैट पर भेजकर घर बैठे दवा पाएं।" else "Sign in to send prescriptions on Pharmacy Chat for verified local delivery.",
            category = "OFFER",
            imagePreset = "hero_pharmacy"
        ),
        Advertisement(
            id = "public_default_2",
            title = if (isHindi) "मौसमी व दैनिक स्वास्थ्य देखभाल ऑफर" else "Seasonal & Daily Wellness Offers",
            subtitle = if (isHindi) "विटामिन, शुगर, बीपी और दैनिक दवाइयों पर विशेष बचत।" else "Savings on chronic care, vitamins & family medical essentials.",
            category = "FESTIVAL",
            imagePreset = "promo_festival"
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(strings.appTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(if (isHindi) "सार्वजनिक स्टोरफ्रंट व पोर्टल" else "Public Pharmacy Storefront", style = MaterialTheme.typography.labelSmall, color = EmeraldPrimary)
                    }
                },
                actions = {
                    LanguageToggleChip(language = language, onToggle = onToggleLanguage)
                    Spacer(Modifier.width(4.dp))
                    TextButton(
                        onClick = { triggerGoogleSignIn(null) },
                        enabled = !isLoading
                    ) {
                        Text(if (isHindi) "साइन इन" else "Sign In", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Hero Storefront Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .clip(RoundedCornerShape(22.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_hero_pharmacy),
                    contentDescription = "Prayag Deen & Sons Pharmacy",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(listOf(Color.Transparent, EmeraldPrimary.copy(alpha = 0.9f))))
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Text(strings.appTitle, style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)
                    Text(strings.appSubtitle, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.92f))
                }
            }

            // Public Store Info Card (Hours, Address, Phone, Map Pin)
            val shopHours = shopSettings?.hours?.ifBlank { "8:00 AM - 10:30 PM" } ?: "8:00 AM - 10:30 PM"
            val shopAddress = shopSettings?.address?.ifBlank { "Prayag Deen & Sons Medical Store" } ?: "Prayag Deen & Sons Medical Store"
            val shopPhone = shopSettings?.phone ?: ""
            val shopLat = shopSettings?.shopLat ?: 26.8467
            val shopLng = shopSettings?.shopLng ?: 80.9462

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Schedule, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (isHindi) "खुले रहने का समय: $shopHours" else "Open Hours: $shopHours",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(shopAddress, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        if (shopPhone.isNotBlank()) {
                            OutlinedButton(
                                onClick = {
                                    runCatching {
                                        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$shopPhone")))
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(shopPhone, maxLines = 1)
                            }
                        }
                        FilledTonalButton(
                            onClick = { LocationUtils.openSingleLocationInGoogleMaps(context, shopLat, shopLng, "Prayag Deen & Sons Medical") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (isHindi) "नक्शा देखें" else "Store Map", maxLines = 1)
                        }
                    }
                }
            }

            // Live Public Advertisement & Offer Showcase
            Text(strings.adsHeader, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(end = 6.dp),
                modifier = Modifier.fillMaxWidth().height(215.dp).testTag("public_ad_showcase")
            ) {
                items(displayAds, key = { it.id }) { ad ->
                    val customBmp = remember(ad.imageBase64) { MediaUtils.base64ToBitmap(ad.imageBase64) }
                    Card(
                        onClick = {
                            if (ad.linkUrl.isNotBlank()) {
                                runCatching {
                                    val url = if (ad.linkUrl.startsWith("http")) ad.linkUrl else "https://${ad.linkUrl}"
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                }
                            } else {
                                triggerGoogleSignIn(RolePortalSelection.CustomerOnboarding)
                            }
                        },
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.width(284.dp).height(210.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (customBmp != null) {
                                Image(
                                    bitmap = customBmp.asImageBitmap(),
                                    contentDescription = ad.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                val resId = when (ad.imagePreset) {
                                    "promo_delivery" -> R.drawable.img_promo_delivery
                                    "promo_festival" -> R.drawable.img_promo_festival
                                    else -> R.drawable.img_hero_pharmacy
                                }
                                Image(
                                    painter = painterResource(id = resId),
                                    contentDescription = ad.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))))
                            )
                            Surface(
                                color = SaffronAccent,
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.align(Alignment.TopStart).padding(10.dp)
                            ) {
                                Text(
                                    ad.category,
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Column(modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)) {
                                Text(ad.title, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 2)
                                Text(ad.subtitle, color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodySmall, maxLines = 2)
                            }
                        }
                    }
                }
            }

            // Medicine Categories Showcase
            Text(if (isHindi) "उपलब्ध दवा श्रेणियां" else "Available Medicine Categories", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("Prescription Refill", "Fever & Pain", "Diabetes & BP", "Baby & Mother Care", "Vitamins & First Aid")) { cat ->
                    FilledTonalButton(onClick = { triggerGoogleSignIn(RolePortalSelection.CustomerOnboarding) }) {
                        Text(cat)
                    }
                }
            }

            // Google Sign-In + Direct Portal Entry Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocalPharmacy, contentDescription = null, tint = EmeraldPrimary)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (isHindi) "ऑर्डर, चैट या पोर्टल के लिए साइन इन करें" else "Sign In to Order, Chat, or Access Staff Portal",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        if (isHindi) "प्राइवेट WhatsApp चैट, ऑर्डर ट्रैकिंग, या मैनेजर/डिलीवरी डैशबोर्ड के लिए Google साइन-इन आवश्यक है।"
                        else "Browse store info freely above. Google Sign-In verifies your session before private Customer chat/orders, Manager desk, or Delivery routes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = { triggerGoogleSignIn(null) },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("google_sign_in_button")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(strings.signInWithGoogle)
                        }
                    }
                    if (!errorMsg.isNullOrBlank()) {
                        Text(errorMsg!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // Role Selection Cards Preview (Tap to Sign In directly into chosen role)
            Text(strings.selectRoleTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            RoleCard(
                icon = Icons.Default.Person,
                title = strings.customerLogin,
                desc = strings.customerDesc,
                accent = EmeraldPrimary,
                testTag = "public_role_customer_card",
                onClick = { triggerGoogleSignIn(RolePortalSelection.CustomerOnboarding) }
            )
            RoleCard(
                icon = Icons.Default.AdminPanelSettings,
                title = strings.managerLogin,
                desc = strings.managerDesc,
                accent = EmeraldSecondary,
                testTag = "public_role_manager_card",
                onClick = { triggerGoogleSignIn(RolePortalSelection.ManagerLogin) }
            )
            RoleCard(
                icon = Icons.Default.DeliveryDining,
                title = strings.deliveryLogin,
                desc = strings.deliveryDesc,
                accent = MaterialTheme.colorScheme.tertiary,
                testTag = "public_role_delivery_card",
                onClick = { triggerGoogleSignIn(RolePortalSelection.DeliveryLogin) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoleSelectionAndPortalHost(
    strings: AppStrings,
    language: AppLanguage,
    portal: RolePortalSelection,
    isBusy: Boolean,
    errorText: String?,
    onToggleLanguage: () -> Unit,
    onSelectPortal: (RolePortalSelection) -> Unit,
    onRegisterCustomer: (String, String, Boolean, String, String, String, String, String, Double, Double, Double) -> Unit,
    onLoginManager: (String, String) -> Unit,
    onLoginDelivery: (String, String) -> Unit,
    onSignOut: () -> Unit
) {
    when (portal) {
        RolePortalSelection.RolePicker -> {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(strings.appTitle, fontWeight = FontWeight.Bold) },
                        actions = {
                            LanguageToggleChip(language = language, onToggle = onToggleLanguage)
                            TextButton(onClick = onSignOut) { Text(strings.signOut) }
                        }
                    )
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(strings.selectRoleTitle, style = MaterialTheme.typography.headlineSmall)
                    RoleCard(
                        icon = Icons.Default.Person,
                        title = strings.customerLogin,
                        desc = strings.customerDesc,
                        accent = EmeraldPrimary,
                        testTag = "role_customer_card",
                        onClick = { onSelectPortal(RolePortalSelection.CustomerOnboarding) }
                    )
                    RoleCard(
                        icon = Icons.Default.AdminPanelSettings,
                        title = strings.managerLogin,
                        desc = strings.managerDesc,
                        accent = EmeraldSecondary,
                        testTag = "role_manager_card",
                        onClick = { onSelectPortal(RolePortalSelection.ManagerLogin) }
                    )
                    RoleCard(
                        icon = Icons.Default.DeliveryDining,
                        title = strings.deliveryLogin,
                        desc = strings.deliveryDesc,
                        accent = MaterialTheme.colorScheme.tertiary,
                        testTag = "role_delivery_card",
                        onClick = { onSelectPortal(RolePortalSelection.DeliveryLogin) }
                    )
                }
            }
        }

        RolePortalSelection.CustomerOnboarding -> {
            CustomerRegistrationScreen(
                strings = strings,
                isBusy = isBusy,
                errorText = errorText,
                onBack = { onSelectPortal(RolePortalSelection.RolePicker) },
                onSubmit = onRegisterCustomer
            )
        }

        RolePortalSelection.ManagerLogin -> {
            StaffCredentialLoginScreen(
                title = strings.managerLogin,
                subtitle = "Individual accounts for Owner & 4 Family Managers (owner, father, me, uncle, brother). Initial demo Owner password: 1234",
                quickHints = listOf("owner" to "1234", "father" to "father1234", "me" to "me1234", "uncle" to "uncle1234", "brother" to "brother1234"),
                isBusy = isBusy,
                errorText = errorText,
                onBack = { onSelectPortal(RolePortalSelection.RolePicker) },
                onSubmit = onLoginManager
            )
        }

        RolePortalSelection.DeliveryLogin -> {
            StaffCredentialLoginScreen(
                title = strings.deliveryLogin,
                subtitle = "Sign in with your assigned delivery username and password. Initial demo account: rahul_dp / 1234",
                quickHints = listOf("rahul_dp" to "1234"),
                isBusy = isBusy,
                errorText = errorText,
                onBack = { onSelectPortal(RolePortalSelection.RolePicker) },
                onSubmit = onLoginDelivery
            )
        }
    }
}

@Composable
private fun RoleCard(
    icon: ImageVector,
    title: String,
    desc: String,
    accent: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = accent.copy(alpha = 0.14f),
                contentColor = accent,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = title, modifier = Modifier.size(30.dp))
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(4.dp))
                Text(desc, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomerRegistrationScreen(
    strings: AppStrings,
    isBusy: Boolean,
    errorText: String?,
    onBack: () -> Unit,
    onSubmit: (String, String, Boolean, String, String, String, String, String, Double, Double, Double) -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var verificationId by remember { mutableStateOf<String?>(null) }
    var otpSent by remember { mutableStateOf(false) }
    var otpSending by remember { mutableStateOf(false) }
    var otpStatusMsg by remember { mutableStateOf<String?>(null) }
    var otpVerified by remember { mutableStateOf(false) }
    var houseNo by remember { mutableStateOf("") }
    var street by remember { mutableStateOf("") }
    var area by remember { mutableStateOf("") }
    var landmark by remember { mutableStateOf("") }
    var lat by remember { mutableDoubleStateOf(26.8467) }
    var lng by remember { mutableDoubleStateOf(80.9462) }
    var accuracy by remember { mutableDoubleStateOf(20.0) }
    var locationStatus by remember { mutableStateOf("Tap 'Use Current GPS' or adjust pin on map manually.") }
    var showAccuracyWarning by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val locPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            scope.launch {
                val gps = LocationUtils.fetchCurrentLocation(context)
                if (gps != null) {
                    lat = gps.lat
                    lng = gps.lng
                    accuracy = gps.accuracyMeters.toDouble()
                    showAccuracyWarning = gps.accuracyMeters > 50f
                    locationStatus = "GPS locked (±${gps.accuracyMeters.toInt()}m). Adjust pin manually if needed."
                } else {
                    locationStatus = "GPS unavailable right now. Please tap map to set pin manually."
                }
            }
        } else {
            locationStatus = "Location permission declined. Manual address & map pin entry enabled."
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.customerLogin) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Full Name *") },
                modifier = Modifier.fillMaxWidth().testTag("cust_name_input")
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it.filter { ch -> ch.isDigit() || ch == '+' }.take(15) },
                    label = { Text("Mobile Number *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.weight(1f).testTag("cust_phone_input")
                )
                FilledTonalButton(
                    onClick = {
                        validationError = null
                        if (phone.length < 10) {
                            validationError = "Enter a valid 10-digit mobile number."
                            return@FilledTonalButton
                        }
                        val activity = context as? Activity
                        if (activity == null) {
                            otpSent = true
                            otpStatusMsg = "Demo OTP mode active (123456)."
                        } else {
                            otpSending = true
                            otpStatusMsg = "Sending SMS OTP..."
                            startPhoneNumberVerification(
                                activity = activity,
                                rawPhone = phone,
                                onCodeSent = { vId ->
                                    otpSending = false
                                    verificationId = vId
                                    otpSent = true
                                    otpStatusMsg = "SMS OTP sent! Enter the 6-digit code."
                                },
                                onAutoVerified = { cred ->
                                    scope.launch {
                                        otpSending = false
                                        verifyAndUpdatePhoneCredential(cred)
                                        otpVerified = true
                                        otpStatusMsg = "Mobile auto-verified via SMS ✓"
                                    }
                                },
                                onVerificationFailed = { errMsg ->
                                    otpSending = false
                                    otpSent = true
                                    verificationId = null
                                    otpStatusMsg = "SMS unavailable ($errMsg). Use Demo OTP 123456 below."
                                }
                            )
                        }
                    },
                    enabled = !otpSending && !otpVerified,
                    modifier = Modifier.testTag("send_otp_button")
                ) { Text(if (otpSending) "..." else if (otpSent) "Resend" else "Verify") }
            }
            if (!otpStatusMsg.isNullOrBlank() && !otpVerified) {
                Text(otpStatusMsg!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (otpSent && !otpVerified) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = otpCode,
                        onValueChange = { otpCode = it.take(6) },
                        label = { Text("6-digit OTP (Demo: 123456)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            validationError = null
                            val vId = verificationId
                            if (otpCode == "123456" && vId == null) {
                                otpVerified = true
                            } else if (vId != null && otpCode.length == 6) {
                                otpSending = true
                                scope.launch {
                                    val res = verifyPhoneOtpCode(vId, otpCode)
                                    otpSending = false
                                    res.onSuccess { otpVerified = true }
                                        .onFailure {
                                            if (otpCode == "123456") otpVerified = true
                                            else validationError = it.localizedMessage ?: "Invalid OTP code."
                                        }
                                }
                            } else {
                                validationError = "Enter the 6-digit SMS OTP (or 123456 in Demo mode)."
                            }
                        },
                        enabled = !otpSending
                    ) { Text("Confirm") }
                }
            }
            if (otpVerified) {
                Text("Mobile verified ✓", color = EmeraldSecondary, style = MaterialTheme.typography.labelLarge)
            }

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Delivery Location & Map Pin", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "We ask location permission only to help pin your doorstep accurately—never silently tracked.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedButton(
                        onClick = {
                            locPermLauncher.launch(
                                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                            )
                        },
                        modifier = Modifier.fillMaxWidth().testTag("request_gps_button")
                    ) {
                        Icon(Icons.Default.GpsFixed, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Use Current GPS Location")
                    }
                    Text(locationStatus, style = MaterialTheme.typography.bodySmall)
                    if (showAccuracyWarning) {
                        Surface(color = StatusWarning.copy(alpha = 0.16f), shape = RoundedCornerShape(8.dp)) {
                            Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = StatusWarning)
                                Spacer(Modifier.width(6.dp))
                                Text(strings.accuracyWarning, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    PharmacyInteractiveMap(
                        pins = listOf(MapPinData("home", "Delivery Pin", area.ifBlank { "Selected Spot" }, lat, lng)),
                        centerLat = lat,
                        centerLng = lng,
                        allowTapToMovePin = true,
                        onMapCoordinateSelected = { newLat, newLng ->
                            lat = newLat
                            lng = newLng
                            locationStatus = "Pin adjusted manually (${"%.4f".format(lat)}, ${"%.4f".format(lng)})"
                        },
                        modifier = Modifier.height(190.dp)
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = houseNo,
                    onValueChange = { houseNo = it },
                    label = { Text("House / Shop No.") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = area,
                    onValueChange = { area = it },
                    label = { Text("Village / Locality *") },
                    modifier = Modifier.weight(1f).testTag("cust_area_input")
                )
            }
            OutlinedTextField(
                value = street,
                onValueChange = { street = it },
                label = { Text("Street / Mohalla") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = landmark,
                onValueChange = { landmark = it },
                label = { Text("Nearby Landmark") },
                modifier = Modifier.fillMaxWidth()
            )

            val err = validationError ?: errorText
            if (!err.isNullOrBlank()) {
                Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = {
                    validationError = null
                    when {
                        name.isBlank() -> validationError = "Please enter your name."
                        phone.length < 8 -> validationError = "Please enter a valid mobile number."
                        area.isBlank() -> validationError = "Please enter your village or locality."
                        else -> onSubmit(name, phone, otpVerified, "Home", houseNo, street, area, landmark, lat, lng, accuracy)
                    }
                },
                enabled = !isBusy,
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("complete_customer_reg_button")
            ) {
                if (isBusy) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                else Text("Save & Open Customer Home")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StaffCredentialLoginScreen(
    title: String,
    subtitle: String,
    quickHints: List<Pair<String, String>>,
    isBusy: Boolean,
    errorText: String?,
    onBack: () -> Unit,
    onSubmit: (String, String) -> Unit
) {
    BackHandler(onBack = onBack)
    var username by remember { mutableStateOf(quickHints.firstOrNull()?.first ?: "") }
    var password by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                quickHints.take(3).forEach { (u, p) ->
                    FilledTonalButton(
                        onClick = { username = u; password = p },
                        modifier = Modifier.weight(1f)
                    ) { Text(u, maxLines = 1) }
                }
            }
            if (quickHints.size > 3) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    quickHints.drop(3).forEach { (u, p) ->
                        FilledTonalButton(
                            onClick = { username = u; password = p },
                            modifier = Modifier.weight(1f)
                        ) { Text(u, maxLines = 1) }
                    }
                }
            }
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("staff_username_input")
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth().testTag("staff_password_input")
            )
            if (!errorText.isNullOrBlank()) {
                Text(errorText, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Button(
                onClick = { onSubmit(username, password) },
                enabled = !isBusy && username.isNotBlank() && password.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("staff_login_submit_button")
            ) {
                if (isBusy) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                else Text("Sign In Securely")
            }
        }
    }
}
