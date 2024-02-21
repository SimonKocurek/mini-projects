package asciifilter.image

import java.awt.Image
import java.awt.image.BufferedImage
import kotlin.math.roundToInt

class ImageRescaler {

    /**
     * Create a rescaled image where 1 pixel will represent one character in the result.
     *
     * @param image Image to rescale (image is not modified in place).
     * @param targetCharacters Number of pixels in width to rescale to.
     * @param characterAspectRatio Aspect ratio of an average character.
     */
    fun rescale(image: BufferedImage, targetCharacters: Int, characterAspectRatio: Double): BufferedImage {
        // We let downscaling algorithm average out the pixel values.
        // We want exactly 1 pixel per character.
        val imageAspectRatio = image.height / image.width.toDouble()
        val rescaledImage = image.getScaledInstance(
            targetCharacters,
            (targetCharacters * imageAspectRatio / characterAspectRatio).roundToInt(),
            Image.SCALE_DEFAULT
        )
        if (rescaledImage is BufferedImage) {
            return rescaledImage
        }

        val scaledBufferedImage = BufferedImage(
            rescaledImage.getWidth(null),
            rescaledImage.getHeight(null),
            // We can't throw away the Alpha channel just yet, as
            // it might be used during brightness normalization.
            image.type
        )
        val graphics = scaledBufferedImage.createGraphics()
        graphics.drawImage(rescaledImage, 0, 0, null)
        graphics.dispose()

        return scaledBufferedImage
    }

}
