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

fun DeferContext.usingTempFile(content: String, suffix: String = "txt"): Path = usingTempFile(content.toByteArray(), suffix)

fun DeferContext.usingTempFile(content: ByteArray = byteArrayOf(), suffix: String): Path {
    val filePath = Path("${Instant.now().epochSecond}.test.$suffix")
    defer { filePath.deleteIfExists() }

    filePath.outputStream().buffered().use { outputStream ->
        outputStream.write(content)
    }

    return filePath
}
