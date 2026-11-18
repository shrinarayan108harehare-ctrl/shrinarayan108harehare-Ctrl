package com.example.ui.manager

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.localization.AppLanguage
import com.example.localization.AppStrings
import com.example.model.ManagerAccount
import com.example.model.RecipientNotification
import com.example.ui.components.AnnouncementInboxSheet
import com.example.ui.components.LanguageToggleChip
import com.example.ui.components.NotificationBellButton
import com.example.ui.components.OfflineBanner
import com.example.viewmodel.ManagerViewModel

private enum class ManagerTab { INBOX, MAPS, BI, MANAGE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagerDashboardScreen(
    vm: ManagerViewModel,
    manager: ManagerAccount,
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
    var selectedTab by rememberSaveable { mutableStateOf(ManagerTab.INBOX) }
    var showAnnouncements by remember { mutableStateOf(false) }

    val conversations by vm.conversations.collectAsStateWithLifecycle()
    val feedback by vm.feedbackMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(feedback) {
        val msg = feedback
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            vm.clearFeedback()
        }
    }

    if (selectedTab != ManagerTab.INBOX) {
        BackHandler { selectedTab = ManagerTab.INBOX }
    }

    val totalUnreadChat = conversations.sumOf { it.unreadByManager }.toInt()
    val unreadNotifs = notifications.count { it.status != "READ" }
    val isOwner = manager.roleType == "owner"

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                strings.appTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "${manager.name} • ${if (isOwner) "OWNER SUPER ADMIN" else "FAMILY MANAGER"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    actions = {
                        LanguageToggleChip(language = language, onToggle = onToggleLanguage)
                        NotificationBellButton(unreadCount = unreadNotifs) { showAnnouncements = true }
                        IconButton(onClick = onSignOut, modifier = Modifier.testTag("manager_sign_out_btn")) {
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
                    selected = selectedTab == ManagerTab.INBOX,
                    onClick = { selectedTab = ManagerTab.INBOX },
                    icon = {
                        BadgedBox(badge = { if (totalUnreadChat > 0) Badge { Text(totalUnreadChat.coerceAtMost(99).toString()) } }) {
                            Icon(Icons.Default.Chat, contentDescription = strings.inbox)
                        }
                    },
                    label = { Text(strings.inbox, maxLines = 1) },
                    modifier = Modifier.testTag("nav_mgr_inbox")
                )
                NavigationBarItem(
                    selected = selectedTab == ManagerTab.MAPS,
                    onClick = { selectedTab = ManagerTab.MAPS },
                    icon = { Icon(Icons.Default.Map, contentDescription = strings.mapsAndRoutes) },
                    label = { Text(strings.mapsAndRoutes, maxLines = 1) },
                    modifier = Modifier.testTag("nav_mgr_maps")
                )
                NavigationBarItem(
                    selected = selectedTab == ManagerTab.BI,
                    onClick = { selectedTab = ManagerTab.BI },
                    icon = { Icon(Icons.Default.Analytics, contentDescription = strings.analytics) },
                    label = { Text(strings.analytics, maxLines = 1) },
                    modifier = Modifier.testTag("nav_mgr_bi")
                )
                NavigationBarItem(
                    selected = selectedTab == ManagerTab.MANAGE,
                    onClick = { selectedTab = ManagerTab.MANAGE },
                    icon = { Icon(Icons.Default.Tune, contentDescription = strings.settings) },
                    label = { Text(strings.settings, maxLines = 1) },
                    modifier = Modifier.testTag("nav_mgr_manage")
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (selectedTab) {
                ManagerTab.INBOX -> ManagerInboxAndChatScreen(vm, strings, language == AppLanguage.HINDI)
                ManagerTab.MAPS -> ManagerMapsAndRoutesScreen(vm, strings, language == AppLanguage.HINDI)
                ManagerTab.BI -> ManagerBiAnalyticsScreen(vm, strings, language == AppLanguage.HINDI)
                ManagerTab.MANAGE -> ManagerCommsAndSettingsScreen(vm, manager, strings)
            }
        }
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
