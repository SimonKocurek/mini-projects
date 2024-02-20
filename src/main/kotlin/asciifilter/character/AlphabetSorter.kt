package asciifilter.character

import asciifilter.image.getBrightness
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.awt.image.BufferedImage

class AlphabetSorter {

    suspend fun sortByBrightness(characterToBitmap: Map<Char, BufferedImage>) = coroutineScope {
        characterToBitmap.mapValues { (_, characterBitmap) ->
            async { characterBitmap.getAverageBrightness() }
        }.mapValues { (_, coroutine) -> coroutine.await() }
            .entries
            .sortedBy { it.value }
            .map {
                AlphabetCharacter(
                    character = it.key,
                    brightness = it.value
                )
            }
    }

    private suspend fun BufferedImage.getAverageBrightness() = coroutineScope {
        val totalBrightness = (0..<height).map { y ->
            async { getLineBrightness(y) }
        }.awaitAll().sum()

        totalBrightness / (width * height).toDouble()
    }

    private fun BufferedImage.getLineBrightness(y: Int): Long {
        var lineBrightness = 0L

        for (x in 0..<width) {
            lineBrightness += getBrightness(x, y)
        }

        return lineBrightness
    }

}
