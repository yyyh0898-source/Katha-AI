package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.kathaDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "katha_user_preferences"
)

data class LocalPrefsState(
    val displayName: String = "Storyteller",
    val languageCode: String = "my",
    val themeMode: String = "dark",
    val accentTheme: String = "velvet_rose",
    val matureContentEnabled: Boolean = false,
    val biometricLockEnabled: Boolean = false,
    val backupPin: String = "1234",
    val autoVoiceEnabled: Boolean = false
)

class LocalPreferencesManager(private val context: Context) {

    private object Keys {
        val DISPLAY_NAME = stringPreferencesKey("display_name")
        val LANGUAGE_CODE = stringPreferencesKey("language_code")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ACCENT_THEME = stringPreferencesKey("accent_theme")
        val MATURE_CONTENT_ENABLED = booleanPreferencesKey("mature_content_enabled")
        val BIOMETRIC_LOCK_ENABLED = booleanPreferencesKey("biometric_lock_enabled")
        val BACKUP_PIN = stringPreferencesKey("backup_pin")
        val AUTO_VOICE_ENABLED = booleanPreferencesKey("auto_voice_enabled")
    }

    val prefsFlow: Flow<LocalPrefsState> = context.kathaDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            LocalPrefsState(
                displayName = prefs[Keys.DISPLAY_NAME] ?: "Storyteller",
                languageCode = prefs[Keys.LANGUAGE_CODE] ?: "my",
                themeMode = prefs[Keys.THEME_MODE] ?: "dark",
                accentTheme = prefs[Keys.ACCENT_THEME] ?: "velvet_rose",
                matureContentEnabled = prefs[Keys.MATURE_CONTENT_ENABLED] ?: false,
                biometricLockEnabled = prefs[Keys.BIOMETRIC_LOCK_ENABLED] ?: false,
                backupPin = prefs[Keys.BACKUP_PIN] ?: "1234",
                autoVoiceEnabled = prefs[Keys.AUTO_VOICE_ENABLED] ?: false
            )
        }

    suspend fun syncFromRemote(
        displayName: String,
        languageCode: String,
        themeMode: String,
        accentTheme: String,
        matureContentEnabled: Boolean,
        biometricLockEnabled: Boolean,
        autoVoiceEnabled: Boolean
    ) {
        context.kathaDataStore.edit { prefs ->
            if (displayName.isNotBlank()) {
                prefs[Keys.DISPLAY_NAME] = displayName.take(80)
            }
            if (languageCode in setOf("my", "en")) {
                prefs[Keys.LANGUAGE_CODE] = languageCode
            }
            if (themeMode in setOf("dark", "light", "system")) {
                prefs[Keys.THEME_MODE] = themeMode
            }
            if (accentTheme in setOf(
                    "velvet_rose",
                    "midnight_violet",
                    "golden_amber",
                    "emerald_jade",
                    "ocean_cyan"
                )
            ) {
                prefs[Keys.ACCENT_THEME] = accentTheme
            }
            prefs[Keys.MATURE_CONTENT_ENABLED] = matureContentEnabled
            prefs[Keys.BIOMETRIC_LOCK_ENABLED] = biometricLockEnabled
            prefs[Keys.AUTO_VOICE_ENABLED] = autoVoiceEnabled
        }
    }

    suspend fun updateLanguage(code: String) {
        val valid = if (code == "en") "en" else "my"
        context.kathaDataStore.edit { prefs ->
            prefs[Keys.LANGUAGE_CODE] = valid
        }
    }

    suspend fun updateThemeMode(mode: String) {
        val valid = if (mode in setOf("dark", "light", "system")) mode else "dark"
        context.kathaDataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = valid
        }
    }

    suspend fun updateAccentTheme(accent: String) {
        val valid = if (accent in setOf(
                "velvet_rose",
                "midnight_violet",
                "golden_amber",
                "emerald_jade",
                "ocean_cyan"
            )
        ) accent else "velvet_rose"
        context.kathaDataStore.edit { prefs ->
            prefs[Keys.ACCENT_THEME] = valid
        }
    }

    suspend fun updateDisplayName(name: String) {
        val clean = name.trim().ifBlank { "Storyteller" }.take(80)
        context.kathaDataStore.edit { prefs ->
            prefs[Keys.DISPLAY_NAME] = clean
        }
    }

    suspend fun updateMatureContent(enabled: Boolean) {
        context.kathaDataStore.edit { prefs ->
            prefs[Keys.MATURE_CONTENT_ENABLED] = enabled
        }
    }

    suspend fun updateBiometricLock(enabled: Boolean, pin: String? = null) {
        context.kathaDataStore.edit { prefs ->
            prefs[Keys.BIOMETRIC_LOCK_ENABLED] = enabled
            if (!pin.isNullOrBlank()) {
                val cleanPin = pin.filter { it.isDigit() }.take(6)
                if (cleanPin.length >= 4) {
                    prefs[Keys.BACKUP_PIN] = cleanPin
                }
            } else if (prefs[Keys.BACKUP_PIN].isNullOrBlank()) {
                prefs[Keys.BACKUP_PIN] = "1234"
            }
        }
    }

    suspend fun updateBackupPin(pin: String) {
        val cleanPin = pin.filter { it.isDigit() }.take(6)
        if (cleanPin.length >= 4) {
            context.kathaDataStore.edit { prefs ->
                prefs[Keys.BACKUP_PIN] = cleanPin
            }
        }
    }

    suspend fun updateAutoVoice(enabled: Boolean) {
        context.kathaDataStore.edit { prefs ->
            prefs[Keys.AUTO_VOICE_ENABLED] = enabled
        }
    }
}
