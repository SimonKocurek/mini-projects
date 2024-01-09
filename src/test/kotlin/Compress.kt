import compress.CompressCli
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import kotlin.io.path.*
import kotlin.test.assertEquals

class Compress {

    companion object {
        private fun getZipFileStream() = Companion::class.java.getResourceAsStream("compress/test.zip")!!

        @JvmStatic
        @BeforeAll
        fun prepare() {
            cleanup()
            getZipFileStream().use { unzip(it) }
        }

        @JvmStatic
        @AfterAll
        fun cleanup() {
            getZipFileStream().use { cleanupUnzipped(it) }
        }
    }

    @AfterEach
    fun cleanupTestFiles() {
        Path("")
            .listDirectoryEntries()
            .filter { it.name.endsWith(".minzip") }
            .forEach { it.deleteIfExists() }
    }

    @Test
    fun largeFile() {
        // Given
        captureStreams { stdOut, stdErr ->
            // When
            CompressCli().main(listOf("test.txt"))

            // Then
            assertEquals("test.txt.minzip (deflated to 59%)\n", stdOut.toString())
            assertEquals("", stdErr.toString())
        }
    }

    @Test
    fun emptyFile() {
        // Given
        captureStreams { stdOut, stdErr ->
            usingTempFile("") {
                // When
                CompressCli().main(listOf(it.pathString))

                // Then
                assertEquals("", stdOut.toString())
                assertEquals("Received empty file. Nothing to compress.\n", stdErr.toString())
            }
        }
    }

    @Test
    fun smallFile() {
        // Given
        captureStreams { stdOut, stdErr ->
            usingTempFile("BCAADDDCCACACACCCCCCCCCCCCCCCCCCCCCCCAAAAAAABBABCCCBABCABCABCACBC") { tempFile ->
                // When
                CompressCli().main(listOf(tempFile.pathString))

                // Then
                assertEquals("${tempFile.pathString}.minzip (deflated to 68%)\n", stdOut.toString())
                assertEquals("", stdErr.toString())
            }
        }
    }

    @Test
    fun singleCharacter() {
        // Given
        captureStreams { stdOut, stdErr ->
            usingTempFile("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA") { tempFile ->
                // When
                CompressCli().main(listOf(tempFile.name))

                // Then
                assertEquals("${tempFile.name}.minzip (deflated to 40%)\n", stdOut.toString())
                assertEquals("", stdErr.toString())
            }
        }
    }

}