package org.jetbrains.kotlinx.jupyter.serialization

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.*
import org.jetbrains.kotlinx.jupyter.api.*
import org.jetbrains.kotlinx.jupyter.api.annotations.JupyterLibrary
import org.jetbrains.kotlinx.jupyter.api.libraries.FieldHandlerFactory
import org.jetbrains.kotlinx.jupyter.api.libraries.JupyterIntegration
import org.jetbrains.kotlinx.jupyter.api.libraries.TypeDetection
import wu.seal.jsontokotlin.library.JsonToKotlinBuilder
import wu.seal.jsontokotlin.model.TargetJsonConverter
import kotlin.reflect.typeOf

/**
 * Variables that have this type get replaced by deserialized value **in the next cell**.
 * [className] is a simple name of the class to be generated that [jsonString] will be deserialized into.
 */
public class DeserializeThis(public val jsonString: String, public val className: String?) {
    override fun toString(): String = jsonString

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DeserializeThis

        return jsonString == other.jsonString && className == other.className
    }

    override fun hashCode(): Int = 31 * jsonString.hashCode() + className.hashCode()
}

/**
 * Variables with values returned by this function get replaced by deserialized value **in the next cell**.
 * [className] is a simple name of the class to be generated that [jsonString] will be deserialized into.
 *
 * Usage:
 * ```kotlin
 * val user = """{"address":{"street","Baker Street","number":"221B"}}""".deserializeJson()
 * // IN THE NEXT CELL:
 * println(user.address.number + " " + user.address.street)
 * ```
 */
public fun String.deserializeJson(className: String? = null): DeserializeThis {
    return DeserializeThis(jsonString = this, className = className)
}

/**
 * Usage: declare a variable of [DeserializeThis] type, where some JSON is stored.
 * In the next cell, this variable will contain the deserialized result.
 * The classes for deserialization will be generated automatically based on the actual JSON.
 *
 * ```kotlin
 * val user = """{"address":{"street","Baker Street","number":"221B"}}""".deserializeJson()
 * // IN THE NEXT CELL:
 * println(user.address.number + " " + user.address.street)
 * ```
 */
@JupyterLibrary
public class SerializationIntegration : JupyterIntegration() {
    override fun Builder.onLoaded() {
        onLoaded {
            val jsonDeserializer = Json {
                @OptIn(ExperimentalSerializationApi::class)
                explicitNulls = false
            }
            declare(VariableDeclaration("jsonDeserializer", jsonDeserializer, typeOf<Json>()))
        }

        import("kotlinx.serialization.*")
        import("kotlinx.serialization.json.*")
        import("org.jetbrains.kotlinx.jupyter.serialization.deserializeJson")

        // required for auto-deserialization below
        import("org.jetbrains.kotlinx.jupyter.serialization.UntypedAny")

        addRenderer(
            createRenderer(
                renderCondition = {
                    val value = it.value
                    value is String && shouldHighlightAsJson(value) ||
                        value is DeserializeThis && shouldHighlightAsJson(value.jsonString)
                },
                renderAction = { it ->
                    JSON(it.value as? String ?: (it.value as DeserializeThis).jsonString)
                },
            )
        )

        val fieldHandler = FieldHandlerFactory.createUpdateHandler<DeserializeThis>(TypeDetection.RUNTIME) { value, prop ->
            try {
                val className = value.className ?: prop.name.replaceFirstChar(Char::titlecaseChar)
                execute(
                    getGeneratedCode(value.jsonString, className) + "\n" +
                        "jsonDeserializer.decodeFromString<$className>(\"\"\"${value.jsonString}\"\"\")"
                ).name
            } catch (e: Exception) {
                display("Error during deserialization: ${e.cause?.message}", id = null)
                null
            }
        }
        notebook.fieldsHandlersProcessor.register(fieldHandler, ProcessingPriority.HIGHEST)
    }
}

internal fun getGeneratedCode(jsonString: String, className: String): String {
    val jsonElement = Json.Default.parseToJsonElement(jsonString)
    if (jsonElement is JsonPrimitive) {
        return "typealias $className = " + when {
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
        .build(jsonString, className)
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

private fun shouldHighlightAsJson(jsonOrNot: String): Boolean {
    if (jsonOrNot.length > 3_000_000) return false
    return try {
        val element = Json.parseToJsonElement(jsonOrNot)
        ((element as? JsonObject)?.entries?.isNotEmpty() == true ||
            (element as? JsonArray)?.isNotEmpty() == true)
    } catch (e: SerializationException) {
        false // Invalid JSON
    }
}