import fulltextsearch.FullTextSearch
import fulltextsearch.InMemoryFullTextSearch
import fulltextsearch.WordTokenizer
import org.junit.jupiter.api.Test
import parsejson.parseJson
import utils.unzip
import utils.withDefer
import kotlin.io.path.Path
import kotlin.io.path.forEachLine
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FullTextSearchTest {

    private fun getZipFileStream() = FullTextSearchTest::class.java.getResourceAsStream("fulltextsearch/test.jsonl.zip")!!

    @Test
    fun canProcessLargeFile() {
        // Given
        val tokenizer = WordTokenizer(
            stopWords = setOf("a", "and", "be", "have", "i", "in", "of", "that", "the", "to"),
            stemmer = { if (it.startsWith("west")) "west" else it }
        )
        val searchEngine = InMemoryFullTextSearch(tokenizer)

        withDefer {
            unzip(::getZipFileStream)

            Path("test.jsonl").forEachLine { line ->
                val parsed = parseJson(line) as Map<String, String>
                searchEngine.insert(
                    FullTextSearch.Entry(
                        indexedText = "${parsed["title"]} ${parsed["abstract"]}",
                        document = parsed
                    )
                )
            }
        }

        // When
        val searchResults = searchEngine.find("West, london.")

        // Then
        assertEquals(14, searchResults.size)

        searchResults.forEach { result ->
            assertTrue("Result should contain west got: $result") {
                tokenizer.tokenizeText(result.indexedText).contains("west")
            }
            assertTrue("Result should contain london got: $result") {
                tokenizer.tokenizeText(result.indexedText).contains("london")
            }
        }

        assertEquals(
            "Wikipedia: West End of London The West End of London (commonly referred to as the West End) is a district of Central London, London, England, west of the City of London and north of the River Thames, in which many of the city's major tourist attractions, shops, businesses, government buildings and entertainment venues, including West End theatres, are concentrated.",
            searchResults.first().indexedText,
            "First entry should be very relevant. It should frequently mention words London or West."
        )

        assertEquals(
            "Wikipedia: International African Institute The International African Institute (IAI) was founded (as the International Institute of African Languages and Cultures - IIALC) in 1926 in London for the study of African languages. Frederick Lugard was the first chairman (1926 to his death in 1945); Diedrich Hermann Westermann (1926 to 1939) and Maurice Delafosse (1926) were the initial co-directors.",
            searchResults.last().indexedText,
            "Last entry not should be very relevant. It should only rarely contain words London or West."
        )
    }

    @Test
    fun canFindInsertedData() {
        // Given
        val searchEngine = InMemoryFullTextSearch(WordTokenizer())

        val firstEntry = searchEngine.insert(
            FullTextSearch.Entry(
                indexedText = "     A b c.,d e ",
                document = mapOf("foo" to "bar")
            )
        )
        val secondEntry = searchEngine.insert(
            FullTextSearch.Entry(
                indexedText = "D d efgh e d",
                document = mapOf("foo2" to "bar2")
            )
        )
        searchEngine.insert(
            FullTextSearch.Entry(
                indexedText = " a a a a a ",
                document = mapOf("should" to "not be returned")
            )
        )

        // When
        val firstResult = searchEngine.find("   b c")
        val commonResult = searchEngine.find(",D. E")
        val secondResult = searchEngine.find("  Ëfğh   ")

        // Then
        assertEquals(listOf(firstEntry), firstResult)
        assertEquals(
            listOf(secondEntry, firstEntry),
            commonResult,
            "Second entry should be first as it is more specific."
        )
        assertEquals(listOf(secondEntry), secondResult)
    }

    @Test
    fun canDeleteEntries() {
        // Given
        val searchEngine = InMemoryFullTextSearch(WordTokenizer())

        val firstEntry = searchEngine.insert(
            FullTextSearch.Entry(
                indexedText = "a",
                document = mapOf("foo" to "bar")
            )
        )
        val secondEntry = searchEngine.insert(
            FullTextSearch.Entry(
                indexedText = "a b",
                document = mapOf("foo2" to "bar2")
            )
        )

        // When
        searchEngine.delete(firstEntry.id)

        // Then
        val result = searchEngine.find("Á")
        assertEquals(listOf(secondEntry), result)
    }
}
