package org.jetbrains.kotlinx.jupyter.ktor.client

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

class NotebookHttpResponse(val ktorResponse: HttpResponse) : HttpResponse() {
    override val call: HttpClientCall get() = ktorResponse.call
    @InternalAPI
    override val content: ByteReadChannel get() = ktorResponse.content
    override val coroutineContext: CoroutineContext get() = ktorResponse.coroutineContext
    override val headers: Headers get() = ktorResponse.headers
    override val requestTime: GMTDate get() = ktorResponse.requestTime
    override val responseTime: GMTDate get() = ktorResponse.responseTime
    override val status: HttpStatusCode get() = ktorResponse.status
    override val version: HttpProtocolVersion get() = ktorResponse.version

    inline fun <reified T> body(): T = runBlocking { ktorResponse.body() }
    fun <T> body(typeInfo: TypeInfo): T = runBlocking { ktorResponse.body(typeInfo) }
    fun bodyAsText(fallbackCharset: Charset = Charsets.UTF_8): String =
        runBlocking { ktorResponse.bodyAsText(fallbackCharset) }
    fun readBytes(): ByteArray = runBlocking { ktorResponse.readBytes() }
    fun readBytes(count: Int): ByteArray = runBlocking { ktorResponse.readBytes(count) }
}
