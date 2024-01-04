import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import wordcount.WordCountCli
import java.io.BufferedInputStream
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.test.assertEquals


class Wordcount {

    @BeforeEach
    fun prepare() {
        cleanup()
        getZipFileStream().use { unzip(it) }
    }

    @AfterEach
    fun cleanup() {
        getZipFileStream().use { cleanupUnzipped(it) }
    }

    private fun getZipFileStream() = javaClass.getResourceAsStream("wordcount/test.txt.zip")!!

    @Test
    fun readBytes() {
        // Given
        captureStreams { stdOut, stdErr ->

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
        captureStreams { stdOut, stdErr ->

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
        captureStreams { stdOut, stdErr ->

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
        captureStreams { stdOut, stdErr ->

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
        captureStreams { stdOut, stdErr ->

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
        captureStreams { stdOut, stdErr ->

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
        Files.newInputStream(Path("test.txt")).use { fileStream ->
            BufferedInputStream(fileStream).use { bufferedStream ->
                captureStreams(bufferedStream) { stdOut, stdErr ->

                    // When
                    WordCountCli().main(listOf("--charset", "UTF-8"))

                    // Then
                    assertEquals("7145 58164 342190\n", stdOut.toString())
                    assertEquals("", stdErr.toString())
                }
            }
        }
    }
}