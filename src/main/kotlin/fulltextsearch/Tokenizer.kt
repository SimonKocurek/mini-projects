package fulltextsearch

import java.text.Normalizer
import java.util.regex.Pattern

interface Tokenizer {

    /**
     * Split indexed text into tokens that can be used for searching.
     */
    fun tokenizeText(text: String): List<String>
}

class WordTokenizer(
    /** Words that should not be indexed */
    private val stopWords: Set<String> = emptySet(),
    /** When indexing text in various languages, a stemmer can be used to retrieve word stem. */
    private val stemmer: (word: String) -> String = { it },
) : Tokenizer {

    private val tokenizerPattern = Pattern.compile("\\W+", Pattern.UNICODE_CHARACTER_CLASS)

    override fun tokenizeText(text: String): List<String> {
        // Videl som veľkého psa.

        val lowercaseText = text.lowercase() // videl som veľkého psa.

        val textWithoutAccents = Normalizer.normalize(lowercaseText, Normalizer.Form.NFD) // videl som velkeho psa.

        val words = tokenizerPattern.split(textWithoutAccents) // [videl, som, velkeho, psa]

        val indexedWords = words.filter { it !in stopWords } // [videl, velkeho, psa]

        val wordStems = indexedWords.map { stemmer(it) } // [videl, velky, pes]

        return wordStems.filter { it.isNotBlank() }
    }

}