package io.github.kitswas.virtualgamepadmobile.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.kitswas.virtualgamepadmobile.ui.utils.HapticPrefs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(context: Context) {
    private val dataStore = context.settingsDataStore

    val baseColor: Flow<BaseColor> = dataStore.data.map { preferences ->
        BaseColor.fromInt(preferences[BASE_COLOR] ?: defaultBaseColor.ordinal)
    }

    val colorScheme: Flow<ColorScheme> = dataStore.data.map { preferences ->
        ColorScheme.fromInt(preferences[COLOR_SCHEME] ?: defaultColorScheme.ordinal)
    }

    val gamepadStyle: Flow<GamepadStyle> = dataStore.data.map { preferences ->
        GamepadStyle.fromInt(preferences[GAMEPAD_STYLE] ?: defaultGamepadStyle.ordinal)
    }

    val pollingDelay: Flow<Int> = dataStore.data.map { preferences ->
        preferences[POLLING_DELAY] ?: defaultPollingDelay
    }

    val stickSnap: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[STICK_SNAP] ?: defaultStickSnap
    }

    suspend fun setStickSnap(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[STICK_SNAP] = enabled }
    }

    val dpadSingle: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[DPAD_SINGLE] ?: defaultDpadSingle
    }

    suspend fun setDpadSingle(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[DPAD_SINGLE] = enabled }
    }

    val hapticFeedbackEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[HAPTIC_FEEDBACK_ENABLED] ?: defaultHapticFeedbackEnabled
    }

    val buttonConfigs: Flow<Map<ButtonComponent, ButtonConfig>> =
        dataStore.data.map { preferences ->
            val jsonString = preferences[BUTTON_CONFIGS]
            if (jsonString != null) {
                try {
                    Json.decodeFromString<Map<ButtonComponent, ButtonConfig>>(jsonString)
                } catch (e: Exception) {
                    defaultButtonConfigs
                }
            } else {
                defaultButtonConfigs
            }
        }

    val presets: Flow<Map<String, Map<ButtonComponent, ButtonConfig>>> =
        dataStore.data.map { preferences -> decodePresets(preferences[PRESETS]) }

    val lastConnectionIpAddress: Flow<String> = dataStore.data.map { preferences ->
        preferences[LAST_CONNECTION_IP_ADDRESS] ?: ""
    }

    val lastConnectionPort: Flow<String> = dataStore.data.map { preferences ->
        preferences[LAST_CONNECTION_PORT] ?: ""
    }

    val saveConnectionCredentials: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[SAVE_CONNECTION_CREDENTIALS] ?: defaultSaveConnectionCredentials
    }

    val fullScreenEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[FULL_SCREEN_ENABLED] ?: defaultFullScreenEnabled
    }

    suspend fun setBaseColor(baseColor: BaseColor) {
        dataStore.edit { preferences ->
            preferences[BASE_COLOR] = baseColor.ordinal
        }
    }

    suspend fun setColorScheme(colorScheme: ColorScheme) {
        dataStore.edit { preferences ->
            preferences[COLOR_SCHEME] = colorScheme.ordinal
        }
    }

    suspend fun setGamepadStyle(style: GamepadStyle) {
        dataStore.edit { preferences ->
            preferences[GAMEPAD_STYLE] = style.ordinal
        }
    }

    suspend fun setPollingDelay(delay: Int) {
        dataStore.edit { preferences ->
            preferences[POLLING_DELAY] = delay
        }
    }

    val hapticPrefs: Flow<HapticPrefs> = dataStore.data.map { p ->
        val d = HapticPrefs()
        HapticPrefs(
            custom = p[HAPTIC_CUSTOM] ?: d.custom,
            intensity = p[HAPTIC_INTENSITY] ?: d.intensity,
            durationMs = p[HAPTIC_DURATION] ?: d.durationMs,
            press = p[HAPTIC_PRESS] ?: d.press,
            release = p[HAPTIC_RELEASE] ?: d.release,
            sticks = p[HAPTIC_STICKS] ?: d.sticks,
            triggers = p[HAPTIC_TRIGGERS] ?: d.triggers,
        )
    }

    suspend fun setHapticPrefs(prefs: HapticPrefs) {
        dataStore.edit { p ->
            p[HAPTIC_CUSTOM] = prefs.custom
            p[HAPTIC_INTENSITY] = prefs.intensity
            p[HAPTIC_DURATION] = prefs.durationMs
            p[HAPTIC_PRESS] = prefs.press
            p[HAPTIC_RELEASE] = prefs.release
            p[HAPTIC_STICKS] = prefs.sticks
            p[HAPTIC_TRIGGERS] = prefs.triggers
        }
    }

    suspend fun setHapticFeedbackEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[HAPTIC_FEEDBACK_ENABLED] = enabled
        }
    }

    suspend fun setButtonConfig(component: ButtonComponent, config: ButtonConfig) {
        dataStore.edit { preferences ->
            val currentJson = preferences[BUTTON_CONFIGS]
            val currentConfigs = if (currentJson != null) {
                try {
                    Json.decodeFromString<Map<ButtonComponent, ButtonConfig>>(currentJson)
                        .toMutableMap()
                } catch (e: Exception) {
                    defaultButtonConfigs.toMutableMap()
                }
            } else {
                defaultButtonConfigs.toMutableMap()
            }
            currentConfigs[component] = config
            preferences[BUTTON_CONFIGS] = Json.encodeToString(currentConfigs)
        }
    }

    suspend fun setAllButtonConfigs(configs: Map<ButtonComponent, ButtonConfig>) {
        dataStore.edit { preferences ->
            preferences[BUTTON_CONFIGS] = Json.encodeToString(configs)
        }
    }

    suspend fun savePreset(name: String, configs: Map<ButtonComponent, ButtonConfig>) {
        dataStore.edit { preferences ->
            val updated = decodePresets(preferences[PRESETS]) + (name to configs)
            preferences[PRESETS] = Json.encodeToString(updated)
        }
    }

    suspend fun deletePreset(name: String) {
        dataStore.edit { preferences ->
            val updated = decodePresets(preferences[PRESETS]) - name
            preferences[PRESETS] = Json.encodeToString(updated)
        }
    }

    suspend fun setLastConnectionCredentials(ipAddress: String, port: String) {
        dataStore.edit { preferences ->
            preferences[LAST_CONNECTION_IP_ADDRESS] = ipAddress
            preferences[LAST_CONNECTION_PORT] = port
        }
    }

    suspend fun setSaveConnectionCredentials(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[SAVE_CONNECTION_CREDENTIALS] = enabled
        }
    }

    suspend fun setFullScreenEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[FULL_SCREEN_ENABLED] = enabled
        }
    }

    suspend fun resetAllSettings() {
        dataStore.edit { preferences ->
            val presetsJson = preferences[PRESETS]
            preferences.clear()
            if (presetsJson != null) {
                preferences[PRESETS] = presetsJson
            }
        }
    }

    private fun decodePresets(json: String?): Map<String, Map<ButtonComponent, ButtonConfig>> {
        if (json == null) return emptyMap()
        return try {
            Json.decodeFromString(json)
        } catch (e: Exception) {
            emptyMap()
        }
    }

    companion object {
        private val COLOR_SCHEME = intPreferencesKey("color_scheme")
        private val BASE_COLOR = intPreferencesKey("base_color")
        private val GAMEPAD_STYLE = intPreferencesKey("gamepad_style")
        private val POLLING_DELAY = intPreferencesKey("polling_delay")
        private val HAPTIC_FEEDBACK_ENABLED = booleanPreferencesKey("haptic_feedback_enabled")
        private val HAPTIC_CUSTOM = booleanPreferencesKey("haptic_custom")
        private val HAPTIC_INTENSITY = intPreferencesKey("haptic_intensity")
        private val HAPTIC_DURATION = intPreferencesKey("haptic_duration")
        private val HAPTIC_PRESS = booleanPreferencesKey("haptic_press")
        private val HAPTIC_RELEASE = booleanPreferencesKey("haptic_release")
        private val HAPTIC_STICKS = booleanPreferencesKey("haptic_sticks")
        private val HAPTIC_TRIGGERS = booleanPreferencesKey("haptic_triggers")
        private val STICK_SNAP = booleanPreferencesKey("stick_snap")
        private val DPAD_SINGLE = booleanPreferencesKey("dpad_single")
        private val BUTTON_CONFIGS = stringPreferencesKey("button_configs")
        private val PRESETS = stringPreferencesKey("button_presets")
        private val LAST_CONNECTION_IP_ADDRESS = stringPreferencesKey("last_connection_ip_address")
        private val LAST_CONNECTION_PORT = stringPreferencesKey("last_connection_port")
        private val SAVE_CONNECTION_CREDENTIALS =
            booleanPreferencesKey("save_connection_credentials")
        private val FULL_SCREEN_ENABLED = booleanPreferencesKey("full_screen_enabled")
    }
}
