package asciifilter

import kotlinx.coroutines.*
import utils.Logger
import java.awt.image.BufferedImage
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

internal interface ImageNormalizer {

    /**
     * Modifies the image in a way so that fewer details are lost
     * when converting the image to ASCII.
     */
    suspend fun normalize(image: BufferedImage): NormalizedImage
}

internal class CoroutineImageNormalizer(
    private val logger: Logger,
) : ImageNormalizer {

    override suspend fun normalize(image: BufferedImage): NormalizedImage = coroutineScope {
        logger.log("normalizing brightness")

        val (minBrightness, maxBrightness) = getBrightnessLimits(image)
        logger.log(
            "original brightness limits", mapOf(
                "minBrightness" to minBrightness,
                "maxBrightness" to maxBrightness
            )
        )

        val normalized = (0..<image.height).map { y ->
            async {
                image.getNormalizedBrightnessLine(y, minBrightness, maxBrightness)
            }
        }.awaitAll()

        logger.log("finished normalizing image brightness")
        NormalizedImage(brightness = normalized)
    }

    private suspend fun getBrightnessLimits(image: BufferedImage): Pair<Int, Int> = coroutineScope {
        val lineBrightnessLimits = (0..<image.height).map { y ->
            async {
                image.getLineBrightnessLimits(y)
            }
        }.awaitAll()

        val minBrightness = lineBrightnessLimits.minOf { it.first }
        val maxBrightness = lineBrightnessLimits.maxOf { it.second }

        return@coroutineScope Pair(minBrightness, maxBrightness)
    }

    private fun BufferedImage.getNormalizedBrightnessLine(y: Int, minBrightness: Int, maxBrightness: Int) = buildList {
        for (x in 0..<width) {
            // If the whole image has the same brightness, there
            // is no brightness information that we can normalize.
            if (maxBrightness == minBrightness) {
                add(maxBrightness)
                continue
            }

            val brightness = getBrightness(x, y)
            val normalizedBrightness =
                ((brightness - minBrightness) * (255.0 / (maxBrightness - minBrightness))).roundToInt()
            val trimmedBrightness = max(0, min(255, normalizedBrightness))
            add(trimmedBrightness)
        }
    }

    private fun BufferedImage.getLineBrightnessLimits(y: Int): Pair<Int, Int> {
        var minBrightness = 255
        var maxBrightness = 0

        for (x in 0..<width) {
            val brightness = getBrightness(x, y)
            minBrightness = min(minBrightness, brightness)
            maxBrightness = max(maxBrightness, brightness)
        }

        return Pair(minBrightness, maxBrightness)
    }

    private fun BufferedImage.getBrightness(x: Int, y: Int): Int {
        val colors = getRGB(x, y)

        // Colors are a packing of Alpha, Red, Green Blue,
        // where each value has 1 byte in the 4-byte Int.
        val pixel = Pixel(
            r = (colors ushr 16) and 0b1111_1111,
            g = (colors ushr 8) and 0b1111_1111,
            b = colors and 0b1111_1111
        )

        return rgbToBrightness(pixel)
    }

    private fun rgbToBrightness(pixel: Pixel): Int {
        val maxValue = max(pixel.r, max(pixel.g, pixel.b))
        val minValue = min(pixel.r, min(pixel.g, pixel.b))

        return ((maxValue + minValue) / 2.0).roundToInt()
    }

    private data class Pixel(val r: Int, val g: Int, val b: Int)
}
