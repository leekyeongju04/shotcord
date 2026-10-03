package com.example.data.preferences

import android.content.Context
import android.os.Build
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.network.DiscordWebhookSender
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "shotcord_settings")

class AppPreferences(private val context: Context) {

    companion object {
        const val DEFAULT_WEBHOOK_URL = "https://discord.com/api/webhooks/1555886521893126206/lF_plFFyPsZXyIE9W1nlxcJp8t2X94LJZasbBiAbU01O28vQGJcVqj39GHAe-wHAvmel"
        const val DEFAULT_USER_NAME = "Kesha"

        val KEY_WEBHOOK_URL = stringPreferencesKey("webhook_url")
        val KEY_USER_NAME = stringPreferencesKey("user_name")
        val KEY_AUTO_SEND = booleanPreferencesKey("auto_send")
        val KEY_SERVICE_ACTIVE = booleanPreferencesKey("service_active")
        val KEY_COMPRESS_QUALITY = intPreferencesKey("compress_quality")

        fun getDefaultUserName(): String {
            val manufacturer = Build.MANUFACTURER.orEmpty().replaceFirstChar { it.uppercase() }
            val model = Build.MODEL.orEmpty()
            return if (manufacturer.isNotBlank() && model.isNotBlank()) {
                if (model.startsWith(manufacturer, ignoreCase = true)) model else "$manufacturer $model"
            } else {
                DEFAULT_USER_NAME
            }
        }

        fun sanitizeWebhookUrl(url: String?): String {
            if (url.isNullOrBlank()) return DEFAULT_WEBHOOK_URL
            val cleaned = url.trim().removeSuffix(",").removeSuffix(";").trim()
            return if (DiscordWebhookSender.isValidWebhookUrl(cleaned)) {
                cleaned
            } else {
                DEFAULT_WEBHOOK_URL
            }
        }
    }

    val webhookUrlFlow: Flow<String> = context.dataStore.data.map { prefs ->
        sanitizeWebhookUrl(prefs[KEY_WEBHOOK_URL])
    }

    val userNameFlow: Flow<String> = context.dataStore.data.map { prefs ->
        val saved = prefs[KEY_USER_NAME]
        if (saved.isNullOrBlank()) DEFAULT_USER_NAME else saved.trim()
    }

    val autoSendFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_AUTO_SEND] ?: true
    }

    val isServiceActiveFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_SERVICE_ACTIVE] ?: true // Default to active listening so screenshots are captured immediately!
    }

    val compressQualityFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_COMPRESS_QUALITY] ?: 85
    }

    suspend fun setWebhookUrl(url: String) {
        val sanitized = sanitizeWebhookUrl(url)
        context.dataStore.edit { prefs ->
            prefs[KEY_WEBHOOK_URL] = sanitized
        }
    }

    suspend fun setUserName(name: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_USER_NAME] = name.trim().ifEmpty { DEFAULT_USER_NAME }
        }
    }

    suspend fun setAutoSend(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_AUTO_SEND] = enabled
        }
    }

    suspend fun setServiceActive(active: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SERVICE_ACTIVE] = active
        }
    }

    suspend fun setCompressQuality(quality: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_COMPRESS_QUALITY] = quality.coerceIn(30, 100)
        }
    }
}
