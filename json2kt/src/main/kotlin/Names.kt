package org.jetbrains.kotlinx.jupyter.json2kt

import java.util.stream.IntStream

/**
 * This class should always hold a valid Kotlin property name, following camelCase convention and starting from a
 * lower case letter.
 */
@JvmInline
internal value class KotlinPropertyName(val value: String)

@JvmInline
internal value class KotlinClassName(val value: String)

@JvmInline
internal value class JsonName(val value: String)

internal fun pluralToSingular(plural: KotlinClassName, parentClassName: KotlinClassName?): KotlinClassName {
    if (plural.value.endsWith("List") && plural.value.length > 4) {
        return KotlinClassName(plural.value.removeSuffix("List"))
    }
    val singularValue = when {
        plural.value.endsWith("ies") -> plural.value.dropLast(3) + "y" // ignoring -ie and -iy words
        plural.value.endsWith("es") -> plural.value.dropLast(2).takeIf {
            it.endsWith('s') || it.endsWith("sh") || it.endsWith("ch") || it.endsWith('x')
            // ignoring -se, -she, -che and -xe words
        } ?: plural.value.dropLast(1) // ignoring -z words
        plural.value.endsWith('s') && plural.value.length > 1 -> plural.value.dropLast(1)
        else -> plural.value
    }
    val itemValue = if (singularValue == plural.value && plural == parentClassName) {
        plural.value + "Item"
    } else singularValue
    return KotlinClassName(itemValue)
}

/** [this] must be a stream of Unicode code points. */
private fun IntStream.splitByCamelHumpsAndPunctuation(): List<String> {
    val currentToken = StringBuilder()
    val result = mutableListOf<String>()
    fun flushToken() {
        if (currentToken.isNotEmpty()) {
            result.add(currentToken.toString())
            currentToken.clear()
        }
    }

    var isPrevUpperCase = false
    forEach { codePoint ->
        val isUpperCase = Character.isUpperCase(codePoint)
        if (!Character.isLetterOrDigit(codePoint)) {
            flushToken()
        } else if (!isPrevUpperCase && isUpperCase) {
            flushToken()
            currentToken.appendCodePoint(codePoint)
        } else {
            currentToken.appendCodePoint(codePoint)
        }
        isPrevUpperCase = isUpperCase
    }
    flushToken()
    return result
}

private inline fun String.replaceFirstCodePoint(transformCodePoint: (Int) -> Int): String {
    if (isEmpty()) return this
    val firstCharCodePoint = codePointAt(0)
    return Character.toString(transformCodePoint(firstCharCodePoint)) +
        substring(Character.charCount(firstCharCodePoint))
}

internal fun jsonNameToKotlin(jsonName: JsonName): KotlinClassName {
    val camelCaseName = jsonName.value.codePoints()
        .dropWhile { !Character.isLetter(it) }
        .splitByCamelHumpsAndPunctuation()
        .joinToString("") { it.replaceFirstCodePoint(Character::toTitleCase) }
    return KotlinClassName(camelCaseName.takeIf { it.isNotEmpty() } ?: "Value")
}

internal fun classNameToPropertyName(className: KotlinClassName): KotlinPropertyName =
    KotlinPropertyName(className.value.replaceFirstCodePoint(Character::toLowerCase))

private inline fun <T> uniqueName(
    baseName: T,
    takenNames: MutableSet<in T>,
    appendSuffix: (name: T, suffix: String) -> T,
): T {
    if (takenNames.add(baseName)) return baseName
    var i = 1
    while (true) {
        val attempt = appendSuffix(baseName, i.toString())
        if (takenNames.add(attempt)) return attempt
        i++
    }
}

internal fun uniqueName(base: KotlinPropertyName, takenNames: MutableSet<in KotlinPropertyName>): KotlinPropertyName =
    uniqueName(base, takenNames) { name, suffix -> KotlinPropertyName(name.value + suffix) }

internal fun uniqueName(base: KotlinClassName, takenNames: MutableSet<in KotlinClassName>): KotlinClassName =
    uniqueName(base, takenNames) { name, suffix -> KotlinClassName(name.value + suffix) }
