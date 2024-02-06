import compress.CompressCli
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import utils.captureStreams
import utils.unzip
import utils.usingTempFile
import utils.withDefer
import java.io.File
import java.nio.file.Files
import kotlin.io.path.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CompressTest {

    private fun getZipFileStream() = CompressTest::class.java.getResourceAsStream("compress/test.zip")!!

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
        withDefer {
            unzip(::getZipFileStream)
            val (stdOut, stdErr) = captureStreams()

            // When
            CompressCli().main(listOf("test.txt"))
            CompressCli().main(listOf("-o", "test.txt.unzipped", "-u", "test.txt.minzip"))

            // Then
            assertEquals(
                "test.txt.minzip (deflated to 57%)\n" +
                        "test.txt.unzipped (inflated to 175%)\n",
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
        withDefer {
            val (stdOut, stdErr) = captureStreams()
            val tempFile = usingTempFile(content = "")

            // When
            CompressCli().main(listOf(tempFile.pathString))

            // Then
            assertEquals("", stdOut.toString())
            assertEquals("Received empty file. Nothing to do.\n", stdErr.toString())
        }
    }

    @Test
    fun smallFile() {
        // Given
        withDefer {
            val (stdOut, stdErr) = captureStreams()
            val tempFile = usingTempFile(content = "CABCABCD")

            // When
            CompressCli().main(listOf(tempFile.name))
            CompressCli().main(listOf("--output", "${tempFile.name}.unzipped", "--unpack", "${tempFile.name}.minzip"))

            // Then
            // With small files and many distinct characters, the header causes file to increase in size
            assertEquals(
                "${tempFile.name}.minzip (deflated to 488%)\n" +
                        "${tempFile.name}.unzipped (inflated to 21%)\n",
                stdOut.toString()
            )
            assertEquals("", stdErr.toString())

            assertEquals(tempFile.toFile().readText(), File("${tempFile.name}.unzipped").readText())
            // With small files, the header causes file to increase in size
            assertTrue(tempFile.toFile().length() < File("${tempFile.name}.minzip").length())
        }
    }

    @Test
    fun singleCharacter() {
        // Given
        withDefer {
            val (stdOut, stdErr) = captureStreams()
            val tempFile = usingTempFile(content = "A".repeat(5000))

            // When
            CompressCli().main(listOf(tempFile.name))
            CompressCli().main(listOf("-o", "${tempFile.name}.unzipped", "-u", "${tempFile.name}.minzip"))

            // Then
            assertEquals(
                "${tempFile.name}.minzip (deflated to 13%)\n" +
                        "${tempFile.name}.unzipped (inflated to 772%)\n",
                stdOut.toString()
            )
            assertEquals("", stdErr.toString())

            Files.readAllBytes(tempFile)

            assertEquals(tempFile.toFile().readText(), File("${tempFile.name}.unzipped").readText())
            assertTrue(tempFile.toFile().length() > File("${tempFile.name}.minzip").length())
        }
    }

    @Test
    fun nonText() {
        // Given
        withDefer {
            val (stdOut, stdErr) = captureStreams()
            val tempFile = usingTempFile(
                content = ByteArray(5000).mapIndexed { index, _ -> index.toByte() }.toByteArray()
            )

            // When
            CompressCli().main(listOf(tempFile.name))
            CompressCli().main(listOf("-o", "${tempFile.name}.unzipped", "-u", "${tempFile.name}.minzip"))

            // Then
            assertEquals(
                "${tempFile.name}.minzip (deflated to 73%)\n" +
                        "${tempFile.name}.unzipped (inflated to 269%)\n",
                stdOut.toString()
            )
            assertEquals("", stdErr.toString())

            Files.readAllBytes(tempFile)

            assertEquals(tempFile.toFile().readText(), File("${tempFile.name}.unzipped").readText())
            assertTrue(tempFile.toFile().length() > File("${tempFile.name}.minzip").length())
        }
    }

    @Test
    fun withUTF8Characters() {
        // Given
        withDefer {
            val (stdOut, stdErr) = captureStreams()
            val tempFile = usingTempFile(
                content = byteArrayOf(
                    97, 44, // Valid ASCII
                    -61, -79, 44, // Valid 2 Octet Sequence
                    -61, 40, 44, // Invalid 2 Octet Sequence
                    -96, -95, 44, // Invalid Sequence Identifier
                    -30, -126, -95, 44, // Valid 3 Octet Sequence
                    -30, 40, -95, 44, // Invalid 3 Octet Sequence (in 2nd Octet)
                    -30, -126, 40, 44, // Invalid 3 Octet Sequence (in 3rd Octet)
                    -16, -112, -116, -68, 44, // Valid 4 Octet Sequence
                    -16, 40, -116, -68, 44, // Invalid 4 Octet Sequence (in 2nd Octet)
                    -16, -112, 40, -68, 44, // Invalid 4 Octet Sequence (in 3rd Octet)
                    -16, 40, -116, 40, 44 // Invalid 4 Octet Sequence (in 4th Octet)
                )
            )

            // When
            CompressCli().main(listOf(tempFile.name))
            CompressCli().main(listOf("-o", "${tempFile.name}.unzipped", "-u", "${tempFile.name}.minzip"))

            // Then
            // With small files and many distinct characters, the header causes file to increase in size
            assertEquals(
                "${tempFile.name}.minzip (deflated to 167%)\n" +
                        "${tempFile.name}.unzipped (inflated to 93%)\n",
                stdOut.toString()
            )
            assertEquals("", stdErr.toString())

            Files.readAllBytes(tempFile)

            assertEquals(tempFile.toFile().readText(), File("${tempFile.name}.unzipped").readText())
            // With small files and many distinct characters, the header causes file to increase in size
            assertTrue(tempFile.toFile().length() < File("${tempFile.name}.minzip").length())
        }
    }
}
