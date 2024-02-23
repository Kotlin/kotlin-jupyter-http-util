package org.jetbrains.kotlinx.jupyter.ktor.client

import kotlinx.serialization.json.JsonElement
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlinx.jupyter.testkit.JupyterReplTestCase
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class KtorClientIntegrationTest : JupyterReplTestCase() {
    @Test
    fun `default client engine`() {
        val engineName = exec("http.ktorClient.engine")?.javaClass?.simpleName
        assertEquals("CIOEngine", engineName)
        val engineName2 = exec("io.ktor.client.HttpClient().engine")?.javaClass?.simpleName
        assertEquals("CIOEngine", engineName2)
    }

    @Test
    fun `calls compilation`() {
        val exec = exec(
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
        exec(
            """
                %use serialization
                @file:DependsOn("io.ktor:ktor-client-mock-jvm:2.3.7")
                
                import io.ktor.http.*
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

        val response1 = exec("""client.get("https://example.org").bodyAsText()""")
        assertEquals(json, response1)

        val response2 = exec("""client.get("https://example.org").body<String>()""")
        assertEquals(json, response2)

        val response3 = exec("""client.get("https://example.org").body<JsonElement>()""")
        assertIs<JsonElement>(response3)
        assertEquals(json, response3.toString())

        val response4 = exec("""client.get("https://example.org").body<A>()""")
        assertEquals("A(b=b,a=A(b=b,a=null))", response4.toString())

        val response5 = exec("""client.get("https://example.org").readBytes()""")
        assertIs<ByteArray>(response5)
        assertEquals(json, response5.toString(Charsets.UTF_8))
    }
}