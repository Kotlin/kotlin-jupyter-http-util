package org.jetbrains.kotlinx.jupyter.json2kt

import kotlinx.serialization.json.*
import java.util.*

/**
 * This class is a simple representation of a type in the generated code.
 * Kt- prefixes are used to differentiate from standard library types.
 */
internal sealed class KotlinType {
    data class KtClass(val clazz: KotlinClass, val nullable: Boolean) : KotlinType()

    data class KtList(val elementType: KotlinType, val nullable: Boolean) : KotlinType()

    data class KtAny(val nullable: Boolean) : KotlinType()

    sealed class Primitive : KotlinType()
    data class KtInt(val nullable: Boolean) : Primitive()
    data class KtLong(val nullable: Boolean) : Primitive()
    data class KtDouble(val nullable: Boolean) : Primitive()
    data class KtString(val nullable: Boolean) : Primitive()
    data class KtBoolean(val nullable: Boolean) : Primitive()

    fun withNullability(nullable: Boolean) = when (this) {
        is KtClass -> copy(nullable = nullable)
        is KtList -> copy(nullable = nullable)
        is KtAny -> copy(nullable = nullable)
        is KtBoolean -> copy(nullable = nullable)
        is KtDouble -> copy(nullable = nullable)
        is KtInt -> copy(nullable = nullable)
        is KtLong -> copy(nullable = nullable)
        is KtString -> copy(nullable = nullable)
    }
}

/**
 * This class is a simple representation of a class in the generated code.
 * The class MUST NOT directly or indirectly reference itself.
 */
internal data class KotlinClass(val name: KotlinClassName, val properties: List<KotlinProperty>) {
    private val hashCode by lazy { Objects.hash(name, properties) }

    /**
     * We use deep structural equality, and hashCode that is consistent with it.
     * As all classes are immutable, it is safely cached.
     */
    override fun hashCode(): Int = hashCode

    /**
     * We use deep structural equality, so this operation can be expensive,
     * proportional to the referenced type tree size.
     * Use referential equality where this isn't necessary.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KotlinClass

        return name == other.name && hashCode == other.hashCode && properties == other.properties
    }
}

/**
 * This class is a simple representation of a class property in the generated code.
 * [kotlinName] should be unique inside each class.
 * [jsonName] is the property name in the original JSON, it may be used to generate [kotlinx.serialization.SerialName]
 * annotation if [kotlinName] is different.
 */
internal data class KotlinProperty(
    val kotlinName: KotlinPropertyName,
    val jsonName: JsonName,
    val type: KotlinType,
)

/** [parentClassName] is the name of parent class. Can be used for intelligent name generation. */
internal fun inferOrConstructKotlinType(
    name: KotlinClassName,
    jsonSamples: List<JsonElement>,
    parentClassName: KotlinClassName?,
): KotlinType {
    var hasNull = false
    var hasNonNullPrimitive = false
    var hasObject = false
    var hasArray = false

    for (sample in jsonSamples) {
        when (sample) {
            is JsonNull -> hasNull = true
            is JsonPrimitive -> hasNonNullPrimitive = true
            is JsonArray -> hasArray = true
            is JsonObject -> hasObject = true
        }
    }

    @Suppress("UNCHECKED_CAST")
    return when {
        // only primitives (possibly including nulls)
        !hasObject && !hasArray -> inferKotlinType(jsonSamples as List<JsonPrimitive>)

        // only objects or nulls
        hasObject && !hasNonNullPrimitive && !hasArray -> KotlinType.KtClass(
            clazz = constructKotlinClass(name, jsonSamples.filterIsInstance<JsonObject>()),
            nullable = hasNull,
        )

        // only arrays or nulls
        !hasObject && !hasNonNullPrimitive -> KotlinType.KtList(
            elementType = inferOrConstructKotlinType(
                name = pluralToSingular(name, parentClassName),
                jsonSamples = jsonSamples.flatMap { it as? JsonArray ?: emptyList() },
                parentClassName = parentClassName,
            ),
            nullable = hasNull,
        )

        // mixed types
        else -> KotlinType.KtAny(nullable = hasNull)
    }
}

private fun constructKotlinClass(
    name: KotlinClassName,
    jsonSamples: List<JsonObject>,
): KotlinClass {
    val kotlinPropertyNames = mutableSetOf<KotlinPropertyName>()
    val properties = jsonSamples.flatMap { it.entries }
        .groupBy({ JsonName(it.key) }, { it.value })
        .map { (jsonName, values) ->
            val kotlinClassName = jsonNameToKotlin(jsonName)
            val kotlinName = uniqueName(classNameToPropertyName(kotlinClassName), kotlinPropertyNames)
            KotlinProperty(
                kotlinName,
                jsonName,
                inferOrConstructKotlinType(kotlinClassName, values, name).let {
                    if (values.size < jsonSamples.size) {
                        // the field is missing in some of the elements
                        it.withNullability(true)
                    } else {
                        it
                    }
                },
            )
        }
    return KotlinClass(name, properties)
}

/**
 * Order is important here, wider types need to be listed last.
 * That way we can compare enum entries by number type wideness
 */
private enum class NumberType : Comparable<NumberType> {
    INT,
    LONG,
    DOUBLE,
}

private fun inferKotlinType(jsonSamples: List<JsonPrimitive>): KotlinType {
    var hasNull = false
    var hasString = false
    var hasBoolean = false
    var numberType: NumberType? = null

    for (sample in jsonSamples) {
        when {
            sample is JsonNull -> hasNull = true
            sample.isString -> hasString = true
            sample.booleanOrNull != null -> hasBoolean = true
            sample.content.toIntOrNull() != null -> numberType = maxOf(numberType ?: NumberType.INT, NumberType.INT)
            sample.content.toLongOrNull() != null -> numberType = maxOf(numberType ?: NumberType.LONG, NumberType.LONG)
            else -> numberType = maxOf(numberType ?: NumberType.LONG, NumberType.LONG)
        }
    }

    return when {
        // no elements or only nulls
        !hasString && !hasBoolean && numberType == null -> KotlinType.KtAny(nullable = true)

        // only strings or nulls
        hasString && !hasBoolean && numberType == null -> KotlinType.KtString(hasNull)

        // only booleans or nulls
        !hasString && hasBoolean && numberType == null -> KotlinType.KtBoolean(hasNull)

        // only numbers or nulls
        !hasString && !hasBoolean && numberType != null -> when (numberType) {
            NumberType.INT -> KotlinType.KtInt(hasNull)
            NumberType.LONG -> KotlinType.KtLong(hasNull)
            NumberType.DOUBLE -> KotlinType.KtDouble(hasNull)
        }

        // mixed types
        else -> KotlinType.KtAny(hasNull)
    }
}
