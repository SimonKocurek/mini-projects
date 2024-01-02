package wordcount

import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.channels.ReadableByteChannel
import java.nio.charset.Charset
import java.nio.charset.CharsetDecoder
import java.nio.file.Path
import java.util.regex.Pattern

class WordCountProcessor(
    private val bytesFlag: Boolean,
    private val wordsFlag: Boolean,
    private val linesFlag: Boolean,
    private val charsFlag: Boolean,
    private val filePath: Path?,
    private val bufferSize: Int = 1024 * 4
) {

    private data class Result(
        val lines: Long,
        val words: Long,
        val chars: Long,
        val bytes: Long
    )

    fun process(channel: ReadableByteChannel) {
        val readResult = readFromStream(channel)
        printOutput(readResult)
    }

    private fun readFromStream(channel: ReadableByteChannel): Result {
        var bytes = 0L
        var lines = 0L
        var chars = 0L
        var words = 0L
        var finishedWithWhitespace = false
        val wordSplitPattern = Pattern.compile("\\s+")

        val byteBuffer = ByteBuffer.allocate(bufferSize)

        // Decoder is stateful and not thread safe, so we cannot reuse it between calls
        val characterDecoder = Charset.defaultCharset().newDecoder()
        // In case some encodings have more characters per 1 byte, we need bigger character
        // buffer, to avoid overflows.
        val characterBuffer = CharBuffer.allocate(bufferSize * 2)

        while (true) {
            val readBytes = channel.read(byteBuffer)
            if (readBytes == -1) {
                byteBuffer.flip()

                // As per documentation, we need to call decode with `endOfInput = true`
                // and flush at the end of reading.
                characterDecoder.decode(byteBuffer, characterBuffer, true)
                characterDecoder.flush(characterBuffer)

                characterBuffer.flip() // Change to read mode
                chars += characterBuffer.limit()
                if (linesFlag) {
                    lines += countNewLines(characterBuffer)
                }
                if (wordsFlag) {
                    words += characterBuffer.toString().split(wordSplitPattern).size
                }
                break
            }

            // When decoding, we sometimes save some bytes for the next iteration, if some multibyte
            // character was missing final bytes.
            bytes += decodeBytesToCharacters(byteBuffer, characterDecoder, characterBuffer)

            characterBuffer.flip() // Change to read mode
            chars += characterBuffer.limit()
            if (linesFlag) {
                lines += countNewLines(characterBuffer)
            }
            if (wordsFlag) {
                words += characterBuffer.toString().split(wordSplitPattern).size
            }
            characterBuffer.clear()
        }

        return Result(
            lines = lines,
            words = words,
            chars = chars,
            bytes = bytes
        )
    }

    /**
     * @return Number of consumed bytes. (Might be lower than the byteBuffer limit, as some bytes at the end can carry over to the next iteration.)
     */
    private fun decodeBytesToCharacters(byteBuffer: ByteBuffer, characterDecoder: CharsetDecoder, characterBuffer: CharBuffer): Long {
        // Flip from writing mode (cursor at the end) to reading mode (cursor at the start)
        byteBuffer.flip()

        var byteBufferWriteMode = false // Assume we start in read mode
        var consumedBytes = byteBuffer.limit().toLong()

        val decodeResult = characterDecoder.decode(byteBuffer, characterBuffer, false)

        if (decodeResult.isOverflow) {
            // It should never happen, that we get more characters than can fit in the buffer,
            // as we read them in each iteration.
            decodeResult.throwException()
        }

        if (decodeResult.isError) {
            decodeResult.throwException()
        }

        if (decodeResult.isUnderflow) {
            // If some character consists of multiple bytes and part of it was cut off at the
            // end of the read buffer, we can copy the first bytes to the start of the buffer
            // and read the rest of the character bytes in the following iteration.
            byteBuffer.compact()
            // Compacting implicitly flips mode from reading to writing.
            byteBufferWriteMode = true
            // These bytes will be read in the next batch, so we don't want to count them twice.
            consumedBytes -= byteBuffer.position()
        }

        if (!byteBufferWriteMode) {
            // Once we finish reading, we want to reset back to writing mode, so that we can read
            // the next batch of bytes.
            byteBuffer.clear()
        }

        return consumedBytes
    }

    private fun countNewLines(buffer: CharBuffer): Long {
        var result = 0L

        for (i in 0..<buffer.limit()) {
            if (buffer[i] == '\n') {
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
