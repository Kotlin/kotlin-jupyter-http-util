package org.jetbrains.kotlinx.jupyter.json2kt

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeAliasSpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import kotlinx.serialization.json.*

/** In addition to the [code] itself, we return [rootTypeName] — the name of the type to be deserialized. */
public class GeneratedCodeResult(
    public val code: String,
    public val rootTypeName: String,

    /**
     * If true, deserialization input needs to be wrapped in a JSON object with a [JSON2KT_WRAPPER_FIELD_NAME] field,
     * and deserialization output will also contain the underlying value in a [JSON2KT_WRAPPER_FIELD_NAME] field.
     */
    public val isWrapped: Boolean,
)

public const val JSON2KT_WRAPPER_FIELD_NAME: String = "value"

/**
 * @param requestedRootTypeName the preferred name for the type to be deserialized.
 * The actual name may turn out to be different and will be contained in [GeneratedCodeResult.rootTypeName].
 */
public fun jsonDataToKotlinCode(json: JsonElement, requestedRootTypeName: String): GeneratedCodeResult {
    val requestedRootTypeKcn = KotlinClassName(requestedRootTypeName)
    return RootType(
        name = requestedRootTypeKcn,
        type = inferOrConstructKotlinType(
            name = requestedRootTypeKcn,
            jsonSamples = listOf(json),
            parentClassName = if (json is JsonObject) null else requestedRootTypeKcn,
        ),
        isWrapped = false,
    )
        .wrapIfNecessary()
        .let { deduplicate(rootType = it, it.collectAllClasses()) }
        .let { disambiguate(rootType = it, it.collectAllClasses()) }
        .let { GeneratedCodeResult(code = generateCodeString(it), rootTypeName = it.name.value, isWrapped = it.isWrapped) }
}

/** Names in [simpleClassNames] are not allowed to be used as generated class names. */
public interface ReservedNames {
    public val simpleClassNames: Iterable<String>

    public companion object : ReservedNames by AllowedImports
}

private object AllowedImports : ReservedNames {
    override val simpleClassNames: MutableSet<String> = mutableSetOf()

    val Boolean = importedClass(com.squareup.kotlinpoet.BOOLEAN)
    val Double = importedClass(com.squareup.kotlinpoet.DOUBLE)
    val Int = importedClass(com.squareup.kotlinpoet.INT)
    val Long = importedClass(com.squareup.kotlinpoet.LONG)
    val String = importedClass(com.squareup.kotlinpoet.STRING)
    val List = importedClass(com.squareup.kotlinpoet.LIST)

    val UntypedAny = importedClass(ClassName("org.jetbrains.kotlinx.jupyter.serialization", "UntypedAny"))
    val UntypedAnyNotNull = importedClass(ClassName("org.jetbrains.kotlinx.jupyter.serialization", "UntypedAnyNotNull"))
    val Serializable = importedClass(ClassName("kotlinx.serialization", "Serializable"))
    val SerialName = importedClass(ClassName("kotlinx.serialization", "SerialName"))

    private fun importedClass(kotlinPoet: ClassName): ClassName {
        simpleClassNames.add(kotlinPoet.simpleName)
        return kotlinPoet
    }
}

private fun KotlinType.toKotlinPoet(): TypeName {
    return when (this) {
        is KotlinType.KtBoolean -> AllowedImports.Boolean.copy(nullable = nullable)
        is KotlinType.KtDouble -> AllowedImports.Double.copy(nullable = nullable)
        is KotlinType.KtInt -> AllowedImports.Int.copy(nullable = nullable)
        is KotlinType.KtLong -> AllowedImports.Long.copy(nullable = nullable)
        is KotlinType.KtString -> AllowedImports.String.copy(nullable = nullable)
        is KotlinType.KtClass -> ClassName("", clazz.name.value).copy(nullable = nullable)
        is KotlinType.KtList -> AllowedImports.List.parameterizedBy(elementType.toKotlinPoet())
            .copy(nullable = nullable)

        is KotlinType.KtAny -> if (nullable) {
            AllowedImports.UntypedAny.copy(nullable = true)
        } else {
            AllowedImports.UntypedAnyNotNull
        }
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
                    .addAnnotation(AllowedImports.Serializable)
                    .build()
            )
            continue
        }

        val classBuilder = TypeSpec.classBuilder(clazz.name.value)
            .addModifiers(KModifier.DATA)
            .addAnnotation(AllowedImports.Serializable)
        val primaryConstructorBuilder = FunSpec.constructorBuilder()

        for (property in clazz.properties) {
            val propertyType = property.type.toKotlinPoet()
            val parameterBuilder = ParameterSpec.builder(property.kotlinName.value, propertyType)
            if (property.jsonName.value != property.kotlinName.value) {
                parameterBuilder.addAnnotation(
                    AnnotationSpec.builder(AllowedImports.SerialName)
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
    return fileBuilder.build()
}

private fun generateCodeString(rootType: RootType): String {
    val sb = StringBuilder()
    outputCodeForType(rootType.name.value, rootType.type, rootType.collectAllClasses()).writeTo(sb)
    return sb.trim().toString()
}

/**
 * Root types which are essentially `Any` / `Any?` / `List<Any>` / etc. have to be wrapped in a class,
 * because serialization fails otherwise,
 * not being able to find a deserializer for `Any` (despite the `@Serializable` annotation on the type use).
 * When a field of a wrapper has such a type, the serialization completes successfully.
 */
private fun RootType.wrapIfNecessary(): RootType {
    if (!type.shouldWrap()) return this
    return RootType(
        name = name,
        type = KotlinType.KtClass(
            clazz = KotlinClass(
                name = name,
                properties = listOf(
                    KotlinProperty(
                        kotlinName = KotlinPropertyName(JSON2KT_WRAPPER_FIELD_NAME),
                        jsonName = JsonName(JSON2KT_WRAPPER_FIELD_NAME),
                        type = type,
                    ),
                ),
            ),
            nullable = false,
        ),
        isWrapped = true,
    )
}

private tailrec fun KotlinType.shouldWrap(): Boolean = when (this) {
    is KotlinType.KtAny -> true

    is KotlinType.KtClass,
    is KotlinType.Primitive -> false

    is KotlinType.KtList -> this.elementType.shouldWrap()
}
