package org.jetbrains.kotlinx.jupyter.json

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.SerializersModule
import org.jetbrains.kotlinx.jupyter.api.*
import org.jetbrains.kotlinx.jupyter.api.annotations.JupyterLibrary
import org.jetbrains.kotlinx.jupyter.api.libraries.JupyterIntegration
import wu.seal.jsontokotlin.library.JsonToKotlinBuilder
import wu.seal.jsontokotlin.model.TargetJsonConverter
import kotlin.reflect.full.createType

fun validateJson(jsonString: String): Boolean {
    return try {
        val element = Json.Default.parseToJsonElement(jsonString)
        element !is JsonPrimitive
            && (runCatching { element.jsonObject.entries.isNotEmpty() }.getOrNull() == true ||
            runCatching { element.jsonArray.size > 0 }.getOrNull() == true)  // JSON is valid
    } catch (e: Exception) {
        false // Invalid JSON
    }
}

private fun String.convertArrayClassToTypeAlias(): String {
    val pattern = "class (\\w+) : ArrayList<([\\w @]+)>\\(\\)".toRegex()
    return pattern.replace(this) { matchResult ->
        "typealias ${matchResult.groupValues[1]} = List<${matchResult.groupValues[2]}>"
    }
}

class DeserializationResult(val src: String, val className: String) {
    override fun toString() = src
}

@JupyterLibrary
class JsonGenerationIntegration : JupyterIntegration() {
    override fun Builder.onLoaded() {
        fun String.printJson() = HTML(
            """
            <body>
              <style>
              json-viewer {
                /* Background, font and indentation */
                --background-color: #0000;
                --font-size: 1.5rem;
              }
              </style>
              <script src="https://unpkg.com/@alenaksu/json-viewer@2.0.0/dist/json-viewer.bundle.js"></script>
              <json-viewer id="json" data='${this.replace("\n", "")}'></json-viewer>
              <script>
                    document.querySelector('#json').expandAll()
              </script>
            </body>
        """.trimIndent()
        )

        onLoaded {
            val jsonDeserializer = Json {
                @OptIn(ExperimentalSerializationApi::class)
                explicitNulls = false
                serializersModule = SerializersModule {
                    this.polymorphicDefaultDeserializer(Any::class) {
                        UntypedSerialization
                    }
                }
            }
            declare(VariableDeclaration("jsonDeserializer", jsonDeserializer, Json::class.createType()))
        }

        addRenderer(object : RendererHandler {
            override val execution: ResultHandlerExecution
                get() = ResultHandlerExecution { _, result ->
                    FieldValue(
                        (result.value as String).printJson(),
                        result.name
                    )
                }

            override fun accepts(value: Any?): Boolean {
                return value is String && validateJson(value)
            }

            override fun replaceVariables(mapping: Map<String, String>): RendererHandler {
                return this
            }
        })
        updateVariable<DeserializationResult> { value, _ ->
            execute(
                """
            import kotlinx.serialization.decodeFromString
            import kotlinx.serialization.json.*
            import kotlinx.serialization.Contextual

            ${getGeneratedCode(value)}
            jsonDeserializer.decodeFromString<${value.className}>(""" + "\"\"\"" + value.src + "\"\"\"" + """)
        """
            ).name
        }
    }
}

fun getGeneratedCode(value: DeserializationResult): String {
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
        .replace("Any?", "@Contextual Any?")
        .replace("<Any>", "<@Contextual Any>")
        .convertArrayClassToTypeAlias()
}

@OptIn(ExperimentalSerializationApi::class)
object UntypedSerialization : DeserializationStrategy<Any> {

    @OptIn(InternalSerializationApi::class)
    override val descriptor: SerialDescriptor
        get() = buildSerialDescriptor(
            "UntypedSerialization",
            SerialKind.CONTEXTUAL,
        )

    override fun deserialize(decoder: Decoder): Any {
        if (decoder !is JsonDecoder) error("This deserializer only supports JSON")

        fun deserialize(element: JsonElement): Any {
            return when (element) {
                is JsonArray -> buildList {
                    element.forEach { add(deserialize(it)) }
                }

                is JsonObject -> buildMap {
                    element.forEach { put(it.key, deserialize(it.value)) }
                }

                is JsonPrimitive -> when {
                    element.isString -> element.content
                    element.booleanOrNull != null -> element.boolean
                    element.intOrNull != null -> element.int
                    element.longOrNull != null -> element.long
                    element.doubleOrNull != null -> element.double
                    else -> error("Unexpected JSON element: $element")
                }
            }
        }

        return deserialize(decoder.decodeJsonElement())
    }
}
