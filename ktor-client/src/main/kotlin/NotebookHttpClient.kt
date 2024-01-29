package org.jetbrains.kotlinx.jupyter.ktor.client

import io.ktor.client.*
import io.ktor.client.engine.*

class NotebookHttpClient(val ktorClient: HttpClient) {
    constructor(
        engine: HttpClientEngine,
        userConfig: HttpClientConfig<out HttpClientEngineConfig> = HttpClientConfig()
    ) : this(HttpClient(engine, userConfig))

    constructor(block: HttpClientConfig<*>.() -> Unit = {}) : this(HttpClient(block))
}

fun <T : HttpClientEngineConfig> NotebookHttpClient(
    engineFactory: HttpClientEngineFactory<T>,
    block: HttpClientConfig<T>.() -> Unit = {}
): NotebookHttpClient = NotebookHttpClient(HttpClient(engineFactory, block))

fun NotebookHttpClient.config(block: HttpClientConfig<*>.() -> Unit): NotebookHttpClient {
    return NotebookHttpClient(ktorClient.config(block))
}


