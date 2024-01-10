package compress

import java.io.File
import java.io.ObjectInputStream

class Unpacker {

    fun unpack(inputFile: File, outputFile: File) {
        // Must be buffered, as we read input byte by byte
        ObjectInputStream(inputFile.inputStream().buffered()).use { inputStream ->
            val header = CompressionHeader.read(inputStream)
            val tree = toHuffmanTree(header.conversions)

            unpackBody(inputStream, header.bodyBytes, tree, outputFile)
        }
    }

    // Because each edge has 1 bit of information, root will be always present,
    // as it does not hold any information.
    private fun toHuffmanTree(conversions: List<CompressionConversion>) = TreeNode().apply {
        conversions.forEach { addConversionToTree(this, it) }
    }

    private fun addConversionToTree(root: TreeNode, conversion: CompressionConversion) {
        var currentNode = root

        for (i in 0 ..<conversion.bitCount) {
            when (val bit = (conversion.bits ushr i) and 1) {
                0 -> {
                    (currentNode.left ?: TreeNode()).let {
                        currentNode.left = it
                        currentNode = it
                    }
                }

                1 -> {
                    (currentNode.right ?: TreeNode()).let {
                        currentNode.right = it
                        currentNode = it
                    }
                }

                // This should never happen, but it's better to have an error message, just in case.
                else -> throw RuntimeException("Bit should be either 1 or 0, but got $bit!")
            }
        }

        currentNode.value = conversion.originalByte
        if (currentNode.left != null || currentNode.right != null) {
            // Should never happen under correct implementation.
            throw RuntimeException("Invalid header found! Assigned a decoding value to a non-leaf node. This could lead to a non-deterministic decoding.")
        }
    }

    private data class TreeNode(
        var left: TreeNode? = null,
        var right: TreeNode? = null,
        /** Byte value *before* compression, only present in leaf nodes. */
        var value: Int? = null,
    )

    private fun unpackBody(
        inputStream: ObjectInputStream,
        expectedBytes: Long,
        huffmanTreeRoot: TreeNode,
        outputFile: File
    ) {
        var currentNode = huffmanTreeRoot
        var remainingBytes = expectedBytes

        outputFile.outputStream().buffered().use { outputStream ->
            whileLoop@while (true) {
                val byte = inputStream.read()
                if (byte == -1) {
                    break
                }

                for (i in 7 downTo 0) {
                    currentNode = when (val bit = (byte ushr i) and 1) {
                        0 -> currentNode.left
                            ?: throw RuntimeException("Found bit series that should not be present in the file body. Compression header does not contain decoding for this sequence.")

                        1 -> currentNode.right
                            ?: throw RuntimeException("Found bit series that should not be present in the file body. Compression header does not contain decoding for this sequence.")

                        // This should never happen, but it's better to have an error message, just in case.
                        else -> throw RuntimeException("Bit should be either 1 or 0, but got $bit!")
                    }

                    currentNode.value?.let { originalByte ->
                        outputStream.write(originalByte)
                        remainingBytes--
                        currentNode = huffmanTreeRoot
                    }

                    if (remainingBytes == 0L) {
                        if (inputStream.read() != -1) {
                            throw RuntimeException("Unpacked all expected bytes, there are still more bytes at the end of the file. Was the file modified after compression?")
                        }
                        break@whileLoop
                    }
                }
            }
        }

        if (remainingBytes > 0) {
            throw RuntimeException("Unpacked whole file body, but $remainingBytes bytes are missing from the end of the file. Was the file modified after compression?")
        }
        if (currentNode != huffmanTreeRoot) {
            // This should never happen, but it might be useful to have informative check
            throw RuntimeException("Finished unpacking file, but still got Byte stuck in the middle of unpacking. Contact developers if this ever happens.")
        }
    }
}