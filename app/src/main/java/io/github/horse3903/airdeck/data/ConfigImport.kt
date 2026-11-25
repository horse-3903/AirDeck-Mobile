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
)

private val exportJson = Json {
    prettyPrint = true
    encodeDefaults = true
}

/** Version 2 layout text: `{ "version": 2, "buttons": { ... }, "groups": [ ... ] }`. */
fun exportLayout(buttons: Map<ButtonComponent, ButtonConfig>, groups: List<LayoutGroup>): String {
    val root = buildJsonObject {
        put("version", LAYOUT_FORMAT_VERSION)
        put("buttons", exportJson.encodeToJsonElement(buttons))
        put("groups", exportJson.encodeToJsonElement(groups))
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
    return LayoutBundle(
        buttons = parseControls(wrapped ?: root, legacy),
        groups = (root["groups"] as? JsonArray)?.let(::parseGroups)
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
            offsetX = (primitive("offsetX")?.floatOrNull ?: base.offsetX).coerceIn(OFFSET_VALUE_RANGE),
            offsetY = (primitive("offsetY")?.floatOrNull ?: base.offsetY).coerceIn(OFFSET_VALUE_RANGE),
            anchor = primitive("anchor")?.contentOrNull
                ?.let { name -> ButtonAnchor.entries.firstOrNull { it.name == name } }
                ?: base.anchor,
            triggerMode = primitive("triggerMode")?.contentOrNull
                ?.let { name -> TriggerMode.entries.firstOrNull { it.name == name } }
                ?: base.triggerMode
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
