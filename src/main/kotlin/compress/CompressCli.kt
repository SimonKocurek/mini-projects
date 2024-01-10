package compress

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import java.io.File
import kotlin.math.roundToInt


fun main(args: Array<String>) {
    CompressCli().main(args)
}

class CompressCli : CliktCommand(
    name = "compress",
    help = """
        Compress files to decrease their size.
        Unpack files to retrieve the original.

        Files are compressed using Huffman encoding.
        
        > On an empty file prints stderr error instead of processing. 
        
        > If a file with name specified in --output argument already exists, prints error and stops.
        
        Examples:
        ```bash
        ${'$'} compress test.txt
        test.txt.minzip (deflated to 10%)
        ```
        ```bash
        ${'$'} compress --output test.json --unpack test.txt.minzip
        test.json (inflated to 1000%)
        ```
    """.trimIndent(),
    printHelpOnEmptyArgs = true
) {

    private val unpack by option(
        "-u",
        "--unpack",
        help = "If specified, file will be unpacked as opposed to compressed."
    ).flag()

    private val outputName by option(
        "-o",
        "--output",
        help = "Name of the generated file. If no name is provided, '.minzip' will be added/removed from the input file."
    )

    private val file by argument(help = "File to compress/unpack.").file(
        mustExist = true,
        canBeDir = false,
        mustBeReadable = true
    )

    override fun run() {
        if (file.length() == 0L) {
            System.err.println("Received empty file. Nothing to do.")
            return
        }

        if (unpack) {
            unpackFile()
        } else {
            compressFile()
        }
    }

    private fun unpackFile() {
        val outputFile = outputName?.let { File(it) } ?: if (file.extension == "minzip") {
            File(file.nameWithoutExtension)
        } else {
            System.err.println("Unknown output file name. Please provide '--output' parameter or specify an input file with '.minzip' extension.")
            return
        }

        if (outputFile.exists()) {
            System.err.println("File ${outputFile.name} already exists. Not compressing.")
            return
        }

        Unpacker().unpack(file, outputFile)

        val inflatedPercent = outputFile.length() / file.length().toDouble() * 100
        println("${outputFile.name} (inflated to ${inflatedPercent.roundToInt()}%)")
    }

    private fun compressFile() {
        val outputFile = File(outputName ?: "${file.absolutePath}.minzip")
        if (outputFile.exists()) {
            System.err.println("File ${outputFile.name} already exists. Not compressing.")
            return
        }

        Compressor().compress(file, outputFile)

        val deflatedPercent = outputFile.length() / file.length().toDouble() * 100
        println("${outputFile.name} (deflated to ${deflatedPercent.roundToInt()}%)")
    }

}
