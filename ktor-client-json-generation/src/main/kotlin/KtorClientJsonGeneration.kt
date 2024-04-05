package org.jetbrains.kotlinx.jupyter.ktor.client.json

import org.jetbrains.kotlinx.jupyter.api.annotations.JupyterLibrary
import org.jetbrains.kotlinx.jupyter.api.libraries.JupyterIntegration
import org.jetbrains.kotlinx.jupyter.json.DeserializeThis
import org.jetbrains.kotlinx.jupyter.ktor.client.NotebookHttpResponse
import io.ktor.client.statement.*

@JupyterLibrary
public object KtorClientJsonGeneration : JupyterIntegration() {
    override fun Builder.onLoaded() {
        import("org.jetbrains.kotlinx.jupyter.ktor.client.json.*")
    }
}

public fun HttpResponse.deserializeJson(className: String = "Response"): DeserializeThis =
    NotebookHttpResponse(this).deserializeJson(className)


public fun NotebookHttpResponse.deserializeJson(className: String = "Response"): DeserializeThis {
    return DeserializeThis(bodyAsText(), className)
}
