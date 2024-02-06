package utils

import java.io.FileOutputStream
import java.io.InputStream
import java.nio.file.Paths
import java.util.zip.ZipInputStream
import kotlin.io.path.*

fun DeferContext.unzip(getZipFileStream: () -> InputStream) {
    // unzipping could fail if the files already existed
    getZipFileStream().use { cleanupUnzipped(it) }

    defer { getZipFileStream().use { cleanupUnzipped(it) } }
    ZipInputStream(getZipFileStream()).use { zippedArchive ->
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
private fun cleanupUnzipped(zipStream: InputStream) {
    ZipInputStream(zipStream).use { zippedArchive ->
        while (true) {
            val zippedFile = zippedArchive.nextEntry ?: return
            val unzippedFile = Paths.get(zippedFile.name)
            unzippedFile.deleteRecursively()
        }
    }
}
