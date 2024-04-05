package org.jetbrains.kotlinx.jupyter.ktor.client.core

import io.ktor.client.call.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*
import io.ktor.util.date.*
import io.ktor.util.reflect.*
import io.ktor.utils.io.*
import kotlinx.coroutines.runBlocking
import java.nio.charset.Charset
import kotlin.coroutines.CoroutineContext

/**
 * A wrapper around [HttpResponse] ([ktorResponse]).
 * It has methods that allow accessing response body without requiring suspendable context.
 */
public class NotebookHttpResponse(public val ktorResponse: HttpResponse) : HttpResponse() {
    override val call: HttpClientCall get() = ktorResponse.call
    @InternalAPI
    override val content: ByteReadChannel get() = ktorResponse.content
    override val coroutineContext: CoroutineContext get() = ktorResponse.coroutineContext
    override val headers: Headers get() = ktorResponse.headers
    override val requestTime: GMTDate get() = ktorResponse.requestTime
    override val responseTime: GMTDate get() = ktorResponse.responseTime
    override val status: HttpStatusCode get() = ktorResponse.status
    override val version: HttpProtocolVersion get() = ktorResponse.version

    /**
     * Tries to receive the payload of the response as a specific type [T].
     *
     * @throws NoTransformationFoundException If no transformation is found for the type [T].
     * @throws DoubleReceiveException If already called [body].
     */
    public inline fun <reified T> body(): T = runBlocking { ktorResponse.body() }

    /**
     * Tries to receive the payload of the response as a specific type [T] described in [typeInfo].
     *
     * @throws NoTransformationFoundException If no transformation is found for the type info [typeInfo].
     * @throws DoubleReceiveException If already called [body].
     */
    public fun <T> body(typeInfo: TypeInfo): T = runBlocking { ktorResponse.body(typeInfo) }

    /**
     * Reads the [HttpResponse.content] as a String. You can pass an optional [fallbackCharset]
     * to specify a charset in the case no one is specified as part of the `Content-Type` response.
     * If no charset specified either as parameter or as part of the response,
     * [io.ktor.client.plugins.HttpPlainText] settings will be used.
     *
     * Note that [fallbackCharset] parameter will be ignored if the response already has a charset.
     *      So it just acts as a fallback, honoring the server preference.
     */
    public fun bodyAsText(fallbackCharset: Charset = Charsets.UTF_8): String =
        runBlocking { ktorResponse.bodyAsText(fallbackCharset) }

    /**
     * Reads the whole [HttpResponse.content] if `Content-Length` is specified.
     * Otherwise, it just reads one byte.
     */
    public fun readBytes(): ByteArray = runBlocking { ktorResponse.readBytes() }

    /**
     * Reads exactly [count] bytes of the [HttpResponse.content].
     */
    public fun readBytes(count: Int): ByteArray = runBlocking { ktorResponse.readBytes(count) }
}
