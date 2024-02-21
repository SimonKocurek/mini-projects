package asciifilter.character

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class AsciiFilterAlphabetFactory(
    private val bitmapper: CharacterBitmapper,
    private val alphabetSorter: AlphabetSorter,
    private val normalizer: AlphabetBrightnessNormalizer,
) {

    suspend fun create(alphabetCharacters: String, fontName: String, fontSize: Int) = coroutineScope {
        val characterToBitmap = alphabetCharacters.associateWith { character ->
            async { bitmapper.get(character, fontName, fontSize) }
        }.mapValues { (_, coroutine) -> coroutine.await() }

        val averageAspectRatio =
            characterToBitmap.values.sumOf { it.height / it.width.toDouble() } / characterToBitmap.size

        val sortedCharacters = alphabetSorter.sortByBrightness(characterToBitmap)
        val normalizedCharacters = normalizer.normalize(sortedCharacters)

        AsciiFilterAlphabet(
            characters = normalizedCharacters,
            averageAspectRatio = averageAspectRatio
        )
    }

}

data class AsciiFilterAlphabet(
    val characters: List<AlphabetCharacter>,
    val averageAspectRatio: Double,
)
