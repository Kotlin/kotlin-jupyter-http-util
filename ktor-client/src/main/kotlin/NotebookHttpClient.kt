package org.jetbrains.kotlinx.jupyter.ktor.client

import io.ktor.client.*
import io.ktor.client.engine.*

/**
 * A wrapper around [HttpClient] ([ktorClient]).
 * It has extensions that allow making HTTP requests without requiring suspendable context.
 */
public class NotebookHttpClient(public val ktorClient: HttpClient) {
    public constructor(
        engine: HttpClientEngine,
        userConfig: HttpClientConfig<out HttpClientEngineConfig> = HttpClientConfig()
    ) : this(HttpClient(engine, userConfig))

    public constructor(block: HttpClientConfig<*>.() -> Unit = {}) : this(HttpClient(block))
}

public fun <T : HttpClientEngineConfig> NotebookHttpClient(
    engineFactory: HttpClientEngineFactory<T>,
    block: HttpClientConfig<T>.() -> Unit = {}
): NotebookHttpClient = NotebookHttpClient(HttpClient(engineFactory, block))

public fun NotebookHttpClient.config(block: HttpClientConfig<*>.() -> Unit): NotebookHttpClient {
    return NotebookHttpClient(ktorClient.config(block))
}


