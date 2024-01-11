package wordcount

import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.channels.ReadableByteChannel
import java.nio.charset.Charset
import java.nio.file.Path

internal class WordCount(
    private val bytesFlag: Boolean,
    private val wordsFlag: Boolean,
    private val linesFlag: Boolean,
    private val charsFlag: Boolean,
    private val filePath: Path?,
    private val charset: Charset,
    private val bufferSize: Int = 1024 * 4,
) {

    fun process(channel: ReadableByteChannel) {
        val instance = WordCountInstance(
            wordsFlag = wordsFlag,
            linesFlag = linesFlag,
            bufferSize = bufferSize,
            charset = charset
        )
        instance.readFromStream(channel)
        printOutput(instance)
    }

    private fun printOutput(instance: WordCountInstance) {
        val output = buildString {
            if (linesFlag) {
                append("${instance.lines} ")
            }

            if (wordsFlag) {
                append("${instance.words} ")
            }

            if (bytesFlag || charsFlag) {
                if (charsFlag) {
                    append("${instance.chars} ")
                } else {
                    append("${instance.bytes} ")
                }
            }

            if (filePath != null) {
                append(filePath)
            }
        }

        println(output.trimEnd())
    }
}

/**
 * Instances are stateful and non-thread-safe.
 */
private class WordCountInstance(
    private val wordsFlag: Boolean,
    private val linesFlag: Boolean,
    bufferSize: Int,
    charset: Charset,
) {

    var lines: Long = 0
        private set
    var words: Long = 0
        private set
    var chars: Long = 0
        private set
    var bytes: Long = 0
        private set

    private var finishedWithWhitespace: Boolean = true
    private val byteBuffer = ByteBuffer.allocate(bufferSize)
    // Decoder is stateful and not thread safe, so we cannot reuse it between instances
    private val characterDecoder = charset.newDecoder()
    // In case some encodings have more characters per 1 byte, we need bigger character
    // buffer, to avoid overflows.
    private val characterBuffer = CharBuffer.allocate(bufferSize * 2)

    fun readFromStream(channel: ReadableByteChannel) {
        while (true) {
            val readBytes = channel.read(byteBuffer)
            if (readBytes == -1) {
                byteBuffer.flip()

                // As per documentation, we need to call decode with `endOfInput = true`
                // and flush at the end of reading.
                characterDecoder.decode(byteBuffer, characterBuffer, true)
                characterDecoder.flush(characterBuffer)

                // There might have been some bytes unconverted to characters before flushing.
                bytes += byteBuffer.limit().toLong()
                updateCharacterMetrics()
                break
            }

            // When decoding, we sometimes save some bytes for the next iteration, if some multibyte
            // character was missing final bytes.
            bytes += decodeBytesToCharacters()
            updateCharacterMetrics()
            characterBuffer.clear()
        }
    }

    private fun updateCharacterMetrics() {
        characterBuffer.flip() // Change to read mode
        chars += characterBuffer.limit()

        if (linesFlag) {
            // Even though Windows uses different System.lineSeparator(),
            // the original `wc` utility is made for UNIX systems and as
            // such it only considers '\n' as a newline.
            lines += characterBuffer.count { it == '\n' }
        }

        if (wordsFlag) {
            updateWordCount()
        }
    }

    private fun updateWordCount() {
        for (c in characterBuffer) {
            if (!c.isWhitespace() && finishedWithWhitespace) {
                words += 1
            }

            finishedWithWhitespace = c.isWhitespace()
        }
    }

    /**
     * @return Number of consumed bytes. (Might be lower than the byteBuffer limit, as some bytes at the end can carry over to the next iteration.)
     */
    private fun decodeBytesToCharacters(): Long {
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

}
