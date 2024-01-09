package compress

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import kotlin.math.roundToInt


fun main(args: Array<String>) {
    CompressCli().main(args)
}

class CompressCli : CliktCommand(
    name = "compress",
    help = """
        Pack files to decrease their size.
        Unpack files to retrieve the original.
        Packing file creates a packed file with same filename and `.minzip` extension.

        Files are compressed using Huffman encoding.
        
        > On an empty file prints stderr error instead of compressing. 
        
        > If a file with .minzip suffix already exists during compression, prints error.
        > Similarly, during decompression if a file without .minzip suffix already exists, prints error.
        
        Examples:
        ```bash
        ${'$'} compress test.txt
        test.txt.minzip (deflated to 10%)
        ```
        ```bash
        ${'$'} compress -d test.txt.minzip
        test.txt (inflated to 1000%)
        ```
    """.trimIndent(),
    printHelpOnEmptyArgs = true
) {

    private val decompress by option("-d", "--decompress", help = "If specified, file will be unpacked.").flag()

    private val file by argument(help = "File to compress/unpack.").file(
        mustExist = true,
        canBeDir = false,
        mustBeReadable = true
    )

    override fun run() {
        if (file.length() == 0L) {
            System.err.println("Received empty file. Nothing to compress.")
            return
        }

        val compressed = Compressor().compress(file)

        val deflatedPercent = compressed.length() / file.length().toDouble() * 100
        println("${compressed.name} (deflated to ${deflatedPercent.roundToInt()}%)")
    }

}
