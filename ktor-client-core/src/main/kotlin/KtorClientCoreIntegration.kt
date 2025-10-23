package org.jetbrains.kotlinx.jupyter.ktor.client.core

import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.jetbrains.kotlinx.jupyter.api.declare
import org.jetbrains.kotlinx.jupyter.api.libraries.JupyterIntegration
import java.util.jar.Attributes
import java.util.jar.Manifest

/**
 * Usage:
 * ```kotlin
 * // Make HTTP requests
 * http.get("https://example.org").bodyAsText()
 *
 * // Use JSON in requests and deserialize it in responses
 * http.post("https://example.org") {
 *     setBody(buildJsonObject {
 *         put("key", "value")
 *     })
 * }.body<MySerializableClass>()
 *
 * // Additional configuration
 * val customClient = http.config {
 *     install(HttpTimeout) {
 *         requestTimeoutMillis = 1000
 *     }
 * }
 * ```
 */
public class KtorClientCoreIntegration : JupyterIntegration() {
    override fun Builder.onLoaded() {
        val ktorVersion = findKtorVersion(KtorClientCoreIntegration::class.java.classLoader)

        fun MutableList<String>.ktorClient(artifactName: String) {
            add("io.ktor:ktor-client-$artifactName-jvm:$ktorVersion")
        }

        dependencies(*buildList {
            ktorClient("core")

            // ktor-client-cio is loaded as a transitive dependency of this artifact,
            // so that it has priority in engine autoselection (it's currently the most popular engine).
            ktorClient("apache")
            ktorClient("apache5")
            ktorClient("java")

            ktorClient("auth")
            ktorClient("serialization")
            ktorClient("encoding")
            ktorClient("json")
            ktorClient("gson")
            ktorClient("jackson")
            ktorClient("logging")
            ktorClient("resources")
            ktorClient("websockets")

            add("io.ktor:ktor-serialization-kotlinx-xml-jvm:$ktorVersion")
        }.toTypedArray())

        import("org.jetbrains.kotlinx.jupyter.ktor.client.core.*")

        onLoaded {
            val httpClient = NotebookHttpClient {
                install(ContentNegotiation) {
                    json(Json {
                        ignoreUnknownKeys = true
                    })
                }
            }
            declare("http" to httpClient)
        }
    }
}

private fun findKtorVersion(classLoader: ClassLoader) =
    requireNotNull(findModuleVersion("io.ktor.client.core", classLoader)) {
        "Failed to find Ktor version for Ktor integration"
    }

/**
 * Attempts to find the implementation version of a given module by inspecting the manifest files
 * available in the provided class loader's resources.
 */
private fun findModuleVersion(
    moduleName: String,
    classLoader: ClassLoader,
): String? {
    val autoModuleKey = Attributes.Name("Automatic-Module-Name")
    val implVersionKey = Attributes.Name.IMPLEMENTATION_VERSION

    return classLoader.getResources("META-INF/MANIFEST.MF")
        .asSequence()
        .mapNotNull { url ->
            runCatching {
                url.openStream().use { Manifest(it) }
            }.getOrNull()?.mainAttributes
        }
        .firstOrNull { it.getValue(autoModuleKey) == moduleName }
        ?.getValue(implVersionKey)
}
