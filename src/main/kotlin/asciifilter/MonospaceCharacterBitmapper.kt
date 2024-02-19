package asciifilter

import utils.Logger
import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

internal class MonospaceCharacterBitmapper(
    private val logger: Logger
) {

    private val fontSizeToPixelWidthCache: MutableMap<Int, Int> = mutableMapOf()

    /**
     * Create a bitmap for the provided character. Monospace character will be rendered with
     * antialiasing in black on white.
     *
     * @param character Any valid UTF-8 character.
     * @param width Pixel width of the result bitmap. Height is always 2x the width.
     * @param cacheFontSize Because each machine might have different fonts we have to
     *      find the most fitting font size that would produce a bitmap with requested
     *      pixel width. This process might be slower, so we might want to cache these
     *      results.
     */
    fun get(character: Char, width: Int, cacheFontSize: Boolean = true): BufferedImage {
        logger.log(
            "converting character to bitmap", mapOf(
                "character" to character.toString(),
                "width" to width,
                "cacheFontSize" to cacheFontSize
            )
        )

        val fontSize = findFontSize(width, cacheFontSize)
        return textToImage(character.toString(), fontSize, width)
    }

    private fun textToImage(text: String, fontSize: Int, pixelWidth: Int): BufferedImage {
        // Monospace fonts have a height of 2x the width.
        val image = BufferedImage(pixelWidth, pixelWidth * 2, BufferedImage.TYPE_INT_RGB)
        val graphics = image.createGraphics()

        graphics.color = Color.WHITE
        graphics.fillRect(0, 0, pixelWidth, pixelWidth * 2)

        // Focus on the quality of generated bitmaps
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)

        graphics.font = getMonospaceFont(fontSize)
        graphics.color = Color.BLACK
        graphics.drawString(text, 0, graphics.fontMetrics.ascent)
        graphics.dispose()

        ImageIO.write(image, "png", File("$text.png"))

        return image
    }

    private fun findFontSize(desiredPixelWidth: Int, cacheFontSize: Boolean): Int {
        if (cacheFontSize) {
            fontSizeToPixelWidthCache[desiredPixelWidth]?.let { fontSize ->
                logger.log(
                    "using cached font size", mapOf(
                        "desiredPixelWidth" to desiredPixelWidth,
                        "fontSize" to fontSize
                    )
                )

                return fontSize
            }
        }

        // We need an image only so that we can generate a graphics object
        val image = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
        val graphics = image.createGraphics()

        for (fontSize in 0..desiredPixelWidth * 2) {
            graphics.font = getMonospaceFont(fontSize)

            // All monospace characters should have the same width, so we can
            // just take a random one.
            val width = graphics.fontMetrics.stringWidth("a")
            if (cacheFontSize) {
                fontSizeToPixelWidthCache[width] = fontSize
            }

            if (width >= desiredPixelWidth) {
                // The font size might not alight perfectly to pixels,
                // but it is the best estimate we have
                if (cacheFontSize) {
                    fontSizeToPixelWidthCache[desiredPixelWidth] = fontSize
                }
                return fontSize
            }
        }

        throw RuntimeException("No font size found for the desired pixel width of ${desiredPixelWidth}px.")
    }

    private fun getMonospaceFont(fontSize: Int) = Font(Font.MONOSPACED, Font.PLAIN, fontSize)
}
