package org.jetbrains.kotlinx.jupyter.json

import org.intellij.lang.annotations.Language
import org.jetbrains.kotlinx.jupyter.testkit.JupyterReplTestCase
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class JsonClassesGenerationTest : JupyterReplTestCase() {
    @Test
    fun end2end() {
        @Language("JSON")
        val json = """
            {
              "firstName": "John",
              "lastName": "Smith",
              "isAlive": true,
              "age": 27,
              "address": {
                "streetAddress": "21 2nd Street",
                "city": "New York",
                "state": "NY",
                "postalCode": "10021-3100"
              },
              "phoneNumbers": [
                {
                  "type": "home",
                  "number": "212 555-1234"
                },
                {
                  "type": "office",
                  "number": "646 555-4567"
                }
              ],
              "children": [
                "Catherine",
                "Thomas",
                "Trevor"
              ],
              "spouse": null
            }
        """.trimIndent()
        exec(
            """
            val x = org.jetbrains.kotlinx.jupyter.json.DeserializationResult(${"\""}""
                $json
            ${"\""}"", "Person")
        """
        )
        val value = exec(
            """
            x   
        """.trimIndent()
        )
        assertEquals(
            "Person(" +
                "firstName=John, " +
                "lastName=Smith, " +
                "isAlive=true, " +
                "age=27, " +
                "address=Address(streetAddress=21 2nd Street, city=New York, state=NY, postalCode=10021-3100), " +
                "phoneNumbers=[PhoneNumber(type=home, number=212 555-1234), " +
                "PhoneNumber(type=office, number=646 555-4567)], " +
                "children=[Catherine, Thomas, Trevor], " +
                "spouse=null)", value.toString()
        )
    }

    @Test
    fun generatedCodeTest() {
        @Language("JSON")
        val json = """
            {
              "property": "propertyValue",
              "additionalValues": [{
                "a": "a",
                "b": [{
                  "c": "c",
                  "d": "d"
                }, {
                  "c": "c",
                  "d": null
                }]
              }, {
                "a": "a",
                "b": [{
                  "c": null,
                  "d": "d"
                }, {
                  "c": "c",
                  "d": "d"
                }]
              }]
            }
        """.trimIndent()
        val value = getGeneratedCode(DeserializationResult(json, "Class"))

        assertGeneratedCode("""
            @Serializable
            data class Class(
                @SerialName("property")
                val `property`: String,
                @SerialName("additionalValues")
                val additionalValues: List<AdditionalValue>
            )

            @Serializable
            data class AdditionalValue(
                @SerialName("a")
                val a: String,
                @SerialName("b")
                val b: List<B>
            )

            @Serializable
            data class B(
                @SerialName("c")
                val c: String?,
                @SerialName("d")
                val d: String?
            )
        """.trimIndent(), value)
        println(value)
    }

    @Test
    fun generatedCodeTest2() {
        @Language("JSON")
        val json = "[{}, {}]"
        val value = getGeneratedCode(DeserializationResult(json, "Class"))

        assertGeneratedCode("""
            typealias Class = List<ClassItem>

            @Serializable
            class ClassItem
        """.trimIndent(), value)
        println(value)
    }

    @Test
    fun generatedCodeTest3() {
        @Language("JSON")
        val json = "[]"
        val value = getGeneratedCode(DeserializationResult(json, "Class"))

        assertGeneratedCode("typealias Class = List<@Contextual Any>", value)
        println(value)
    }

    @Test
    fun generatedCodeTest4() {
        @Language("JSON")
        val json = "[12]"
        val value = getGeneratedCode(DeserializationResult(json, "Class"))

        assertGeneratedCode("typealias Class = List<Int>", value)
        println(value)
    }

    @Test
    fun generatedCodeTest5() {
        @Language("JSON")
        val json = "12"
        val value = getGeneratedCode(DeserializationResult(json, "Class"))

        assertEquals("typealias Class = Int", value)
        println(value)
    }

    @Test
    fun generatedCodeTest7() {
        @Language("JSON")
        val json = """
            [12, ""]
        """.trimIndent()
        val value = getGeneratedCode(DeserializationResult(json, "Class"))

        assertGeneratedCode("typealias Class = List<@Contextual Any>", value)
        println(value)
    }

    @Test
    fun generatedCodeTest6() {
        @Language("JSON")
        val json = "[null]"
        val value = getGeneratedCode(DeserializationResult(json, "Class"))

        assertGeneratedCode("typealias Class = List<@Contextual Any?>", value)
        println(value)
    }

    fun assertGeneratedCode(expected: String, actual: String) {
        assertEquals("""
            
            import kotlinx.serialization.SerialName
            import kotlinx.serialization.Serializable


        """.trimIndent() + expected, actual)
    }
}