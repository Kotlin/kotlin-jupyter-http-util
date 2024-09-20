@file:Suppress("RedundantVisibilityModifier")

package org.jetbrains.kotlinx.jupyter.serialization

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlinx.jupyter.api.MimeTypedResultEx
import org.jetbrains.kotlinx.jupyter.testkit.JupyterReplTestCase
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
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
                public data class Person(
                    public val firstName: String,
                    public val lastName: String,
                    public val isAlive: Boolean,
                    public val age: Int,
                    public val address: Address,
                    public val phoneNumbers: List<PhoneNumber>,
                    public val children: List<String>,
                    public val spouse: UntypedAny?,
                )
                
                @Serializable
                public data class Address(
                    public val streetAddress: String,
                    public val city: String,
                    public val state: String,
                    public val postalCode: String,
                )
                
                @Serializable
                public data class PhoneNumber(
                    public val type: String,
                    public val number: String,
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
                public data class Response(
                    public val `property`: String,
                    public val additionalValues: List<AdditionalValue>,
                )
                
                @Serializable
                public data class AdditionalValue(
                    public val a: String,
                    public val b: List<B>,
                )
                
                @Serializable
                public data class B(
                    public val c: String?,
                    public val d: String?,
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
                public typealias Response = List<ResponseItem>
                
                @Serializable
                public data object ResponseItem
            """.trimIndent(),
            expectedDeserialized = "[ResponseItem, ResponseItem]",
        )
    }

    @Test
    fun `empty array`() {
        end2end(
            json = "[]",
            expectedGenerated = "public typealias Response = List<UntypedAny?>",
            expectedDeserialized = "[]",
            serializableImport = false
        )
    }

    @Test
    fun `array with int`() {
        end2end(
            json = "[12]",
            expectedGenerated = "public typealias Response = List<Int>",
            expectedDeserialized = "[12]",
            serializableImport = false
        )
    }

    @Test
    fun `array with numbers`() {
        end2end(
            json = "[12, 12.9]",
            expectedGenerated = "public typealias Response = List<Double>",
            expectedDeserialized = "[12.0, 12.9]",
            serializableImport = false,
        )
    }

    @Test
    fun int() {
        end2end(
            json = "12",
            expectedGenerated = "public typealias Response = Int",
            expectedDeserialized = "12",
            serializableImport = false,
        )
    }

    @Test
    fun `heterogeneous array`() {
        end2end(
            json = """
                [12, ""]
            """.trimIndent(),
            expectedGenerated = "public typealias Response = List<UntypedAnyNotNull>",
            expectedDeserialized = "[12, ]",
            serializableImport = false
        )
    }

    @Test
    fun `array with null`() {
        end2end(
            json = "[null]",
            expectedGenerated = "public typealias Response = List<UntypedAny?>",
            expectedDeserialized = "[null]",
            serializableImport = false
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
    fun `nullable int array`() {
        end2end(
            json = "[12, null]",
            expectedGenerated = "public typealias Response = List<Int?>",
            expectedDeserialized = "[12, null]",
            serializableImport = false
        )
    }

    @Test
    fun `array with objects with property of different types`() {
        end2end(
            json = """
                [{"a": "string"}, {"a": 12}]
            """.trimIndent(),
            expectedGenerated = """
                public typealias Response = List<ResponseItem>
                
                @Serializable
                public data class ResponseItem(
                    public val a: UntypedAnyNotNull,
                )
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

    @Test
    fun `dollar escaping`() {
        end2end(
            json = """{"a": "${'$'}string"}""",
            expectedGenerated = """
                @Serializable
                public data class Response(
                    public val a: String,
                )
            """.trimIndent(),
            expectedDeserialized = "Response(a=\$string)",
        )
    }

    @Test
    fun `same complex structure in different fields`() {
        val json = """
            {
              "a": {
                "c": {
                  "d": 1
                },
                "e": {
                  "f": 1
                }
              },
              "b": {
                "c": {
                  "d": 1
                },
                "e": {
                  "f": 1
                }
              }
            }
        """.trimIndent()
        getGeneratedCode(json, "Class")
        end2end(
            json = json,
            expectedGenerated = """
                @Serializable
                public data class Response(
                    public val a: A,
                    public val b: B,
                )
                
                @Serializable
                public data class A(
                    public val c: C,
                    public val e: E,
                )
                
                @Serializable
                public data class C(
                    public val d: Int,
                )
                
                @Serializable
                public data class E(
                    public val f: Int,
                )
                
                @Serializable
                public data class B(
                    public val c: C,
                    public val e: E,
                )
            """.trimIndent(),
            expectedDeserialized = "Response(a=A(c=C(d=1), e=E(f=1)), b=B(c=C(d=1), e=E(f=1)))",
        )
    }

    @Test
    fun `property with 'list' name`() {
        val json = """{"list": [{}]}"""
        end2end(
            json = json,
            expectedGenerated = """
                @Serializable
                public data class Response(
                    public val list: kotlin.collections.List<List>,
                )
                
                @Serializable
                public data object List
            """.trimIndent(),
            expectedDeserialized = "Response(list=[List])",
        )
    }

    @Test
    fun `class deduplication`() {
        val json = """{"a": {"c": {}}, "b": {"c": {}}}"""
        end2end(
            json = json,
            expectedGenerated = """
                @Serializable
                public data class Response(
                    public val a: A,
                    public val b: B,
                )
                
                @Serializable
                public data class A(
                    public val c: C,
                )
                
                @Serializable
                public data object C
                
                @Serializable
                public data class B(
                    public val c: C,
                )
            """.trimIndent(),
            expectedDeserialized = "Response(a=A(c=C), b=B(c=C))",
        )
    }

    @Test
    fun `class deduplication and disambiguation`() {
        end2end(
            json = """
                {
                  "links": {
                    "a": "a"
                  },
                  "b1": {
                    "links": {
                    }
                  },
                  "b2": {
                    "links": {
                    }
                  }
                }
            """.trimIndent(),
            expectedGenerated = """
                @Serializable
                public data class Response(
                    public val links: Links,
                    public val b1: B1,
                    public val b2: B2,
                )
                
                @Serializable
                public data class Links(
                    public val a: String,
                )
                
                @Serializable
                public data class B1(
                    public val links: Links1,
                )
                
                @Serializable
                public data object Links1
                
                @Serializable
                public data class B2(
                    public val links: Links1,
                )
            """.trimIndent(),
            expectedDeserialized = "Response(links=Links(a=a), b1=B1(links=Links1), b2=B2(links=Links1))",
        )
    }

    @Test
    fun `list item naming`() {
        val json = """
            {
              "addressList": [{}],
              "names": [{}],
              "names ": [{"value": 1}],
              "addresses": [{}],
              "butterflies": [{}],
              "radishes": [{}],
              "beaches": [{}],
              "taxes": [{}],
              "tags": [{}]
            }
        """.trimIndent()
        end2end(
            json = json,
            expectedGenerated = """
                @Serializable
                public data class Response(
                    public val addressList: List<Address>,
                    public val names: List<Name>,
                    @SerialName("names ")
                    public val names1: List<Name1>,
                    public val addresses: List<Address>,
                    public val butterflies: List<Butterfly>,
                    public val radishes: List<Radish>,
                    public val beaches: List<Beach>,
                    public val taxes: List<Tax>,
                    public val tags: List<Tag>,
                )
                
                @Serializable
                public data object Address
                
                @Serializable
                public data object Name
                
                @Serializable
                public data class Name1(
                    public val `value`: Int,
                )
                
                @Serializable
                public data object Butterfly
                
                @Serializable
                public data object Radish
                
                @Serializable
                public data object Beach
                
                @Serializable
                public data object Tax
                
                @Serializable
                public data object Tag
            """.trimIndent(),
            expectedDeserialized = "Response(" +
                "addressList=[Address], " +
                "names=[Name], " +
                "names1=[Name1(value=1)], " +
                "addresses=[Address], " +
                "butterflies=[Butterfly], " +
                "radishes=[Radish], " +
                "beaches=[Beach], " +
                "taxes=[Tax], " +
                "tags=[Tag])",
            serialNameImport = true,
        )
    }

    @Test
    fun returnGeneratedCode_objects() {
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
                    {"type": "home", "number": "212 555-1234"},
                    {"type": "office", "number": "646 555-4567"}
                  ],
                  "children": ["Catherine", "Thomas", "Trevor"],
                  "spouse": null
                }
            """.trimIndent()

        val expectedOutput = """
                public data class Person(
                    public val firstName: String,
                    public val lastName: String,
                    public val isAlive: Boolean,
                    public val age: Int,
                    public val address: Address,
                    public val phoneNumbers: List<PhoneNumber>,
                    public val children: List<String>,
                    public val spouse: Any?,
                )
                
                public data class Address(
                    public val streetAddress: String,
                    public val city: String,
                    public val state: String,
                    public val postalCode: String,
                )
                
                public data class PhoneNumber(
                    public val type: String,
                    public val number: String,
                )
            """.trimIndent()

        assertEquals(expectedOutput, DeserializeThis(json, "Person").getCode())
    }

    @Test
    fun returnGeneratedCode_arrays() {
        val json = """
            [{"a": "string"}, {"a": 12}]
        """.trimIndent()

        val expectedOutput = """
            public typealias Response = List<ResponseItem>
            
            public data class ResponseItem(
                public val a: Any,
            )
        """.trimIndent()
        assertEquals(expectedOutput, DeserializeThis(json, "Response").getCode())
    }

    @Test
    fun getCode_throwsOnInvalidJson() {
        val json = "[}"
        assertFailsWith<IllegalArgumentException> {
            DeserializeThis(json, "Response").getCode()
        }
    }

    private fun end2end(
        @Language("JSON") json: String,
        @Language("kotlin") expectedGenerated: String,
        expectedDeserialized: String,
        valName: String = "response",
        generatedClassName: String? = null,
        serializableImport: Boolean = true,
        serialNameImport: Boolean = false,
    ) {
        val value = getGeneratedCode(json, generatedClassName ?: valName.replaceFirstChar(kotlin.Char::titlecaseChar))
        val expectedGeneratedWithImports = (if (serialNameImport) {
            "import kotlinx.serialization.SerialName\n"
        } else "") + (if (serializableImport) {
            "import kotlinx.serialization.Serializable\n"
        } else "") + (if (serializableImport || serialNameImport) "\n" else "") +
            expectedGenerated.trimEnd()
        assertEquals(expectedGeneratedWithImports, value)

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
            val escaped = json.replace("$", "\${'$'}")
            """
                ${"\""}""
                    $escaped
                ${"\""}"".trimIndent()
            """.trimIndent()
        }
        execRaw(
            "val $valName = $stringLiteral.${String::deserializeJson.name}(" +
                if (generatedClassName != null) "\"$generatedClassName\")" else "null)"
        )
        return execRaw(valName)
    }
}
