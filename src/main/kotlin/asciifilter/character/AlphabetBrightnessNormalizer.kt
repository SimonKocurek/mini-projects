package asciifilter.character

import kotlin.math.max
import kotlin.math.min

/**
 * Many characters will have very similar brightness.
 *
 * To better utilize all characters from alphabet,
 * we want to exaggerate brightness differences.
 */
class AlphabetBrightnessNormalizer {

    fun normalize(sortedAlphabet: List<AlphabetCharacter>): List<AlphabetCharacter> {
        val minBrightness = sortedAlphabet.first().brightness
        val maxBrightness = sortedAlphabet.last().brightness

        return sortedAlphabet.map {
            // Bring to a 0-1 range.
            val normalizedCurrent = (it.brightness - minBrightness) / (maxBrightness - minBrightness)
            // Bring to a normalized range.
            val normalizedTarget = normalizedCurrent * 255.0
            // Make sure we still fit in the image range.
            val trimmedBrightness = max(0.0, min(255.0, normalizedTarget))

            AlphabetCharacter(
                character = it.character,
                brightness = trimmedBrightness
            )
        }
    }

}
