package org.jetbrains.kotlinx.jupyter.serialization

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import kotlinx.serialization.serializer

@Suppress("unused")
public typealias UntypedAny = @Serializable(with = UntypedSerialization::class) Any

@Suppress("unused")
public typealias UntypedAnyNotNull = @Serializable(with = UntypedSerializationNotNull::class) Any

/**
 * This serializer supports deserializing JSON primitives to corresponding Kotlin primitives,
 * lists to Kotlin [List]`<Any?>` recursively, and objects to Kotlin [Map]`<String, Any?>` recursively.
 *
 * It is required when there's no expected type to deserialize into.
 *
 * It also supports serializing its outputs back.
 */
public object UntypedSerialization : KSerializer<Any?> {
    @OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
    override val descriptor: SerialDescriptor
        get() = buildSerialDescriptor("UntypedSerialization", SerialKind.CONTEXTUAL)

    override fun deserialize(decoder: Decoder): Any? {
        if (decoder !is JsonDecoder) error("This deserializer only supports JSON")

        fun deserialize(element: JsonElement): Any? {
            return when (element) {
                is JsonArray -> buildList {
                    element.forEach { add(deserialize(it)) }
                }

                is JsonObject -> buildMap {
                    element.forEach { put(it.key, deserialize(it.value)) }
                }

                is JsonPrimitive -> when {
                    element.isString -> element.content
                    element is JsonNull -> null
                    element.booleanOrNull != null -> element.boolean
                    element.intOrNull != null -> element.int
                    element.longOrNull != null -> element.long
                    element.doubleOrNull != null -> element.double
                    else -> error("Unexpected JSON element during deserialization: $element")
                }
            }
        }

        return deserialize(decoder.decodeJsonElement())
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun serialize(encoder: Encoder, value: Any?) {
        when (value) {
            null -> encoder.encodeNull()
            is String -> encoder.encodeString(value)
            is Double -> encoder.encodeDouble(value)
            is Float -> encoder.encodeFloat(value)
            is Number -> encoder.encodeLong(value.toLong())
            is Boolean -> encoder.encodeBoolean(value)
            is List<*> -> ListSerializer(UntypedSerialization).serialize(encoder, value)

            is Map<*, *> -> {
                value.keys.forEach {
                    require(it is String) { "Only Maps with String keys are supported" }
                }
                @Suppress("UNCHECKED_CAST")
                MapSerializer(serializer<String>(), UntypedSerialization).serialize(encoder, value as Map<String, *>)
            }

            else -> {
                throw IllegalArgumentException("Unexpected type during serialization: $value")
            }
        }
    }
}

/** Like [UntypedSerialization], but for non-nullable Any */
public object UntypedSerializationNotNull : KSerializer<Any> {
    @OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
    override val descriptor: SerialDescriptor
        get() = buildSerialDescriptor("UntypedSerialization", SerialKind.CONTEXTUAL)

    override fun deserialize(decoder: Decoder): Any {
        return UntypedSerialization.deserialize(decoder) ?: throw IllegalArgumentException("Unexpected null")
    }

    override fun serialize(encoder: Encoder, value: Any) {
        UntypedSerialization.serialize(encoder, value)
    }
}
