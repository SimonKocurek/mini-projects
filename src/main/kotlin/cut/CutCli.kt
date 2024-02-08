package cut

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.path

fun main(args: Array<String>) {
    CutCli().main(args)
}

internal class CutCli : CliktCommand(
    name = "cut",
    help = """
        Cut out selected portions of each line of a file.
        
        Outputs result in format "<line1 field1><delimiter><line1 field2>\n<line2 field1><delimiter><line2 field2>\n"
        
        > If enough delimited fields are not found in the line, whole line is printed out.

        Examples:
        ```bash
        ${'$'} cut -d : -f 1,7 /etc/passwd
        nobody:/usr/bin/false
        root:/bin/sh
        ```
        ```bash
        ${'$'} who | cut -f 6 -d ' '
        console
        ttys000
        ```
    """.trimIndent(),
    printHelpOnEmptyArgs = true
) {

    private val fields by option(
        "-f",
        "--fields",
        help = "List of indexes of fields (delimited by -d option) that should be printed out. Fields are indexed starting with 1. Example -f '1,2'"
    ).split(",").required()

    private val delimiter by option(
        "-d",
        "--delimiter",
        help = "Delimiter character to split fields. Delimiter is also used to separate fields in the output. Default: TAB."
    ).default("\t")

    private val filePath by argument(
        name = "file",
        help = "A pathname of an input file. If no file operands are specified, the standard input shall be used."
    ).path(mustExist = true, canBeDir = false, mustBeReadable = true).optional()

    override fun run() {
        val fieldIndexes = fields.map {
            try {
                it.toInt().let { parsed ->
                    if (parsed <= 0) {
                        throw NumberFormatException("Found non-positive index.")
                    } else {
                        // While cut fields are indexed starting with 1, in Java we index arrays by 0.
                        parsed - 1
                    }
                }
            } catch (e: NumberFormatException) {
                System.err.println("Each field identifier must a valid positive number. Field $it is not a positive number.")
                return
            }
        }

        val inputStream = filePath?.toFile()?.inputStream() ?: System.`in`
        inputStream.bufferedReader().use { reader ->
            Cut().printFields(reader, fieldIndexes, delimiter)
        }
    }

}
