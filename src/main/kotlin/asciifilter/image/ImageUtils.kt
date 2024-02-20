package asciifilter.image

import java.awt.image.BufferedImage
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

fun BufferedImage.getBrightness(x: Int, y: Int): Int {
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

fun rgbToBrightness(pixel: Pixel): Int {
    val maxValue = max(pixel.r, max(pixel.g, pixel.b))
    val minValue = min(pixel.r, min(pixel.g, pixel.b))

    return ((maxValue + minValue) / 2.0).roundToInt()
}

data class Pixel(val r: Int, val g: Int, val b: Int)
