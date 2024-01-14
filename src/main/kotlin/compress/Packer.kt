package compress

import java.io.File
import java.io.ObjectOutputStream
import java.util.Comparator
import java.util.PriorityQueue
import java.util.Stack

internal class Packer {

    fun pack(inputFile: File, outputFile: File) {
        val frequencies = getCharacterFrequencies(inputFile)
        val tree = buildHuffmanTree(frequencies)
        val conversions = toConversions(tree)

        // Buffering needed, as we output data byte by byte
        ObjectOutputStream(outputFile.outputStream().buffered()).use { outputStream ->
            HeaderWriter(tree).writeToStream(outputStream)
            packBodyToStream(conversions, inputFile, outputStream)
        }
    }

    /**
     * @return Number of occurrences of each character in the packed file.
     */
    private fun getCharacterFrequencies(file: File): Map<Int, Long> = buildMap {
        file.bufferedReader().use { reader ->
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
        val stack = Stack<Triple<TreeNode, Int, List<Boolean>>>().apply { add(Triple(tree, 0, emptyList())) }

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

    private fun packBodyToStream(conversions: List<Conversion>, file: File, outputStream: ObjectOutputStream) {
        val characterConversion = conversions.associateBy { it.originalCharacter }

        var bitsInBuffer = 0
        var buffer = 0

        // Must be buffered because we read character by character.
        file.bufferedReader().use { inputStream ->
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
                        outputStream.writeByte(buffer)
                    }
                }
            }
        }

        if (bitsInBuffer > 0) {
            outputStream.writeByte(buffer)
        }
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

internal class HeaderWriter(private val tree: Packer.TreeNode) {

    internal fun writeToStream(outputStream: ObjectOutputStream) {
        // Body size
        outputStream.writeLong(tree.frequency)

        // Tree
        writeTreeToStream(outputStream)
    }

    private fun writeTreeToStream(outputStream: ObjectOutputStream) {
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
        val stack = Stack<Packer.TreeNode>().apply { add(tree) }

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
