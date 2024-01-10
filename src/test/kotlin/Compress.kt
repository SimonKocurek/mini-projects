import compress.CompressCli
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files
import kotlin.io.path.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
            .filter { it.name.endsWith(".minzip") || it.name.endsWith(".unzipped") }
            .forEach { it.deleteIfExists() }
    }

    @Test
    fun largeFile() {
        // Given
        captureStreams { stdOut, stdErr ->
            // When
            CompressCli().main(listOf("test.txt"))
            CompressCli().main(listOf("-o", "test.txt.unzipped", "-u", "test.txt.minzip"))

            // Then
            assertEquals(
                "test.txt.minzip (deflated to 59%)\n" +
                        "test.txt.unzipped (inflated to 1100%)\n",
                stdOut.toString()
            )
            assertEquals("", stdErr.toString())

            assertEquals(File("test.txt").readText(), File("test.txt.unzipped").readText())
            assertTrue(File("test.txt").length() > File("test.txt.minzip").length())
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
                assertEquals("Received empty file. Nothing to do.\n", stdErr.toString())
            }
        }
    }

    @Test
    fun smallFile() {
        // Given
        captureStreams { stdOut, stdErr ->
            usingTempFile("CABCABCD") { tempFile ->
                // When
                CompressCli().main(listOf(tempFile.name))
                CompressCli().main(
                    listOf(
                        "--output",
                        "${tempFile.name}.unzipped",
                        "--unpack",
                        "${tempFile.name}.minzip",
                    )
                )

                // Then
                assertEquals(
                    "${tempFile.name}.minzip (deflated to 363%)\n" +
                            "${tempFile.name}.unzipped (inflated to 28%)\n",
                    stdOut.toString()
                )
                assertEquals("", stdErr.toString())

                assertEquals(tempFile.toFile().readText(), File("${tempFile.name}.unzipped").readText())
                // With small files, the header causes file to increase in size
                assertTrue(tempFile.toFile().length() < File("${tempFile.name}.minzip").length())
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
                CompressCli().main(listOf("-o", "${tempFile.name}.unzipped", "-u", "${tempFile.name}.minzip"))

                // Then
                assertEquals(
                    "${tempFile.name}.minzip (deflated to 48%)\n" +
                            "${tempFile.name}.unzipped (inflated to 208%)\n",
                    stdOut.toString()
                )
                assertEquals("", stdErr.toString())

                Files.readAllBytes(tempFile)

                assertEquals(tempFile.toFile().readText(), File("${tempFile.name}.unzipped").readText())
                assertTrue(tempFile.toFile().length() > File("${tempFile.name}.minzip").length())
            }
        }
    }
}