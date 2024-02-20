package asciifilter.character

import utils.Logger
import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.image.BufferedImage

class CharacterBitmapper(
    private val logger: Logger
) {

    /**
     * Create a bitmap for the provided character. Rendered with antialiasing in black on white.
     *
     * @param character Any valid UTF-8 character.
     * @param fontSize Font size of the character in the resulting bitmap.
     */
    internal fun get(character: Char, fontSize: Int): BufferedImage {
        logger.log(
            "converting character to bitmap", mapOf(
                "character" to character.toString(),
                "fontSize" to fontSize,
            )
        )

        val dimensions = findCharacterDimensions(character, fontSize)
        return textToImage(character.toString(), fontSize, dimensions)
    }

    private fun textToImage(text: String, fontSize: Int, dimensions: CharacterDimensions): BufferedImage {
        val image = BufferedImage(dimensions.width, dimensions.height, BufferedImage.TYPE_INT_RGB)
        val graphics = image.createGraphics()

        graphics.color = Color.WHITE
        graphics.fillRect(0, 0, dimensions.width, dimensions.height)

        // Focus on the quality of generated bitmaps
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)

        graphics.font = getMonospaceFont(fontSize)
        graphics.color = Color.BLACK
        graphics.drawString(text, 0, graphics.fontMetrics.ascent)
        graphics.dispose()

        return image
    }

    private fun findCharacterDimensions(character: Char, fontSize: Int): CharacterDimensions {
        // We need an image only so that we can generate a graphics object
        // and figure out dimensions of the text.
        val image = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
        val graphics = image.createGraphics()

        graphics.font = getMonospaceFont(fontSize)
        graphics.dispose()

        return CharacterDimensions(
            width = graphics.fontMetrics.charWidth(character),
            height = graphics.fontMetrics.height,
        )
    }

    private fun getMonospaceFont(fontSize: Int) = Font(Font.MONOSPACED, Font.PLAIN, fontSize)

    private data class CharacterDimensions(val width: Int, val height: Int)
}
