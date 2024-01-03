package wordcount

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.path
import java.io.FileInputStream
import java.nio.channels.Channels
import java.nio.file.Path

fun main(args: Array<String>) {
    WordCountCli().main(args)
}

class WordCountCli : CliktCommand(
    name = "wordcount",
    help = """Word, line, and byte or character count.
Outputs result in format "<lines> <words> <chars or bytes> <file path>\n"

Examples:
```bash
$ wordcount -l test.txt
1234 test.txt
```
```bash
$ cat test.txt | wordcount
1234 5678 9123
```"""
) {

    private val bytesFlag: Boolean by option("-c", "--bytes", help = "Print the byte counts.").flag()
    private val wordsFlag: Boolean by option("-w", "--words", help = "Print the word counts.").flag()
    private val linesFlag: Boolean by option("-l", "--lines", help = "Print the newline counts.").flag()
    private val charsFlag: Boolean by option(
        "-m",
        "--chars",
        help = "Print the character counts (assuming current locale)."
    ).flag()
    private val filePath: Path? by argument(
        name = "file",
        help = "A pathname of an input file. If no file operands are specified, the standard input shall be used."
    ).path(mustExist = true, canBeDir = false, mustBeReadable = true).optional()

    private val isDefaultConfiguration by lazy { !bytesFlag && !wordsFlag && !linesFlag && !charsFlag }

    override fun run() {
        val processor = WordCount(
            bytesFlag = if (isDefaultConfiguration) true else bytesFlag,
            wordsFlag = if (isDefaultConfiguration) true else wordsFlag,
            linesFlag = if (isDefaultConfiguration) true else linesFlag,
            charsFlag = charsFlag,
            filePath = filePath,
        )

        val inputStream = filePath?.let { FileInputStream(it.toFile()) } ?: System.`in`
        inputStream.use { stream ->
            Channels.newChannel(stream).use { channel ->
                processor.process(channel)
            }
        }
    }

}