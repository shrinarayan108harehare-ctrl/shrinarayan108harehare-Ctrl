package com.example.ui.delivery

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.localization.AppLanguage
import com.example.localization.AppStrings
import com.example.model.DeliveryPerson
import com.example.model.OrderStatus
import com.example.model.PharmacyOrder
import com.example.model.RecipientNotification
import com.example.model.UiState
import com.example.ui.components.AnnouncementInboxSheet
import com.example.ui.components.LanguageToggleChip
import com.example.ui.components.NotificationBellButton
import com.example.ui.components.OfflineBanner
import com.example.ui.components.StatusPill
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.SaffronAccent
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import com.example.util.LocationUtils
import com.example.viewmodel.DeliveryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryDashboardScreen(
    vm: DeliveryViewModel,
    deliveryPerson: DeliveryPerson,
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
    val context = LocalContext.current
    val ordersState by vm.assignedOrdersState.collectAsStateWithLifecycle()
    val routePlans by vm.routePlans.collectAsStateWithLifecycle()
    val isTracking by vm.isTracking.collectAsStateWithLifecycle()
    val feedback by vm.feedback.collectAsStateWithLifecycle()

    var showAnnouncements by remember { mutableStateOf(false) }
    var issueOrderTarget by remember { mutableStateOf<PharmacyOrder?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(feedback) {
        val msg = feedback
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            vm.clearFeedback()
        }
    }

    val locPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        if (perms.values.any { it }) {
            vm.startShiftTracking()
        }
    }

    val unreadNotifs = notifications.count { it.status != "READ" }
    val isHindi = language == AppLanguage.HINDI

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(strings.appTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("${deliveryPerson.name} • Delivery Partner", style = MaterialTheme.typography.labelSmall, color = EmeraldPrimary)
                        }
                    },
                    actions = {
                        LanguageToggleChip(language = language, onToggle = onToggleLanguage)
                        NotificationBellButton(unreadCount = unreadNotifs) { showAnnouncements = true }
                        IconButton(onClick = onSignOut, modifier = Modifier.testTag("delivery_sign_out_btn")) {
                            Icon(Icons.Default.Logout, contentDescription = strings.signOut)
                        }
                    }
                )
                OfflineBanner(isOnline = isOnline, strings = strings)
            }
        }
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Consent-Gated Active Shift GPS Tracking Banner
            item {
                Surface(
                    color = if (isTracking) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (isTracking) Color.White else MaterialTheme.colorScheme.onSurface,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().testTag("delivery_shift_tracking_card")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.GpsFixed, contentDescription = null)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    if (isTracking) strings.shiftTrackingOn else strings.shiftTrackingOff,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Explicit shift tracking: updates manager every 20s only while ON. Automatically stops when switched OFF.",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                        Switch(
                            checked = isTracking,
                            onCheckedChange = { enable ->
                                if (enable) {
                                    locPermLauncher.launch(
                                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                                    )
                                } else {
                                    vm.stopShiftTracking()
                                }
                            },
                            modifier = Modifier.testTag("delivery_shift_switch")
                        )
                    }
                }
            }

            // Shared Multi-Stop Route Plans
            if (routePlans.isNotEmpty()) {
                item {
                    Text("Assigned Route Plans", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                items(routePlans, key = { it.id }) { plan ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Route, contentDescription = null, tint = EmeraldPrimary)
                                Spacer(Modifier.width(6.dp))
                                Text(plan.name, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                "${plan.stopLabels.size} stops • ${"%.1f".format(plan.totalEstimatedDistanceKm)} km • ~${plan.totalEstimatedMinutes} min",
                                style = MaterialTheme.typography.bodySmall
                            )
                            plan.stopLabels.forEachIndexed { idx, lbl ->
                                Text("${idx + 1}. $lbl", style = MaterialTheme.typography.labelSmall)
                            }
                            if (plan.stopLats.isNotEmpty() && plan.stopLats.size == plan.stopLngs.size) {
                                FilledTonalButton(
                                    onClick = {
                                        val stops = plan.stopLats.indices.map { i ->
                                            PharmacyOrder(
                                                deliveryLat = plan.stopLats[i],
                                                deliveryLng = plan.stopLngs[i]
                                            )
                                        }
                                        LocationUtils.launchMultiStopNavigation(context, stops.first().deliveryLat, stops.first().deliveryLng, stops)
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Navigation, contentDescription = null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Start Route Navigation in Google Maps")
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text("My Assigned Deliveries", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            when (val st = ordersState) {
                is UiState.Loading -> item {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is UiState.Error -> item {
                    Text(st.message, color = MaterialTheme.colorScheme.error)
                }
                is UiState.Success -> {
                    if (st.data.isEmpty()) {
                        item {
                            Card {
                                Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.DeliveryDining, contentDescription = null, modifier = Modifier.size(40.dp), tint = EmeraldPrimary)
                                    Spacer(Modifier.height(8.dp))
                                    Text("No assigned orders right now.", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    } else {
                        items(st.data, key = { it.id }) { order ->
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("${order.orderNumber} • ${order.customerName}", fontWeight = FontWeight.Bold)
                                        StatusPill(order.status, isHindi)
                                    }
                                    Text("Items: ${order.itemsSummary}", style = MaterialTheme.typography.bodyMedium)
                                    Text("Address: ${order.deliveryAddress} (Landmark: ${order.deliveryLandmark})", style = MaterialTheme.typography.bodySmall)
                                    if (order.quotedBillAmount > 0.0) {
                                        Text("Collect Bill: ₹${"%.0f".format(order.quotedBillAmount)}", fontWeight = FontWeight.Bold, color = SaffronAccent)
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                        OutlinedButton(
                                            onClick = {
                                                runCatching {
                                                    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${order.customerPhone}")))
                                                }
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Call", maxLines = 1)
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                LocationUtils.openSingleLocationInGoogleMaps(context, order.deliveryLat, order.deliveryLng, order.customerName)
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Map", maxLines = 1)
                                        }
                                        Button(
                                            onClick = { LocationUtils.launchNavigationTo(context, order.deliveryLat, order.deliveryLng) },
                                            modifier = Modifier.weight(1f).testTag("dp_nav_${order.id}")
                                        ) {
                                            Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text(strings.navigate, maxLines = 1)
                                        }
                                    }

                                    if (order.status != OrderStatus.DELIVERED.wireValue && order.status != OrderStatus.CANCELLED.wireValue) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                                            if (order.status == OrderStatus.ASSIGNED.wireValue) {
                                                FilledTonalButton(
                                                    onClick = { vm.updateOrderStatus(order, OrderStatus.PICKED_UP) },
                                                    modifier = Modifier.weight(1f)
                                                ) { Text(strings.markPickedUp, maxLines = 1) }
                                            }
                                            if (order.status in listOf(OrderStatus.ASSIGNED.wireValue, OrderStatus.PICKED_UP.wireValue)) {
                                                FilledTonalButton(
                                                    onClick = { vm.updateOrderStatus(order, OrderStatus.OUT_FOR_DELIVERY) },
                                                    modifier = Modifier.weight(1f)
                                                ) { Text(strings.markOutForDelivery, maxLines = 1) }
                                            }
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                            Button(
                                                onClick = { vm.updateOrderStatus(order, OrderStatus.DELIVERED) },
                                                colors = ButtonDefaults.buttonColors(containerColor = StatusSuccess),
                                                modifier = Modifier.weight(1f).testTag("dp_delivered_${order.id}")
                                            ) {
                                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text(strings.markDelivered, maxLines = 1)
                                            }
                                            OutlinedButton(
                                                onClick = { issueOrderTarget = order },
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusError),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(Icons.Default.ReportProblem, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text(strings.reportIssue, maxLines = 1)
                                            }
                                        }
                                    }
                                    if (order.deliveryIssue.isNotBlank()) {
                                        Text("Reported Issue: ${order.deliveryIssue}", color = StatusError, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    issueOrderTarget?.let { ord ->
        var issueText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { issueOrderTarget = null },
            title = { Text("Report Issue — ${ord.orderNumber}") },
            text = {
                OutlinedTextField(
                    value = issueText,
                    onValueChange = { issueText = it },
                    label = { Text("Describe issue (e.g., Customer unreachable, closed gate)") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    enabled = issueText.isNotBlank(),
                    onClick = {
                        vm.updateOrderStatus(ord, OrderStatus.fromRaw(ord.status), issueReport = issueText.trim())
                        issueOrderTarget = null
                    }
                ) { Text("Submit Issue") }
            },
            dismissButton = { TextButton(onClick = { issueOrderTarget = null }) { Text("Cancel") } }
        )
    }

    if (showAnnouncements) {
        AnnouncementInboxSheet(
            strings = strings,
            notifications = notifications,
            allowReplyInChat = false,
            notificationsEnabled = notificationsEnabled,
            onToggleNotifications = onToggleNotifications,
            onMarkOpened = onMarkNotificationOpened,
            onMarkRead = onMarkNotificationRead,
            onReplyInChat = { _, _ -> },
            onDismiss = { showAnnouncements = false }
        )
    }
}
