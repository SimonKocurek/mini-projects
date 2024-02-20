package asciifilter.image

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import utils.Logger
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

class AsciiArtImageFactory(
    private val logger: Logger,
    private val rescaler: ImageRescaler,
    private val normalizer: ImageBrightnessNormalizer,
) {

    suspend fun prepare(imageFile: File, characterWidth: Int, characterAspectRatio: Double): BufferedImage {
        val image = withContext(Dispatchers.IO) { ImageIO.read(imageFile) }
        logger.log("finished reading image", mapOf("file" to imageFile))

        val rescaledImage = rescaler.rescale(image, characterWidth, characterAspectRatio)
        normalizer.normalize(rescaledImage)

        return rescaledImage
    }

}
