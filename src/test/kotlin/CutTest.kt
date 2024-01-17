import cut.CutCli
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.io.path.Path
import kotlin.io.path.deleteIfExists
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.test.assertEquals

class CutTest {

    companion object {
        private fun getZipFileStream() = Companion::class.java.getResourceAsStream("cut/cut.zip")!!

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
            .filter { it.name.endsWith("test.csv") }
            .forEach { it.deleteIfExists() }
    }

    @Test
    fun simpleField() {
        // Given
        captureStreams { stdOut, stdErr ->
            // When
            CutCli().main(listOf("-f", "1", "sample.tsv"))

            // Then
            assertEquals(listOf("f0", "0", "5", "10", "15", "20").joinToString("") { "$it\n" }, stdOut.toString())
            assertEquals("", stdErr.toString())
        }
    }

    @Test
    fun simpleDifferentField() {
        // Given
        captureStreams { stdOut, stdErr ->
            // When
            CutCli().main(listOf("-f", "2", "sample.tsv"))

            // Then
            assertEquals(listOf("f1", "1", "6", "11", "16", "21").joinToString("") { "$it\n" }, stdOut.toString())
            assertEquals("", stdErr.toString())
        }
    }

    @Test
    fun multipleFields() {
        // Given
        captureStreams { stdOut, stdErr ->
            // When
            CutCli().main(listOf("-f", "1,2", "sample.tsv"))

            // Then
            assertEquals(
                listOf("f0\tf1", "0\t1", "5\t6", "10\t11", "15\t16", "20\t21").joinToString("") { "$it\n" },
                stdOut.toString()
            )
            assertEquals("", stdErr.toString())
        }
    }

    @Test
    fun differentDelimiter() {
        // Given
        captureStreams { stdOut, stdErr ->
            // When
            CutCli().main(listOf("-f", "1", "-d", ",", "fourchords.csv"))

            // Then
            assertEquals(
                listOf(
                    "Song title",
                    "\"10000 Reasons (Bless the Lord)\"",
                    "\"20 Good Reasons\"",
                    "\"Adore You\"",
                    "\"Africa\""
                ).joinToString("") { "$it\n" },
                stdOut
                    .toString()
                    .replace("\uFEFF", "") // Remove UTF-8 BOM
                    .split("\n")
                    .take(5)
                    .joinToString("") { "$it\n" }
            )
            assertEquals("", stdErr.toString())
        }
    }

    @Test
    fun multipleFieldsAndDifferentDelimiter() {
        // Given
        // File is deleted using @AfterEach
        File("complex.test.csv").bufferedWriter().use { writer ->
            File("fourchords.csv").readLines().take(5).forEach { line ->
                writer.write(line)
                writer.write("\n")
            }
        }

        captureStreams { stdOut, stdErr ->
            // When
            CutCli().main(listOf("-f", "1,2", "-d", ",", "complex.test.csv"))

            // Then
            assertEquals(
                listOf(
                    "Song title,Artist",
                    "\"10000 Reasons (Bless the Lord)\",Matt Redman and Jonas Myrin",
                    "\"20 Good Reasons\",Thirsty Merc",
                    "\"Adore You\",Harry Styles",
                    "\"Africa\",Toto"
                ).joinToString("") { "$it\n" },
                stdOut
                    .toString()
                    .replace("\uFEFF", "") // Remove UTF-8 BOM
                    .split("\n")
                    .take(5)
                    .joinToString("") { "$it\n" }
            )
            assertEquals("", stdErr.toString())
        }
    }

    @Test
    fun readFromStream() {
        // Given
        // File is deleted using @AfterEach
        File("stream.test.csv").bufferedWriter().use { writer ->
            File("fourchords.csv").readLines().takeLast(5).forEach { line ->
                writer.write(line)
                writer.write("\n")
            }
        }

        captureStreams(newStdIn = File("stream.test.csv").inputStream()) { stdOut, stdErr ->
            // When
            CutCli().main(listOf("-f", "1,2", "-d", ","))

            // Then
            assertEquals(
                listOf(
                    "\"Young Volcanoes\",Fall Out Boy",
                    "\"You Found Me\",The Fray",
                    "\"You'll Think Of Me\",Keith Urban",
                    "\"You're Not Sorry\",Taylor Swift",
                    "\"Zombie\",The Cranberries"
                ).joinToString("") { "$it\n" },
                stdOut
                    .toString()
                    .replace("\uFEFF", "") // Remove UTF-8 BOM
                    .split("\n")
                    .take(5)
                    .joinToString("") { "$it\n" }
            )
            assertEquals("", stdErr.toString())
        }
    }

    @Test
    fun readWholeCsv() {
        // Given
        captureStreams { stdOut, stdErr ->
            // When
            CutCli().main(listOf("-f", "2", "-d", ",", "fourchords.csv"))

            // Then
            assertEquals(157, stdOut.toString().lineSequence().count())
            assertEquals("", stdErr.toString())
        }
    }

}