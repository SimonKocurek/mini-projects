package asciifilter

import asciifilter.character.AlphabetCharacter
import asciifilter.image.getBrightness
import kotlinx.coroutines.*
import utils.Logger
import java.awt.image.BufferedImage
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

internal class AsciiFilter(
    private val logger: Logger,
) {
    /**
     * @param image Image scaled to the expected size, where each pixel should represent a single character.
     * @param sortedAlphabet Brightness sorted alphabet.
     */
    suspend fun convert(image: BufferedImage, sortedAlphabet: List<AlphabetCharacter>): String {
        return AsciiFilterInstance(
            logger = logger,
            image = image,
            sortedAlphabet = sortedAlphabet,
        ).convert()
    }
}

private class AsciiFilterInstance(
    private val logger: Logger,
    private val image: BufferedImage,
    private val sortedAlphabet: List<AlphabetCharacter>,
) {

    suspend fun convert() = coroutineScope {
        logger.log("converting image to ASCII")

        (0..<image.height).map { y ->
            async { convertLineToString(y) }
        }.awaitAll().joinToString(separator = "")
    }

    private fun convertLineToString(y: Int) = buildString {
        for (x in 0..<image.width) {
            val brightness = image.getBrightness(x, y).toDouble()

            val binarySearchResult = sortedAlphabet.binarySearch { it.brightness.compareTo(brightness) }
            val insertionIndex = if (binarySearchResult < 0) abs(binarySearchResult + 1) else binarySearchResult

            // The insertion index is likely going to land between 2 close letters.
            // We want the closer one of those.
            val trimmedLowerIndex = max(0, min(sortedAlphabet.size - 1, insertionIndex - 1))
            val trimmedHigherIndex = max(0, min(sortedAlphabet.size - 1, insertionIndex))

            val closerIndex = if (
                brightnessDifference(sortedAlphabet[trimmedLowerIndex].brightness, brightness) <
                brightnessDifference(sortedAlphabet[trimmedHigherIndex].brightness, brightness)
            ) trimmedLowerIndex else trimmedHigherIndex

            val mostSimilarCharacter = sortedAlphabet[closerIndex].character
            append(mostSimilarCharacter)
        }
        append("\n")
    }

    private fun brightnessDifference(a: Double, b: Double) = abs(max(a, b) - min(a, b))
}
