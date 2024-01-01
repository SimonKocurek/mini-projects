package wordcount

import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.Charset
import java.nio.charset.CharsetDecoder
import java.nio.charset.CodingErrorAction
import java.nio.file.Path

class WordCountProcessor(
    private val bytesFlag: Boolean,
    private val wordsFlag: Boolean,
    private val linesFlag: Boolean,
    private val charsFlag: Boolean,
    private val filePath: Path?,
) {

    private data class Result(
        val lines: Long,
        val words: Long,
        val chars: Long,
        val bytes: Long
    )

    fun process(stream: InputStream) {
        val readResult = readFromStream(stream)
        printOutput(readResult)
    }

    private fun readFromStream(stream: InputStream): Result {
        var bytes = 0L
        var lines = 0L
        var chars = 0L
        var words = 0L
        var finishedWithWhitespace = false

        val estimatedPageSize = 1024 * 4
        val byteArray = ByteArray(estimatedPageSize)
        val byteBuffer = ByteBuffer.wrap(byteArray)

        val characterDecoder = Charset.defaultCharset().newDecoder().apply {
            onUnmappableCharacter(CodingErrorAction.REPORT)
            onMalformedInput(CodingErrorAction.REPORT)
        }
        val charBuffer = CharBuffer.allocate(estimatedPageSize)

        var readBytes = stream.read(byteArray)
        while (readBytes != -1) {
            val readCharacters = if (wordsFlag || charsFlag) {
                decodeCharacters(characterDecoder, byteBuffer, charBuffer)
            } else 0

            bytes += readBytes
            chars += readCharacters

            if (linesFlag) {
                lines += countNewLines(readBytes, byteArray)
            }

            if (wordsFlag) {
                for (i in 0..<readCharacters) {
                    val isWhitespace = charBuffer[i].isWhitespace()
                    finishedWithWhitespace = isWhitespace
                }
            }

            readBytes = stream.read(byteArray)
        }

        return Result(
            lines = lines,
            words = words,
            chars = chars,
            bytes = bytes
        )
    }

    private fun decodeCharacters(characterDecoder: CharsetDecoder, byteBuffer: ByteBuffer, charBuffer: CharBuffer): Int {
        val result = characterDecoder.decode(byteBuffer, charBuffer, false)

        if (result.isError || result.isMalformed || result.isUnderflow || result.isUnmappable || result.isOverflow) {
            result.throwException()
        }

        return result.length()
    }

    private fun countNewLines(readBytes: Int, buffer: ByteArray): Long {
        var result = 0L

        val newline = '\n'.code.toByte()

        for (i in 0..<readBytes) {
            if (buffer[i] == newline) {
                result++
            }
        }

        return result
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
