package wordcount

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.path
import java.io.FileInputStream
import java.io.InputStream
import java.lang.StringBuilder
import java.nio.charset.Charset
import java.nio.file.Path

class WordCount : CliktCommand(help = "Word, line, and byte or character count.") {

    private val bytesFlag: Boolean by option("-c", "--bytes", help = "Print the byte counts.").flag()
    private val wordsFlag: Boolean by option("-w", "--words", help = "Print the word counts.").flag()
    private val linesFlag: Boolean by option("-l", "--lines", help = "Print the newline counts.").flag()
    private val charsFlag: Boolean by option("-m", "--chars", help = "Print the character counts (assuming current locale).").flag()
    private val filePath: Path? by argument(name = "file", help = "A pathname of an input file. If no file operands are specified, the standard input shall be used.").path(mustExist = true, canBeDir = false, mustBeReadable = true).optional()

    private data class Result(
        val lines: Long,
        val words: Long,
        val chars: Long,
        val bytes: Long
    )

    override fun run() {
        val inputStream = filePath?.let { FileInputStream(it.toFile()) } ?: System.`in`
        inputStream.use {
            val readResult = readFromStream(it)
            printOutput(readResult)
        }
    }

    private fun readFromStream(stream: InputStream): Result {
        var finishedWithWhitespace = false
        var lines = 0L
        var words = 0L
        var chars = 0L
        var bytes = 0L

        val estimatedPageSize = 1024 * 4
        val buffer = ByteArray(estimatedPageSize)

        var readBytes = stream.read(buffer)
        while (readBytes != -1) {
            val charset = Charset.defaultCharset()
            charset.decode()

            if (linesFlag) {
                for (i in 0..<readBytes) {
                    if (buffer[i] == '\n'.code.toByte()) {
                        lines += 1
                    }
                }
            }

            if (wordsFlag) {
//                for (i in 0..<readBytes) {
//                    if (buffer[i].toInt().toChar().isWhitespace()) {
//                        lines += 1
//                    }
//                }
//                output.append("$words ")
            }

            if (bytesFlag || charsFlag) {
                if (charsFlag) {
//                    output.append("$chars ")
                } else {
                    bytes += readBytes
                }
            }

            readBytes = stream.read(buffer)
        }

        return Result(
            lines = lines,
            words = words,
            chars = chars,
            bytes =bytes
        )
    }

    private fun printOutput(result: Result) {
        val output = StringBuilder()

        if (linesFlag) {
            output.append("${result.lines} ")
        }
        if (wordsFlag) {
            output.append("${result.words} ")
        }
        if (bytesFlag || charsFlag) {
            if (charsFlag) {
                output.append("${result.chars} ")
            } else {
                output.append("${result.bytes} ")
            }
        }
        if (filePath != null) {
            output.append(filePath)
        }

        println(output.toString().trimEnd())
    }

}