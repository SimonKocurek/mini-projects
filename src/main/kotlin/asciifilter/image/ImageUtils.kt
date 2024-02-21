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
        a = if (colorModel.hasAlpha()) (colors ushr 24) and 0b1111_1111 else 0b1111_1111,
        r = (colors ushr 16) and 0b1111_1111,
        g = (colors ushr 8) and 0b1111_1111,
        b = colors and 0b1111_1111
    )

    return pixel.toBrightness()
}

fun Pixel.toBrightness(): Int {
    val maxValue = max(r, max(g, b))
    val minValue = min(r, min(g, b))

    val colorBrightness = ((maxValue + minValue) / 2.0).roundToInt()
    // Transparency will be considered white
    val transparencyBrightness = 255 - a

    return max(0,  min(255, colorBrightness + transparencyBrightness))
}

data class Pixel(val a: Int, val r: Int, val g: Int, val b: Int)
