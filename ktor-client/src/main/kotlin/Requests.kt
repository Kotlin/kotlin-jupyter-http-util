package org.jetbrains.kotlinx.jupyter.ktor.client

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking


/**
 * Executes an [HttpClient]'s request with the parameters specified using [builder].
 *
 * Learn more from [Making requests](https://ktor.io/docs/request.html).
 */
public fun NotebookHttpClient.request(
    builder: HttpRequestBuilder = HttpRequestBuilder()
): NotebookHttpResponse = NotebookHttpResponse(runBlocking { ktorClient.request(builder) })

/**
 * Executes an [HttpClient]'s request with the parameters specified in [block].
 *
 * Learn more from [Making requests](https://ktor.io/docs/request.html).
 */
public inline fun NotebookHttpClient.request(crossinline block: HttpRequestBuilder.() -> Unit): NotebookHttpResponse =
    NotebookHttpResponse(runBlocking { ktorClient.request(block) })

/**
 * Executes an [HttpClient]'s request with the [urlString] and the parameters configured in [block].
 *
 * Learn more from [Making requests](https://ktor.io/docs/request.html).
 */
public suspend inline fun NotebookHttpClient.request(
    urlString: String,
    crossinline block: HttpRequestBuilder.() -> Unit = {}
): NotebookHttpResponse = NotebookHttpResponse(runBlocking { ktorClient.request(urlString, block) })

/**
 * Executes an [HttpClient]'s request with the [url] and the parameters configured in [block].
 *
 * Learn more from [Making requests](https://ktor.io/docs/request.html).
 */
public suspend inline fun NotebookHttpClient.request(
    url: Url,
    crossinline block: HttpRequestBuilder.() -> Unit = {}
): NotebookHttpResponse = NotebookHttpResponse(runBlocking { ktorClient.request(url, block) })

/**
 * Executes an [HttpClient]'s GET request with the parameters configured in [builder].
 *
 * Learn more from [Making requests](https://ktor.io/docs/request.html).
 */
public fun NotebookHttpClient.get(builder: HttpRequestBuilder): NotebookHttpResponse =
    NotebookHttpResponse(runBlocking { ktorClient.get(builder) })


/**
 * Executes an [HttpClient]'s POST request with the parameters configured in [builder].
 *
 * Learn more from [Making requests](https://ktor.io/docs/request.html).
 */
public fun NotebookHttpClient.post(builder: HttpRequestBuilder): NotebookHttpResponse =
    NotebookHttpResponse(runBlocking { ktorClient.post(builder) })

/**
 * Executes a [HttpClient] PUT request with the parameters configured in [builder].
 *
 * Learn more from [Making requests](https://ktor.io/docs/request.html).
 */
public fun NotebookHttpClient.put(builder: HttpRequestBuilder): HttpResponse  =
    runBlocking { ktorClient.put(builder) }

/**
 * Executes a [HttpClient] DELETE request with the parameters configured in [builder].
 *
 * Learn more from [Making requests](https://ktor.io/docs/request.html).
 */
public fun NotebookHttpClient.delete(builder: HttpRequestBuilder): NotebookHttpResponse =
    NotebookHttpResponse(runBlocking { ktorClient.delete(builder) })

/**
 * Executes a [HttpClient] OPTIONS request with the parameters configured in [builder].
 *
 * Learn more from [Making requests](https://ktor.io/docs/request.html).
 */
public fun NotebookHttpClient.options(builder: HttpRequestBuilder): NotebookHttpResponse =
    NotebookHttpResponse(runBlocking { ktorClient.options(builder) })

/**
 * Executes a [HttpClient] PATCH request with the parameters configured in [builder].
 *
 * Learn more from [Making requests](https://ktor.io/docs/request.html).
 */
public fun NotebookHttpClient.patch(builder: HttpRequestBuilder): NotebookHttpResponse =
    NotebookHttpResponse(runBlocking { ktorClient.patch(builder) })

/**
 * Executes a [HttpClient] HEAD request with the parameters configured in [builder].
 *
 * Learn more from [Making requests](https://ktor.io/docs/request.html).
 */
public fun NotebookHttpClient.head(builder: HttpRequestBuilder): HttpResponse  =
    runBlocking { ktorClient.head(builder) }

/**
 * Executes an [HttpClient]'s GET request with the parameters configured in [block].
 *
 * Learn more from [Making requests](https://ktor.io/docs/request.html).
 */
public inline fun NotebookHttpClient.get(crossinline block: HttpRequestBuilder.() -> Unit): NotebookHttpResponse =
    NotebookHttpResponse(runBlocking { ktorClient.get(block) })

/**
 * Executes an [HttpClient]'s POST request with the parameters configured in [block].
 *
 * Learn more from [Making requests](https://ktor.io/docs/request.html).
 */
public inline fun NotebookHttpClient.post(crossinline block: HttpRequestBuilder.() -> Unit): NotebookHttpResponse =
    NotebookHttpResponse(runBlocking { ktorClient.post(block) })

/**
 * Executes an [HttpClient]'s PUT request with the parameters configured in [block].
 *
 * Learn more from [Making requests](https://ktor.io/docs/request.html).
 */
public inline fun NotebookHttpClient.put(crossinline block: HttpRequestBuilder.() -> Unit): NotebookHttpResponse =
    NotebookHttpResponse(runBlocking { ktorClient.put(block) })

/**
 * Executes an [HttpClient]'s DELETE request with the parameters configured in [block].
 *
 * Learn more from [Making requests](https://ktor.io/docs/request.html).
 */
public inline fun NotebookHttpClient.delete(crossinline block: HttpRequestBuilder.() -> Unit): NotebookHttpResponse =
    NotebookHttpResponse(runBlocking { ktorClient.delete(block) })

/**
 * Executes an [HttpClient]'s OPTIONS request with the parameters configured in [block].
 *
 * Learn more from [Making requests](https://ktor.io/docs/request.html).
 */
public inline fun NotebookHttpClient.options(crossinline block: HttpRequestBuilder.() -> Unit): NotebookHttpResponse =
    NotebookHttpResponse(runBlocking { ktorClient.options(block) })

/**
 * Executes an [HttpClient]'s PATCH request with the parameters configured in [block].
 *
 * Learn more from [Making requests](https://ktor.io/docs/request.html).
 */
public inline fun NotebookHttpClient.patch(crossinline block: HttpRequestBuilder.() -> Unit): NotebookHttpResponse =
    NotebookHttpResponse(runBlocking { ktorClient.patch(block) })

/**
 * Executes an [HttpClient]'s HEAD request with the parameters configured in [block].
 *
 * Learn more from [Making requests](https://ktor.io/docs/request.html).
 */
public inline fun NotebookHttpClient.head(crossinline block: HttpRequestBuilder.() -> Unit): NotebookHttpResponse =
    NotebookHttpResponse(runBlocking { ktorClient.head(block) })

/**
 * Executes an [HttpClient]'s GET request with the specified [url] and
 * an optional [block] receiving an [HttpRequestBuilder] for configuring the request.
 *
 * Learn more from [Making requests](https://ktor.io/docs/request.html).
 */
public inline fun NotebookHttpClient.get(
    urlString: String,
    crossinline block: HttpRequestBuilder.() -> Unit = {}
): NotebookHttpResponse = NotebookHttpResponse(runBlocking { ktorClient.get(urlString, block) })

/**
 * Executes an [HttpClient]'s POST request with the specified [url] and
 * an optional [block] receiving an [HttpRequestBuilder] for configuring the request.
 *
 * Learn more from [Making requests](https://ktor.io/docs/request.html).
 */
public inline fun NotebookHttpClient.post(
    urlString: String,
    crossinline block: HttpRequestBuilder.() -> Unit = {}
): NotebookHttpResponse = NotebookHttpResponse(runBlocking { ktorClient.post(urlString, block) })

/**
 * Executes an [HttpClient]'s PUT request with the specified [url] and
 * an optional [block] receiving an [HttpRequestBuilder] for configuring the request.
 *
 * Learn more from [Making requests](https://ktor.io/docs/request.html).
 */
public inline fun NotebookHttpClient.put(
    urlString: String,
    crossinline block: HttpRequestBuilder.() -> Unit = {}
): NotebookHttpResponse = NotebookHttpResponse(runBlocking { ktorClient.put(urlString, block) })

/**
 * Executes an [HttpClient]'s DELETE request with the specified [url] and
 * an optional [block] receiving an [HttpRequestBuilder] for configuring the request.
 *
 * Learn more from [Making requests](https://ktor.io/docs/request.html).
 */
public inline fun NotebookHttpClient.delete(
    urlString: String,
    crossinline block: HttpRequestBuilder.() -> Unit = {}
): NotebookHttpResponse = NotebookHttpResponse(runBlocking { ktorClient.delete(urlString, block) })

/**
 * Executes an [HttpClient]'s OPTIONS request with the specified [url] and
 * an optional [block] receiving an [HttpRequestBuilder] for configuring the request.
 *
 * Learn more from [Making requests](https://ktor.io/docs/request.html).
 */
public inline fun NotebookHttpClient.options(
    urlString: String,
    crossinline block: HttpRequestBuilder.() -> Unit = {}
): NotebookHttpResponse = NotebookHttpResponse(runBlocking { ktorClient.options(urlString, block) })

/**
 * Executes an [HttpClient]'s PATCH request with the specified [url] and
 * an optional [block] receiving an [HttpRequestBuilder] for configuring the request.
 *
 * Learn more from [Making requests](https://ktor.io/docs/request.html).
 */
public suspend inline fun NotebookHttpClient.patch(
    urlString: String,
    crossinline block: HttpRequestBuilder.() -> Unit = {}
): NotebookHttpResponse = NotebookHttpResponse(runBlocking { ktorClient.patch(urlString, block) })

/**
 * Executes an [HttpClient]'s HEAD request with the specified [url] and
 * an optional [block] receiving an [HttpRequestBuilder] for configuring the request.
 *
 * Learn more from [Making requests](https://ktor.io/docs/request.html).
 */
public suspend inline fun NotebookHttpClient.head(
    urlString: String,
    crossinline block: HttpRequestBuilder.() -> Unit = {}
): NotebookHttpResponse = NotebookHttpResponse(runBlocking { ktorClient.head(urlString, block) })
