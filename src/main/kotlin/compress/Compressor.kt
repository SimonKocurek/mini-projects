package compress

import java.io.File
import java.io.ObjectOutputStream
import java.util.Comparator
import java.util.PriorityQueue
import java.util.Stack

internal class Compressor {

    fun compress(inputFile: File, outputFile: File) {
        val frequencies = getCharacterFrequencies(inputFile)
        val tree = buildHuffmanTree(frequencies)
        val conversions = toConversions(tree)

        // Buffering needed, as we output data byte by byte
        ObjectOutputStream(outputFile.outputStream().buffered()).use { outputStream ->
            // Because we write and read in the streaming manner, we first need
            // to write expected number of bytes in the body to the header, and
            // only then can we start writing compressed body.
            val expectedBodyBytes = HeaderWriter(tree, conversions.size).writeToStream(outputStream)
            val writtenBodyBytes = compressBodyToStream(conversions, inputFile, outputStream)

            if (expectedBodyBytes != writtenBodyBytes) {
                throw RuntimeException(
                    "Number of expected bytes to be in the compressed body $expectedBodyBytes does " +
                            "not match with the number of bytes that were actually written to the body: $writtenBodyBytes. " +
                            "This should not happen. Please report the issue to the developers."
                )
            }
        }
    }

    /**
     * @return Number of occurrences of each character in the compressed file.
     */
    private fun getCharacterFrequencies(file: File): Map<Int, Long> = buildMap {
        file.reader().buffered().use { reader ->
            while (true) {
                val character = reader.read()
                if (character == -1) {
                    break
                }

                merge(character, 1) { current, added -> current + added }
            }
        }
    }

    /**
     * Builds an encoding tree, where going to the left represents '0' bit and going to right '1' bit.
     */
    private fun buildHuffmanTree(frequencies: Map<Int, Long>): TreeNode {
        val nodeOrder = getNodePriorityQueue(frequencies)

        while (nodeOrder.size > 1) {
            val leastFrequent = nodeOrder.poll()
            val secondLeastFrequent = nodeOrder.poll()

            val parentNode = TreeNode(
                left = leastFrequent,
                right = secondLeastFrequent,
                frequency = leastFrequent.frequency + secondLeastFrequent.frequency,
            )
            nodeOrder.add(parentNode)
        }

        val root = nodeOrder.poll()
        if (root.value != null) {
            // If we have only 1 character in the whole file, it can happen that the root of the tree
            // will be a leaf as well. In such case we would be trying to encode that character using 0
            // bits. To prevent that, we artificially assign it bit '0'.
            return TreeNode(left = root, frequency = root.frequency)
        }
        return root
    }

    /**
     * Get priority queue, that always returns least frequent node.
     */
    private fun getNodePriorityQueue(frequencies: Map<Int, Long>): PriorityQueue<TreeNode> {
        val lowerFrequencyFirst = Comparator.comparingLong<TreeNode> { it.frequency }
        val result = PriorityQueue(lowerFrequencyFirst)

        val nodes = frequencies.entries.map {
            TreeNode(frequency = it.value, value = it.key)
        }
        result.addAll(nodes)

        return result
    }

    internal data class TreeNode(
        val left: TreeNode? = null,
        val right: TreeNode? = null,
        /** Number of occurrences of characters from the subtree in the compressed file. */
        val frequency: Long,
        /** Original character value, only present in leaf nodes. */
        val value: Int? = null,
    )

    /**
     * @return conversion rules used for mapping each character to possibly fewer bits.
     *  (character can be mapped to more bits than in uncompressed version if it is not frequent)
     */
    private fun toConversions(tree: TreeNode): List<Conversion> = buildList {
        // Because the tree can get pretty deep when many different characters are used with
        // very varied frequencies, we should avoid using recursion.
        val stack = Stack<Triple<TreeNode, Int, List<Boolean>>>()
        stack.add(Triple(tree, 0, emptyList()))

        while (stack.isNotEmpty()) {
            val (node, depth, encoding) = stack.pop()

            node.value?.let {
                add(
                    Conversion(
                        originalCharacter = it,
                        bits = encoding,
                    )
                )
            }

            node.left?.let {
                val newEncoding = encoding.toMutableList().apply { add(false) }
                stack.add(Triple(it, depth + 1, newEncoding))
            }
            node.right?.let {
                val newEncoding = encoding.toMutableList().apply { add(true) }
                stack.add(Triple(it, depth + 1, newEncoding))
            }
        }
    }

    /**
     * @return Number of bytes that was written
     */
    private fun compressBodyToStream(
        conversions: List<Conversion>,
        file: File,
        outputStream: ObjectOutputStream
    ): Long {
        val characterConversion = conversions.associateBy { it.originalCharacter }

        var writtenBytes = 0L

        var bitsInBuffer = 0
        var buffer = 0

        // Must be buffered because we read character by character.
        file.reader().buffered().use { inputStream ->
            while (true) {
                val readCharacter = inputStream.read()
                if (readCharacter == -1) {
                    break
                }

                val convertedAs = characterConversion[readCharacter]
                    ?: throw RuntimeException("File contains character $readCharacter that was not found when calculating character frequency table. Is the file being modified?")

                // This could be sped up by using byte chunks instead of setting bit by bit.
                convertedAs.bits.forEach { bit ->
                    bitsInBuffer++
                    buffer = when (bit) {
                        true -> (buffer shl 1) or 1
                        false -> (buffer shl 1)
                    }

                    if (bitsInBuffer == 8) {
                        bitsInBuffer = 0
                        writtenBytes++
                        outputStream.writeByte(buffer)
                    }
                }
            }
        }

        if (bitsInBuffer > 0) {
            writtenBytes++
            outputStream.writeByte(buffer)
        }

        return writtenBytes
    }

    private data class Conversion(
        /** Character before compression */
        val originalCharacter: Int,
        /** Bits after compression */
        val bits: List<Boolean>,
    ) {
        override fun equals(other: Any?): Boolean = throw UnsupportedOperationException()
        override fun hashCode() = throw UnsupportedOperationException()
    }
}

internal class HeaderWriter(
    private val tree: Compressor.TreeNode,
    private val leafCount: Int,
) {

    /**
     * @return Number of bytes that is expected to be in the compressed body.
     */
    internal fun writeToStream(outputStream: ObjectOutputStream): Long {
        // Body size
        val expectedBodyByteCount = getExpectedBodyByteCount()
        outputStream.writeLong(expectedBodyByteCount)

        // Tree size
        outputStream.writeInt(leafCount)

        // Tree
        var nextValueIsLeaf = false
        getEncodedTree().forEach {
            if (nextValueIsLeaf) {
                nextValueIsLeaf = false
                outputStream.writeInt(it)
                return@forEach
            }

            nextValueIsLeaf = it == 0b00
            outputStream.writeByte(it)
        }


        return expectedBodyByteCount
    }

    /**
     * @return The expected number of bytes the compressed body should have.
     */
    private fun getExpectedBodyByteCount(): Long {
        var bits = 0L

        // Because the tree can get pretty deep when many different characters are used with
        // very varied frequencies, we should avoid using recursion.
        val stack = Stack<Pair<Compressor.TreeNode, Int>>()
        stack.add(Pair(tree, 0))

        while (stack.isNotEmpty()) {
            val (node, depth) = stack.pop()

            if (node.value != null) {
                bits += node.frequency * depth
                continue
            }

            node.left?.let { stack.add(Pair(it, depth + 1)) }
            node.right?.let { stack.add(Pair(it, depth + 1)) }
        }

        return bits / 8 + (if (bits % 8 > 0) 1 else 0)
    }

    /**
     * @return A list of bits how the tree should be encoded:
     *  - 00 -> Is leaf
     *  - 01 -> Has right node
     *  - 10 -> Has left node
     *  - 11 -> Has left and right node
     *  - <full int value> -> original encoding of character
     */
    private fun getEncodedTree(): List<Int> = buildList {
        // Because the tree can get pretty deep when many different characters are used with
        // very varied frequencies, we should avoid using recursion.
        val stack = Stack<Compressor.TreeNode>()
        stack.add(tree)

        while (stack.isNotEmpty()) {
            val node = stack.pop()

            if (node.value != null) {
                add(0b00)
                add(node.value)
                continue // Technically not needed, but only leaf nodes have value, so we can skip the subtree logic
            }

            if (node.left != null && node.right != null) {
                add(0b11)
                stack.add(node.left)
                stack.add(node.right)
                continue
            }

            node.left?.let {
                add(0b10)
                stack.add(it)
            }
            node.right?.let {
                add(0b01)
                stack.add(it)
            }
        }
    }
}
