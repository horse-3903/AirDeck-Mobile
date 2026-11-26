package io.github.horse3903.airdeck.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.horse3903.airdeck.ui.utils.HapticPrefs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

/** The built-in layout. It is kept as a saved layout but cannot be renamed, deleted or changed. */
const val FACTORY_LAYOUT = "Default"

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
                    defaultButtonConfigs + Json.decodeFromString<Map<ButtonComponent, ButtonConfig>>(jsonString)
                } catch (e: Exception) {
                    defaultButtonConfigs
                }
            } else {
                defaultButtonConfigs
            }
        }

    val layoutGroups: Flow<List<LayoutGroup>> = dataStore.data.map { preferences ->
        decodeGroups(preferences[LAYOUT_GROUPS]) ?: defaultLayoutGroups
    }

    suspend fun setLayoutGroups(groups: List<LayoutGroup>) {
        dataStore.edit { preferences -> preferences[LAYOUT_GROUPS] = encodeGroups(groups) }
    }

    val presetGroups: Flow<Map<String, List<LayoutGroup>>> =
        dataStore.data.map { preferences -> decodePresetGroups(preferences[PRESET_GROUPS]) }

    /** The saved layout currently in use, or null when the working layout was never saved under a name. */
    val activeLayout: Flow<String?> = dataStore.data.map { preferences -> preferences[ACTIVE_LAYOUT] }

    suspend fun setActiveLayout(name: String?) {
        dataStore.edit { preferences ->
            if (name == null) preferences.remove(ACTIVE_LAYOUT) else preferences[ACTIVE_LAYOUT] = name
        }
    }

    /** Makes a saved layout the working one. */
    suspend fun applyPreset(name: String) {
        dataStore.edit { preferences ->
            val configs = decodePresets(preferences[PRESETS])[name] ?: return@edit
            preferences[BUTTON_CONFIGS] = Json.encodeToString(configs)
            decodePresetGroups(preferences[PRESET_GROUPS])[name]?.let { preferences[LAYOUT_GROUPS] = encodeGroups(it) }
            preferences[ACTIVE_LAYOUT] = name
        }
    }

    /** The built-in layout is always kept as a saved layout, so it can be picked again. */
    suspend fun ensureFactoryLayout() {
        if (presets.first().isNotEmpty()) return
        savePreset(FACTORY_LAYOUT, defaultButtonConfigs, defaultLayoutGroups)
        if (buttonConfigs.first() == defaultButtonConfigs && layoutGroups.first() == defaultLayoutGroups) {
            setActiveLayout(FACTORY_LAYOUT)
        }
    }

    /** Saves the layouts in this order. Layouts not named here keep their place after the others. */
    suspend fun reorderPresets(order: List<String>) {
        dataStore.edit { preferences ->
            val configs = decodePresets(preferences[PRESETS])
            val groups = decodePresetGroups(preferences[PRESET_GROUPS])
            val names = order.filter { it in configs } + configs.keys.filter { it !in order }
            preferences[PRESETS] = Json.encodeToString(names.associateWith { configs.getValue(it) })
            preferences[PRESET_GROUPS] = Json.encodeToString(names.filter { it in groups }.associateWith { groups.getValue(it) })
        }
    }

    suspend fun renamePreset(old: String, new: String) {
        if (old == new) return
        dataStore.edit { preferences ->
            val configs = decodePresets(preferences[PRESETS])
            if (old !in configs || new in configs) return@edit
            preferences[PRESETS] = Json.encodeToString(configs.mapKeys { (k, _) -> if (k == old) new else k })
            val groups = decodePresetGroups(preferences[PRESET_GROUPS])
            preferences[PRESET_GROUPS] = Json.encodeToString(groups.mapKeys { (k, _) -> if (k == old) new else k })
            if (preferences[ACTIVE_LAYOUT] == old) preferences[ACTIVE_LAYOUT] = new
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

    val appPrefs: Flow<AppPrefs> = dataStore.data.map { p ->
        val d = AppPrefs()
        AppPrefs(
            opacity = p[PREF_OPACITY] ?: d.opacity,
            glowEnabled = p[PREF_GLOW] ?: d.glowEnabled,
            glowStrength = p[PREF_GLOW_STRENGTH] ?: d.glowStrength,
            keepScreenOn = p[PREF_KEEP_ON] ?: d.keepScreenOn,
            deadZone = p[PREF_DEAD_ZONE] ?: d.deadZone,
            sensitivity = p[PREF_SENSITIVITY] ?: d.sensitivity,
            clickEnabled = p[PREF_CLICK] ?: d.clickEnabled,
            clickHoldMs = p[PREF_CLICK_MS] ?: d.clickHoldMs,
            invertLeftY = p[PREF_INVERT_L] ?: d.invertLeftY,
            invertRightY = p[PREF_INVERT_R] ?: d.invertRightY,
            dpadDiagonal = p[PREF_DIAGONAL] ?: d.dpadDiagonal,
            autoReconnect = p[PREF_RECONNECT] ?: d.autoReconnect,
            reconnectSeconds = p[PREF_RECONNECT_S] ?: d.reconnectSeconds,
            confirmDisconnect = p[PREF_CONFIRM] ?: d.confirmDisconnect,
            gameRumble = p[PREF_GAME_RUMBLE] ?: d.gameRumble,
        )
    }

    suspend fun setAppPrefs(prefs: AppPrefs) {
        dataStore.edit { p ->
            p[PREF_OPACITY] = prefs.opacity
            p[PREF_GLOW] = prefs.glowEnabled
            p[PREF_GLOW_STRENGTH] = prefs.glowStrength
            p[PREF_KEEP_ON] = prefs.keepScreenOn
            p[PREF_DEAD_ZONE] = prefs.deadZone
            p[PREF_SENSITIVITY] = prefs.sensitivity
            p[PREF_CLICK] = prefs.clickEnabled
            p[PREF_CLICK_MS] = prefs.clickHoldMs
            p[PREF_INVERT_L] = prefs.invertLeftY
            p[PREF_INVERT_R] = prefs.invertRightY
            p[PREF_DIAGONAL] = prefs.dpadDiagonal
            p[PREF_RECONNECT] = prefs.autoReconnect
            p[PREF_RECONNECT_S] = prefs.reconnectSeconds
            p[PREF_CONFIRM] = prefs.confirmDisconnect
            p[PREF_GAME_RUMBLE] = prefs.gameRumble
        }
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

    suspend fun savePreset(
        name: String,
        configs: Map<ButtonComponent, ButtonConfig>,
        groups: List<LayoutGroup>
    ) {
        dataStore.edit { preferences ->
            val updated = decodePresets(preferences[PRESETS]) + (name to configs)
            preferences[PRESETS] = Json.encodeToString(updated)
            preferences[PRESET_GROUPS] =
                Json.encodeToString(decodePresetGroups(preferences[PRESET_GROUPS]) + (name to groups))
        }
    }

    suspend fun deletePreset(name: String) {
        dataStore.edit { preferences ->
            val updated = decodePresets(preferences[PRESETS]) - name
            preferences[PRESETS] = Json.encodeToString(updated)
            preferences[PRESET_GROUPS] =
                Json.encodeToString(decodePresetGroups(preferences[PRESET_GROUPS]) - name)
            if (preferences[ACTIVE_LAYOUT] == name) preferences.remove(ACTIVE_LAYOUT)
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
            val presetGroupsJson = preferences[PRESET_GROUPS]
            preferences.clear()
            if (presetsJson != null) {
                preferences[PRESETS] = presetsJson
            }
            if (presetGroupsJson != null) {
                preferences[PRESET_GROUPS] = presetGroupsJson
            }
        }
    }

    private fun decodePresetGroups(json: String?): Map<String, List<LayoutGroup>> {
        if (json == null) return emptyMap()
        return try {
            Json.decodeFromString(json)
        } catch (e: Exception) {
            emptyMap()
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
        private val PREF_OPACITY = intPreferencesKey("pref_opacity")
        private val PREF_GLOW = booleanPreferencesKey("pref_glow")
        private val PREF_GLOW_STRENGTH = intPreferencesKey("pref_glow_strength")
        private val PREF_KEEP_ON = booleanPreferencesKey("pref_keep_screen_on")
        private val PREF_DEAD_ZONE = intPreferencesKey("pref_dead_zone")
        private val PREF_SENSITIVITY = intPreferencesKey("pref_sensitivity")
        private val PREF_CLICK = booleanPreferencesKey("pref_click")
        private val PREF_CLICK_MS = intPreferencesKey("pref_click_ms")
        private val PREF_INVERT_L = booleanPreferencesKey("pref_invert_left_y")
        private val PREF_INVERT_R = booleanPreferencesKey("pref_invert_right_y")
        private val PREF_DIAGONAL = intPreferencesKey("pref_dpad_diagonal")
        private val PREF_RECONNECT = booleanPreferencesKey("pref_reconnect")
        private val PREF_RECONNECT_S = intPreferencesKey("pref_reconnect_seconds")
        private val PREF_CONFIRM = booleanPreferencesKey("pref_confirm_disconnect")
        private val PREF_GAME_RUMBLE = booleanPreferencesKey("pref_game_rumble")
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
        private val LAYOUT_GROUPS = stringPreferencesKey("layout_groups")
        private val PRESETS = stringPreferencesKey("button_presets")
        private val PRESET_GROUPS = stringPreferencesKey("button_preset_groups")
        private val ACTIVE_LAYOUT = stringPreferencesKey("active_layout")
        private val LAST_CONNECTION_IP_ADDRESS = stringPreferencesKey("last_connection_ip_address")
        private val LAST_CONNECTION_PORT = stringPreferencesKey("last_connection_port")
        private val SAVE_CONNECTION_CREDENTIALS =
            booleanPreferencesKey("save_connection_credentials")
        private val FULL_SCREEN_ENABLED = booleanPreferencesKey("full_screen_enabled")
    }
}
