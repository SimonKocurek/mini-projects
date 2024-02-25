package utils

import java.awt.image.BufferedImage
import java.awt.image.BufferedImage.TYPE_INT_ARGB
import java.awt.image.BufferedImage.TYPE_INT_RGB
import java.nio.file.Path
import javax.imageio.ImageIO

fun DeferContext.withImageFile(colors: List<List<Pixel>>, suffix: String = "png"): Path {
    val tempFile = usingTempFile(suffix = suffix)

    val image = createImage(
        colors, colorSpace = when (suffix) {
            "png", "bmp" -> TYPE_INT_ARGB
            else -> TYPE_INT_RGB
        }
    )

    ImageIO.write(image, suffix, tempFile.toFile())

    return tempFile
}

fun createImage(colors: List<List<Pixel>>, colorSpace: Int): BufferedImage {
    val image = BufferedImage(colors[0].size, colors.size, colorSpace)

    colors.forEachIndexed { y, line ->
        line.forEachIndexed { x, color ->
            val pixelColor =
                ((color.a and 0xFF) shl 24) or
                        ((color.r and 0xFF) shl 16) or
                        ((color.g and 0xFF) shl 8) or
                        (color.b and 0xFF)
            image.setRGB(x, y, pixelColor)
        }
    }

    return image
}

data class Pixel(
    val a: Int = 255,
    val r: Int,
    val g: Int,
    val b: Int,
)
