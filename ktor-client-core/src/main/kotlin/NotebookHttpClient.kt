package org.jetbrains.kotlinx.jupyter.ktor.client.core

import io.ktor.client.*
import io.ktor.client.engine.*
import java.util.ServiceLoader

/**
 * A wrapper around [HttpClient] ([ktorClient]).
 * It has extensions that allow making HTTP requests without requiring suspendable context.
 */
public class NotebookHttpClient(public val ktorClient: HttpClient) {
    public constructor(
        engine: HttpClientEngine,
        userConfig: HttpClientConfig<out HttpClientEngineConfig> = HttpClientConfig()
    ) : this(HttpClient(engine, userConfig))

    /**
     * Constructs an asynchronous [NotebookHttpClient] using optional [block] for configuring this client.
     *
     * The [HttpClientEngine] is selected from the dependencies using [ServiceLoader].
     * The first found implementation that provides [HttpClientEngineContainer] service implementation is used.
     * An exception is thrown if no implementations found.
     *
     * See https://ktor.io/docs/http-client-engines.html
     */
    public constructor(block: HttpClientConfig<*>.() -> Unit = {}) : this(HttpClient(block))
}

/**
 * Creates an asynchronous [NotebookHttpClient] with the specified [HttpClientEngineFactory] and optional [block]
 * configuration.
 * Note that a specific platform may require a specific engine for processing requests.
 * You can learn more about available engines from [Engines](https://ktor.io/docs/http-client-engines.html).
 */
public fun <T : HttpClientEngineConfig> NotebookHttpClient(
    engineFactory: HttpClientEngineFactory<T>,
    block: HttpClientConfig<T>.() -> Unit = {}
): NotebookHttpClient = NotebookHttpClient(HttpClient(engineFactory, block))

/**
 * Returns a new [NotebookHttpClient] by copying this client's configuration
 * and additionally configured by the [block] parameter.
 */
public fun NotebookHttpClient.config(block: HttpClientConfig<*>.() -> Unit): NotebookHttpClient {
    return NotebookHttpClient(ktorClient.config(block))
}
