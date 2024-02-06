import cut.CutCli
import org.junit.jupiter.api.Test
import utils.captureStreams
import utils.unzip
import utils.usingTempFile
import utils.withDefer
import java.io.File
import kotlin.io.path.name
import kotlin.test.assertEquals

class CutTest {

    private fun getZipFileStream() = CutTest::class.java.getResourceAsStream("cut/cut.zip")!!

    @Test
    fun simpleField() {
        // Given
        withDefer {
            unzip(::getZipFileStream)
            val (stdOut, stdErr) = captureStreams()

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
        withDefer {
            unzip(::getZipFileStream)
            val (stdOut, stdErr) = captureStreams()

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
        withDefer {
            unzip(::getZipFileStream)
            val (stdOut, stdErr) = captureStreams()

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
        withDefer {
            unzip(::getZipFileStream)
            val (stdOut, stdErr) = captureStreams()

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
        withDefer {
            unzip(::getZipFileStream)
            val (stdOut, stdErr) = captureStreams()
            val tempFile = usingTempFile(content =
                File("fourchords.csv")
                    .readLines()
                    .take(5)
                    .joinToString("\n")
            )

            // When
            CutCli().main(listOf("-f", "1,2", "-d", ",", tempFile.name))

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
        withDefer {
            unzip(::getZipFileStream)
            val (stdOut, stdErr) = captureStreams(
                newStdIn = File("fourchords.csv")
                    .readLines()
                    .takeLast(5)
                    .joinToString("\n")
                    .byteInputStream()
            )

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
        withDefer {
            unzip(::getZipFileStream)
            val (stdOut, stdErr) = captureStreams()

            // When
            CutCli().main(listOf("-f", "2", "-d", ",", "fourchords.csv"))

            // Then
            assertEquals(157, stdOut.toString().lineSequence().count())
            assertEquals("", stdErr.toString())
        }
    }
}
