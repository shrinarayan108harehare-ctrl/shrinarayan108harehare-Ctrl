package com.example.util

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.localization.AppLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "pds_preferences")

class PreferencesStore(private val context: Context) {
    private val keyLang = stringPreferencesKey("language_code")
    private val keyNotif = booleanPreferencesKey("notifications_enabled")
    private val keyDraftChat = stringPreferencesKey("draft_chat_message")

    val languageFlow: Flow<AppLanguage> = context.dataStore.data.map { prefs ->
        if (prefs[keyLang] == AppLanguage.HINDI.code) AppLanguage.HINDI else AppLanguage.ENGLISH
    }

    val notificationsEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[keyNotif] ?: true
    }

    val draftChatFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[keyDraftChat] ?: ""
    }

    suspend fun setLanguage(lang: AppLanguage) {
        context.dataStore.edit { it[keyLang] = lang.code }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[keyNotif] = enabled }
    }

    suspend fun saveDraftChat(text: String) {
        context.dataStore.edit { it[keyDraftChat] = text }
    }
}
