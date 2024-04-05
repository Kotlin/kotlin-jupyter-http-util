package org.jetbrains.kotlinx.jupyter.ktor.client

import io.ktor.client.statement.*
import org.jetbrains.kotlinx.jupyter.api.annotations.JupyterLibrary
import org.jetbrains.kotlinx.jupyter.api.libraries.JupyterIntegration
import org.jetbrains.kotlinx.jupyter.serialization.DeserializeThis
import org.jetbrains.kotlinx.jupyter.ktor.client.core.NotebookHttpResponse

@JupyterLibrary
public object KtorClientIntegration2 : JupyterIntegration() {
    override fun Builder.onLoaded() {
        import("org.jetbrains.kotlinx.jupyter.ktor.client.*")
    }
}

public fun HttpResponse.deserializeJson(className: String? = null): DeserializeThis =
    NotebookHttpResponse(this).deserializeJson(className)


public fun NotebookHttpResponse.deserializeJson(className: String? = null): DeserializeThis {
    return DeserializeThis(bodyAsText(), className)
}
