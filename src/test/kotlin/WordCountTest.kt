import org.junit.jupiter.api.Test
import utils.captureStreams
import utils.unzip
import utils.withDefer
import wordcount.WordCountCli
import kotlin.io.path.Path
import kotlin.io.path.inputStream
import kotlin.test.assertEquals


class WordCountTest {

    private fun getZipFileStream() = WordCountTest::class.java.getResourceAsStream("wordcount/test.txt.zip")!!

    @Test
    fun readBytes() {
        // Given
        withDefer {
            unzip(::getZipFileStream)
            val (stdOut, stdErr) = captureStreams()

            // When
            WordCountCli().main(listOf("-c", "test.txt"))

            // Then
            assertEquals("342190 test.txt\n", stdOut.toString())
            assertEquals("", stdErr.toString())
        }
    }

    @Test
    fun readLines() {
        // Given
        withDefer {
            unzip(::getZipFileStream)
            val (stdOut, stdErr) = captureStreams()

            // When
            WordCountCli().main(listOf("-l", "test.txt"))

            // Then
            assertEquals("7145 test.txt\n", stdOut.toString())
            assertEquals("", stdErr.toString())
        }
    }

    @Test
    fun readWords() {
        // Given
        withDefer {
            unzip(::getZipFileStream)
            val (stdOut, stdErr) = captureStreams()

            // When
            WordCountCli().main(listOf("-w", "test.txt"))

            // Then
            assertEquals("58164 test.txt\n", stdOut.toString())
            assertEquals("", stdErr.toString())
        }
    }

    @Test
    fun readCharsUtf() {
        // Given
        withDefer {
            unzip(::getZipFileStream)
            val (stdOut, stdErr) = captureStreams()

            // When
            WordCountCli().main(listOf("-m", "test.txt", "--charset", "UTF-8"))

            // Then
            assertEquals("339292 test.txt\n", stdOut.toString())
            assertEquals("", stdErr.toString())
        }
    }

    @Test
    fun readCharsUtf16() {
        // Given
        withDefer {
            unzip(::getZipFileStream)
            val (stdOut, stdErr) = captureStreams()

            // When
            WordCountCli().main(listOf("-m", "test.txt", "--charset", "UTF-16"))

            // Then
            assertEquals("171095 test.txt\n", stdOut.toString())
            assertEquals("", stdErr.toString())
        }
    }

    @Test
    fun readWithoutFlags() {
        // Given
        withDefer {
            unzip(::getZipFileStream)
            val (stdOut, stdErr) = captureStreams()

            // When
            WordCountCli().main(listOf("test.txt", "--charset", "UTF-8"))

            // Then
            assertEquals("7145 58164 342190 test.txt\n", stdOut.toString())
            assertEquals("", stdErr.toString())
        }
    }

    @Test
    fun readStreamWithoutFlags() {
        // Given
        withDefer {
            unzip(::getZipFileStream)
            val bufferedStream = Path("test.txt").inputStream().buffered()
            defer { bufferedStream.close() }
            val (stdOut, stdErr) = captureStreams(newStdIn = bufferedStream)

            // When
            WordCountCli().main(listOf("--charset", "UTF-8"))

            // Then
            assertEquals("7145 58164 342190\n", stdOut.toString())
            assertEquals("", stdErr.toString())
        }
    }
}