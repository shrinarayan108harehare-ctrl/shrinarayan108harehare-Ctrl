package com.example.ui.manager

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AiImageResolution
import com.example.localization.AppStrings
import com.example.model.Advertisement
import com.example.model.BusinessSetting
import com.example.model.DeliveryPerson
import com.example.model.ManagerAccount
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.SaffronAccent
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import com.example.util.ReportExporter
import com.example.util.TimezoneUtils
import com.example.viewmodel.ManagerViewModel

@Composable
fun ManagerCommsAndSettingsScreen(
    vm: ManagerViewModel,
    currentManager: ManagerAccount,
    strings: AppStrings
) {
    val isOwner = currentManager.roleType == "owner"
    val subTabs = buildList {
        add("Announcements")
        add("Ads & AI")
        add("Delivery Staff")
        add("Expenses")
        add("Settings")
        if (isOwner) {
            add("Managers")
            add("Audit Log")
        }
    }
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        ScrollableTabRow(selectedTabIndex = selectedTab.coerceAtMost(subTabs.lastIndex)) {
            subTabs.forEachIndexed { idx, title ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    text = { Text(title, maxLines = 1) },
                    modifier = Modifier.testTag("manage_subtab_$idx")
                )
            }
        }

        when (selectedTab) {
            0 -> AnnouncementsManagerPane(vm, strings)
            1 -> AdvertisementsManagerPane(vm)
            2 -> DeliveryStaffManagerPane(vm)
            3 -> BusinessExpensesPane(vm)
            4 -> ShopSettingsAndSecurityPane(vm)
            5 -> if (isOwner) ManageFamilyManagersPane(vm, strings)
            6 -> if (isOwner) OwnerAuditLogPane(vm, strings)
        }
    }
}

@Composable
private fun AnnouncementsManagerPane(vm: ManagerViewModel, strings: AppStrings) {
    val customers by vm.customers.collectAsStateWithLifecycle()
    val announcements by vm.announcements.collectAsStateWithLifecycle()
    val allNotifs by vm.allNotifications.collectAsStateWithLifecycle()

    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("GENERAL") }
    var audience by remember { mutableStateOf("ALL_CUSTOMERS") }
    var selectedCustomerIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    LazyColumn(
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Campaign, contentDescription = null, tint = EmeraldPrimary)
                        Spacer(Modifier.width(8.dp))
                        Text(strings.announcement, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Announcement Title *") }, modifier = Modifier.fillMaxWidth().testTag("ann_title_input"))
                    OutlinedTextField(value = message, onValueChange = { message = it }, label = { Text("Announcement Message *") }, minLines = 2, modifier = Modifier.fillMaxWidth().testTag("ann_msg_input"))
                    Text("Category:", style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(listOf("GENERAL", "URGENT", "FESTIVAL", "DELIVERY_UPDATE")) { cat ->
                            FilterChip(selected = category == cat, onClick = { category = cat }, label = { Text(cat) })
                        }
                    }
                    Text("Audience:", style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(listOf("ALL_CUSTOMERS", "ALL_DELIVERY", "ALL_USERS", "SELECTED_CUSTOMERS")) { aud ->
                            FilterChip(selected = audience == aud, onClick = { audience = aud }, label = { Text(aud) })
                        }
                    }
                    if (audience == "SELECTED_CUSTOMERS") {
                        customers.take(10).forEach { c ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = c.userId in selectedCustomerIds,
                                    onCheckedChange = { ok ->
                                        selectedCustomerIds = if (ok) selectedCustomerIds + c.userId else selectedCustomerIds - c.userId
                                    }
                                )
                                Text("${c.name} (${c.phone})")
                            }
                        }
                    }
                    Button(
                        onClick = {
                            vm.sendAnnouncement(title.trim(), message.trim(), category, audience, selectedCustomerIds.toList())
                            title = ""; message = ""
                        },
                        enabled = title.isNotBlank() && message.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().testTag("send_announcement_btn")
                    ) { Text("Send Announcement & Create Inbox Records") }
                }
            }
        }
        item { Text("Announcement History & Delivery Status", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        items(announcements, key = { it.id }) { ann ->
            val recips = allNotifs.filter { it.announcementId == ann.id }
            val pending = recips.count { it.status == "PENDING" }
            val sent = recips.count { it.status == "SENT_TO_FCM" }
            val failed = recips.count { it.status == "FAILED" }
            val opened = recips.count { it.status == "OPENED" }
            val read = recips.count { it.status == "READ" }
            Card {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(ann.title, fontWeight = FontWeight.Bold)
                        Text(ann.category, style = MaterialTheme.typography.labelSmall, color = SaffronAccent)
                    }
                    Text(ann.message, style = MaterialTheme.typography.bodySmall)
                    Text(
                        "Targets: ${ann.recipientCount} | Pending: $pending • Sent: $sent • Failed: $failed • Opened: $opened • Read: $read",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun AdvertisementsManagerPane(vm: ManagerViewModel) {
    val ads by vm.advertisements.collectAsStateWithLifecycle()
    val bannerBmp by vm.aiBannerBitmap.collectAsStateWithLifecycle()
    val aiBusy by vm.aiBusy.collectAsStateWithLifecycle()

    var title by remember { mutableStateOf("") }
    var subtitle by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("OFFER") }
    var preset by remember { mutableStateOf("hero_pharmacy") }
    var linkUrl by remember { mutableStateOf("") }
    var aiPrompt by remember { mutableStateOf("Indian community pharmacy monsoon immunity vitamins and fast home delivery poster, emerald green and warm saffron aesthetic") }
    var selectedRes by remember { mutableStateOf(AiImageResolution.RES_1K) }

    LazyColumn(
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Create / Publish Customer Ad Banner", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Banner Title *") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = subtitle, onValueChange = { subtitle = it }, label = { Text("Subtitle / Offer Details") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = linkUrl, onValueChange = { linkUrl = it }, label = { Text("Clickable Link (Optional URL)") }, modifier = Modifier.fillMaxWidth())
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(listOf("OFFER", "CATEGORY", "FESTIVAL", "DELIVERY")) { c ->
                            FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c) })
                        }
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(listOf("hero_pharmacy", "promo_delivery", "promo_festival")) { p ->
                            FilterChip(selected = preset == p, onClick = { preset = p }, label = { Text(p) })
                        }
                    }

                    // Gemini 3 Pro Image Preview Studio with 1K/2K/4K resolution selector
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(12.dp)) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("AI Promo Poster Generator (gemini-3-pro-image-preview)", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            OutlinedTextField(value = aiPrompt, onValueChange = { aiPrompt = it }, label = { Text("Poster Prompt") }, modifier = Modifier.fillMaxWidth())
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                AiImageResolution.entries.forEach { res ->
                                    FilterChip(
                                        selected = selectedRes == res,
                                        onClick = { selectedRes = res },
                                        label = { Text(res.label) },
                                        modifier = Modifier.testTag("ai_img_res_${res.label}")
                                    )
                                }
                            }
                            FilledTonalButton(
                                onClick = { vm.generateAiPromoBanner(aiPrompt, selectedRes) },
                                enabled = !aiBusy && aiPrompt.isNotBlank(),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (aiBusy) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                                else {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Generate ${selectedRes.label} Ad Image")
                                }
                            }
                            bannerBmp?.let { bmp ->
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "Generated AI Promo Banner",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(10.dp))
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            val encoded = bannerBmp?.let { vm.encodeBannerBitmap(it) }.orEmpty()
                            vm.saveAd(null, title.trim(), subtitle.trim(), category, preset, encoded, linkUrl.trim(), true, 1L)
                            title = ""; subtitle = ""; linkUrl = ""
                        },
                        enabled = title.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Publish Advertisement") }
                }
            }
        }
        items(ads, key = { it.id }) { ad ->
            Card {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(ad.title, fontWeight = FontWeight.Bold)
                        Text("${ad.category} • ${ad.subtitle}", style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(
                        checked = ad.active,
                        onCheckedChange = { active ->
                            vm.saveAd(ad.id, ad.title, ad.subtitle, ad.category, ad.imagePreset, ad.imageBase64, ad.linkUrl, active, ad.priority)
                        }
                    )
                    IconButton(onClick = { vm.deleteAd(ad) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Ad", tint = StatusError)
                    }
                }
            }
        }
    }
}

@Composable
private fun DeliveryStaffManagerPane(vm: ManagerViewModel) {
    val staff by vm.deliveryPersonnel.collectAsStateWithLifecycle()
    var editTarget by remember { mutableStateOf<DeliveryPerson?>(null) }
    var showCreate by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Button(onClick = { showCreate = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Add Delivery Person Account")
            }
        }
        items(staff, key = { it.id }) { dp ->
            Card {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.DeliveryDining, contentDescription = null, tint = EmeraldPrimary)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(dp.name, fontWeight = FontWeight.Bold)
                            Text("User: ${dp.username} • ${dp.mobile}", style = MaterialTheme.typography.bodySmall)
                            Text(if (dp.active) "Active" else "Deactivated", style = MaterialTheme.typography.labelSmall, color = if (dp.active) StatusSuccess else StatusError)
                        }
                    }
                    FilledTonalButton(onClick = { editTarget = dp }) { Text("Edit / Reset") }
                }
            }
        }
    }

    if (showCreate || editTarget != null) {
        val target = editTarget
        var name by remember(target) { mutableStateOf(target?.name ?: "") }
        var mobile by remember(target) { mutableStateOf(target?.mobile ?: "") }
        var username by remember(target) { mutableStateOf(target?.username ?: "") }
        var password by remember(target) { mutableStateOf("") }
        var active by remember(target) { mutableStateOf(target?.active ?: true) }

        AlertDialog(
            onDismissRequest = { showCreate = false; editTarget = null },
            title = { Text(if (target == null) "New Delivery Person" else "Edit ${target.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name *") })
                    OutlinedTextField(value = mobile, onValueChange = { mobile = it }, label = { Text("Mobile *") })
                    OutlinedTextField(value = username, onValueChange = { username = it }, enabled = target == null, label = { Text("Username *") })
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(if (target == null) "Password *" else "Reset Password (leave blank to keep)") },
                        visualTransformation = PasswordVisualTransformation()
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = active, onCheckedChange = { active = it })
                        Text("Account Active")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        vm.upsertDeliveryPerson(target, name.trim(), mobile.trim(), username.trim(), password.takeIf { it.isNotBlank() }, active)
                        showCreate = false; editTarget = null
                    },
                    enabled = name.isNotBlank() && username.isNotBlank() && (target != null || password.isNotBlank())
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showCreate = false; editTarget = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun BusinessExpensesPane(vm: ManagerViewModel) {
    val expenses by vm.expenses.collectAsStateWithLifecycle()
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("FUEL") }
    var amount by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    LazyColumn(
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Payments, contentDescription = null, tint = SaffronAccent)
                        Spacer(Modifier.width(8.dp))
                        Text("Record Business Expense", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Expense Title *") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount (₹) *") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(listOf("FUEL", "SALARY", "RENT", "UTILITIES", "PACKAGING", "OTHER")) { cat ->
                            FilterChip(selected = category == cat, onClick = { category = cat }, label = { Text(cat) })
                        }
                    }
                    OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth())
                    Button(
                        onClick = {
                            vm.addExpense(title.trim(), category, amount.toDoubleOrNull() ?: 0.0, TimezoneUtils.todayDateKey(), notes.trim())
                            title = ""; amount = ""; notes = ""
                        },
                        enabled = title.isNotBlank() && (amount.toDoubleOrNull() ?: 0.0) > 0.0,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Add Expense") }
                }
            }
        }
        items(expenses, key = { it.id }) { exp ->
            Card {
                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(exp.title, fontWeight = FontWeight.Bold)
                        Text("${exp.category} • ${exp.expenseDateKey} • By ${exp.recordedByManagerName}", style = MaterialTheme.typography.bodySmall)
                    }
                    Text("₹${"%.0f".format(exp.amount)}", fontWeight = FontWeight.Bold, color = StatusError)
                }
            }
        }
    }
}

@Composable
private fun ShopSettingsAndSecurityPane(vm: ManagerViewModel) {
    val context = LocalContext.current
    val settings by vm.shopSettings.collectAsStateWithLifecycle()

    var shopName by remember(settings) { mutableStateOf(settings?.shopName ?: "PRAYAG DEEN & SONS") }
    var phone by remember(settings) { mutableStateOf(settings?.phone ?: "") }
    var address by remember(settings) { mutableStateOf(settings?.address ?: "") }
    var hours by remember(settings) { mutableStateOf(settings?.hours ?: "8:00 AM - 10:30 PM") }
    var baseFee by remember(settings) { mutableStateOf((settings?.baseDeliveryCharge ?: 20.0).toString()) }
    var freeAbove by remember(settings) { mutableStateOf((settings?.freeDeliveryAbove ?: 500.0).toString()) }
    var acctMethod by remember(settings) { mutableStateOf(settings?.accountingMethod ?: "ACCRUAL") }
    var newPassword by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Settings, contentDescription = null, tint = EmeraldPrimary)
                    Spacer(Modifier.width(8.dp))
                    Text("Pharmacy & Delivery Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                OutlinedTextField(value = shopName, onValueChange = { shopName = it }, label = { Text("Shop Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Support Phone") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Shop Address") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = hours, onValueChange = { hours = it }, label = { Text("Operating Hours") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = baseFee, onValueChange = { baseFee = it }, label = { Text("Base Delivery ₹") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = freeAbove, onValueChange = { freeAbove = it }, label = { Text("Free Above ₹") }, modifier = Modifier.weight(1f))
                }
                Text("Accounting Method:", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("ACCRUAL", "CASH").forEach { m ->
                        FilterChip(selected = acctMethod == m, onClick = { acctMethod = m }, label = { Text(m) })
                    }
                }
                Button(
                    onClick = {
                        vm.saveSettings(
                            BusinessSetting(
                                shopName = shopName.trim(),
                                phone = phone.trim(),
                                address = address.trim(),
                                hours = hours.trim(),
                                baseDeliveryCharge = baseFee.toDoubleOrNull() ?: 20.0,
                                freeDeliveryAbove = freeAbove.toDoubleOrNull() ?: 500.0,
                                shopLat = settings?.shopLat ?: 26.8467,
                                shopLng = settings?.shopLng ?: 80.9462,
                                accountingMethod = acctMethod
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Save Pharmacy Settings") }
            }
        }

        Card {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LockReset, contentDescription = null, tint = SaffronAccent)
                    Spacer(Modifier.width(8.dp))
                    Text("Change Your Manager Password", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("New Password (min 4 chars)") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth().testTag("change_own_pwd_input")
                )
                FilledTonalButton(
                    onClick = {
                        vm.changeOwnPassword(newPassword)
                        newPassword = ""
                    },
                    enabled = newPassword.length >= 4,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Update Password") }
            }
        }

        OutlinedButton(
            onClick = { vm.exportBackupJson { json -> ReportExporter.exportBusinessBackupJson(context, json) } },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Backup, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Export Full Business JSON Backup")
        }
    }
}

@Composable
private fun ManageFamilyManagersPane(vm: ManagerViewModel, strings: AppStrings) {
    val managers by vm.managers.collectAsStateWithLifecycle()
    var editTarget by remember { mutableStateOf<ManagerAccount?>(null) }
    var showCreate by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = EmeraldPrimary)
                Spacer(Modifier.width(8.dp))
                Text(strings.manageManagers, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Text("Owner Super Admin + Family Manager accounts (Father, Me, Elder Uncle, Brother).", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { showCreate = true },
                modifier = Modifier.fillMaxWidth().testTag("add_family_manager_btn")
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Add Family Manager")
            }
        }
        items(managers, key = { it.id }) { m ->
            Card {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${m.name} (${m.username})", fontWeight = FontWeight.Bold)
                        Text("Role: ${m.roleType} • Last login: ${TimezoneUtils.formatDateTimeDisplay(m.lastLoginAt)}", style = MaterialTheme.typography.bodySmall)
                        Text(if (m.active) "Active" else "Deactivated", style = MaterialTheme.typography.labelSmall, color = if (m.active) StatusSuccess else StatusError)
                    }
                    FilledTonalButton(onClick = { editTarget = m }, modifier = Modifier.testTag("edit_mgr_${m.id}")) {
                        Text("Manage")
                    }
                }
            }
        }
    }

    if (showCreate) {
        var name by remember { mutableStateOf("") }
        var mobile by remember { mutableStateOf("") }
        var username by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text("Add Family Manager") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name *") })
                    OutlinedTextField(value = mobile, onValueChange = { mobile = it }, label = { Text("Mobile *") })
                    OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Username (e.g., uncle2) *") })
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Initial Password (min 4 chars) *") },
                        visualTransformation = PasswordVisualTransformation()
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = name.isNotBlank() && username.isNotBlank() && password.length >= 4,
                    onClick = {
                        vm.createManagerByOwner(name.trim(), mobile.trim(), username.trim(), password)
                        showCreate = false
                    }
                ) { Text("Create Manager") }
            },
            dismissButton = { TextButton(onClick = { showCreate = false }) { Text("Cancel") } }
        )
    }

    editTarget?.let { target ->
        var name by remember(target) { mutableStateOf(target.name) }
        var mobile by remember(target) { mutableStateOf(target.mobile) }
        var password by remember(target) { mutableStateOf("") }
        var active by remember(target) { mutableStateOf(target.active) }

        AlertDialog(
            onDismissRequest = { editTarget = null },
            title = { Text("Manage ${target.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Display Name") })
                    OutlinedTextField(value = mobile, onValueChange = { mobile = it }, label = { Text("Mobile") })
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Reset Password (optional)") },
                        visualTransformation = PasswordVisualTransformation()
                    )
                    if (target.roleType != "owner") {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = active, onCheckedChange = { active = it })
                            Text("Account Active")
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    vm.updateManagerByOwner(target, name.trim(), mobile.trim(), password.takeIf { it.isNotBlank() }, active)
                    editTarget = null
                }) { Text("Save Changes") }
            },
            dismissButton = { TextButton(onClick = { editTarget = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun OwnerAuditLogPane(vm: ManagerViewModel, strings: AppStrings) {
    val context = LocalContext.current
    val logs by vm.auditLogs.collectAsStateWithLifecycle()
    val managers by vm.managers.collectAsStateWithLifecycle()
    val settings by vm.shopSettings.collectAsStateWithLifecycle()

    var filterManagerId by remember { mutableStateOf("ALL") }
    var filterAction by remember { mutableStateOf("ALL") }
    var search by remember { mutableStateOf("") }

    val filtered = logs.filter { log ->
        val mOk = filterManagerId == "ALL" || log.managerId == filterManagerId
        val aOk = filterAction == "ALL" || log.actionPerformed.contains(filterAction, ignoreCase = true)
        val q = search.trim().lowercase()
        val sOk = q.isBlank() ||
            log.recordId.lowercase().contains(q) ||
            log.managerName.lowercase().contains(q) ||
            log.actionPerformed.lowercase().contains(q) ||
            log.dateKey.lowercase().contains(q)
        mOk && aOk && sOk
    }

    val totalFinancialChanges = logs.count { it.actionPerformed.contains("BILL") || it.actionPerformed.contains("COST") || it.actionPerformed.contains("FINANCIAL") }
    val totalCancellations = logs.count { it.actionPerformed.contains("CANCEL") }
    val todayChanges = logs.count { it.dateKey == TimezoneUtils.todayDateKey() }

    LazyColumn(
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.History, contentDescription = null, tint = EmeraldPrimary)
                Spacer(Modifier.width(8.dp))
                Text(strings.auditLogTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Text("Append-only tamper-evident ledger (Old vs New values + SHA-256 hash chain).", style = MaterialTheme.typography.bodySmall)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Surface(color = EmeraldPrimary.copy(alpha = 0.08f), shape = RoundedCornerShape(10.dp), modifier = Modifier.weight(1f)) {
                    Column(Modifier.padding(10.dp)) {
                        Text("Today", style = MaterialTheme.typography.labelSmall)
                        Text("$todayChanges", fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                    }
                }
                Surface(color = SaffronAccent.copy(alpha = 0.1f), shape = RoundedCornerShape(10.dp), modifier = Modifier.weight(1f)) {
                    Column(Modifier.padding(10.dp)) {
                        Text("Financial Edits", style = MaterialTheme.typography.labelSmall)
                        Text("$totalFinancialChanges", fontWeight = FontWeight.Bold, color = SaffronAccent)
                    }
                }
                Surface(color = StatusError.copy(alpha = 0.1f), shape = RoundedCornerShape(10.dp), modifier = Modifier.weight(1f)) {
                    Column(Modifier.padding(10.dp)) {
                        Text("Cancellations", style = MaterialTheme.typography.labelSmall)
                        Text("$totalCancellations", fontWeight = FontWeight.Bold, color = StatusError)
                    }
                }
            }
        }
        item {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                label = { Text("Filter by Order ID, Date (YYYY-MM-DD), Manager, or Action") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item {
                    FilterChip(selected = filterManagerId == "ALL", onClick = { filterManagerId = "ALL" }, label = { Text("All Managers") })
                }
                items(managers, key = { it.id }) { m ->
                    FilterChip(selected = filterManagerId == m.id, onClick = { filterManagerId = m.id }, label = { Text(m.name) })
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                FilledTonalButton(
                    onClick = { ReportExporter.exportAuditLogsCsv(context, settings?.shopName ?: "PRAYAG DEEN & SONS", filtered) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Export CSV")
                }
                FilledTonalButton(
                    onClick = { ReportExporter.exportAuditLogsPdf(context, settings?.shopName ?: "PRAYAG DEEN & SONS", filtered) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Export PDF")
                }
            }
        }
        items(filtered, key = { it.id }) { log ->
            Card {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${log.managerName} • ${log.actionPerformed}", fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                        Text("${log.dateKey} ${log.timeDisplay}", style = MaterialTheme.typography.labelSmall)
                    }
                    Text("Record: ${log.recordType} / ${log.recordId} • Field: ${log.fieldChanged.ifBlank { "-" }}", style = MaterialTheme.typography.bodySmall)
                    if (log.previousValue.isNotBlank() || log.newValue.isNotBlank()) {
                        Text("Old: ${log.previousValue.ifBlank { "-" }}  →  New: ${log.newValue.ifBlank { "-" }}", style = MaterialTheme.typography.labelMedium)
                    }
                    if (log.modificationReason.isNotBlank()) {
                        Text("Reason: ${log.modificationReason}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("Hash: ${log.entryHash.take(14)}...", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
            }
        }
    }
}
