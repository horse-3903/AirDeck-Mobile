package io.github.horse3903.airdeck.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

const val LAYOUT_FORMAT_VERSION = 2

/** A layout as shared between devices: where every control sits, and which controls are grouped. */
class LayoutBundle(
    val buttons: Map<ButtonComponent, ButtonConfig>,
    /** Null when the text had no groups, so the current ones should stay. */
    val groups: List<LayoutGroup>?,
    /** The settings this layout overrides, see [withBehaviour]. Null when the text had none. */
    val behaviour: Map<String, Int>? = null,
)

private val choiceKeys = mapOf("curve" to listOf("linear", "precise", "quick"), "tilt" to listOf("off", "left", "right"),
    "trigL" to listOf("button", "drag", "gradient"), "trigR" to listOf("button", "drag", "gradient"))
private val flagKeys = setOf("snap", "click", "invL", "invR", "tiltInvert", "dpadSingle", "stickL", "stickR", "rumble", "hap", "hapCustom", "hapPress", "hapRel", "hapStick", "hapTrig", "hapConn")
private val rangedKeys = mapOf("dead" to 0..40, "sens" to 50..150, "clickMs" to 200..1500, "tiltRange" to 15..60, "tiltDead" to 0..15, "diag" to 20..60, "hapInt" to 5..100, "hapMs" to 5..80)

/** Overrides as shared text: flags as true or false, choices by name, the rest as numbers. */
fun behaviourToJson(b: Map<String, Int>): JsonObject = buildJsonObject {
    for ((k, v) in b) {
        val choices = choiceKeys[k]
        when {
            choices != null -> put(k, choices.getOrElse(v) { choices[0] })
            k in flagKeys -> put(k, v != 0)
            else -> put(k, v)
        }
    }
}

private fun parseBehaviour(o: JsonObject): Map<String, Int> {
    val out = linkedMapOf<String, Int>()
    for ((k, e) in o) {
        val p = e as? JsonPrimitive ?: continue
        val choices = choiceKeys[k]
        val range = rangedKeys[k]
        when {
            k in flagKeys -> p.booleanOrNull?.let { out[k] = if (it) 1 else 0 }
            choices != null -> choices.indexOf(p.contentOrNull).takeIf { it >= 0 }?.let { out[k] = it }
            range != null -> p.floatOrNull?.let { out[k] = it.toInt().coerceIn(range) }
        }
    }
    return out
}

private val exportJson = Json {
    prettyPrint = true
    encodeDefaults = true
}

/** Version 2 layout text: `{ "version": 2, "buttons": { ... }, "groups": [ ... ] }`. */
fun exportLayout(buttons: Map<ButtonComponent, ButtonConfig>, groups: List<LayoutGroup>, behaviour: Map<String, Int> = emptyMap()): String {
    val root = buildJsonObject {
        put("version", LAYOUT_FORMAT_VERSION)
        put("buttons", exportJson.encodeToJsonElement(buttons))
        put("groups", exportJson.encodeToJsonElement(groups))
        if (behaviour.isNotEmpty()) put("behaviour", behaviourToJson(behaviour))
    }
    return exportJson.encodeToString(JsonObject.serializer(), root)
}

/**
 * Parses an imported layout, either the current format or the older bare map of controls.
 * Anything missing or unreadable (a whole control, or any single field of one) falls back to its
 * default, and unknown controls are ignored.
 * Throws only when the text is not a JSON object at all.
 */
fun parseLayout(text: String): LayoutBundle {
    val root = Json.parseToJsonElement(text).jsonObject
    val wrapped = root["buttons"] as? JsonObject
    // The older format left out fields that matched the plain defaults, so a missing field there means that
    val legacy = wrapped == null
    val buttons = parseControls(wrapped ?: root, legacy)
    // Older layouts kept trigger input and stick position on the control
    val fromControls = linkedMapOf<String, Int>()
    for ((c, k) in listOf(ButtonComponent.LEFT_TRIGGER to "trigL", ButtonComponent.RIGHT_TRIGGER to "trigR")) {
        val i = TriggerMode.offered.indexOf(buttons[c]?.triggerMode)
        if (i > 0) fromControls[k] = i
    }
    for ((c, k) in listOf(ButtonComponent.LEFT_ANALOG_STICK to "stickL", ButtonComponent.RIGHT_ANALOG_STICK to "stickR")) {
        if (buttons[c]?.stickMode == StickMode.FLOATING) fromControls[k] = 1
    }
    val behaviour = (root["behaviour"] as? JsonObject)?.let(::parseBehaviour)
    return LayoutBundle(
        buttons = buttons,
        groups = (root["groups"] as? JsonArray)?.let(::parseGroups),
        behaviour = if (fromControls.isEmpty()) behaviour else fromControls + (behaviour ?: emptyMap())
    )
}

private fun parseControls(root: JsonObject, legacy: Boolean): Map<ButtonComponent, ButtonConfig> {
    val result = defaultButtonConfigs.toMutableMap()
    for ((key, value) in root) {
        val component = ButtonComponent.entries.firstOrNull { it.name == key } ?: continue
        val obj = value as? JsonObject ?: continue
        val base = if (legacy) ButtonConfig(component)
        else defaultButtonConfigs[component] ?: ButtonConfig.default(component)
        fun primitive(name: String) = runCatching { obj[name]?.jsonPrimitive }.getOrNull()
        result[component] = ButtonConfig(
            component = component,
            visible = primitive("visible")?.booleanOrNull ?: base.visible,
            scale = (primitive("scale")?.floatOrNull ?: base.scale).coerceIn(SCALE_VALUE_RANGE),
            widthScale = (primitive("widthScale")?.floatOrNull ?: base.widthScale).coerceIn(STRETCH_VALUE_RANGE),
            heightScale = (primitive("heightScale")?.floatOrNull ?: base.heightScale).coerceIn(STRETCH_VALUE_RANGE),
            offsetX = (primitive("offsetX")?.floatOrNull ?: base.offsetX).coerceIn(OFFSET_VALUE_RANGE),
            offsetY = (primitive("offsetY")?.floatOrNull ?: base.offsetY).coerceIn(OFFSET_VALUE_RANGE),
            anchor = primitive("anchor")?.contentOrNull
                ?.let { name -> ButtonAnchor.entries.firstOrNull { it.name == name } }
                ?: base.anchor,
            triggerMode = primitive("triggerMode")?.contentOrNull
                ?.let { name -> TriggerMode.entries.firstOrNull { it.name == name } }
                ?: base.triggerMode,
            stickMode = primitive("stickMode")?.contentOrNull
                ?.let { name -> StickMode.entries.firstOrNull { it.name == name } }
                ?: base.stickMode
        )
    }
    return result
}

private fun parseGroups(array: JsonArray): List<LayoutGroup> {
    val taken = mutableSetOf<ButtonComponent>()
    val groups = mutableListOf<LayoutGroup>()
    for (element in array) {
        val obj = element as? JsonObject ?: continue
        val id = (obj["id"] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() } ?: continue
        if (groups.any { it.id == id }) continue
        val name = (obj["name"] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() } ?: id
        val members = runCatching { obj["members"]?.jsonArray }.getOrNull().orEmpty()
            .mapNotNull { m -> (m as? JsonPrimitive)?.contentOrNull }
            .mapNotNull { n -> ButtonComponent.entries.firstOrNull { it.name == n } }
            .filter { taken.add(it) }
        if (members.isNotEmpty()) groups += LayoutGroup(id, name, members)
    }
    return groups
}

/** Every saved layout as one text: `{ "version": 2, "presets": { "name": { buttons, groups }, ... } }`. */
fun exportPresets(
    presets: Map<String, Map<ButtonComponent, ButtonConfig>>,
    presetGroups: Map<String, List<LayoutGroup>>,
    presetBehaviour: Map<String, Map<String, Int>> = emptyMap()
): String {
    val root = buildJsonObject {
        put("version", LAYOUT_FORMAT_VERSION)
        put("presets", buildJsonObject {
            for ((name, buttons) in presets) {
                put(name, buildJsonObject {
                    put("buttons", exportJson.encodeToJsonElement(buttons))
                    put("groups", exportJson.encodeToJsonElement(presetGroups[name] ?: defaultLayoutGroups))
                    presetBehaviour[name]?.takeIf { it.isNotEmpty() }?.let { put("behaviour", behaviourToJson(it)) }
                })
            }
        })
    }
    return exportJson.encodeToString(JsonObject.serializer(), root)
}

/**
 * Reads saved layouts from text made by [exportPresets]. A single exported layout is accepted too and
 * comes back under [singleName]. Throws when the text is not a JSON object or holds no layouts.
 */
fun parsePresets(text: String, singleName: String): Map<String, LayoutBundle> {
    val root = Json.parseToJsonElement(text).jsonObject
    val many = root["presets"] as? JsonObject
    val result = linkedMapOf<String, LayoutBundle>()
    if (many != null) {
        for ((name, value) in many) {
            if (name.isBlank() || value !is JsonObject) continue
            result[name] = parseLayout(value.toString())
        }
    } else {
        result[singleName] = parseLayout(text)
    }
    require(result.isNotEmpty())
    return result
}
