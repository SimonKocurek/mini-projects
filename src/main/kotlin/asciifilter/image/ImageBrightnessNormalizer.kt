package asciifilter.image

import kotlinx.coroutines.*
import utils.Logger
import java.awt.image.BufferedImage
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class ImageBrightnessNormalizer(
    private val logger: Logger,
) {

    /**
     * Modifies the image in a way so that fewer details are lost
     * when converting the image to ASCII.
     *
     * Stretches brightness range from current min-max brightness
     * levels to the targetMinBrightness-targetMaxBrightness levels.
     *
     * If the whole image has the same brightness, the new brightness
     * will be the average of the target brightness.
     *
     * @param image Image to normalize.
     * @param target New minimal and maximal brightness.
     */
    suspend fun normalize(image: BufferedImage, target: Limits = Limits(0, 255)) = coroutineScope {
        logger.log("normalizing brightness")

        val currentLimits = getBrightnessLimits(image)
        logger.log(
            "original brightness limits", mapOf(
                "minBrightness" to currentLimits.min,
                "maxBrightness" to currentLimits.max
            )
        )

        (0..<image.height).map { y ->
            async {
                image.normalizeLineBrightness(y, target = target, current = currentLimits)
            }
        }.awaitAll()

        logger.log("finished normalizing image brightness")
    }

    private suspend fun getBrightnessLimits(image: BufferedImage): Limits = coroutineScope {
        val lineBrightnessLimits = (0..<image.height).map { y ->
            async {
                image.getLineBrightnessLimits(y)
            }
        }.awaitAll()

        Limits(
            min = lineBrightnessLimits.minOf { it.min },
            max = lineBrightnessLimits.maxOf { it.max }
        )
    }

    private fun BufferedImage.getLineBrightnessLimits(y: Int): Limits {
        var minBrightness = 255
        var maxBrightness = 0

        for (x in 0..<width) {
            val brightness = getBrightness(x, y)
            minBrightness = min(minBrightness, brightness)
            maxBrightness = max(maxBrightness, brightness)
        }

        return Limits(
            min = minBrightness,
            max = maxBrightness
        )
    }

    private fun BufferedImage.normalizeLineBrightness(y: Int, target: Limits, current: Limits) {
        for (x in 0..<width) {
            // If the whole image has the same brightness, there
            // is no brightness information that we can normalize.
            if (current.min == current.max) {
                val averageBrightness = (target.min + target.max) / 2
                setRGB(x, y, brightnessToColor(averageBrightness))
                continue
            }

            val brightness = getBrightness(x, y)

            // Bring to a 0-1 range.
            val normalizedCurrent = (brightness - current.min) / (current.max - current.min).toDouble()
            // Bring to a target.min-target.max range.
            val normalizedTarget = target.min + normalizedCurrent * (target.max - target.min)
            // Make sure we still fit in the image range.
            val trimmedBrightness = max(0, min(255, normalizedTarget.roundToInt()))
            setRGB(x, y, brightnessToColor(trimmedBrightness))
        }
    }

    private fun brightnessToColor(brightness: Int): Int {
        val maskedBrightness = brightness and 0xFF
        return maskedBrightness or (maskedBrightness shl 8) or (maskedBrightness shl 16) or (0xFF shl 24)
    }

    data class Limits(val min: Int, val max: Int)

}
