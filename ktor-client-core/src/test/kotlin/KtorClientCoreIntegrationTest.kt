package org.jetbrains.kotlinx.jupyter.ktor.client.core

import kotlinx.serialization.json.JsonElement
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.jupyter.repl.result.EvalResultEx
import org.jetbrains.kotlinx.jupyter.testkit.JupyterReplTestCase
import org.jetbrains.kotlinx.jupyter.testkit.ReplProvider
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class KtorClientCoreIntegrationTest : JupyterReplTestCase(
    replProvider = ReplProvider.withDefaultClasspathResolution(),
) {
    @Test
    fun `default client engine`() {
        val engineName = execRaw("http.ktorClient.engine")?.javaClass?.simpleName
        assertEquals("CIOEngine", engineName)
        val engineName2 = execRaw("io.ktor.client.HttpClient().engine")?.javaClass?.simpleName
        assertEquals("CIOEngine", engineName2)
    }

    @Test
    fun `calls compilation`() {
        val exec = execRaw(
            """
                import io.ktor.client.request.*
                
                lazy { 
                    http.get("https://example.org").body<String>()
                    http.get("https://example.org") {
                        header("Authorization", "Basic 123")
                        parameter("param", "value")
                    }.bodyAsText()
                    http.post("https://example.org") {
                        header("Authorization", "Basic 123")
                        setBody("body")
                    }.readBytes()
                }
            """.trimIndent()
        )
        // checking only compilation, so that the test doesn't involve actual network calls
        assertIs<Lazy<*>>(exec)
    }

    @Test
    fun `mock calls`() {
        @Language("JSON")
        val json = """{"b":"b","a":{"b":"b","a":null}}"""
        execRaw(
            """
                %use serialization
                @file:DependsOn("io.ktor:ktor-client-mock-jvm:2.3.7")
                
                import io.ktor.client.engine.mock.*
                import io.ktor.client.plugins.contentnegotiation.*
                import io.ktor.http.*
                import io.ktor.serialization.kotlinx.json.*
                
                @Serializable
                data class A(val b: String, val a: A?)
                
                val client = NotebookHttpClient(MockEngine) {
                    install(ContentNegotiation) {
                        json()
                    }
                    engine {
                        addHandler {
                            respond(
                                content = ""${'"'}$json""${'"'},
                                status = HttpStatusCode.OK,
                                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                            )
                        }
                    }
                }
            """.trimIndent()
        )

        val response1 = execRaw("""client.get("https://example.org").bodyAsText()""")
        assertEquals(json, response1)

        val response2 = execRaw("""client.get("https://example.org").body<String>()""")
        assertEquals(json, response2)

        val response3 = execRaw("""client.get("https://example.org").body<JsonElement>()""")
        assertIs<JsonElement>(response3)
        assertEquals(json, response3.toString())

        val response4 = execRaw("""client.get("https://example.org").body<A>()""")
        assertEquals("A(b=b, a=A(b=b, a=null))", response4.toString())

        val response5 = execRaw("""client.get("https://example.org").readBytes()""")
        assertIs<ByteArray>(response5)
        assertEquals(json, response5.toString(Charsets.UTF_8))
    }

    @Test
    fun `create dataframe from response`() {
        val json = """[{"a": 1}, {"a": 2}, {"a": 3}]"""
        execRaw(
            """
                %use serialization
                @file:DependsOn("io.ktor:ktor-client-mock-jvm:2.3.7")
                
                import io.ktor.client.engine.mock.*
                import io.ktor.client.plugins.contentnegotiation.*
                import io.ktor.http.*
                import io.ktor.serialization.kotlinx.json.*
                
                @Serializable
                data class A(val b: String, val a: A?)
                
                val client = NotebookHttpClient(MockEngine) {
                    install(ContentNegotiation) {
                        json()
                    }
                    engine {
                        addHandler {
                            respond(
                                content = ""${'"'}$json""${'"'},
                                status = HttpStatusCode.OK,
                                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                            )
                        }
                    }
                }
            """.trimIndent()
        )
        val response = execRaw("""client.get("https://example.org").toDataFrame()""")
        assertIs<DataFrame<*>>(response)
    }

    @Test
    fun `cannot create dataframe from response that doesn't return json`() {
        execRaw(
            """
                %use serialization
                @file:DependsOn("io.ktor:ktor-client-mock-jvm:2.3.7")
                
                import io.ktor.client.engine.mock.*
                import io.ktor.client.plugins.contentnegotiation.*
                import io.ktor.http.*
                import io.ktor.serialization.kotlinx.json.*
                
                val client = NotebookHttpClient(MockEngine) {
                    install(ContentNegotiation) {
                        json()
                    }
                    engine {
                        addHandler {
                            respond(
                                content = ""${'"'}error""${'"'},
                                status = HttpStatusCode.InternalServerError,
                            )
                        }
                    }
                }
            """.trimIndent()
        )
        val res = execEx("""client.get("https://example.org").toDataFrame()""")
        assertIs<EvalResultEx.Error>(res)
        assertIs<IllegalStateException>(res.error.cause)
    }
}