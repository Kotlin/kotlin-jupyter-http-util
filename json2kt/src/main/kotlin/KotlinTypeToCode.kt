package org.jetbrains.kotlinx.jupyter.json2kt

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import kotlinx.serialization.json.*

public fun jsonDataToKotlinCode(json: JsonElement, rootTypeName: String): String {
    val type = inferOrConstructKotlinType(
        name = KotlinClassName(rootTypeName),
        jsonSamples = listOf(json),
        parentClassName = if (json is JsonObject) null else KotlinClassName(rootTypeName),
    )
        .let { deduplicate(rootType = it, it.collectAllClasses()) }
        .let { disambiguate(rootTypeName = rootTypeName, rootType = it, it.collectAllClasses()) }
    return generateCodeString(type, rootTypeName)
}

private val untypedAnyClassName = ClassName("org.jetbrains.kotlinx.jupyter.serialization", "UntypedAny")
private val untypedAnyNotNullClassName = ClassName("org.jetbrains.kotlinx.jupyter.serialization", "UntypedAnyNotNull")
private val serializableClassName = ClassName("kotlinx.serialization", "Serializable")
private val serialNameClassName = ClassName("kotlinx.serialization", "SerialName")

private fun KotlinType.toKotlinPoet(): TypeName {
    return when (this) {
        is KotlinType.KtBoolean -> BOOLEAN.copy(nullable = nullable)
        is KotlinType.KtDouble -> DOUBLE.copy(nullable = nullable)
        is KotlinType.KtInt -> INT.copy(nullable = nullable)
        is KotlinType.KtLong -> LONG.copy(nullable = nullable)
        is KotlinType.KtString -> STRING.copy(nullable = nullable)
        is KotlinType.KtClass -> ClassName("", clazz.name.value).copy(nullable = nullable)
        is KotlinType.KtList -> LIST.parameterizedBy(elementType.toKotlinPoet()).copy(nullable = nullable)
        is KotlinType.KtAny -> if (nullable) untypedAnyClassName.copy(nullable = true) else untypedAnyNotNullClassName
    }
}

/**
 * Returns a [FileSpec] with generated code for all [classes].
 * Additionally, if [rootType] isn't a class, generates a typealias [rootTypeName] for it.
 */
private fun outputCodeForType(rootTypeName: String, rootType: KotlinType, classes: Iterable<KotlinClass>): FileSpec {
    val fileBuilder = FileSpec.builder("", rootTypeName)

    if (rootType !is KotlinType.KtClass || rootType.clazz.name.value != rootTypeName) {
        fileBuilder.addTypeAlias(TypeAliasSpec.builder(rootTypeName, rootType.toKotlinPoet()).build())
    }

    for (clazz in classes) {
        if (clazz.properties.isEmpty()) {
            fileBuilder.addType(
                TypeSpec.objectBuilder(clazz.name.value)
                    .addModifiers(KModifier.DATA)
                    .addAnnotation(serializableClassName)
                    .build()
            )
            continue
        }

        val classBuilder = TypeSpec.classBuilder(clazz.name.value)
            .addModifiers(KModifier.DATA)
            .addAnnotation(serializableClassName)
        val primaryConstructorBuilder = FunSpec.constructorBuilder()

        for (property in clazz.properties) {
            val propertyType = property.type.toKotlinPoet()
            val parameterBuilder = ParameterSpec.builder(property.kotlinName.value, propertyType)
            if (property.jsonName.value != property.kotlinName.value) {
                parameterBuilder.addAnnotation(
                    AnnotationSpec.builder(serialNameClassName)
                        .addMember("%S", property.jsonName.value)
                        .build()
                )
            }
            primaryConstructorBuilder.addParameter(parameterBuilder.build())
            classBuilder.addProperty(
                PropertySpec.builder(property.kotlinName.value, propertyType)
                    .initializer(property.kotlinName.value)
                    .build()
            )
        }
        classBuilder.primaryConstructor(primaryConstructorBuilder.build())
        fileBuilder.addType(classBuilder.build())
    }

    fileBuilder.indent("    ")
    fileBuilder.defaultImports.add(untypedAnyClassName.packageName)
    fileBuilder.addKotlinDefaultImports(includeJs = false)
    return fileBuilder.build()
}

private fun generateCodeString(rootType: KotlinType, rootTypeName: String): String {
    val sb = StringBuilder()
    outputCodeForType(rootTypeName, rootType, rootType.collectAllClasses()).writeTo(sb)
    return sb.trim().toString()
}
