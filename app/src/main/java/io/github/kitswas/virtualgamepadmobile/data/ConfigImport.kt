package io.github.kitswas.virtualgamepadmobile.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Parses an imported layout. Anything missing or unreadable (a whole component, or any single
 * field of one) falls back to the default value, and unknown components are ignored.
 * Throws only when the text is not a JSON object at all.
 */
fun parseLenientButtonConfigs(text: String): Map<ButtonComponent, ButtonConfig> {
    val root = Json.parseToJsonElement(text).jsonObject
    val result = defaultButtonConfigs.toMutableMap()
    for ((key, value) in root) {
        val component = ButtonComponent.entries.firstOrNull { it.name == key } ?: continue
        val obj = value as? JsonObject ?: continue
        val base = defaultButtonConfigs[component] ?: ButtonConfig.default(component)
        fun primitive(name: String) = runCatching { obj[name]?.jsonPrimitive }.getOrNull()
        result[component] = ButtonConfig(
            component = component,
            visible = primitive("visible")?.booleanOrNull ?: base.visible,
            scale = primitive("scale")?.floatOrNull ?: base.scale,
            offsetX = primitive("offsetX")?.floatOrNull ?: base.offsetX,
            offsetY = primitive("offsetY")?.floatOrNull ?: base.offsetY,
            anchor = primitive("anchor")?.contentOrNull
                ?.let { name -> ButtonAnchor.entries.firstOrNull { it.name == name } }
                ?: base.anchor
        )
    }
    return result
}
