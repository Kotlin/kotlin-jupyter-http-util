package org.jetbrains.kotlinx.jupyter.serialization

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlinx.jupyter.api.MimeTypedResultEx
import org.jetbrains.kotlinx.jupyter.testkit.JupyterReplTestCase
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class JsonSerializationIntegrationTest : JupyterReplTestCase() {
    @Test
    fun person() {
        end2end(
            json = """
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
                    {"type": "home", "number": "212 555-1234"},
                    {"type": "office", "number": "646 555-4567"}
                  ],
                  "children": ["Catherine", "Thomas", "Trevor"],
                  "spouse": null
                }
            """.trimIndent(),
            expectedGenerated = """
                @Serializable
                data class Person(
                    @SerialName("firstName")
                    val firstName: String,
                    @SerialName("lastName")
                    val lastName: String,
                    @SerialName("isAlive")
                    val isAlive: Boolean,
                    @SerialName("age")
                    val age: Int,
                    @SerialName("address")
                    val address: Address,
                    @SerialName("phoneNumbers")
                    val phoneNumbers: List<PhoneNumber>,
                    @SerialName("children")
                    val children: List<String>,
                    @SerialName("spouse")
                    val spouse: UntypedAny?
                )
                
                @Serializable
                data class Address(
                    @SerialName("streetAddress")
                    val streetAddress: String,
                    @SerialName("city")
                    val city: String,
                    @SerialName("state")
                    val state: String,
                    @SerialName("postalCode")
                    val postalCode: String
                )
                
                @Serializable
                data class PhoneNumber(
                    @SerialName("type")
                    val type: String,
                    @SerialName("number")
                    val number: String
                )
            """.trimIndent(),
            expectedDeserialized = "Person(" +
                "firstName=John, " +
                "lastName=Smith, " +
                "isAlive=true, " +
                "age=27, " +
                "address=Address(streetAddress=21 2nd Street, city=New York, state=NY, postalCode=10021-3100), " +
                "phoneNumbers=[PhoneNumber(type=home, number=212 555-1234), " +
                "PhoneNumber(type=office, number=646 555-4567)], " +
                "children=[Catherine, Thomas, Trevor], " +
                "spouse=null)",
            valName = "person",
        )
    }

    @Test
    fun `property type inference`() {
        @Language("JSON")
        val json = """
            {
              "property": "propertyValue",
              "additionalValues": [{
                "a": "a",
                "b": [{"c": "c", "d": "d"}, {"c": "c", "d": null}]
              }, {
                "a": "a",
                "b": [{"c": null, "d": "d"}, {"c": "c", "d": "d"}]
              }]
            }
        """.trimIndent()
        end2end(
            json = json,
            expectedGenerated = """
                @Serializable
                data class Response(
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
            """.trimIndent(),
            expectedDeserialized = "Response(" +
                "property=propertyValue, " +
                "additionalValues=[" +
                "AdditionalValue(a=a, b=[B(c=c, d=d), B(c=c, d=null)]), " +
                "AdditionalValue(a=a, b=[B(c=null, d=d), B(c=c, d=d)])" +
                "])"
        )
    }

    @Test
    fun `array with empty objects`() {
        end2end(
            json = "[{}, {}]",
            expectedGenerated = """
                typealias Response = List<ResponseItem>
                
                @Serializable
                data object ResponseItem
            """.trimIndent(),
            expectedDeserialized = "[ResponseItem, ResponseItem]",
        )
    }

    @Test
    fun `empty array`() {
        end2end(
            json = "[]",
            expectedGenerated = "typealias Response = List<UntypedAny?>",
            expectedDeserialized = "[]",
        )
    }

    @Test
    fun `array with int`() {
        end2end(
            json = "[12]",
            expectedGenerated = "typealias Response = List<Int>",
            expectedDeserialized = "[12]",
        )
    }

    @Test
    fun int() {
        end2end(
            json = "12",
            expectedGenerated = "typealias Response = Int",
            expectedDeserialized = "12",
            addImports = false,
        )
    }

    @Test
    fun `heterogeneous array`() {
        end2end(
            json = """
                [12, ""]
            """.trimIndent(),
            expectedGenerated = "typealias Response = List<UntypedAny?>",
            expectedDeserialized = "[12, ]",
        )
    }

    @Test
    fun `array with null`() {
        end2end(
            json = "[null]",
            expectedGenerated = "typealias Response = List<UntypedAny?>",
            expectedDeserialized = "[null]",
        )
    }

    @Test
    fun `incorrect JSON`() {
        // TODO: Assert error messages
        assertIncorrectJsonIsHandled("")
        assertIncorrectJsonIsHandled("[1")
        assertIncorrectJsonIsHandled("NaN")
        assertIncorrectJsonIsHandled("""{1:"value"}""")
    }

    @Test
    @Disabled("Doesn't work yet")
    fun `nullable int array`() {
        end2end(
            json = "[12, null]",
            expectedGenerated = "typealias Response = List<Int?>",
            expectedDeserialized = "[12, null]",
        )
    }

    @Test
    @Disabled("Doesn't work yet")
    fun `array with objects with property of different types`() {
        end2end(
            json = """
                [{"a": "string"}, {"a": 12}]
            """.trimIndent(),
            expectedGenerated = """
                typealias Response = List<ResponseItem>
                
                @Serializable
                data class ResponseItem(val a: UntypedAny)
            """.trimIndent(),
            expectedDeserialized = "[ResponseItem(a=string), ResponseItem(a=12)]",
        )
    }

    @Test
    fun `json rendering`() {
        val json = buildJsonObject {
            put("a", JsonPrimitive("b"))
        }
        fun check(input: String) {
            val res = execRendered(input)
            val applicationJson = assertIs<MimeTypedResultEx>(res)
                .toJson(overrideId = null)["data"]
                ?.let { assertIs<JsonObject>(it)["application/json"] }
            assertEquals(json, applicationJson)
        }

        check("\"\"\"$json\"\"\"")
        check("org.jetbrains.kotlinx.jupyter.serialization.DeserializeThis(\"\"\"$json\"\"\", \"Class\")")
    }

    private fun end2end(
        @Language("JSON") json: String,
        @Language("kotlin") expectedGenerated: String,
        expectedDeserialized: String,
        valName: String = "response",
        generatedClassName: String? = null,
        addImports: Boolean = true,
    ) {
        val value = getGeneratedCode(json, generatedClassName ?: valName.replaceFirstChar(kotlin.Char::titlecaseChar))
        if (addImports) {
            assertGeneratedCode(expectedGenerated, value)
        } else {
            assertEquals(expectedGenerated, value)
        }

        val value2 = execDeserialization(json = json, valName = valName, generatedClassName = generatedClassName)

        assertEquals(
            expected = expectedDeserialized,
            actual = value2.toString()
        )
    }

    private fun assertIncorrectJsonIsHandled(incorrectJson: String) {
        val generatedClassName = "Class"
        val value = execDeserialization(incorrectJson, valName = "x", generatedClassName = generatedClassName)
        assertEquals(
            expected = DeserializeThis(incorrectJson, className = generatedClassName),
            actual = value,
        )
    }

    private fun execDeserialization(json: String, valName: String, generatedClassName: String? = null): Any? {
        val stringLiteral = if (json.isBlank()) {
            "\"$json\""
        } else {
            """
                ${"\""}""
                    $json
                ${"\""}"".trimIndent()
            """.trimIndent()
        }
        execRaw(
            "val $valName = $stringLiteral.${String::deserializeJson.name}(" +
                if (generatedClassName != null) "\"$generatedClassName\")" else "null)"
        )
        return execRaw(valName)
    }

    private fun assertGeneratedCode(expected: String, actual: String) {
        assertEquals(
            """
                
                import kotlinx.serialization.SerialName
                import kotlinx.serialization.Serializable
    
    
            """.trimIndent() + expected, actual
        )
    }
}
