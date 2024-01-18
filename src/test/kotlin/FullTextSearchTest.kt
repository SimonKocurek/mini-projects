import fulltextsearch.FullTextSearch
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import parsejson.parseJson
import kotlin.io.path.Path
import kotlin.io.path.forEachLine

class FullTextSearchTest {

    companion object {
        private fun getZipFileStream() = Companion::class.java.getResourceAsStream("fulltextsearch/test.jsonl.zip")!!

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
    fun canProcessLargeFile() {
        // Given
        val searchEngine = FullTextSearch()

        Path("test.jsonl").forEachLine { line ->
            val parsed = parseJson(line) as Map<String, String>
            searchEngine.insert(FullTextSearch.Entry(
                indexedText = parsed["title"] + " " + parsed["abstract"],
                document = parsed
            ))
        }

        // When

        // Then
    }

}
