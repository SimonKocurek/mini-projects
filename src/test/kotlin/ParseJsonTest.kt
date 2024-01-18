import org.junit.jupiter.api.*
import parsejson.JsonParsingException
import parsejson.parseJson
import java.math.BigDecimal
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.io.path.name
import kotlin.streams.toList
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ParseJsonTest {

    companion object {
        private fun getZipFileStream() = Companion::class.java.getResourceAsStream("parsejson/test.zip")!!

        @JvmStatic
        @BeforeAll
        fun prepare() {
            cleanup()
            getZipFileStream().use { unzip(it) }
        }

        @JvmStatic
        @AfterAll
        fun cleanup() {
            getZipFileStream().use { cleanupUnzipped(it) }
        }
    }

    @Test
    fun errorOnInvalidJson() {
        // Given
        val filePaths = Files
            .list(Path("test"))
            .filter { it.name.startsWith("fail") }
            .toList()

        filePaths.forEach { filePath ->
            val fileContent = Files.readAllLines(filePath).joinToString(System.lineSeparator())

            // When, Then
            assertThrows<JsonParsingException>("Parsing should have failed for JSON (file $filePath): $fileContent") {
                parseJson(fileContent)
            }
        }

        assertTrue(filePaths.isNotEmpty(), "Some file paths should be processed. Was the test correctly set up?")
    }

    @Test
    fun passOnValidJson() {
        // Given
        val filePaths = Files
            .list(Path("test"))
            .filter { it.name.startsWith("pass") }
            .toList()

        filePaths.forEach { filePath ->
            val fileContent = Files.readAllLines(filePath).joinToString(System.lineSeparator())

            // When, Then
            assertDoesNotThrow("Parsing should have passed for JSON (file $filePath): $fileContent") {
                parseJson(fileContent)
            }
        }

        assertTrue(filePaths.isNotEmpty(), "Some file paths should be processed. Was the test correctly set up?")
    }

    @Test
    fun parseWhitespace() {
        // Given

        // When
        val result = parseJson(" \n \t \r {}")

        // Then
        assertEquals(emptyMap<String, Any?>(), result)
    }

    @Test
    fun parseStringCannotContainControlCharacters() {
        // Given

        // When
        val e = assertThrows<JsonParsingException> {
            parseJson("\"abc123\n\t\r\"")
        }

        // Then
        assertTrue(e.message!!.contains('\n'.code.toString()), "Error should mention control character. Got message ${e.message}")
        assertTrue(e.message!!.contains("control"), "Error should mention control characters")
    }

    @Test
    fun parseString() {
        // Given

        // When
        val result = parseJson("\"abc123\\\" \\\\ \\/ \\b \\f \\n \\r \\t \\uD83D\\uDE00  \"")

        // Then
        assertEquals("abc123\" \\ / \b ${Char(0xC)} \n \r \t 😀  ", result)
    }

    @Test
    fun parseStringInvalidUnicode() {
        // Given

        // When
        val e = assertThrows<JsonParsingException> {
            parseJson("\"\\uD83v\"")
        }

        // Then
        assertTrue(e.message!!.contains('v'), "Error should mention invalid character. Got message ${e.message}")
        assertTrue(e.message!!.contains("hex digit"), "Error should mention control characters")
    }

    @Test
    fun parseTrue() {
        // Given

        // When
        val result = parseJson("  true  ")

        // Then
        assertEquals(true, result)
    }

    @Test
    fun parseFalse() {
        // Given

        // When
        val result = parseJson("false  ")

        // Then
        assertEquals(false, result)
    }

    @Test
    fun parseNull() {
        // Given

        // When
        val result = parseJson("\nnull")

        // Then
        assertEquals(null, result)
    }

    @Test
    fun errorContainsKeyStack() {
        // Given

        // When
        val e = assertThrows<JsonParsingException> {
            parseJson("{ \"foo\": {\"bar\": [true, [], [null, {error} ] false] } }")
        }

        // Then
        assertEquals("Expected '\"' but got 'e' at index 36. Path: foo->bar->2->1.", e.message)
    }

    @Test
    fun errorOnExtraCharacters() {
        // Given

        // When
        val e = assertThrows<JsonParsingException> {
            parseJson("{ } []")
        }

        // Then
        assertEquals("Expected end of input, but got extra character '[' at position 4.", e.message)
    }

    @Test
    fun parseBasicNumber() {
        // Given
        val number = "123456789123456789123456789"

        // When
        val result = parseJson(number) as BigDecimal

        // Then
        assertEquals(number, result.toString())
    }

    @Test
    fun parseComplicatedNumber() {
        // Given
        val number = "-123456789123456789.0987654321e-12"

        // When
        val result = parseJson(number) as BigDecimal

        // Then
        assertEquals("-123456.7891234567890987654321", result.toEngineeringString())
    }

    @Test
    fun parseObject() {
        // Given

        // When
        val result = parseJson("{ \"foo\": [\"bar\", -123.567, null], \"123\": null\n}") as Map<*, *>
        val foo = result["foo"] as List<Any?>
        val bar = foo[0] as String
        val number = foo[1] as BigDecimal
        val empty = foo[2]
        val secondKey = result["123"]

        // Then
        assertEquals(2, result.size)
        assertEquals(3, foo.size)
        assertEquals("bar", bar)
        assertEquals(BigDecimal("-123.567"), number)
        assertEquals(null, empty)
        assertEquals(null, secondKey)
    }

    @Test
    fun parseDeeplyNested() {
        // Given
        val input = buildString {
            append(CharArray(2000) { '[' })
            append("123")
            append(CharArray(2000) { ']' })
        }

        // When, Then
        assertDoesNotThrow {
            parseJson(input)
        }
    }

}