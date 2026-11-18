package com.example.ui.manager

import android.Manifest
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.localization.AppStrings
import com.example.model.ChatMessage
import com.example.model.Conversation
import com.example.model.CustomerProfile
import com.example.model.DeliveryPerson
import com.example.model.OrderFinancial
import com.example.model.OrderStatus
import com.example.model.PharmacyOrder
import com.example.ui.components.ImageLightboxDialog
import com.example.ui.components.StatusPill
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.EmeraldSecondary
import com.example.ui.theme.SaffronAccent
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.WhatsAppIncoming
import com.example.ui.theme.WhatsAppOutgoing
import com.example.ui.theme.WhatsAppWallpaper
import com.example.util.LocationUtils
import com.example.util.MediaUtils
import com.example.util.TimezoneUtils
import com.example.viewmodel.ManagerViewModel
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun ManagerInboxAndChatScreen(
    vm: ManagerViewModel,
    strings: AppStrings,
    isHindi: Boolean
) {
    val conversations by vm.conversations.collectAsStateWithLifecycle()
    val orders by vm.orders.collectAsStateWithLifecycle()
    val financials by vm.financials.collectAsStateWithLifecycle()
    val customers by vm.customers.collectAsStateWithLifecycle()
    val deliveryStaff by vm.deliveryPersonnel.collectAsStateWithLifecycle()
    val selectedCid by vm.selectedConversationId.collectAsStateWithLifecycle()
    val selectedMessages by vm.selectedMessages.collectAsStateWithLifecycle()
    val rxExtraction by vm.rxExtractionResult.collectAsStateWithLifecycle()
    val aiReplyDraft by vm.aiReplyDraft.collectAsStateWithLifecycle()
    val aiBusy by vm.aiBusy.collectAsStateWithLifecycle()

    var statusFilter by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }
    var showManualOrderDialog by remember { mutableStateOf(false) }

    val activeConversation = conversations.firstOrNull { it.customerId == selectedCid }

    if (activeConversation != null) {
        BackHandler { vm.openConversation(null) }
        val relatedOrder = orders.firstOrNull { it.id == activeConversation.activeOrderId }
            ?: orders.firstOrNull { it.customerId == activeConversation.customerId }
        val relatedFin = relatedOrder?.let { ord -> financials.firstOrNull { it.orderId == ord.id } }
        val activeCustomer = customers.firstOrNull { it.userId == activeConversation.customerId }
            ?: CustomerProfile(
                userId = activeConversation.customerId,
                name = activeConversation.customerName,
                phone = activeConversation.customerPhone,
                defaultArea = activeConversation.customerArea
            )

        ManagerConversationDetailPane(
            vm = vm,
            strings = strings,
            isHindi = isHindi,
            conversation = activeConversation,
            customer = activeCustomer,
            order = relatedOrder,
            financial = relatedFin,
            messages = selectedMessages,
            deliveryStaff = deliveryStaff.filter { it.active },
            rxExtraction = rxExtraction,
            aiReplyDraft = aiReplyDraft,
            aiBusy = aiBusy,
            onBack = { vm.openConversation(null) }
        )
        return
    }

    val filterTabs = listOf(
        "ALL" to "All",
        "NEW" to "New",
        "PENDING" to "Pending",
        "PROCESSING" to "Processing",
        "READY" to "Ready",
        "ASSIGNED" to "Assigned",
        "OUT_FOR_DELIVERY" to "Out",
        "DELIVERED" to "Delivered",
        "CANCELLED" to "Cancelled"
    )

    val filtered = conversations.filter { conv ->
        val matchesStatus = statusFilter == "ALL" || conv.activeOrderStatus == statusFilter
        val q = searchQuery.trim().lowercase()
        val matchesSearch = q.isBlank() ||
            conv.customerName.lowercase().contains(q) ||
            conv.customerPhone.lowercase().contains(q) ||
            conv.activeOrderId.lowercase().contains(q) ||
            conv.customerArea.lowercase().contains(q)
        matchesStatus && matchesSearch
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(strings.searchHint, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(50),
                modifier = Modifier.weight(1f).testTag("manager_inbox_search")
            )
            FilledTonalButton(
                onClick = { showManualOrderDialog = true },
                modifier = Modifier.minimumInteractiveComponentSize().testTag("manual_order_btn")
            ) {
                Icon(Icons.Default.AddShoppingCart, contentDescription = "New Manual Order")
                Spacer(Modifier.width(4.dp))
                Text("Order")
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(filterTabs) { (key, label) ->
                FilterChip(
                    selected = statusFilter == key,
                    onClick = { statusFilter = key },
                    label = { Text(label) }
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(strings.noOrdersFound, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filtered, key = { it.customerId }) { conv ->
                    Card(
                        onClick = { vm.openConversation(conv.customerId) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth().testTag("inbox_row_${conv.customerId}")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = EmeraldPrimary.copy(alpha = 0.14f),
                                modifier = Modifier.size(46.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        conv.customerName.take(1).uppercase().ifBlank { "C" },
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldPrimary
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(conv.customerName, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(TimezoneUtils.formatTimeDisplay(conv.lastMessageAt), style = MaterialTheme.typography.labelSmall)
                                }
                                Text("${conv.customerPhone} • ${conv.customerArea}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(3.dp))
                                Text(conv.lastMessage, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (conv.activeOrderStatus.isNotBlank()) {
                                    Spacer(Modifier.height(4.dp))
                                    StatusPill(conv.activeOrderStatus, isHindi)
                                }
                            }
                            if (conv.unreadByManager > 0) {
                                Spacer(Modifier.width(8.dp))
                                Badge { Text(conv.unreadByManager.toString()) }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showManualOrderDialog) {
        ManualOrderDialog(
            customers = customers,
            onDismiss = { showManualOrderDialog = false },
            onCreate = { cust, itemsSummary, rxReq ->
                vm.createManualOrder(cust, itemsSummary, rxReq)
                showManualOrderDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManagerConversationDetailPane(
    vm: ManagerViewModel,
    strings: AppStrings,
    isHindi: Boolean,
    conversation: Conversation,
    customer: CustomerProfile,
    order: PharmacyOrder?,
    financial: OrderFinancial?,
    messages: List<ChatMessage>,
    deliveryStaff: List<DeliveryPerson>,
    rxExtraction: String?,
    aiReplyDraft: String?,
    aiBusy: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var replyText by remember { mutableStateOf("") }
    var expandOrderPanel by remember { mutableStateOf(false) }
    var showAttachSheet by remember { mutableStateOf(false) }
    var showCreateChatOrderDialog by remember { mutableStateOf(false) }
    var showSaleReportDialog by remember { mutableStateOf(false) }
    var showAssignDialog by remember { mutableStateOf(false) }
    var showDeliveredDialog by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }
    var lightboxImg by remember { mutableStateOf<Pair<String, String>?>(null) }

    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var recordFile by remember { mutableStateOf<File?>(null) }
    var recordStart by remember { mutableLongStateOf(0L) }
    var isRecording by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var playingId by remember { mutableStateOf<String?>(null) }

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
            MediaUtils.uriToBitmap(context, uri)?.let { bmp ->
                vm.sendManagerReply(
                    conversation = conversation,
                    text = "Pharmacy Photo",
                    messageType = "image",
                    mediaBase64 = MediaUtils.compressBitmapToBase64(bmp),
                    mediaFileName = "mgr_photo_${System.currentTimeMillis()}.jpg",
                    mediaMimeType = "image/jpeg"
                )
            }
        }
    }
    val cameraPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val u = MediaUtils.createTempCameraUri(context)
            cameraUri = u
            cameraLauncher.launch(u)
        }
    }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
            if (mime.startsWith("video/")) {
                val info = MediaUtils.readSmallDocumentAsBase64(context, uri)
                val name = info?.first ?: "pharmacy_video.mp4"
                val b64 = info?.second ?: ""
                if (b64.isNotBlank()) {
                    vm.sendManagerReply(
                        conversation = conversation,
                        text = "Video message ($name)",
                        messageType = "video",
                        mediaBase64 = b64,
                        mediaFileName = name,
                        mediaMimeType = mime
                    )
                } else {
                    vm.showFeedback("Uploading video $name to Firebase Storage...")
                    scope.launch {
                        MediaUtils.uploadChatMediaToStorage(context, uri, conversation.customerId, "mgr_vid", mime)
                            .onSuccess { (uploadedName, url) ->
                                vm.sendManagerReply(
                                    conversation = conversation,
                                    text = "Video message ($uploadedName)",
                                    messageType = "video",
                                    mediaUrl = url,
                                    mediaFileName = uploadedName,
                                    mediaMimeType = mime
                                )
                            }
                            .onFailure { err ->
                                vm.showFeedback("Video upload failed: ${err.localizedMessage}")
                            }
                    }
                }
            } else {
                MediaUtils.uriToBitmap(context, uri)?.let { bmp ->
                    vm.sendManagerReply(
                        conversation = conversation,
                        text = "Pharmacy Image",
                        messageType = "image",
                        mediaBase64 = MediaUtils.compressBitmapToBase64(bmp),
                        mediaFileName = MediaUtils.queryFileName(context, uri) ?: "mgr_img.jpg",
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
            val name = res?.first ?: "pharmacy_doc.pdf"
            val b64 = res?.second ?: ""
            if (b64.isNotBlank()) {
                vm.sendManagerReply(
                    conversation = conversation,
                    text = "Document: $name",
                    messageType = "document",
                    mediaBase64 = b64,
                    mediaFileName = name,
                    mediaMimeType = mime
                )
            } else {
                vm.showFeedback("Uploading $name to Firebase Storage...")
                scope.launch {
                    MediaUtils.uploadChatMediaToStorage(context, uri, conversation.customerId, "mgr_doc", mime)
                        .onSuccess { (uploadedName, url) ->
                            vm.sendManagerReply(
                                conversation = conversation,
                                text = "Document: $uploadedName",
                                messageType = "document",
                                mediaUrl = url,
                                mediaFileName = uploadedName,
                                mediaMimeType = mime
                            )
                        }
                        .onFailure { err ->
                            vm.showFeedback("Document upload failed: ${err.localizedMessage}")
                        }
                }
            }
        }
    }
    val micPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            runCatching {
                val (r, f) = MediaUtils.createVoiceRecorder(context)
                recorder = r; recordFile = f; recordStart = System.currentTimeMillis(); isRecording = true
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(WhatsAppWallpaper)) {
        // Header + Back
        Surface(color = EmeraldPrimary, contentColor = Color.White) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(conversation.customerName, fontWeight = FontWeight.Bold, maxLines = 1)
                        Text("${conversation.customerPhone} • ${conversation.customerArea}", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.85f))
                    }
                    FilledTonalButton(
                        onClick = { showCreateChatOrderDialog = true },
                        modifier = Modifier.padding(end = 6.dp).testTag("chat_create_order_btn")
                    ) {
                        Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("+ Order")
                    }
                    if (order != null) {
                        StatusPill(order.status, isHindi)
                    }
                }

                // Prominent 4 Action Ribbon + Assign/Rx
                if (order != null) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                    ) {
                        item {
                            FilledTonalButton(
                                onClick = {
                                    LocationUtils.openSingleLocationInGoogleMaps(
                                        context,
                                        order.deliveryLat,
                                        order.deliveryLng,
                                        "${order.customerName} (${order.deliveryArea})"
                                    )
                                },
                                modifier = Modifier.testTag("action_location_btn")
                            ) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(strings.locationAction)
                            }
                        }
                        item {
                            FilledTonalButton(
                                onClick = { showSaleReportDialog = true },
                                modifier = Modifier.testTag("action_sale_report_btn")
                            ) {
                                Icon(Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(strings.saleReportAction)
                            }
                        }
                        item {
                            Button(
                                onClick = { showDeliveredDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = StatusSuccess),
                                modifier = Modifier.testTag("action_delivered_btn")
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(strings.deliveredAction)
                            }
                        }
                        item {
                            Button(
                                onClick = { showCancelDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = StatusError),
                                modifier = Modifier.testTag("action_cancelled_btn")
                            ) {
                                Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(strings.cancelledAction)
                            }
                        }
                        item {
                            OutlinedButton(
                                onClick = { showAssignDialog = true },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                            ) {
                                Icon(Icons.Default.DeliveryDining, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Assign DP")
                            }
                        }
                        if (order.prescriptionRequired && !order.prescriptionVerified) {
                            item {
                                Button(
                                    onClick = { vm.verifyOrderPrescription(order) },
                                    colors = ButtonDefaults.buttonColors(containerColor = SaffronAccent)
                                ) {
                                    Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(strings.verifyPrescription)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Expandable Order Information Panel
        if (order != null) {
            Surface(color = MaterialTheme.colorScheme.surface) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { expandOrderPanel = !expandOrderPanel },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Order ${order.orderNumber} • Bill ₹${"%.0f".format(financial?.billValue ?: order.quotedBillAmount)} • Cost ₹${"%.0f".format(financial?.costValue ?: 0.0)}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(if (expandOrderPanel) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = "Toggle Order Info")
                    }
                    if (expandOrderPanel) {
                        Spacer(Modifier.height(6.dp))
                        Text("Items: ${order.itemsSummary}", style = MaterialTheme.typography.bodySmall)
                        Text("Address: ${order.deliveryAddress} (Landmark: ${order.deliveryLandmark})", style = MaterialTheme.typography.bodySmall)
                        if (financial != null) {
                            Text(
                                "Bill: ₹${financial.billValue} | Disc: ₹${financial.discountAmount} | Ref: ₹${financial.refundAmount} | Net Sales: ₹${financial.netProductSales} | Net Business Profit: ₹${financial.netBusinessProfit}",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldPrimary
                            )
                        }
                        Text("Assigned DP: ${order.assignedDeliveryPersonName.ifBlank { "Unassigned" }}", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        // Messages + Gemini Rx/Reply Assist
        if (!rxExtraction.isNullOrBlank() || !aiReplyDraft.isNullOrBlank()) {
            Surface(color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (!rxExtraction.isNullOrBlank()) {
                        Text("AI Prescription Draft (Verify Physically Before Dispensing):", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Text(rxExtraction, style = MaterialTheme.typography.bodySmall)
                    }
                    if (!aiReplyDraft.isNullOrBlank()) {
                        Text("AI Suggested Reply:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Text(aiReplyDraft, style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = { replyText = aiReplyDraft }) { Text("Use This Reply") }
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                val isManagerMsg = msg.senderRole != "customer"
                val bmp = remember(msg.mediaBase64, msg.messageType) {
                    if (msg.mediaBase64.isNotBlank() && msg.messageType in listOf("image", "prescription")) {
                        MediaUtils.base64ToBitmap(msg.mediaBase64)
                    } else null
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isManagerMsg) Arrangement.End else Arrangement.Start
                ) {
                    Surface(
                        color = if (isManagerMsg) WhatsAppOutgoing else WhatsAppIncoming,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.widthIn(max = 300.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("${msg.senderName} (${msg.senderRole})", style = MaterialTheme.typography.labelSmall, color = EmeraldPrimary, fontWeight = FontWeight.Bold)
                            if (bmp != null) {
                                Spacer(Modifier.height(4.dp))
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "Customer attachment",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(160.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { lightboxImg = msg.mediaBase64 to msg.mediaFileName.ifBlank { "Rx Photo" } }
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                                    if (!msg.prescriptionVerified) {
                                        FilledTonalButton(onClick = { vm.verifyPrescriptionMessage(conversation.customerId, msg.id) }) {
                                            Text("Verify Rx")
                                        }
                                    } else {
                                        Text("Rx Verified ✓", color = StatusSuccess, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    }
                                    OutlinedButton(
                                        enabled = !aiBusy,
                                        onClick = { vm.extractPrescription(bmp) }
                                    ) {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("AI Read")
                                    }
                                }
                            }
                            if (msg.messageType == "audio" && msg.mediaBase64.isNotBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable {
                                        if (playingId == msg.id) {
                                            runCatching { mediaPlayer?.stop(); mediaPlayer?.release() }
                                            playingId = null
                                        } else {
                                            runCatching { mediaPlayer?.stop(); mediaPlayer?.release() }
                                            playingId = msg.id
                                            mediaPlayer = MediaUtils.playBase64Audio(context, msg.mediaBase64) { playingId = null }
                                        }
                                    }.padding(vertical = 4.dp)
                                ) {
                                    Icon(if (playingId == msg.id) Icons.Default.Stop else Icons.Default.PlayArrow, contentDescription = "Play voice")
                                    Text("Play Voice (${msg.mediaDurationSec}s)")
                                }
                            } else if (msg.messageType in listOf("document", "video")) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(SaffronAccent.copy(alpha = 0.12f))
                                        .clickable {
                                            val ok = MediaUtils.openAttachment(
                                                context = context,
                                                base64 = msg.mediaBase64,
                                                mediaUrl = msg.mediaUrl,
                                                fileName = msg.mediaFileName,
                                                mimeType = msg.mediaMimeType
                                            )
                                            if (!ok) {
                                                vm.showFeedback("No viewer or file payload available for ${msg.mediaFileName.ifBlank { "attachment" }}.")
                                            }
                                        }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        if (msg.messageType == "document") Icons.Default.PictureAsPdf else Icons.Default.Videocam,
                                        contentDescription = null,
                                        tint = SaffronAccent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
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
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text(TimezoneUtils.formatTimeDisplay(msg.createdAt), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                if (!isManagerMsg && msg.text.isNotBlank()) {
                                    Text(
                                        "AI Draft Reply",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = EmeraldPrimary,
                                        modifier = Modifier.clickable { vm.draftReply(msg.text, if (isHindi) "Hindi" else "English") }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Reply Composer
        Surface(color = MaterialTheme.colorScheme.surface) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { showAttachSheet = true }) {
                    Icon(Icons.Default.AttachFile, contentDescription = "Attach file or media")
                }
                IconButton(onClick = { cameraPerm.launch(Manifest.permission.CAMERA) }) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Take photo")
                }
                OutlinedTextField(
                    value = replyText,
                    onValueChange = { replyText = it },
                    placeholder = { Text("Reply to ${conversation.customerName}...") },
                    modifier = Modifier.weight(1f).testTag("manager_reply_input")
                )
                Spacer(Modifier.width(6.dp))
                if (replyText.isNotBlank()) {
                    FilledIconButton(
                        onClick = {
                            val txt = replyText.trim()
                            replyText = ""
                            vm.sendManagerReply(conversation = conversation, text = txt)
                        },
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = EmeraldPrimary),
                        modifier = Modifier.testTag("manager_reply_send_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send Reply")
                    }
                } else {
                    FilledIconButton(
                        onClick = {
                            if (isRecording) {
                                runCatching { recorder?.stop(); recorder?.release() }
                                recorder = null
                                isRecording = false
                                val dur = ((System.currentTimeMillis() - recordStart) / 1000L).coerceAtLeast(1L)
                                val b64 = recordFile?.let { MediaUtils.encodeAudioFileBase64(it) }.orEmpty()
                                vm.sendManagerReply(
                                    conversation = conversation,
                                    text = "Voice Reply (${dur}s)",
                                    messageType = "audio",
                                    mediaBase64 = b64,
                                    mediaFileName = recordFile?.name ?: "mgr_voice.m4a",
                                    mediaMimeType = "audio/mp4",
                                    mediaDurationSec = dur
                                )
                            } else {
                                micPerm.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = if (isRecording) StatusError else EmeraldSecondary)
                    ) {
                        Icon(if (isRecording) Icons.Default.Stop else Icons.Default.Mic, contentDescription = "Voice reply")
                    }
                }
            }
        }
    }

    if (showAttachSheet) {
        ModalBottomSheet(onDismissRequest = { showAttachSheet = false }) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Send Attachment to ${conversation.customerName}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    FilledTonalButton(
                        onClick = {
                            showAttachSheet = false
                            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Image")
                    }
                    FilledTonalButton(
                        onClick = {
                            showAttachSheet = false
                            pdfLauncher.launch(arrayOf("application/pdf", "application/*"))
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("PDF / Doc")
                    }
                }
                FilledTonalButton(
                    onClick = {
                        showAttachSheet = false
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Videocam, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Video")
                }
                Spacer(Modifier.height(14.dp))
            }
        }
    }

    if (showCreateChatOrderDialog) {
        var itemsInput by remember { mutableStateOf("") }
        var rxRequired by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showCreateChatOrderDialog = false },
            title = { Text("Create Order for ${conversation.customerName}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = itemsInput,
                        onValueChange = { itemsInput = it },
                        label = { Text("Medicines & Quantities *") },
                        placeholder = { Text("e.g., Azithromycin 500 x1, Dolo 650 x2") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = rxRequired, onCheckedChange = { rxRequired = it })
                        Text("Prescription medicine")
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = itemsInput.isNotBlank(),
                    onClick = {
                        vm.createManualOrder(customer, itemsInput.trim(), rxRequired)
                        showCreateChatOrderDialog = false
                    }
                ) { Text("Create Order") }
            },
            dismissButton = { TextButton(onClick = { showCreateChatOrderDialog = false }) { Text("Cancel") } }
        )
    }

    if (showSaleReportDialog && order != null) {
        SaleReportDialog(
            order = order,
            existing = financial,
            strings = strings,
            onDismiss = { showSaleReportDialog = false },
            onSave = { bill, disc, ref, cost, dist, chg, exp, notes, reason ->
                vm.saveOrderFinancials(order, financial, bill, disc, ref, cost, dist, chg, exp, notes, reason)
                showSaleReportDialog = false
            }
        )
    }

    if (showAssignDialog && order != null) {
        PickDeliveryPersonDialog(
            title = "Assign Delivery Partner for ${order.orderNumber}",
            staff = deliveryStaff,
            onDismiss = { showAssignDialog = false },
            onSelect = { dp ->
                vm.assignDelivery(order, dp)
                showAssignDialog = false
            }
        )
    }

    if (showDeliveredDialog && order != null) {
        if (order.status == OrderStatus.DELIVERED.wireValue) {
            AlertDialog(
                onDismissRequest = { showDeliveredDialog = false },
                title = { Text("Already Delivered") },
                text = { Text("${order.orderNumber} is already marked as DELIVERED.") },
                confirmButton = { TextButton(onClick = { showDeliveredDialog = false }) { Text("OK") } }
            )
        } else {
            PickDeliveryPersonDialog(
                title = "Select Who Delivered ${order.orderNumber}",
                staff = deliveryStaff,
                onDismiss = { showDeliveredDialog = false },
                onSelect = { dp ->
                    vm.updateOrderStatus(order, OrderStatus.DELIVERED, deliveredBy = dp, reason = "Marked delivered via Manager desk")
                    showDeliveredDialog = false
                }
            )
        }
    }

    if (showCancelDialog && order != null) {
        var cancelReason by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Confirm Order Cancellation") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Are you sure you want to cancel ${order.orderNumber}? This records an audit entry.")
                    OutlinedTextField(
                        value = cancelReason,
                        onValueChange = { cancelReason = it },
                        label = { Text("Cancellation Reason *") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError),
                    enabled = cancelReason.isNotBlank(),
                    onClick = {
                        vm.updateOrderStatus(order, OrderStatus.CANCELLED, cancelReason = cancelReason.trim(), reason = cancelReason.trim())
                        showCancelDialog = false
                    }
                ) { Text("Confirm Cancel") }
            },
            dismissButton = { TextButton(onClick = { showCancelDialog = false }) { Text("Back") } }
        )
    }

    lightboxImg?.let { (b64, title) ->
        ImageLightboxDialog(base64 = b64, title = title, onDismiss = { lightboxImg = null })
    }
}

@Composable
private fun SaleReportDialog(
    order: PharmacyOrder,
    existing: OrderFinancial?,
    strings: AppStrings,
    onDismiss: () -> Unit,
    onSave: (Double, Double, Double, Double, Double, Double, Double, String, String) -> Unit
) {
    var bill by remember { mutableStateOf((existing?.billValue ?: order.quotedBillAmount).takeIf { it > 0 }?.toString() ?: "") }
    var disc by remember { mutableStateOf((existing?.discountAmount ?: 0.0).toString()) }
    var ref by remember { mutableStateOf((existing?.refundAmount ?: 0.0).toString()) }
    var cost by remember { mutableStateOf((existing?.costValue ?: 0.0).takeIf { it > 0 }?.toString() ?: "") }
    var dist by remember { mutableStateOf((existing?.deliveryDistanceKm ?: order.estimatedDistanceKm).toString()) }
    var delChg by remember { mutableStateOf((existing?.deliveryChargeCollected ?: order.quotedDeliveryCharge).toString()) }
    var delExp by remember { mutableStateOf((existing?.deliveryExpensePaid ?: 0.0).toString()) }
    var notes by remember { mutableStateOf(existing?.internalNotes ?: "") }
    var reason by remember { mutableStateOf(if (existing == null) "Initial bill entry" else "Updated sale figures") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confidential Sale Report — ${order.orderNumber}") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(value = bill, onValueChange = { bill = it }, label = { Text(strings.billValue) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                OutlinedTextField(value = disc, onValueChange = { disc = it }, label = { Text(strings.discountAmount) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                OutlinedTextField(value = ref, onValueChange = { ref = it }, label = { Text(strings.refundAmount) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                OutlinedTextField(value = cost, onValueChange = { cost = it }, label = { Text(strings.costValue) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                OutlinedTextField(value = dist, onValueChange = { dist = it }, label = { Text(strings.deliveryDistanceKm) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                OutlinedTextField(value = delChg, onValueChange = { delChg = it }, label = { Text(strings.deliveryChargeCollected) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                OutlinedTextField(value = delExp, onValueChange = { delExp = it }, label = { Text(strings.deliveryExpensePaid) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text(strings.internalNotes) })
                OutlinedTextField(value = reason, onValueChange = { reason = it }, label = { Text("Audit Reason") })
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(
                    bill.toDoubleOrNull() ?: 0.0,
                    disc.toDoubleOrNull() ?: 0.0,
                    ref.toDoubleOrNull() ?: 0.0,
                    cost.toDoubleOrNull() ?: 0.0,
                    dist.toDoubleOrNull() ?: 0.0,
                    delChg.toDoubleOrNull() ?: 0.0,
                    delExp.toDoubleOrNull() ?: 0.0,
                    notes.trim(),
                    reason.trim()
                )
            }) { Text(strings.saveFinancials) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun PickDeliveryPersonDialog(
    title: String,
    staff: List<DeliveryPerson>,
    onDismiss: () -> Unit,
    onSelect: (DeliveryPerson) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            if (staff.isEmpty()) {
                Text("No active delivery staff found. Add one in Manage > Delivery Staff.")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    staff.forEach { dp ->
                        FilledTonalButton(
                            onClick = { onSelect(dp) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("${dp.name} (${dp.mobile})")
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
private fun ManualOrderDialog(
    customers: List<CustomerProfile>,
    onDismiss: () -> Unit,
    onCreate: (CustomerProfile, String, Boolean) -> Unit
) {
    var selectedCust by remember { mutableStateOf(customers.firstOrNull()) }
    var items by remember { mutableStateOf("") }
    var rxReq by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Manual Order") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Customer:", style = MaterialTheme.typography.labelLarge)
                if (customers.isEmpty()) {
                    Text("No registered customers yet.", color = MaterialTheme.colorScheme.error)
                } else {
                    customers.take(8).forEach { c ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable { selectedCust = c }
                        ) {
                            FilterChip(
                                selected = selectedCust?.userId == c.userId,
                                onClick = { selectedCust = c },
                                label = { Text("${c.name} (${c.phone})") }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = items,
                    onValueChange = { items = it },
                    label = { Text("Medicines & Quantities *") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = rxReq, onCheckedChange = { rxReq = it })
                    Text("Prescription required")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { selectedCust?.let { onCreate(it, items.trim(), rxReq) } },
                enabled = selectedCust != null && items.isNotBlank()
            ) { Text("Create Order") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
