package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.localization.AppLanguage
import com.example.localization.AppStrings
import com.example.model.OrderStatus
import com.example.model.RecipientNotification
import com.example.model.SalesTrendPoint
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.EmeraldSecondary
import com.example.ui.theme.SaffronAccent
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusInfo
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.util.MediaUtils
import com.example.util.TimezoneUtils

@Composable
fun OfflineBanner(isOnline: Boolean, strings: AppStrings) {
    if (!isOnline) {
        Surface(
            color = StatusWarning,
            contentColor = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("offline_banner")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.WifiOff, contentDescription = "Offline", modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(strings.offlineBanner, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun LanguageToggleChip(
    language: AppLanguage,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onToggle,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .testTag("language_toggle_button")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Language, contentDescription = "Switch Language", modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                text = if (language == AppLanguage.ENGLISH) "EN / हिं" else "हिं / EN",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
fun NotificationBellButton(
    unreadCount: Int,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag("notifications_bell_button")
    ) {
        BadgedBox(
            badge = {
                if (unreadCount > 0) {
                    Badge { Text(unreadCount.coerceAtMost(99).toString()) }
                }
            }
        ) {
            Icon(Icons.Default.Campaign, contentDescription = "Announcements")
        }
    }
}

@Composable
fun StatusPill(statusRaw: String, isHindi: Boolean = false) {
    val st = OrderStatus.fromRaw(statusRaw)
    val (bg, fg) = when (st) {
        OrderStatus.NEW, OrderStatus.PENDING -> SaffronAccent.copy(alpha = 0.16f) to SaffronAccent
        OrderStatus.PROCESSING, OrderStatus.READY -> StatusInfo.copy(alpha = 0.16f) to StatusInfo
        OrderStatus.ASSIGNED, OrderStatus.PICKED_UP, OrderStatus.OUT_FOR_DELIVERY -> EmeraldSecondary.copy(alpha = 0.16f) to EmeraldSecondary
        OrderStatus.DELIVERED -> StatusSuccess.copy(alpha = 0.16f) to StatusSuccess
        OrderStatus.CANCELLED -> StatusError.copy(alpha = 0.16f) to StatusError
    }
    Surface(
        shape = RoundedCornerShape(50),
        color = bg,
        contentColor = fg
    ) {
        Text(
            text = if (isHindi) st.labelHi else st.labelEn,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun ImageLightboxDialog(
    base64: String,
    title: String,
    onDismiss: () -> Unit
) {
    val bmp = remember(base64) { MediaUtils.base64ToBitmap(base64) }
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(20.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close preview")
                    }
                }
                Spacer(Modifier.height(8.dp))
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(380.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Text("Unable to decode image preview.", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnouncementInboxSheet(
    strings: AppStrings,
    notifications: List<RecipientNotification>,
    allowReplyInChat: Boolean,
    notificationsEnabled: Boolean = true,
    onToggleNotifications: ((Boolean) -> Unit)? = null,
    onMarkOpened: (String) -> Unit,
    onMarkRead: (String) -> Unit,
    onReplyInChat: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var replyingTo by remember { mutableStateOf<RecipientNotification?>(null) }
    var replyText by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(strings.announcementsInbox, style = MaterialTheme.typography.headlineSmall)
                Text(
                    "${notifications.count { it.status != "READ" }} ${strings.unreadCount}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (onToggleNotifications != null) {
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Push & Alert Notifications", style = MaterialTheme.typography.labelLarge)
                    Switch(
                        checked = notificationsEnabled,
                        onCheckedChange = onToggleNotifications
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            if (notifications.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No announcements yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.height(420.dp)
                ) {
                    items(notifications, key = { it.id }) { item ->
                        val isUnread = item.status != "READ"
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isUnread) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (item.status == "PENDING" || item.status == "SENT_TO_FCM" || item.status == "FAILED") {
                                        onMarkOpened(item.id)
                                    }
                                }
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        item.category,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "${item.status} • ${TimezoneUtils.formatDateTimeDisplay(item.createdAt)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(item.title, style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.height(4.dp))
                                Text(item.message, style = MaterialTheme.typography.bodyMedium)
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (item.status != "READ") {
                                        FilledTonalButton(
                                            onClick = { onMarkRead(item.id) },
                                            modifier = Modifier.testTag("mark_read_${item.id}")
                                        ) {
                                            Icon(Icons.Default.MarkEmailRead, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Mark Read")
                                        }
                                    } else {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusSuccess, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Read", style = MaterialTheme.typography.labelSmall, color = StatusSuccess)
                                        }
                                    }
                                    if (allowReplyInChat) {
                                        TextButton(onClick = { replyingTo = item }) {
                                            Text("Reply in Chat")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (replyingTo != null) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = replyText,
                    onValueChange = { replyText = it },
                    label = { Text("Reply to: ${replyingTo!!.title}") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = { replyingTo = null; replyText = "" }) { Text("Cancel") }
                    Button(
                        onClick = {
                            if (replyText.isNotBlank()) {
                                onReplyInChat(replyingTo!!.title, replyText.trim())
                                onMarkRead(replyingTo!!.id)
                                replyingTo = null
                                replyText = ""
                                onDismiss()
                            }
                        }
                    ) { Text("Send to Manager") }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun SalesAndProfitChart(
    points: List<SalesTrendPoint>,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(160.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("No sales trend data in selected range", style = MaterialTheme.typography.bodyMedium)
        }
        return
    }
    Column(modifier = modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).background(EmeraldPrimary, CircleShape))
                Spacer(Modifier.width(4.dp))
                Text("Net Sales", style = MaterialTheme.typography.labelSmall)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).background(SaffronAccent, CircleShape))
                Spacer(Modifier.width(4.dp))
                Text("Net Profit", style = MaterialTheme.typography.labelSmall)
            }
        }
        Spacer(Modifier.height(8.dp))
        val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                .padding(14.dp)
        ) {
            val maxVal = points.maxOf { maxOf(it.netSales, it.netProfit) }.coerceAtLeast(100.0).toFloat()
            val count = points.size
            val slotW = size.width / count.coerceAtLeast(1)
            val barW = (slotW * 0.34f).coerceAtMost(28f)

            for (i in 0..3) {
                val y = size.height * (i / 3f)
                drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.5f)
            }

            val profitPath = Path()
            points.forEachIndexed { idx, pt ->
                val centerX = idx * slotW + slotW / 2f
                val salesH = ((pt.netSales.toFloat() / maxVal) * size.height).coerceIn(4f, size.height)
                drawRect(
                    color = EmeraldPrimary,
                    topLeft = Offset(centerX - barW / 2f, size.height - salesH),
                    size = Size(barW, salesH)
                )
                val profitY = (size.height - ((pt.netProfit.toFloat() / maxVal) * size.height)).coerceIn(0f, size.height)
                if (idx == 0) profitPath.moveTo(centerX, profitY) else profitPath.lineTo(centerX, profitY)
                drawCircle(SaffronAccent, radius = 6f, center = Offset(centerX, profitY))
            }
            if (points.size > 1) {
                drawPath(profitPath, color = SaffronAccent, style = Stroke(width = 4f))
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            points.takeLast(6).forEach {
                Text(it.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
