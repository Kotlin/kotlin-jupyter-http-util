package org.jetbrains.kotlinx.jupyter.json

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.*
import org.jetbrains.kotlinx.jupyter.api.*
import org.jetbrains.kotlinx.jupyter.api.annotations.JupyterLibrary
import org.jetbrains.kotlinx.jupyter.api.libraries.JupyterIntegration
import wu.seal.jsontokotlin.library.JsonToKotlinBuilder
import wu.seal.jsontokotlin.model.TargetJsonConverter
import kotlin.reflect.typeOf

class DeserializationResult(val src: String, val className: String) {
    override fun toString() = src
}

@JupyterLibrary
class JsonGenerationIntegration : JupyterIntegration() {
    override fun Builder.onLoaded() {
        onLoaded {
            val jsonDeserializer = Json {
                @OptIn(ExperimentalSerializationApi::class)
                explicitNulls = false
            }
            declare(VariableDeclaration("jsonDeserializer", jsonDeserializer, typeOf<Json>()))
        }

        updateVariable<DeserializationResult> { value, _ ->
            execute(
                """
                    import kotlinx.serialization.decodeFromString
                    import kotlinx.serialization.json.*
                    import org.jetbrains.kotlinx.jupyter.json.UntypedAny
        
                    ${getGeneratedCode(value)}
                    jsonDeserializer.decodeFromString<${value.className}>(""" + "\"\"\"" + value.src + "\"\"\"" + """)
                """.trimIndent()
            ).name
        }
    }
}

internal fun getGeneratedCode(value: DeserializationResult): String {
    val jsonElement = Json.Default.parseToJsonElement(value.src)
    if (jsonElement is JsonPrimitive) {
        return "typealias ${value.className} = " + when {
            jsonElement.isString -> String::class.simpleName
            jsonElement.booleanOrNull != null -> Boolean::class.simpleName
            jsonElement.intOrNull != null -> Int::class.simpleName
            jsonElement.longOrNull != null -> Long::class.simpleName
            jsonElement.doubleOrNull != null -> Double::class.simpleName
            else -> Nothing::class.simpleName + "?"
        }
    }
    return JsonToKotlinBuilder()
        .setAnnotationLib(TargetJsonConverter.Serializable)
        .build(value.src, value.className)
        .replace("Any?", "UntypedAny?")
        .replace("<Any>", "<UntypedAny?>")
        .let {
            "class (\\w+)$".toRegex().replace(it) { matchResult ->
                "data object " + matchResult.groupValues[1]
            }
        }
        .convertArrayClassToTypeAlias()
}

private fun String.convertArrayClassToTypeAlias(): String {
    val pattern = "class (\\w+) : ArrayList<([\\w @?]+)>\\(\\)".toRegex()
    return pattern.replace(this) { matchResult ->
        "typealias ${matchResult.groupValues[1]} = List<${matchResult.groupValues[2]}>"
    }
}
