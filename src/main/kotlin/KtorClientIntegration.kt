package org.jetbrains.kotlinx.jupyter.ktor.client

import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.jetbrains.kotlinx.jupyter.api.VariableDeclaration
import org.jetbrains.kotlinx.jupyter.api.VariableName
import org.jetbrains.kotlinx.jupyter.api.annotations.JupyterLibrary
import org.jetbrains.kotlinx.jupyter.api.declare
import org.jetbrains.kotlinx.jupyter.api.libraries.JupyterIntegration


@JupyterLibrary
class KtorClientIntegration : JupyterIntegration() {
    override fun Builder.onLoaded() {

        val ktorVersion = "2.3.7"

        dependencies("io.ktor:ktor-client-core-jvm:$ktorVersion")

        // ktor-client-apache is loaded as a transitive dependency of this artifact,
        // so that it has priority in engine autoselection
        dependencies("io.ktor:ktor-client-apache5-jvm:$ktorVersion")
        dependencies("io.ktor:ktor-client-java-jvm:$ktorVersion")
        dependencies("io.ktor:ktor-client-cio-jvm:$ktorVersion")

        dependencies("io.ktor:ktor-client-auth-jvm:$ktorVersion")
        dependencies("io.ktor:ktor-client-serialization-jvm:$ktorVersion")
        dependencies("io.ktor:ktor-client-encoding-jvm:$ktorVersion")
        dependencies("io.ktor:ktor-client-json-jvm:$ktorVersion")
        dependencies("io.ktor:ktor-client-gson-jvm:$ktorVersion")
        dependencies("io.ktor:ktor-client-jackson-jvm:$ktorVersion")
        dependencies("io.ktor:ktor-client-logging-jvm:$ktorVersion")
        dependencies("io.ktor:ktor-client-resources-jvm:$ktorVersion")
        dependencies("io.ktor:ktor-client-websockets-jvm:$ktorVersion")

        import("org.jetbrains.kotlinx.jupyter.ktor.client.*")

        onLoaded {
            val httpClient = NotebookHttpClient {
                install(ContentNegotiation) {
                    json(Json {
                       ignoreUnknownKeys = true
                    })
                }
            }
            declare("httpClient" to httpClient)
        }
    }
}
