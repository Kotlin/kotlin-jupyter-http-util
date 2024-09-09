package org.jetbrains.kotlinx.jupyter.json2kt

import java.util.*

internal fun RootType.collectAllClasses(): Iterable<KotlinClass> {
    class IdentityHashSet<E> : MutableSet<E> by Collections.newSetFromMap(IdentityHashMap())

    /**
     * Iterates using DFS over the tree (directed acyclic graph, to be precise) of referenced classes.
     * Collects all encountered classes into [allClasses] and [visited].
     */
    fun doCollectAllClasses(
        currentType: KotlinType,
        // used for fast checking if we already visited this class
        visited: IdentityHashSet<in KotlinClass>,
        // used for preserving visit order
        allClasses: MutableList<in KotlinClass>
    ) {
        when (currentType) {
            is KotlinType.KtClass -> {
                if (!visited.add(currentType.clazz)) return
                allClasses.add(currentType.clazz)
                // a class may reference other classes in its properties
                for (property in currentType.clazz.properties) {
                    doCollectAllClasses(property.type, visited, allClasses)
                }
            }

            // a list may reference other classes in its element type
            is KotlinType.KtList -> doCollectAllClasses(currentType.elementType, visited, allClasses)

            // no classes referenced
            is KotlinType.Primitive, is KotlinType.KtAny -> {}
        }
    }

    return mutableListOf<KotlinClass>().also { doCollectAllClasses(this.type, IdentityHashSet(), it) }
}

/**
 * Replaces classes that are keys in [replace] by their corresponding values.
 * Replacement is done in the across all classes directly and indirectly referenced by [type].
 * Also, gives all classes that are keys in [rename] a new name (corresponding value).
 * Renaming is also applied in the newly replaced classes and those they reference.
 */
private fun renameAndReplace(
    type: KotlinType,
    /** As structural equality may be slow for [KotlinClass], use reference equality where it makes sense. */
    replace: /* mutable */ IdentityHashMap<KotlinClass, KotlinClass> = IdentityHashMap(),
    rename: IdentityHashMap<KotlinClass, out KotlinClassName> = IdentityHashMap(),
    resultCache: /* mutable */ IdentityHashMap<KotlinClass, KotlinClass>,
): KotlinType {
    return when (type) {
        is KotlinType.Primitive, is KotlinType.KtAny -> type

        is KotlinType.KtClass -> {
            val newClazz = renameAndReplace(type.clazz, replace, rename, resultCache)
            if (newClazz === type.clazz) type else type.copy(clazz = newClazz)
        }

        is KotlinType.KtList -> {
            val newElementType = renameAndReplace(type.elementType, replace, rename, resultCache)
            if (newElementType === type.elementType) type else type.copy(elementType = newElementType)
        }
    }
}

/**
 * Gets rid of duplicate classes. Classes are duplicates when they have the same name and structure.
 * In other words, when [KotlinClass.equals] (deep structural equality) returns `true`.
 */
internal fun deduplicate(rootType: RootType, allClasses: Iterable<KotlinClass>): RootType {
    val classesByName = allClasses.groupBy { it.name }

    val replace = IdentityHashMap<KotlinClass, KotlinClass>()

    for (sameNamedClasses in classesByName.values) {
        if (sameNamedClasses.size == 1) continue

        // group by deep structural equality
        val sameClassesByOriginal = sameNamedClasses.groupByTo(hashMapOf()) { it }

        // quick way out if all classes are unique
        if (sameClassesByOriginal.size == sameNamedClasses.size) continue

        for ((original, copies) in sameClassesByOriginal) {
            for (copy in copies) {
                if (copy !== original) {
                    replace[copy] = original
                }
            }
        }
    }

    return RootType(
        name = rootType.name,
        type = renameAndReplace(rootType.type, replace = replace, resultCache = IdentityHashMap())
    )
}

/**
 * This class describes the root type. If root type is a class, it should have [name] as its type.
 *  If root type is not a class, a type alias named [name] is generated.
 */
internal class RootType(val name: KotlinClassName, val type: KotlinType)

/**
 * Renames classes that have the same name as some other classes, or names that are reserved.
 * Additionally, if [rootType] is a class, we do not rename it (unless its name is reserved),
 * and rename all other same-named instead.
 * If [rootType] is not a class, we need to rename all classes named [rootType]`.name`,
 * so that we can generate a typealias with that name.
 */
internal fun disambiguate(rootType: RootType, allClasses: Iterable<KotlinClass>): RootType {
    var rootTypeName = rootType.name

    val takenNames = mutableSetOf<KotlinClassName>()
    allClasses.mapTo(takenNames) { it.name }
    val reservedNames = ReservedNames.simpleClassNames.map { KotlinClassName(it) }
    takenNames.addAll(reservedNames)

    val classesByName = allClasses.groupBy { it.name }
    val rename = IdentityHashMap<KotlinClass, KotlinClassName>()

    if (rootTypeName in reservedNames) {
        // as much as we'd like not to rename root type, if it has a reserved name, we have to
        rootTypeName = uniqueName(rootTypeName, takenNames)
        if (rootType.type is KotlinType.KtClass) {
            rename[rootType.type.clazz] = rootTypeName
        }
    } else {
        // if top-level type is one of the generated classes, we will need to make sure it isn't renamed,
        // but all others with its name are
        takenNames.add(rootTypeName)
        classesByName[rootTypeName]?.let { classes ->
            for (clazz in classes) {
                if (rootType.type is KotlinType.KtClass) {
                    /** rename all classes except [rootType] */
                    if (clazz === rootType.type.clazz) continue
                    rename[clazz] = uniqueName(clazz.name, takenNames)
                } else {
                    /** rename all classes to free up [rootType]`.name` for [rootType]'s typealias */
                    rename[clazz] = uniqueName(clazz.name, takenNames)
                }
            }
        }
    }

    for ((name, classes) in classesByName) {
        // already scheduled to be renamed above
        if (name == rootTypeName || name == rootType.name && name !in reservedNames) continue

        fun ensureUniqueNames() {
            for (clazz in classes) {
                // already scheduled to be renamed
                if (clazz in rename) continue
                val newName = uniqueName(clazz.name, takenNames)
                if (newName != name) {
                    rename[clazz] = newName
                }
            }
        }

        if (name in reservedNames) {
            // renaming all classes
            ensureUniqueNames()
            continue
        }

        if (classes.size == 1) continue

        // one of the classes is allowed to be named `name`
        takenNames.remove(name)
        ensureUniqueNames()
    }

    return RootType(rootTypeName, renameAndReplace(rootType.type, rename = rename, resultCache = IdentityHashMap()))
}

/**
 * Replaces classes that are keys in [replace] by their corresponding values.
 * Replacement is done in the across all classes directly and indirectly referenced by [clazz].
 * Also, gives all classes that are keys in [rename] a new name (corresponding value).
 * Renaming is also applied in the newly replaced classes and those they reference.
 */
private fun renameAndReplace(
    clazz: KotlinClass,
    replace: /* mutable */ IdentityHashMap<KotlinClass, KotlinClass> = IdentityHashMap(),
    rename: IdentityHashMap<KotlinClass, out KotlinClassName> = IdentityHashMap(),
    resultCache: /* mutable */ IdentityHashMap<KotlinClass, KotlinClass>,
): KotlinClass {
    // replace the class if needed
    val newClass = replace[clazz] ?: clazz

    val resultFromCache = resultCache[newClass]
    if (resultFromCache != null) return resultFromCache

    val newProperties = newClass.properties.map { property ->
        val newPropertyType = renameAndReplace(property.type, replace, rename, resultCache)
        if (newPropertyType === property.type) property else property.copy(type = newPropertyType)
    }

    val propertiesChanged = newProperties.asSequence().zip(newClass.properties.asSequence())
        .any { (new, old) -> old !== new }

    if (!propertiesChanged && newClass !in rename) {
        // this way we can skip iterating over properties of this class in the future
        resultCache[newClass] = newClass
        return newClass
    }

    return KotlinClass(name = rename[newClass] ?: newClass.name, properties = newProperties)
        // to use this class instance in the future instead of creating a new for one each occurrence,
        // we'll replace it instead of renaming
        .also { replace[clazz] = it }
        .also { resultCache[newClass] = it }
}
