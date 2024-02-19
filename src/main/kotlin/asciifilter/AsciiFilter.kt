package asciifilter

import kotlinx.coroutines.*
import utils.Logger
import kotlin.math.abs
import kotlin.math.min

internal class AsciiFilter(
    private val logger: Logger,
) {
    suspend fun convert(image: NormalizedImage, alphabetWithBitmaps: Map<Char, NormalizedImage>): String {
        return AsciiFilterInstance(
            logger = logger,
            image = image,
            alphabetWithBitmaps = alphabetWithBitmaps
        ).convert()
    }
}

private class AsciiFilterInstance(
    private val logger: Logger,
    private val image: NormalizedImage,
    private val alphabetWithBitmaps: Map<Char, NormalizedImage>
) {
    private val kernelWidth = alphabetWithBitmaps.values.first().width
    private val kernelHeight = alphabetWithBitmaps.values.first().height

    suspend fun convert() = coroutineScope {
        logger.log("converting image to ASCII")

        (0..<image.height step kernelHeight).map { y ->
            async { convertLineToString(y) }
        }.awaitAll().joinToString(separator = "")
    }

    private suspend fun convertLineToString(y: Int) = buildString {
        for (x in 0..<image.width step kernelWidth) {
            val mostSimilarCharacter = getMostSimilarCharacter(
                xStart = x,
                yStart = y,
                width = min(image.width - x, kernelWidth),
                height = min(image.height - y, kernelHeight),
            )
            append(mostSimilarCharacter)
        }
        append("\n")
    }

    private suspend fun getMostSimilarCharacter(xStart: Int, yStart: Int, width: Int, height: Int): Char =
        coroutineScope {
            alphabetWithBitmaps
                .mapValues { (_, characterImage) ->
                    async {
                        getImageDifference(
                            other = characterImage,
                            xStart = xStart,
                            yStart = yStart,
                            width = width,
                            height = height
                        )
                    }
                }
                .minBy { (_, asyncDifference) -> asyncDifference.await() }
                .key
        }

    private suspend fun getImageDifference(
        other: NormalizedImage,
        xStart: Int,
        yStart: Int,
        width: Int,
        height: Int
    ): Long = coroutineScope {
        (0..<height).map { y ->
            async {
                var difference = 0L

                for (x in 0..<width) {
                    difference += abs(image.brightness[yStart + y][xStart + x] - other.brightness[y][x])
                }

                difference
            }
        }.awaitAll().sum()
    }
}
