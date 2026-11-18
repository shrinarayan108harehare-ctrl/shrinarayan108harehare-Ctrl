package com.example.ui.customer

import android.Manifest
import android.content.Intent
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.localization.AppLanguage
import com.example.localization.AppStrings
import com.example.model.Advertisement
import com.example.model.ChatMessage
import com.example.model.CustomerAddress
import com.example.model.CustomerProfile
import com.example.model.OrderStatus
import com.example.model.PharmacyOrder
import com.example.model.RecipientNotification
import com.example.model.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.example.ui.components.AnnouncementInboxSheet
import com.example.ui.components.ImageLightboxDialog
import com.example.ui.components.LanguageToggleChip
import com.example.ui.components.MapPinData
import com.example.ui.components.NotificationBellButton
import com.example.ui.components.OfflineBanner
import com.example.ui.components.PharmacyInteractiveMap
import com.example.ui.components.StatusPill
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.EmeraldSecondary
import com.example.ui.theme.SaffronAccent
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.WhatsAppHeader
import com.example.ui.theme.WhatsAppIncoming
import com.example.ui.theme.WhatsAppOutgoing
import com.example.ui.theme.WhatsAppWallpaper
import com.example.util.LocationUtils
import com.example.util.MediaUtils
import com.example.util.TimezoneUtils
import com.example.viewmodel.CustomerViewModel
import com.example.viewmodel.PendingLocalMessage
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

private enum class CustomerTab { HOME, CHAT, ORDERS, AI }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDashboardScreen(
    vm: CustomerViewModel,
    strings: AppStrings,
    language: AppLanguage,
    isOnline: Boolean,
    notifications: List<RecipientNotification>,
    notificationsEnabled: Boolean = true,
    onToggleNotifications: (Boolean) -> Unit = {},
    onToggleLanguage: () -> Unit,
    onMarkNotificationOpened: (String) -> Unit,
    onMarkNotificationRead: (String) -> Unit,
    onSignOut: () -> Unit
) {
    var selectedTab by rememberSaveable { mutableStateOf(CustomerTab.HOME) }
    var showAnnouncements by remember { mutableStateOf(false) }

    val profile by vm.profile.collectAsStateWithLifecycle()
    val ads by vm.ads.collectAsStateWithLifecycle()
    val conversation by vm.conversation.collectAsStateWithLifecycle()
    val messagesState by vm.messagesState.collectAsStateWithLifecycle()
    val pendingMessages by vm.pendingMessages.collectAsStateWithLifecycle()
    val ordersState by vm.ordersState.collectAsStateWithLifecycle()
    val addresses by vm.addresses.collectAsStateWithLifecycle()
    val shopSettings by vm.shopSettings.collectAsStateWithLifecycle()
    val statusBanner by vm.statusBanner.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(statusBanner) {
        val msg = statusBanner
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            vm.clearBanner()
        }
    }

    LaunchedEffect(selectedTab) {
        if (selectedTab == CustomerTab.CHAT) {
            vm.markInboxRead()
        }
    }

    if (selectedTab != CustomerTab.HOME) {
        BackHandler { selectedTab = CustomerTab.HOME }
    }

    val unreadAnnouncements = notifications.count { it.status != "READ" }
    val unreadChat = (conversation?.unreadByCustomer ?: 0L).toInt()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                TopAppBar(
                    colors = if (selectedTab == CustomerTab.CHAT) {
                        TopAppBarDefaults.topAppBarColors(
                            containerColor = WhatsAppHeader,
                            titleContentColor = Color.White,
                            actionIconContentColor = Color.White
                        )
                    } else {
                        TopAppBarDefaults.topAppBarColors()
                    },
                    title = {
                        Column {
                            Text(
                                text = if (selectedTab == CustomerTab.CHAT) "Prayag Deen & Sons Pharmacy" else strings.appTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (selectedTab == CustomerTab.CHAT) "Direct Manager WhatsApp Desk" else (profile?.name ?: "Customer"),
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1
                            )
                        }
                    },
                    actions = {
                        LanguageToggleChip(language = language, onToggle = onToggleLanguage)
                        NotificationBellButton(unreadCount = unreadAnnouncements) {
                            showAnnouncements = true
                        }
                        IconButton(onClick = onSignOut, modifier = Modifier.testTag("customer_sign_out_btn")) {
                            Icon(Icons.Default.Logout, contentDescription = strings.signOut)
                        }
                    }
                )
                OfflineBanner(isOnline = isOnline, strings = strings)
            }
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == CustomerTab.HOME,
                    onClick = { selectedTab = CustomerTab.HOME },
                    icon = { Icon(Icons.Default.Home, contentDescription = strings.home) },
                    label = { Text(strings.home, maxLines = 1) },
                    modifier = Modifier.testTag("nav_customer_home")
                )
                NavigationBarItem(
                    selected = selectedTab == CustomerTab.CHAT,
                    onClick = { selectedTab = CustomerTab.CHAT },
                    icon = {
                        BadgedBox(badge = { if (unreadChat > 0) Badge { Text(unreadChat.toString()) } }) {
                            Icon(Icons.Default.Chat, contentDescription = strings.chat)
                        }
                    },
                    label = { Text(strings.chat, maxLines = 1) },
                    modifier = Modifier.testTag("nav_customer_chat")
                )
                NavigationBarItem(
                    selected = selectedTab == CustomerTab.ORDERS,
                    onClick = { selectedTab = CustomerTab.ORDERS },
                    icon = { Icon(Icons.Default.ReceiptLong, contentDescription = strings.orders) },
                    label = { Text(strings.orders, maxLines = 1) },
                    modifier = Modifier.testTag("nav_customer_orders")
                )
                NavigationBarItem(
                    selected = selectedTab == CustomerTab.AI,
                    onClick = { selectedTab = CustomerTab.AI },
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = strings.aiAssistant) },
                    label = { Text(strings.aiAssistant, maxLines = 1) },
                    modifier = Modifier.testTag("nav_customer_ai")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                CustomerTab.HOME -> CustomerHomeTab(
                    strings = strings,
                    isHindi = language == AppLanguage.HINDI,
                    ads = ads,
                    ordersState = ordersState,
                    shopPhone = shopSettings?.phone ?: "",
                    shopHours = shopSettings?.hours ?: "8:00 AM - 10:30 PM",
                    onOpenChat = { selectedTab = CustomerTab.CHAT },
                    onOpenOrders = { selectedTab = CustomerTab.ORDERS },
                    onSendMedicineQuickAsk = { cat ->
                        vm.sendChatMessage("Hello, please share available medicines & prices in: $cat")
                        selectedTab = CustomerTab.CHAT
                    }
                )

                CustomerTab.CHAT -> CustomerChatTab(
                    vm = vm,
                    strings = strings,
                    isHindi = language == AppLanguage.HINDI,
                    messagesState = messagesState,
                    pendingMessages = pendingMessages,
                    addresses = addresses
                )

                CustomerTab.ORDERS -> CustomerOrdersAndLocationTab(
                    vm = vm,
                    strings = strings,
                    isHindi = language == AppLanguage.HINDI,
                    profile = profile,
                    ordersState = ordersState,
                    addresses = addresses
                )

                CustomerTab.AI -> CustomerAiPharmacistTab(
                    vm = vm,
                    isHindi = language == AppLanguage.HINDI
                )
            }
        }
    }

    if (showAnnouncements) {
        AnnouncementInboxSheet(
            strings = strings,
            notifications = notifications,
            allowReplyInChat = true,
            notificationsEnabled = notificationsEnabled,
            onToggleNotifications = onToggleNotifications,
            onMarkOpened = onMarkNotificationOpened,
            onMarkRead = onMarkNotificationRead,
            onReplyInChat = { title, reply ->
                vm.replyToAnnouncementInChat(title, reply)
                selectedTab = CustomerTab.CHAT
            },
            onDismiss = { showAnnouncements = false }
        )
    }
}

@Composable
private fun CustomerHomeTab(
    strings: AppStrings,
    isHindi: Boolean,
    ads: List<Advertisement>,
    ordersState: UiState<List<PharmacyOrder>>,
    shopPhone: String,
    shopHours: String,
    onOpenChat: () -> Unit,
    onOpenOrders: () -> Unit,
    onSendMedicineQuickAsk: (String) -> Unit
) {
    val context = LocalContext.current
    val activeOrders = (ordersState as? UiState.Success)?.data
        ?.filter { it.status !in listOf("DELIVERED", "CANCELLED") }
        ?: emptyList()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Dedicated 40-50% Advertisement Showcase Space (does not interfere with WhatsApp chat tab)
        Text(strings.adsHeader, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        val displayAds = if (ads.isNotEmpty()) ads else listOf(
            Advertisement(
                id = "default_1",
                title = if (isHindi) "प्रयाग दीन एंड संस मेडिकल — तेज़ होम डिलीवरी" else "Prayag Deen & Sons — Fast Doorstep Delivery",
                subtitle = if (isHindi) "अपना पर्चा WhatsApp चैट पर भेजें और घर बैठे दवा पाएं।" else "Upload your prescription on Pharmacy Chat for verified local delivery.",
                category = "OFFER",
                imagePreset = "hero_pharmacy"
            ),
            Advertisement(
                id = "default_2",
                title = if (isHindi) "त्योहार व मौसमी स्वास्थ्य देखभाल ऑफर" else "Seasonal & Festival Wellness Offers",
                subtitle = if (isHindi) "विटामिन, शुगर, बीपी और दैनिक दवाइयों पर विशेष छूट।" else "Special savings on chronic care, vitamins & daily essentials.",
                category = "FESTIVAL",
                imagePreset = "promo_festival"
            )
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(end = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .testTag("customer_ad_showcase")
        ) {
            items(displayAds, key = { it.id }) { ad ->
                AdShowcaseCard(
                    ad = ad,
                    onClick = {
                        if (ad.linkUrl.isNotBlank()) {
                            runCatching {
                                val url = if (ad.linkUrl.startsWith("http")) ad.linkUrl else "https://${ad.linkUrl}"
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            }
                        } else {
                            onOpenChat()
                        }
                    }
                )
            }
        }

        // Direct WhatsApp-like Pharmacy Chat CTA Card
        Card(
            colors = CardDefaults.cardColors(containerColor = EmeraldPrimary),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    if (isHindi) "सीधे मैनेजर से दवा मंगाएं (WhatsApp चैट)" else "Order Medicines via Direct Pharmacy Chat",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    if (isHindi) "दवा का नाम लिखें, पर्चा/फोटो/PDF/वीडियो या वॉइस संदेश भेजें।" else "Send medicine names, camera prescriptions, PDFs, or voice notes directly to our pharmacy desk.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
                Button(
                    onClick = onOpenChat,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = SaffronAccent,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("home_open_chat_button")
                ) {
                    Icon(Icons.Default.Chat, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (isHindi) "फार्मेसी चैट खोलें" else "Open Pharmacy Chat")
                }
            }
        }

        if (activeOrders.isNotEmpty()) {
            Card(
                onClick = onOpenOrders,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.LocalShipping, contentDescription = null, tint = EmeraldPrimary)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Active Order: ${activeOrders.first().orderNumber}", fontWeight = FontWeight.Bold)
                            Text(activeOrders.first().itemsSummary, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    StatusPill(activeOrders.first().status, isHindi)
                }
            }
        }

        Text(if (isHindi) "त्वरित दवा श्रेणियां" else "Quick Category Inquiry", style = MaterialTheme.typography.titleMedium)
        val categories = listOf(
            "Prescription Refill",
            "Fever & Pain",
            "Diabetes & BP",
            "Baby & Mother Care",
            "Vitamins & First Aid"
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(categories) { cat ->
                FilledTonalButton(onClick = { onSendMedicineQuickAsk(cat) }) {
                    Text(cat)
                }
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Prayag Deen & Sons Medical", fontWeight = FontWeight.Bold)
                Text("Hours: $shopHours", style = MaterialTheme.typography.bodySmall)
                if (shopPhone.isNotBlank()) {
                    Text("Desk Phone: $shopPhone", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun AdShowcaseCard(ad: Advertisement, onClick: () -> Unit) {
    val customBmp = remember(ad.imageBase64) { MediaUtils.base64ToBitmap(ad.imageBase64) }
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .width(308.dp)
            .height(254.dp)
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
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.82f))))
            )
            Surface(
                color = SaffronAccent,
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
            ) {
                Text(
                    ad.category,
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(14.dp)
            ) {
                Text(ad.title, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 2)
                Spacer(Modifier.height(4.dp))
                Text(ad.subtitle, color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodySmall, maxLines = 2)
                if (ad.linkUrl.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Tap to open offer", color = Color.White, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomerChatTab(
    vm: CustomerViewModel,
    strings: AppStrings,
    isHindi: Boolean,
    messagesState: UiState<List<ChatMessage>>,
    pendingMessages: List<PendingLocalMessage>,
    addresses: List<CustomerAddress>
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val savedDraft by vm.draftText.collectAsStateWithLifecycle()
    var messageInput by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(savedDraft) {
        if (messageInput.isBlank() && savedDraft.isNotBlank()) {
            messageInput = savedDraft
        }
    }

    var showAttachSheet by remember { mutableStateOf(false) }
    var showConfirmOrderDialog by remember { mutableStateOf(false) }
    var lightboxImage by remember { mutableStateOf<Pair<String, String>?>(null) }

    // Audio Recording & Playback state
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var recordFile by remember { mutableStateOf<File?>(null) }
    var recordStartMs by remember { mutableLongStateOf(0L) }
    var isRecording by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var playingMsgId by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { recorder?.stop(); recorder?.release() }
            runCatching { mediaPlayer?.release() }
        }
    }

    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = cameraUri
        if (ok && uri != null) {
            val bmp = MediaUtils.uriToBitmap(context, uri)
            if (bmp != null) {
                val b64 = MediaUtils.compressBitmapToBase64(bmp)
                vm.sendChatMessage(
                    text = "Prescription / Camera Photo",
                    messageType = "prescription",
                    mediaBase64 = b64,
                    mediaFileName = "camera_rx_${System.currentTimeMillis()}.jpg",
                    mediaMimeType = "image/jpeg"
                )
            }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
            if (mime.startsWith("video/")) {
                val info = MediaUtils.readSmallDocumentAsBase64(context, uri)
                val name = info?.first ?: "medicine_video.mp4"
                val b64 = info?.second ?: ""
                if (b64.isNotBlank()) {
                    vm.sendChatMessage(
                        text = "Video message ($name)",
                        messageType = "video",
                        mediaBase64 = b64,
                        mediaFileName = name,
                        mediaMimeType = mime
                    )
                } else {
                    val cid = Firebase.auth.currentUser?.uid.orEmpty()
                    vm.showStatusMessage("Uploading video $name to Firebase Storage...")
                    scope.launch {
                        MediaUtils.uploadChatMediaToStorage(context, uri, cid, "vid", mime)
                            .onSuccess { (uploadedName, url) ->
                                vm.sendChatMessage(
                                    text = "Video message ($uploadedName)",
                                    messageType = "video",
                                    mediaUrl = url,
                                    mediaFileName = uploadedName,
                                    mediaMimeType = mime
                                )
                            }
                            .onFailure { err ->
                                vm.showStatusMessage("Video upload failed: ${err.localizedMessage}. Enable Firebase Storage bucket or pick a shorter clip.")
                            }
                    }
                }
            } else {
                val bmp = MediaUtils.uriToBitmap(context, uri)
                if (bmp != null) {
                    val b64 = MediaUtils.compressBitmapToBase64(bmp)
                    vm.sendChatMessage(
                        text = "Prescription / Medicine Image",
                        messageType = "image",
                        mediaBase64 = b64,
                        mediaFileName = MediaUtils.queryFileName(context, uri) ?: "image.jpg",
                        mediaMimeType = mime
                    )
                }
            }
        }
    }

    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val mime = context.contentResolver.getType(uri) ?: "application/pdf"
            val res = MediaUtils.readSmallDocumentAsBase64(context, uri)
            val name = res?.first ?: "prescription.pdf"
            val b64 = res?.second ?: ""
            if (b64.isNotBlank()) {
                vm.sendChatMessage(
                    text = "Document attached: $name",
                    messageType = "document",
                    mediaBase64 = b64,
                    mediaFileName = name,
                    mediaMimeType = mime
                )
            } else {
                val cid = Firebase.auth.currentUser?.uid.orEmpty()
                vm.showStatusMessage("Uploading large document $name to Firebase Storage...")
                scope.launch {
                    MediaUtils.uploadChatMediaToStorage(context, uri, cid, "doc", mime)
                        .onSuccess { (uploadedName, url) ->
                            vm.sendChatMessage(
                                text = "Document attached: $uploadedName",
                                messageType = "document",
                                mediaUrl = url,
                                mediaFileName = uploadedName,
                                mediaMimeType = mime
                            )
                        }
                        .onFailure { err ->
                            vm.showStatusMessage("Document upload failed: ${err.localizedMessage}. Enable Firebase Storage or choose a PDF <=420KB.")
                        }
                }
            }
        }
    }

    val micPermLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            runCatching {
                val (rec, file) = MediaUtils.createVoiceRecorder(context)
                recorder = rec
                recordFile = file
                recordStartMs = System.currentTimeMillis()
                isRecording = true
            }
        }
    }

    val cameraPermLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val uri = MediaUtils.createTempCameraUri(context)
            cameraUri = uri
            cameraLauncher.launch(uri)
        }
    }

    val messages = (messagesState as? UiState.Success)?.data ?: emptyList()
    LaunchedEffect(messages.size, pendingMessages.size) {
        val total = messages.size + pendingMessages.size
        if (total > 0) listState.animateScrollToItem(total - 1)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WhatsAppWallpaper)
    ) {
        // Confirm Order Bar
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    if (isHindi) "दवा की सूची या पर्चा भेजें, फिर ऑर्डर कन्फर्म करें" else "Send medicines or Rx, then confirm order & pin",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.weight(1f)
                )
                FilledTonalButton(
                    onClick = { showConfirmOrderDialog = true },
                    modifier = Modifier.testTag("chat_place_order_btn")
                ) {
                    Text(strings.placeOrderFromChat, maxLines = 1)
                }
            }
        }

        // Messages List
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (messagesState) {
                is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                is UiState.Error -> Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text(messagesState.message, color = MaterialTheme.colorScheme.error)
                }
                is UiState.Success -> {
                    if (messages.isEmpty() && pendingMessages.isEmpty()) {
                        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                            Card {
                                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        if (isHindi) "प्रयाग दीन एंड संस मेडिकल में आपका स्वागत है" else "Welcome to Prayag Deen & Sons Chat",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        if (isHindi) "दवाओं के नाम लिखें या कैमरा आइकन से पर्चा भेजें।" else "Type medicine names & quantities, or tap the camera/attach icon to send a prescription.",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(messages, key = { it.id }) { msg ->
                                WhatsAppChatBubble(
                                    msg = msg,
                                    isOutgoing = msg.senderRole == "customer",
                                    isPlayingVoice = playingMsgId == msg.id,
                                    onImageClick = { lightboxImage = msg.mediaBase64 to msg.mediaFileName.ifBlank { "Prescription" } },
                                    onOpenAttachment = {
                                        val ok = MediaUtils.openAttachment(
                                            context = context,
                                            base64 = msg.mediaBase64,
                                            mediaUrl = msg.mediaUrl,
                                            fileName = msg.mediaFileName,
                                            mimeType = msg.mediaMimeType
                                        )
                                        if (!ok) {
                                            vm.showStatusMessage("No compatible viewer or file payload available for ${msg.mediaFileName.ifBlank { "attachment" }}.")
                                        }
                                    },
                                    onToggleVoice = {
                                        if (playingMsgId == msg.id) {
                                            runCatching { mediaPlayer?.stop(); mediaPlayer?.release() }
                                            mediaPlayer = null
                                            playingMsgId = null
                                        } else {
                                            runCatching { mediaPlayer?.stop(); mediaPlayer?.release() }
                                            playingMsgId = msg.id
                                            mediaPlayer = MediaUtils.playBase64Audio(context, msg.mediaBase64) {
                                                playingMsgId = null
                                            }
                                        }
                                    }
                                )
                            }
                            items(pendingMessages, key = { it.localId }) { pending ->
                                PendingBubble(
                                    pending = pending,
                                    onRetry = { vm.retryPendingMessage(pending.localId) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Composer Bar
        Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 4.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { showAttachSheet = true },
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("chat_attach_btn")
                ) {
                    Icon(Icons.Default.AttachFile, contentDescription = strings.attachPrescription)
                }
                IconButton(
                    onClick = { cameraPermLauncher.launch(Manifest.permission.CAMERA) },
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("chat_camera_btn")
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = strings.takePhoto)
                }
                OutlinedTextField(
                    value = messageInput,
                    onValueChange = {
                        messageInput = it
                        vm.saveDraft(it)
                    },
                    placeholder = { Text(strings.sendMessageHint, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    maxLines = 4,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.weight(1f).testTag("chat_message_input")
                )
                Spacer(Modifier.width(6.dp))
                if (messageInput.isNotBlank()) {
                    FilledIconButton(
                        onClick = {
                            val txt = messageInput.trim()
                            messageInput = ""
                            vm.sendChatMessage(txt)
                        },
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = EmeraldPrimary),
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("chat_send_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                    }
                } else {
                    FilledIconButton(
                        onClick = {
                            if (isRecording) {
                                runCatching {
                                    recorder?.stop()
                                    recorder?.release()
                                }
                                recorder = null
                                isRecording = false
                                val durSec = ((System.currentTimeMillis() - recordStartMs) / 1000L).coerceAtLeast(1L)
                                val b64 = recordFile?.let { MediaUtils.encodeAudioFileBase64(it) }.orEmpty()
                                vm.sendChatMessage(
                                    text = "Voice message (${durSec}s)",
                                    messageType = "audio",
                                    mediaBase64 = b64,
                                    mediaFileName = recordFile?.name ?: "voice.m4a",
                                    mediaMimeType = "audio/mp4",
                                    mediaDurationSec = durSec
                                )
                            } else {
                                micPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = if (isRecording) MaterialTheme.colorScheme.error else EmeraldSecondary
                        ),
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("chat_voice_btn")
                    ) {
                        Icon(
                            if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = if (isRecording) strings.stopRecording else strings.recordVoice
                        )
                    }
                }
            }
        }
    }

    if (showAttachSheet) {
        ModalBottomSheet(onDismissRequest = { showAttachSheet = false }) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(strings.attachPrescription, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    FilledTonalButton(
                        onClick = {
                            showAttachSheet = false
                            cameraPermLauncher.launch(Manifest.permission.CAMERA)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(strings.takePhoto)
                    }
                    FilledTonalButton(
                        onClick = {
                            showAttachSheet = false
                            photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(strings.pickImage)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    FilledTonalButton(
                        onClick = {
                            showAttachSheet = false
                            pdfLauncher.launch(arrayOf("application/pdf", "application/*"))
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(strings.pickPdf)
                    }
                    FilledTonalButton(
                        onClick = {
                            showAttachSheet = false
                            photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Videocam, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(strings.pickVideo)
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (showConfirmOrderDialog) {
        ConfirmOrderDialog(
            addresses = addresses,
            onDismiss = { showConfirmOrderDialog = false },
            onConfirm = { addr, summary, rxReq ->
                vm.placeOrder(addr, summary, rxReq, UUID.randomUUID().toString())
                showConfirmOrderDialog = false
            }
        )
    }

    lightboxImage?.let { (b64, title) ->
        ImageLightboxDialog(base64 = b64, title = title, onDismiss = { lightboxImage = null })
    }
}

@Composable
private fun WhatsAppChatBubble(
    msg: ChatMessage,
    isOutgoing: Boolean,
    isPlayingVoice: Boolean,
    onImageClick: () -> Unit,
    onOpenAttachment: () -> Unit,
    onToggleVoice: () -> Unit
) {
    val bmp = remember(msg.mediaBase64, msg.messageType) {
        if (msg.mediaBase64.isNotBlank() && msg.messageType in listOf("image", "prescription")) {
            MediaUtils.base64ToBitmap(msg.mediaBase64)
        } else null
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isOutgoing) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (isOutgoing) WhatsAppOutgoing else WhatsAppIncoming,
            shape = RoundedCornerShape(
                topStart = 14.dp,
                topEnd = 14.dp,
                bottomStart = if (isOutgoing) 14.dp else 4.dp,
                bottomEnd = if (isOutgoing) 4.dp else 14.dp
            ),
            shadowElevation = 1.dp,
            modifier = Modifier.widthIn(max = 296.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                if (!isOutgoing) {
                    Text(msg.senderName, style = MaterialTheme.typography.labelSmall, color = EmeraldPrimary, fontWeight = FontWeight.Bold)
                }
                if (msg.prescriptionVerified) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = StatusSuccess, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Rx Verified by Pharmacist", style = MaterialTheme.typography.labelSmall, color = StatusSuccess, fontWeight = FontWeight.Bold)
                    }
                }
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = msg.mediaFileName.ifBlank { "Attached photo" },
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(onClick = onImageClick)
                    )
                    Spacer(Modifier.height(6.dp))
                }
                if (msg.messageType == "audio" && msg.mediaBase64.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(EmeraldPrimary.copy(alpha = 0.08f))
                            .clickable(onClick = onToggleVoice)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            if (isPlayingVoice) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = "Play voice message",
                            tint = EmeraldPrimary
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Voice (${msg.mediaDurationSec}s)", style = MaterialTheme.typography.bodySmall, color = EmeraldPrimary)
                    }
                    Spacer(Modifier.height(4.dp))
                } else if (msg.messageType in listOf("document", "video")) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(SaffronAccent.copy(alpha = 0.12f))
                            .clickable(onClick = onOpenAttachment)
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            if (msg.messageType == "document") Icons.Default.PictureAsPdf else Icons.Default.Videocam,
                            contentDescription = null,
                            tint = SaffronAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                msg.mediaFileName.ifBlank { msg.messageType.uppercase() },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text("Tap to open", style = MaterialTheme.typography.labelSmall, color = EmeraldPrimary)
                        }
                        Icon(Icons.Default.OpenInNew, contentDescription = "Open attachment", tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.height(4.dp))
                }
                if (msg.text.isNotBlank()) {
                    Text(msg.text, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF111B21))
                }
                Spacer(Modifier.height(3.dp))
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        TimezoneUtils.formatTimeDisplay(msg.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF667781)
                    )
                    if (isOutgoing) {
                        Icon(
                            if (msg.deliveryStatus == "read") Icons.Default.DoneAll else Icons.Default.Check,
                            contentDescription = msg.deliveryStatus,
                            tint = if (msg.deliveryStatus == "read") Color(0xFF53BDEB) else Color(0xFF667781),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PendingBubble(pending: PendingLocalMessage, onRetry: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Surface(
            color = WhatsAppOutgoing.copy(alpha = 0.78f),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(pending.text, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF111B21))
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        if (pending.failed) Icons.Default.ErrorOutline else Icons.Default.Schedule,
                        contentDescription = null,
                        tint = if (pending.failed) Color.Red else Color.Gray,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        if (pending.failed) "Failed — Tap to retry" else "Sending...",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (pending.failed) Color.Red else Color.Gray,
                        modifier = if (pending.failed) Modifier.clickable(onClick = onRetry) else Modifier
                    )
                }
            }
        }
    }
}

@Composable
private fun ConfirmOrderDialog(
    addresses: List<CustomerAddress>,
    onDismiss: () -> Unit,
    onConfirm: (CustomerAddress, String, Boolean) -> Unit
) {
    var selectedAddr by remember(addresses) { mutableStateOf(addresses.firstOrNull { it.primaryAddress } ?: addresses.firstOrNull()) }
    var itemsSummary by remember { mutableStateOf("") }
    var rxRequired by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirm Order & Location") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = itemsSummary,
                    onValueChange = { itemsSummary = it },
                    label = { Text("Medicines & Quantities *") },
                    placeholder = { Text("e.g., Dolo 650 x2, Pan-D x1") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = rxRequired, onCheckedChange = { rxRequired = it })
                    Text("Prescription medicine (Manager pharmacist verification required)", style = MaterialTheme.typography.bodySmall)
                }
                Text("Select Delivery Address:", style = MaterialTheme.typography.labelLarge)
                if (addresses.isEmpty()) {
                    Text("No saved address found. Please add an address in Orders & Pin tab.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                } else {
                    addresses.forEach { addr ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedAddr = addr }
                                .padding(vertical = 4.dp)
                        ) {
                            FilterChip(
                                selected = selectedAddr?.id == addr.id,
                                onClick = { selectedAddr = addr },
                                label = { Text("${addr.label}: ${addr.villageOrArea}") }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { selectedAddr?.let { onConfirm(it, itemsSummary.trim(), rxRequired) } },
                enabled = selectedAddr != null && itemsSummary.isNotBlank()
            ) { Text("Confirm Order") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun CustomerOrdersAndLocationTab(
    vm: CustomerViewModel,
    strings: AppStrings,
    isHindi: Boolean,
    profile: CustomerProfile?,
    ordersState: UiState<List<PharmacyOrder>>,
    addresses: List<CustomerAddress>
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showAddAddressDialog by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var ratingTarget by remember { mutableStateOf<PharmacyOrder?>(null) }

    val orders = (ordersState as? UiState.Success)?.data ?: emptyList()

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = EmeraldPrimary)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(profile?.name?.ifBlank { "Customer Profile" } ?: "Customer Profile", fontWeight = FontWeight.Bold)
                            Text(profile?.phone?.ifBlank { "Add mobile number" } ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    OutlinedButton(
                        onClick = { showEditProfileDialog = true },
                        modifier = Modifier.testTag("edit_profile_btn")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (isHindi) "प्रोफ़ाइल बदलें" else "Edit Profile")
                    }
                }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(if (isHindi) "सहेजे गए डिलीवरी पते" else "Saved Delivery Locations", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                FilledTonalButton(onClick = { showAddAddressDialog = true }, modifier = Modifier.testTag("add_address_btn")) {
                    Icon(Icons.Default.AddLocationAlt, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (isHindi) "नया पता" else "Add Address")
                }
            }
        }
        items(addresses, key = { it.id }) { addr ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(addr.label, fontWeight = FontWeight.Bold)
                                if (addr.primaryAddress) {
                                    Spacer(Modifier.width(8.dp))
                                    Text("DEFAULT", style = MaterialTheme.typography.labelSmall, color = EmeraldPrimary, fontWeight = FontWeight.Bold)
                                }
                            }
                            Text(addr.fullAddress, style = MaterialTheme.typography.bodySmall)
                            Text("GPS: ${"%.4f".format(addr.lat)}, ${"%.4f".format(addr.lng)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { LocationUtils.openSingleLocationInGoogleMaps(context, addr.lat, addr.lng, addr.label) }) {
                            Icon(Icons.Default.OpenInNew, contentDescription = "View pin on Maps")
                        }
                    }
                    if (!addr.primaryAddress || addresses.size > 1) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (!addr.primaryAddress) {
                                TextButton(onClick = { vm.setDefaultAddress(addr) }) {
                                    Text(if (isHindi) "डिफ़ॉल्ट सेट करें" else "Set Default")
                                }
                            }
                            if (addresses.size > 1) {
                                TextButton(onClick = { vm.deleteAddress(addr.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(if (isHindi) "हटाएं" else "Delete", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(6.dp))
            Text(if (isHindi) "मेरे ऑर्डर" else "My Orders", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        if (ordersState is UiState.Loading) {
            item { Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
        } else if (orders.isEmpty()) {
            item { Text(strings.noOrdersFound, style = MaterialTheme.typography.bodyMedium) }
        } else {
            items(orders, key = { it.id }) { order ->
                val isActiveDelivery = order.status in listOf("ASSIGNED", "PICKED_UP", "OUT_FOR_DELIVERY")
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(order.orderNumber, fontWeight = FontWeight.Bold)
                            StatusPill(order.status, isHindi)
                        }
                        Text(order.itemsSummary, style = MaterialTheme.typography.bodyMedium)
                        Text("Deliver to: ${order.deliveryAddress}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (order.quotedBillAmount > 0.0 || order.quotedDeliveryCharge > 0.0) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                if (order.quotedBillAmount > 0.0) {
                                    Text(
                                        "Bill: ₹${"%.0f".format(order.quotedBillAmount)}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = SaffronAccent,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                if (order.quotedDeliveryCharge > 0.0) {
                                    Text(
                                        "Delivery: ₹${"%.0f".format(order.quotedDeliveryCharge)}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = EmeraldPrimary
                                    )
                                }
                            }
                        }
                        if (order.prescriptionRequired) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (order.prescriptionVerified) Icons.Default.VerifiedUser else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (order.prescriptionVerified) StatusSuccess else StatusWarning,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    if (order.prescriptionVerified) strings.verifiedBadge else strings.prescriptionVerificationRequired,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (order.prescriptionVerified) StatusSuccess else StatusWarning
                                )
                            }
                        }
                        if (order.assignedDeliveryPersonName.isNotBlank()) {
                            Text("Delivery Partner: ${order.assignedDeliveryPersonName}", style = MaterialTheme.typography.labelMedium, color = EmeraldPrimary)
                        }
                        // Consent-Gated Live Location Share ONLY during active delivery
                        if (isActiveDelivery) {
                            Surface(color = EmeraldPrimary.copy(alpha = 0.08f), shape = RoundedCornerShape(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(strings.shareLiveLocation, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                        Text(
                                            if (order.liveLocationShared) strings.liveLocationActive else "Disabled automatically once delivered",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                    Switch(
                                        checked = order.liveLocationShared,
                                        onCheckedChange = { enable ->
                                            scope.launch {
                                                val gps = LocationUtils.fetchCurrentLocation(context)
                                                vm.toggleLiveLocation(
                                                    orderId = order.id,
                                                    enabled = enable,
                                                    lat = gps?.lat ?: order.deliveryLat,
                                                    lng = gps?.lng ?: order.deliveryLng,
                                                    accuracy = gps?.accuracyMeters?.toDouble() ?: 25.0
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                        }
                        if (order.status == OrderStatus.DELIVERED.wireValue && order.customerRating == 0L) {
                            FilledTonalButton(onClick = { ratingTarget = order }) {
                                Icon(Icons.Default.Star, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Rate Delivery")
                            }
                        } else if (order.customerRating > 0L) {
                            Text("Rated: ${order.customerRating}/5 ★ — ${order.customerFeedback}", style = MaterialTheme.typography.labelMedium, color = SaffronAccent)
                        }
                    }
                }
            }
        }
    }

    if (showEditProfileDialog) {
        var nameInput by remember(profile) { mutableStateOf(profile?.name ?: "") }
        var phoneInput by remember(profile) { mutableStateOf(profile?.phone ?: "") }
        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text(if (isHindi) "प्रोफ़ाइल संपादित करें" else "Edit Profile") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text(if (isHindi) "पूरा नाम *" else "Full Name *") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = { phoneInput = it },
                        label = { Text(if (isHindi) "मोबाइल नंबर *" else "Mobile Number *") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = nameInput.isNotBlank() && phoneInput.isNotBlank(),
                    onClick = {
                        vm.updateProfile(nameInput.trim(), phoneInput.trim())
                        showEditProfileDialog = false
                    }
                ) { Text(if (isHindi) "सहेजें" else "Save") }
            },
            dismissButton = { TextButton(onClick = { showEditProfileDialog = false }) { Text("Cancel") } }
        )
    }

    if (showAddAddressDialog) {
        AddCustomerAddressDialog(
            strings = strings,
            onDismiss = { showAddAddressDialog = false },
            onSave = { addr, isPrimary ->
                vm.saveAddress(addr, isPrimary)
                showAddAddressDialog = false
            }
        )
    }

    ratingTarget?.let { ord ->
        var stars by remember { mutableIntStateOf(5) }
        var feedback by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { ratingTarget = null },
            title = { Text("Rate ${ord.orderNumber}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        (1..5).forEach { s ->
                            IconButton(onClick = { stars = s }) {
                                Icon(Icons.Default.Star, contentDescription = "$s stars", tint = if (s <= stars) SaffronAccent else Color.LightGray)
                            }
                        }
                    }
                    OutlinedTextField(
                        value = feedback,
                        onValueChange = { feedback = it },
                        label = { Text("Feedback (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    vm.submitRating(ord.id, stars.toLong(), feedback)
                    ratingTarget = null
                }) { Text("Submit") }
            },
            dismissButton = { TextButton(onClick = { ratingTarget = null }) { Text("Close") } }
        )
    }
}

@Composable
private fun AddCustomerAddressDialog(
    strings: AppStrings,
    onDismiss: () -> Unit,
    onSave: (CustomerAddress, Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var label by remember { mutableStateOf("Home") }
    var houseNo by remember { mutableStateOf("") }
    var street by remember { mutableStateOf("") }
    var area by remember { mutableStateOf("") }
    var landmark by remember { mutableStateOf("") }
    var lat by remember { mutableDoubleStateOf(26.8467) }
    var lng by remember { mutableDoubleStateOf(80.9462) }
    var accuracy by remember { mutableDoubleStateOf(20.0) }
    var isPrimary by remember { mutableStateOf(true) }

    val gpsPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
        if (perms.values.any { it }) {
            scope.launch {
                val gps = LocationUtils.fetchCurrentLocation(context)
                if (gps != null) {
                    lat = gps.lat
                    lng = gps.lng
                    accuracy = gps.accuracyMeters.toDouble()
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Delivery Address") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text("Label (Home / Work / Shop)") })
                OutlinedTextField(value = houseNo, onValueChange = { houseNo = it }, label = { Text("House / Building No.") })
                OutlinedTextField(value = street, onValueChange = { street = it }, label = { Text("Street") })
                OutlinedTextField(value = area, onValueChange = { area = it }, label = { Text("Village / Locality *") })
                OutlinedTextField(value = landmark, onValueChange = { landmark = it }, label = { Text("Landmark") })
                OutlinedButton(
                    onClick = { gpsPerm.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.GpsFixed, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Detect Current GPS")
                }
                if (accuracy > 50.0) {
                    Text(strings.accuracyWarning, style = MaterialTheme.typography.labelSmall, color = StatusWarning)
                }
                PharmacyInteractiveMap(
                    pins = listOf(MapPinData("pin", label, area, lat, lng)),
                    centerLat = lat,
                    centerLng = lng,
                    allowTapToMovePin = true,
                    onMapCoordinateSelected = { nLat, nLng -> lat = nLat; lng = nLng },
                    modifier = Modifier.height(150.dp)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isPrimary, onCheckedChange = { isPrimary = it })
                    Text("Set as primary address")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val full = listOf(houseNo, street, area, landmark).filter { it.isNotBlank() }.joinToString(", ")
                    onSave(
                        CustomerAddress(
                            id = UUID.randomUUID().toString(),
                            label = label.ifBlank { "Home" },
                            houseNo = houseNo,
                            street = street,
                            villageOrArea = area,
                            landmark = landmark,
                            fullAddress = full,
                            lat = lat,
                            lng = lng,
                            accuracyMeters = accuracy,
                            primaryAddress = isPrimary
                        ),
                        isPrimary
                    )
                },
                enabled = area.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun CustomerAiPharmacistTab(
    vm: CustomerViewModel,
    isHindi: Boolean
) {
    val turns by vm.aiTurns.collectAsStateWithLifecycle()
    val streaming by vm.aiStreamingText.collectAsStateWithLifecycle()
    val aiError by vm.aiError.collectAsStateWithLifecycle()
    var input by remember { mutableStateOf("") }
    var useDeepThinking by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    if (isHindi) "AI दवा सहायक (Search & Maps Grounded)" else "AI Pharmacist Helper (Search & Maps Grounded)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "General medicine guidance only. Confirm prescriptions and dosage with Prayag Deen & Sons pharmacist or your doctor.",
                    style = MaterialTheme.typography.labelSmall
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Deep Clinical Reasoning (Thinking HIGH)", style = MaterialTheme.typography.labelMedium)
                    Switch(checked = useDeepThinking, onCheckedChange = { useDeepThinking = it })
                }
            }
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val prompts = listOf(
                "What does Paracetamol + Ibuprofen do?",
                "How should ORS be prepared?",
                "Precautions when taking antibiotics?"
            )
            items(prompts) { p ->
                FilledTonalButton(onClick = { vm.askPharmacistAi(p, useDeepThinking) }) { Text(p, maxLines = 1) }
            }
        }

        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(turns) { turn ->
                Surface(
                    color = if (turn.isUser) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (turn.isUser) Color.White else MaterialTheme.colorScheme.onSurface,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(turn.text, style = MaterialTheme.typography.bodyMedium)
                        if (turn.groundingNote.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(turn.groundingNote, style = MaterialTheme.typography.labelSmall, color = SaffronAccent)
                        }
                    }
                }
            }
            if (streaming != null) {
                item {
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(streaming!!.ifBlank { "Thinking..." }, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }

        if (!aiError.isNullOrBlank()) {
            Text(aiError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = { Text("Ask about medicines, usage or storage...") },
                modifier = Modifier.weight(1f).testTag("customer_ai_input")
            )
            FilledIconButton(
                onClick = {
                    val q = input.trim()
                    if (q.isNotBlank()) {
                        input = ""
                        vm.askPharmacistAi(q, useDeepThinking)
                    }
                },
                enabled = streaming == null && input.isNotBlank(),
                modifier = Modifier.testTag("customer_ai_send")
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Ask AI")
            }
        }
    }
}
