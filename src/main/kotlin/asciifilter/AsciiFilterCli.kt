package asciifilter

import asciifilter.character.AlphabetBrightnessNormalizer
import asciifilter.character.AlphabetSorter
import asciifilter.character.AsciiFilterAlphabetFactory
import asciifilter.character.CharacterBitmapper
import asciifilter.image.AsciiArtImageFactory
import asciifilter.image.ImageBrightnessNormalizer
import asciifilter.image.ImageRescaler
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import kotlinx.coroutines.*
import utils.LoggerFactory
import java.awt.Font


fun main(args: Array<String>) {
    AsciiFilterCli().main(args)
}

internal class AsciiFilterCli : CliktCommand(
    name = "asciifilter",
    help = """
        Convert images to ASCII art using any set of characters you choose.

        - Can automatically find out the brightness of characters.
        - Can adapt to character sets with different aspect ratios. (For example Korean or Hebrew have different aspect ratio compared to Latin).
        - Parallerized using Kotlin coroutines for faster execution on large images.
        
        > Use white background for best results.

        Examples:
        ```bash
        ${'$'} asciifilter fish.jpg
         ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` `
         ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` `
         ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` `
         ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` `
         ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` `
         ` ` ` ` ` ` ` ` ` ` ` ` OmqmU ` ` ` ` ` ` ` ` ` `
        ` ` ` ` ` ` ` ` ` ` ` ` UjjcjjjjjjjjO``jjjjo` ` ` 
        ` ` ` ` ` ` ` ` ` ` ` #Ujt[jjjjjj<jj   j@@?jjj` ` 
        ` ` ` ` ` ` ` ` ` ` ````lvvvjv/jvjvjO `Ojjj?f ` ` 
        ` ` ` ` ` ` ` `   *j&````       &Uvjjq```tzz` ` ` 
        ` ` ` ` ` ` dOjjjjjjjvv         ``vxjj&x``O ` ` ` 
         ` ` ` ` ` dcjjjvjjjvjvv&    ````z&ttfUx{` ` ` ` `
         ` ` ` ` ` ` &ivvvjvjjvvvz``````x{xJY{j  ` ` ` ` `
         ` ` `Udivvv`````tfj<jttffq````U*{tt]{z` ` ` ` ` `
         ` ` dXvjvjjvz    ]Jxxxx*xx````j dUtvvv` ` ` ` ` `
         ` ` ddJvtfz{x&```:fzz{{{z ` ` ` UmUdmU` ` ` ` ` `
         ` ` `ddZxxJJ*m` mUjjjUd ` ` ` ` ` ` ` ` ` ` ` ` `
        ` ` ` `#dddIh ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` 
        ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` 
        ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` 
        ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` 
        ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` 
        ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` ` `
        ```
    """.trimIndent(),
    printHelpOnEmptyArgs = true
) {

    private val file by argument(
        help = "Image to convert to ASCII art. " +
                "Use white background for best results. " +
                "Supported formats are: jpg, png, gif, bmp, wbmp"
    ).file(mustExist = true, canBeDir = false, mustBeReadable = true)

    private val alphabet by option(
        "-a",
        "--alphabet",
        help = "Set of characters (in arbitrary order) that should be used to generate target ASCII image. " +
                "Any UTF-8 characters can be used, as long as they all have same aspect ratio. " +
                "Default: \$@B%8&WM#*oahkbdpqwmZO0QLCJUYXzcvunxrjft/\\|()1{}[]?-_+~<>i!lI;:,\"^`'. "
    ).default("\$@B%8&WM#*oahkbdpqwmZO0QLCJUYXzcvunxrjft/\\|()1{}[]?-_+~<>i!lI;:,\"^`'. ")

    private val targetWidth by option(
        "-w",
        "--width",
        help = "The number of character width of the result image. Default: 50"
    ).int().default(100)

    private val fontName by option(
        "-f",
        "--font",
        help = "Name of the font to use when figuring out character brightness and aspect ratio. " +
                "Monospace fonts should be used to get correct results. " +
                "Default: Monospaced"
    ).default(Font.MONOSPACED)

    private val verbose by option(
        "-v",
        "--verbose",
        help = "Enable debug logging."
    ).flag()

    private val fontSize by option(
        "--fontSize",
        help = "The font size that will be used to calculate how bright or dark each character is. " +
                "Lower values will lead to faster startup and lower memory usage. " +
                "However, lowering this too much will result in worse ASCII image quality. " +
                "Default: 10"
    ).int().default(10)

    override fun run() = runBlocking(Dispatchers.Default) {
        // Dependencies
        val logger = LoggerFactory.create("AsciiFilter", enabled = verbose)
        val asciiFilterAlphabetFactory = AsciiFilterAlphabetFactory(
            bitmapper = CharacterBitmapper(logger),
            alphabetSorter = AlphabetSorter(),
            normalizer = AlphabetBrightnessNormalizer()
        )
        val asciiArtImageFactory = AsciiArtImageFactory(
            logger = logger,
            rescaler = ImageRescaler(),
            normalizer = ImageBrightnessNormalizer(logger)
        )
        val filter = AsciiFilter(logger)

        // Processing
        val uniqueCharacters = alphabet.toList().distinct().joinToString("")
        val alphabet = asciiFilterAlphabetFactory.create(uniqueCharacters, fontName, fontSize)
        val image = asciiArtImageFactory.prepare(file, targetWidth, alphabet.averageAspectRatio)
        val asciiImage = filter.convert(image, alphabet.characters)

        // Output
        println(asciiImage)
    }

}
