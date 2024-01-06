import org.junit.jupiter.api.*
import parsejson.JsonParsingException
import parsejson.parseJson
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.io.path.name
import kotlin.streams.toList
import kotlin.test.assertTrue

class ParseJson {

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
            assertThrows<JsonParsingException>("Parsing should have failed for JSON: $fileContent") {
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
            assertDoesNotThrow("Parsing should have passed for JSON: $fileContent") {
                parseJson(fileContent)
            }
        }

        assertTrue(filePaths.isNotEmpty(), "Some file paths should be processed. Was the test correctly set up?")
    }

}