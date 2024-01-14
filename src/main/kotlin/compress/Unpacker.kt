package compress

import java.io.File
import java.io.ObjectInputStream
import java.util.Stack

internal class Unpacker {

    fun unpack(inputFile: File, outputFile: File) {
        // Must be buffered, as we read input byte by byte
        ObjectInputStream(inputFile.inputStream().buffered()).use { inputStream ->
            val header = HeaderReader().readFromStream(inputStream)
            unpackBody(inputStream, header.expectedBodyCharacters, header.tree, outputFile)
        }
    }

    private fun unpackBody(
        inputStream: ObjectInputStream,
        expectedCharacters: Long,
        huffmanTreeRoot: TreeNode,
        outputFile: File
    ) {
        var currentNode = huffmanTreeRoot
        var remainingCharacters = expectedCharacters

        outputFile.bufferedWriter().use { outputStream ->
            whileLoop@ while (true) {
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

                    currentNode.value?.let { originalCharacter ->
                        outputStream.write(originalCharacter)
                        remainingCharacters--
                        currentNode = huffmanTreeRoot
                    }

                    if (remainingCharacters == 0L) {
                        if (inputStream.read() != -1) {
                            throw RuntimeException("Unpacked all expected characters, there are still more bytes at the end of the file. Was the file modified after compression?")
                        }
                        break@whileLoop
                    }
                }
            }
        }

        if (remainingCharacters > 0) {
            throw RuntimeException("Unpacked whole file body, but $remainingCharacters characters are missing from the end of the file. Was the file modified after compression?")
        }
        if (currentNode != huffmanTreeRoot) {
            // This should never happen, but it might be useful to have informative check
            throw RuntimeException("Finished unpacking file, but still got a character stuck in the middle of unpacking. Contact developers if this ever happens.")
        }
    }

    internal data class TreeNode(
        var left: TreeNode? = null,
        var right: TreeNode? = null,
        /** Character value *before* compression, only present in leaf nodes. */
        var value: Int? = null,
    )

}


internal class HeaderReader {

    internal fun readFromStream(inputStream: ObjectInputStream): Result {
        // Body size
        val expectedBodyCharacters = inputStream.readLong()

        // Tree
        val tree = readTree(inputStream)

        return Result(
            expectedBodyCharacters = expectedBodyCharacters,
            tree = tree
        )
    }

    private fun readTree(inputStream: ObjectInputStream): Unpacker.TreeNode {
        val tree = Unpacker.TreeNode()

        // Because the tree can get pretty deep when many different characters are used with
        // very varied frequencies, we should avoid using recursion.
        val stack = Stack<Unpacker.TreeNode>().apply { add(tree) }

        while (stack.isNotEmpty()) {
            val node = stack.pop()

            when (val byte = inputStream.read()) {
                0b00 -> {
                    node.value = inputStream.readInt()
                }

                0b10 -> {
                    node.left = Unpacker.TreeNode()
                    stack.add(node.left)
                }

                0b01 -> {
                    node.right = Unpacker.TreeNode()
                    stack.add(node.right)
                }

                0b11 -> {
                    node.left = Unpacker.TreeNode()
                    stack.add(node.left)
                    // Note: It is important to read left first, as that is how it is serialized
                    node.right = Unpacker.TreeNode()
                    stack.add(node.right)
                }

                -1 -> throw RuntimeException("Reached end of file, before finishing reading compression header.")
                else -> throw RuntimeException(
                    "Read unexpected identifier in the header. " +
                            "Expected one of these bytes when reading Huffman tree: 00, 01, 10, 11. " +
                            "But received $byte."
                )
            }
        }

        return tree
    }

    internal data class Result(
        val expectedBodyCharacters: Long,
        val tree: Unpacker.TreeNode
    )

}
