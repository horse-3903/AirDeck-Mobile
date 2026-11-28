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

    /** What the layout in use overrides in Settings. Empty when it overrides nothing. */
    val layoutBehaviour: Flow<Map<String, Int>> = dataStore.data.map { decodeBehaviour(it[LAYOUT_BEHAVIOUR]) }

    val presetBehaviour: Flow<Map<String, Map<String, Int>>> = dataStore.data.map { decodePresetBehaviour(it[PRESET_BEHAVIOUR]) }

    /** Saves the overrides for the layout in use, into its saved layout too. Overriding on the built-in layout makes an unsaved layout. */
    suspend fun setLayoutBehaviour(map: Map<String, Int>) {
        dataStore.edit { p ->
            p[LAYOUT_BEHAVIOUR] = Json.encodeToString(map)
            val active = p[ACTIVE_LAYOUT]
            if (active != null && active != FACTORY_LAYOUT && active in decodePresets(p[PRESETS])) {
                p[PRESET_BEHAVIOUR] = Json.encodeToString(decodePresetBehaviour(p[PRESET_BEHAVIOUR]) + (active to map))
            } else if (active == FACTORY_LAYOUT && map.isNotEmpty()) {
                p.remove(ACTIVE_LAYOUT)
            }
        }
    }

    /** Changes one control of the layout in use, and the saved layout it belongs to. */
    suspend fun updateLayoutConfig(component: ButtonComponent, transform: (ButtonConfig) -> ButtonConfig) {
        dataStore.edit { p ->
            val current = p[BUTTON_CONFIGS]?.let { runCatching { Json.decodeFromString<Map<ButtonComponent, ButtonConfig>>(it) }.getOrNull() }.orEmpty()
            val full = defaultButtonConfigs + current
            val updated = full + (component to transform(full.getValue(component)))
            p[BUTTON_CONFIGS] = Json.encodeToString(updated)
            val active = p[ACTIVE_LAYOUT]
            if (active != null && active != FACTORY_LAYOUT) {
                val all = decodePresets(p[PRESETS])
                if (active in all) p[PRESETS] = Json.encodeToString(all + (active to updated))
            } else if (active == FACTORY_LAYOUT) {
                p.remove(ACTIVE_LAYOUT)
            }
        }
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
            val behaviour = decodePresetBehaviour(preferences[PRESET_BEHAVIOUR])[name]
            if (behaviour.isNullOrEmpty()) preferences.remove(LAYOUT_BEHAVIOUR) else preferences[LAYOUT_BEHAVIOUR] = Json.encodeToString(behaviour)
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
            val behaviour = decodePresetBehaviour(preferences[PRESET_BEHAVIOUR])
            preferences[PRESET_BEHAVIOUR] = Json.encodeToString(behaviour.mapKeys { (k, _) -> if (k == old) new else k })
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
            stickRing = p[PREF_RING] ?: d.stickRing,
            tiltTarget = p[PREF_TILT] ?: d.tiltTarget,
            tiltRange = p[PREF_TILT_RANGE] ?: d.tiltRange,
            tiltDead = p[PREF_TILT_DEAD] ?: d.tiltDead,
            tiltInvert = p[PREF_TILT_INVERT] ?: d.tiltInvert,
            controlSize = p[PREF_SIZE] ?: d.controlSize,
            dimAfterSeconds = p[PREF_DIM] ?: d.dimAfterSeconds,
            stickCurve = p[PREF_CURVE] ?: d.stickCurve,
            curveCustom = Curve.decode(p[PREF_CURVE_P]),
            tiltCurve = p[PREF_TCURVE] ?: d.tiltCurve,
            tiltCurveCustom = Curve.decode(p[PREF_TCURVE_P]),
            triggerCurve = p[PREF_RCURVE] ?: d.triggerCurve,
            triggerCurveCustom = Curve.decode(p[PREF_RCURVE_P]),
            connectionHaptics = p[PREF_CONN_HAPTICS] ?: d.connectionHaptics,
            triggerLeft = p[PREF_TRIG_L] ?: d.triggerLeft,
            triggerRight = p[PREF_TRIG_R] ?: d.triggerRight,
            stickLeftFloat = p[PREF_STICK_L] ?: d.stickLeftFloat,
            stickRightFloat = p[PREF_STICK_R] ?: d.stickRightFloat,
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
            p[PREF_RING] = prefs.stickRing
            p[PREF_TILT] = prefs.tiltTarget
            p[PREF_TILT_RANGE] = prefs.tiltRange
            p[PREF_TILT_DEAD] = prefs.tiltDead
            p[PREF_TILT_INVERT] = prefs.tiltInvert
            p[PREF_SIZE] = prefs.controlSize
            p[PREF_DIM] = prefs.dimAfterSeconds
            p[PREF_CURVE] = prefs.stickCurve
            p[PREF_CURVE_P] = prefs.curveCustom.encode()
            p[PREF_TCURVE] = prefs.tiltCurve
            p[PREF_TCURVE_P] = prefs.tiltCurveCustom.encode()
            p[PREF_RCURVE] = prefs.triggerCurve
            p[PREF_RCURVE_P] = prefs.triggerCurveCustom.encode()
            p[PREF_CONN_HAPTICS] = prefs.connectionHaptics
            p[PREF_TRIG_L] = prefs.triggerLeft
            p[PREF_TRIG_R] = prefs.triggerRight
            p[PREF_STICK_L] = prefs.stickLeftFloat
            p[PREF_STICK_R] = prefs.stickRightFloat
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

    /** [behaviour] null leaves what the saved layout already overrides as it is. */
    suspend fun savePreset(
        name: String,
        configs: Map<ButtonComponent, ButtonConfig>,
        groups: List<LayoutGroup>,
        behaviour: Map<String, Int>? = null
    ) {
        dataStore.edit { preferences ->
            val updated = decodePresets(preferences[PRESETS]) + (name to configs)
            preferences[PRESETS] = Json.encodeToString(updated)
            preferences[PRESET_GROUPS] =
                Json.encodeToString(decodePresetGroups(preferences[PRESET_GROUPS]) + (name to groups))
            if (behaviour != null) {
                val all = decodePresetBehaviour(preferences[PRESET_BEHAVIOUR])
                preferences[PRESET_BEHAVIOUR] = Json.encodeToString(if (behaviour.isEmpty()) all - name else all + (name to behaviour))
            }
        }
    }

    suspend fun deletePreset(name: String) {
        dataStore.edit { preferences ->
            val updated = decodePresets(preferences[PRESETS]) - name
            preferences[PRESETS] = Json.encodeToString(updated)
            preferences[PRESET_GROUPS] =
                Json.encodeToString(decodePresetGroups(preferences[PRESET_GROUPS]) - name)
            preferences[PRESET_BEHAVIOUR] = Json.encodeToString(decodePresetBehaviour(preferences[PRESET_BEHAVIOUR]) - name)
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

    suspend fun snapshotSettings(): Preferences = dataStore.data.first()

    @Suppress("UNCHECKED_CAST")
    suspend fun restoreSettings(snapshot: Preferences) {
        dataStore.edit { p ->
            p.clear()
            p.putAll(*snapshot.asMap().map { (k, v) -> (k as Preferences.Key<Any>) to v }.toTypedArray())
        }
    }

    suspend fun resetAllSettings() {
        dataStore.edit { preferences ->
            val presetsJson = preferences[PRESETS]
            val presetGroupsJson = preferences[PRESET_GROUPS]
            val presetBehaviourJson = preferences[PRESET_BEHAVIOUR]
            preferences.clear()
            if (presetBehaviourJson != null) {
                preferences[PRESET_BEHAVIOUR] = presetBehaviourJson
            }
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
        private val PREF_RING = booleanPreferencesKey("pref_stick_ring")
        private val LAYOUT_BEHAVIOUR = stringPreferencesKey("layout_behaviour")
        private val PRESET_BEHAVIOUR = stringPreferencesKey("preset_behaviour")
        private val PREF_TILT = intPreferencesKey("pref_tilt_target")
        private val PREF_TILT_RANGE = intPreferencesKey("pref_tilt_range")
        private val PREF_TILT_DEAD = intPreferencesKey("pref_tilt_dead")
        private val PREF_TILT_INVERT = booleanPreferencesKey("pref_tilt_invert")
        private val PREF_SIZE = intPreferencesKey("pref_control_size")
        private val PREF_DIM = intPreferencesKey("pref_dim_seconds")
        private val PREF_CURVE_P = stringPreferencesKey("pref_curve_custom")
        private val PREF_TCURVE = intPreferencesKey("pref_tilt_curve")
        private val PREF_RCURVE = intPreferencesKey("pref_trigger_curve")
        private val PREF_RCURVE_P = stringPreferencesKey("pref_trigger_curve_custom")
        private val PREF_TCURVE_P = stringPreferencesKey("pref_tilt_curve_custom")
        private val PREF_CURVE = intPreferencesKey("pref_stick_curve")
        private val PREF_TRIG_L = intPreferencesKey("pref_trigger_left")
        private val PREF_TRIG_R = intPreferencesKey("pref_trigger_right")
        private val PREF_STICK_L = booleanPreferencesKey("pref_stick_left_float")
        private val PREF_STICK_R = booleanPreferencesKey("pref_stick_right_float")
        private val PREF_CONN_HAPTICS = booleanPreferencesKey("pref_connection_haptics")
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

private fun decodeBehaviour(text: String?): Map<String, Int> =
    text?.let { runCatching { Json.decodeFromString<Map<String, Int>>(it) }.getOrNull() }.orEmpty()

private fun decodePresetBehaviour(text: String?): Map<String, Map<String, Int>> =
    text?.let { runCatching { Json.decodeFromString<Map<String, Map<String, Int>>>(it) }.getOrNull() }.orEmpty()
