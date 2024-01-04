import java.io.*
import java.nio.file.Files
import java.nio.file.Paths
import java.util.zip.ZipInputStream
import kotlin.io.path.createFile


fun captureStreams(
    newStdIn: InputStream = ByteArrayInputStream(ByteArray(1000)),
    action: (stdOut: ByteArrayOutputStream, stdErr: ByteArrayOutputStream) -> Unit
) {
    val stdIn = System.`in`
    val stdOut = System.out
    val stdErr = System.err

    try {
        ByteArrayOutputStream(1000).use { newStdOut ->
            PrintStream(newStdOut).use { newStdOutStream ->
                ByteArrayOutputStream(1000).use { newStdErr ->
                    PrintStream(newStdErr).use { newStdErrStream ->
                        System.setIn(newStdIn)
                        System.setOut(newStdOutStream)
                        System.setErr(newStdErrStream)

                        action(newStdOut, newStdErr)
                    }
                }
            }
        }
    } finally {
        System.setErr(stdErr)
        System.setOut(stdOut)
        System.setIn(stdIn)
    }
}

fun unzip(zipStream: InputStream) {
    ZipInputStream(zipStream).use { zippedArchive ->
        while (true) {
            val zippedFile = zippedArchive.nextEntry ?: return
            val outputFilePath = Paths.get(zippedFile.name)

            if (outputFilePath.parent != null) {
                println("!Warning: Not unzipping nested file with path $outputFilePath")
                continue
            }

            outputFilePath.createFile()
            FileOutputStream(outputFilePath.toFile()).use { outputStream ->
                zippedArchive.copyTo(outputStream)
            }
        }
    }
}

fun cleanupUnzipped(zipStream: InputStream) {
    ZipInputStream(zipStream).use { zippedArchive ->
        while (true) {
            val zippedFile = zippedArchive.nextEntry ?: return
            val unzippedFile = Paths.get(zippedFile.name)
            Files.deleteIfExists(unzippedFile)
        }
    }
}
