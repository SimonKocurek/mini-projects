import java.io.*
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Instant
import java.util.zip.ZipInputStream
import kotlin.io.path.*

private object Dummy

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

@OptIn(ExperimentalPathApi::class)
fun usingResourceFile(savedAs: String, resourcePath: String, callable: () -> Unit) {
    try {
        Path(savedAs).deleteRecursively()

        Dummy::class.java.getResourceAsStream(resourcePath)!!.use { inputStream ->
            FileOutputStream(savedAs).use {  outputStream ->
                inputStream.copyTo(outputStream)
            }
        }

        callable()
    } finally {
        Path(savedAs).deleteRecursively()
    }
}

fun usingTempFile(content: String, callable: (filePath: Path) -> Unit)  = usingTempFile(content.toByteArray(), callable)

fun usingTempFile(content: ByteArray, callable: (filePath: Path) -> Unit) {
    val filePath = Path("${Instant.now().epochSecond}-test.temp.txt")

    try {
        filePath.outputStream().buffered().use { outputStream ->
            outputStream.write(content)
        }

        callable(filePath)
    } finally {
        filePath.deleteIfExists()
    }
}

fun unzip(zipStream: InputStream) {
    ZipInputStream(zipStream).use { zippedArchive ->
        while (true) {
            val zippedFile = zippedArchive.nextEntry ?: return
            val outputFilePath = Paths.get(zippedFile.name)

            if (outputFilePath.parent?.exists() == false) {
                outputFilePath.createParentDirectories()
            }

            if (zippedFile.isDirectory) {
                outputFilePath.createDirectory()
                continue
            }

            outputFilePath.createFile()
            FileOutputStream(outputFilePath.toFile()).use { outputStream ->
                zippedArchive.copyTo(outputStream)
            }
        }
    }
}

@OptIn(ExperimentalPathApi::class)
fun cleanupUnzipped(zipStream: InputStream) {
    ZipInputStream(zipStream).use { zippedArchive ->
        while (true) {
            val zippedFile = zippedArchive.nextEntry ?: return
            val unzippedFile = Paths.get(zippedFile.name)
            unzippedFile.deleteRecursively()
        }
    }
}
