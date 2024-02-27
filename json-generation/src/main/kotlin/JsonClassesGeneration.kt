package org.jetbrains.kotlinx.jupyter.json

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.*
import org.jetbrains.kotlinx.jupyter.api.*
import org.jetbrains.kotlinx.jupyter.api.annotations.JupyterLibrary
import org.jetbrains.kotlinx.jupyter.api.libraries.JupyterIntegration
import wu.seal.jsontokotlin.library.JsonToKotlinBuilder
import wu.seal.jsontokotlin.model.TargetJsonConverter
import kotlin.reflect.typeOf

/**
 * Variables that have this type get replaced by deserialized value **in the next cell**.
 * [className] is a simple name of the class to be generated that [jsonString] will be deserialized into.
 */
public class DeserializeThis(public val jsonString: String, public val className: String) {
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
 * Usage: declare a variable of [DeserializeThis] type, where some JSON is stored.
 * In the next cell, this variable will contain the deserialized result.
 * The classes for deserialization will be generated automatically based on the actual JSON.
 *
 * ```kotlin
 * val user = DeserializeThis("""{"address":{"street","Baker Street","number":"221B"}}""", "User")
 * // IN THE NEXT CELL:
 * println(user.address.number + " " + user.address.street)
 */
@JupyterLibrary
public class JsonGenerationIntegration : JupyterIntegration() {
    override fun Builder.onLoaded() {
        onLoaded {
            val jsonDeserializer = Json {
                @OptIn(ExperimentalSerializationApi::class)
                explicitNulls = false
            }
            declare(VariableDeclaration("jsonDeserializer", jsonDeserializer, typeOf<Json>()))
        }

        // required for auto-deserialization below
        import("kotlinx.serialization.decodeFromString")
        import("org.jetbrains.kotlinx.jupyter.json.UntypedAny")

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

        updateVariableByRuntimeType<DeserializeThis> { value, _ ->
            try {
                execute(
                    """
                        ${getGeneratedCode(value)}
                        jsonDeserializer.decodeFromString<${value.className}>(""" + "\"\"\"" + value.jsonString + "\"\"\"" + """)
                    """.trimIndent()
                ).name
            } catch (e: Exception) {
                System.err.println("Error during deserialization: ${e.cause?.message}")
                null
            }
        }
    }
}

internal fun getGeneratedCode(value: DeserializeThis): String {
    val jsonElement = Json.Default.parseToJsonElement(value.jsonString)
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
        .build(value.jsonString, value.className)
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
    return try {
        val element = Json.parseToJsonElement(jsonOrNot)
        ((element as? JsonObject)?.entries?.isNotEmpty() == true ||
            (element as? JsonArray)?.isNotEmpty() == true)
    } catch (e: SerializationException) {
        false // Invalid JSON
    }
}
