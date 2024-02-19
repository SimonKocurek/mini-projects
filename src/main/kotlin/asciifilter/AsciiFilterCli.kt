package asciifilter

import com.github.ajalt.clikt.core.CliktCommand
import kotlinx.coroutines.*
import utils.LoggerFactory
import java.io.File
import javax.imageio.ImageIO


fun main(args: Array<String>) {
    AsciiFilterCli().main(args)
}

internal class AsciiFilterCli : CliktCommand(

) {

    //    kernelSize positive int
    val targetWidth = 30

    // Invert

    // Alphabet - does not need to be ASCII. Should be monospace. No sorting needed.
    //
    // $@B%8&WM#*oahkbdpqwmZO0QLCJUYXzcvunxrjft/\|()1{}[]?-_+~<>i!lI;:,"^`'.
    //  .:-=+*#%@
    //  !"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\]^_`abcdefghijklmnopqrstuvwxyz{|}~
    // АаБбВвГгДдЕеËëЖжЗзИиЙйКкЛлМмНнОоПпРрСсТтУуФфХхЦцЧчШшЩщЪъЫыЬьЭэЮюЯя
    // ΑαΒβΓγΔδΕεΖζΗηΘθΙιΚκΛλΜμΝνΞξΟοΠπΡρΣςσΤτΥυΦφΧχΨψΩω
    val alphabet = " !\"#\$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~"

    // file
    val file = File("c.gif")

    // Debug
    val verbose: Boolean = false

    override fun run() = runBlocking(Dispatchers.Default) {
        // Dependencies
        val logger = LoggerFactory.create("AsciiFilter", enabled = !verbose)
        val normalizer = CoroutineImageNormalizer(logger)
        val bitmapper = MonospaceCharacterBitmapper(logger)
        val filter = AsciiFilter(logger)

        // Input
        val image = withContext(Dispatchers.IO) { ImageIO.read(file) }
        logger.log("finished reading image", mapOf("file" to filter))

        // Processing
        val kernelSize = image.width / targetWidth
        val alphabetWithBitmaps = getAlphabetWithBitmaps(bitmapper, normalizer, kernelSize)
        // TODO adjust min and max based on alphabet
        val normalizedImage = normalizer.normalize(image)

        val asciiImage = filter.convert(
            image = normalizedImage,
            alphabetWithBitmaps = alphabetWithBitmaps
        )

        // Output
        println(asciiImage)
    }

    private suspend fun getAlphabetWithBitmaps(
        bitmapper: MonospaceCharacterBitmapper,
        normalizer: CoroutineImageNormalizer,
        kernelSize: Int
    ) = coroutineScope {
        alphabet.map { character ->
            async {
                val characterBitmap = bitmapper.get(character, kernelSize)
                val normalizedBitmap = normalizer.normalize(characterBitmap)

                Pair(character, normalizedBitmap)
            }
        }.awaitAll().associateBy({ it.first }, { it.second })
    }

}
