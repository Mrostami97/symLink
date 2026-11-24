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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.itellDataStore: DataStore<Preferences> by preferencesDataStore(name = "itell_prefs")

data class UserPreferences(
    val languageCode: String = "fa",
    val darkTheme: Boolean = true,
    val deviceRole: String = "", // "", "HOST", "COMPANION"
    val deviceAlias: String = Build.MODEL ?: "Android",
    val preferredSimSlot: Int = 1,
    val autoReconnect: Boolean = true,
    val vibrateOnIncoming: Boolean = true,
    val onboardingCompleted: Boolean = false
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val LANGUAGE = stringPreferencesKey("language_code")
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val DEVICE_ROLE = stringPreferencesKey("device_role")
        val DEVICE_ALIAS = stringPreferencesKey("device_alias")
        val PREFERRED_SIM = intPreferencesKey("preferred_sim_slot")
        val AUTO_RECONNECT = booleanPreferencesKey("auto_reconnect")
        val VIBRATE_INCOMING = booleanPreferencesKey("vibrate_on_incoming")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }

    val preferencesFlow: Flow<UserPreferences> = context.itellDataStore.data.map { prefs ->
        UserPreferences(
            languageCode = prefs[Keys.LANGUAGE] ?: "fa",
            darkTheme = prefs[Keys.DARK_THEME] ?: true,
            deviceRole = prefs[Keys.DEVICE_ROLE] ?: "",
            deviceAlias = prefs[Keys.DEVICE_ALIAS]?.takeIf { it.isNotBlank() }
                ?: (Build.MODEL ?: "Android"),
            preferredSimSlot = prefs[Keys.PREFERRED_SIM] ?: 1,
            autoReconnect = prefs[Keys.AUTO_RECONNECT] ?: true,
            vibrateOnIncoming = prefs[Keys.VIBRATE_INCOMING] ?: true,
            onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false
        )
    }

    suspend fun setLanguage(code: String) {
        context.itellDataStore.edit { it[Keys.LANGUAGE] = code }
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.itellDataStore.edit { it[Keys.DARK_THEME] = enabled }
    }

    suspend fun setDeviceRole(role: String) {
        context.itellDataStore.edit { it[Keys.DEVICE_ROLE] = role }
    }

    suspend fun setDeviceAlias(alias: String) {
        context.itellDataStore.edit { it[Keys.DEVICE_ALIAS] = alias.trim() }
    }

    suspend fun setPreferredSimSlot(slot: Int) {
        context.itellDataStore.edit { it[Keys.PREFERRED_SIM] = slot.coerceIn(1, 2) }
    }

    suspend fun setAutoReconnect(enabled: Boolean) {
        context.itellDataStore.edit { it[Keys.AUTO_RECONNECT] = enabled }
    }

    suspend fun setVibrateOnIncoming(enabled: Boolean) {
        context.itellDataStore.edit { it[Keys.VIBRATE_INCOMING] = enabled }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.itellDataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }
}
