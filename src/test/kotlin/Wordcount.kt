import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import wordcount.WordCount
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.util.zip.ZipInputStream
import kotlin.io.path.createFile
import kotlin.test.assertEquals


class Wordcount {

    private val testFilePath = Paths.get("test.txt")

    @BeforeEach
    fun prepare() {
        extractTestFile()
    }

    private fun extractTestFile() {
        cleanup()

        javaClass.getResourceAsStream("wordcount/test.txt.zip")?.use { zipStream ->
            ZipInputStream(zipStream).use { zippedArchive ->
                zippedArchive.nextEntry?.let { zippedFile ->
                    val outputFilePath = testFilePath.createFile()
                    FileOutputStream(outputFilePath.toFile()).use { outputStream ->
                        zippedArchive.copyTo(outputStream)
                    }
                }
            }
        }
    }

    @AfterEach
    fun cleanup() {
        Files.deleteIfExists(testFilePath)
    }

    @Test
    fun readBytes() {
        // Given
        captureStreams { stdIn, stdOut, stdErr ->

            // When
            WordCount().main(listOf("-c", "test.txt"))

            // Then
            assertEquals("342190 test.txt\n", stdOut.toString())
            assertEquals("", stdErr.toString())
        }
    }

    @Test
    fun readLines() {
        // Given
        captureStreams { stdIn, stdOut, stdErr ->

            // When
            WordCount().main(listOf("-l", "test.txt"))

            // Then
            assertEquals("7145 test.txt\n", stdOut.toString())
            assertEquals("", stdErr.toString())
        }
    }

    @Test
    fun readWords() {
        // Given
        captureStreams { stdIn, stdOut, stdErr ->

            // When
            WordCount().main(listOf("-w", "test.txt"))

            // Then
            assertEquals("58164 test.txt\n", stdOut.toString())
            assertEquals("", stdErr.toString())
        }
    }
}