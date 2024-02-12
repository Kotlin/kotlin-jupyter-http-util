package org.jetbrains.kotlinx.jupyter.ktor.client

import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.jetbrains.kotlinx.jupyter.api.annotations.JupyterLibrary
import org.jetbrains.kotlinx.jupyter.api.declare
import org.jetbrains.kotlinx.jupyter.api.libraries.JupyterIntegration


@JupyterLibrary
class KtorClientIntegration : JupyterIntegration() {
    override fun Builder.onLoaded() {
        val ktorVersion = "2.3.7"

        fun ktorClient(artifactName: String) {
            dependencies("io.ktor:ktor-client-$artifactName-jvm:$ktorVersion")
        }

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

        dependencies("io.ktor:ktor-serialization-kotlinx-xml-jvm:$ktorVersion")

        import("org.jetbrains.kotlinx.jupyter.ktor.client.*")

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
