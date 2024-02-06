package utils

import java.io.FileOutputStream
import java.nio.file.Path
import java.time.Instant
import kotlin.io.path.*

private object Dummy

@OptIn(ExperimentalPathApi::class)
fun DeferContext.usingResourceFile(savedAs: String, resourcePath: String) {
    Path(savedAs).deleteRecursively()

    defer { Path(savedAs).deleteRecursively() }
    Dummy::class.java.getResourceAsStream(resourcePath)!!.use { inputStream ->
        FileOutputStream(savedAs).use { outputStream ->
            inputStream.copyTo(outputStream)
        }
    }
}

fun DeferContext.usingTempFile(content: String): Path = usingTempFile(content.toByteArray())

fun DeferContext.usingTempFile(content: ByteArray): Path {
    val filePath = Path("${Instant.now().epochSecond}-test.temp.txt")
    defer { filePath.deleteIfExists() }

    filePath.outputStream().buffered().use { outputStream ->
        outputStream.write(content)
    }

    return filePath
}
