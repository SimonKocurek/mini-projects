package compress

import java.io.File
import java.io.ObjectOutputStream
import java.util.BitSet
import java.util.Comparator
import java.util.PriorityQueue

internal class Compressor {

    /**
     * Create a packed file with `.minzip` extension added.
     * Packed file has a header with format where each field is 1 byte:
     * [header length, *[original byte, compressed byte, encoding bits], body expected bytes]
     */
    fun compress(inputFile: File, outputFile: File) {
        val frequencies = getByteFrequencies(inputFile)
        val tree = buildHuffmanTree(frequencies)
        val conversions = toConversions(tree)

        // Buffering needed, as we output data byte by byte
        ObjectOutputStream(outputFile.outputStream().buffered()).use { outputStream ->
            CompressionHeader(conversions, tree.frequency).write(outputStream)
            compressBodyToStream(conversions, inputFile, outputStream)
        }
    }

    /**
     * @return Number of occurrences of each byte in the compressed file.
     */
    private fun getByteFrequencies(file: File): Map<Int, Long> = buildMap {
        file.inputStream().buffered().use { stream ->
            while (true) {
                val byte = stream.read()
                if (byte == -1) {
                    break
                }

                merge(byte, 1) { current, added -> current + added }
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
            TreeNode(
                frequency = it.value,
                value = it.key
            )
        }
        result.addAll(nodes)

        return result
    }

    private data class TreeNode(
        val left: TreeNode? = null,
        val right: TreeNode? = null,
        /** Number of occurrences of bytes from the subtree in the compressed file. */
        val frequency: Long,
        /** Byte value, only present in leaf nodes. */
        val value: Int? = null,
    )

    /**
     * @return conversion rules used for mapping each byte to possibly fewer bits.
     */
    private fun toConversions(tree: TreeNode): List<CompressionConversion> = buildList {
        // We can use recursion as the tree won't ever be deeper than 64 levels (Int size),
        // so we don't risk hitting StackOverflowError.
        addSubtreeConversions(
            node = tree,
            currentEncoding = 0,
            depth = 0,
            results = this
        )
    }

    private fun addSubtreeConversions(
        node: TreeNode,
        currentEncoding: Int,
        depth: Int,
        results: MutableList<CompressionConversion>
    ) {
        node.value?.let {
            results.add(
                CompressionConversion(
                    originalByte = it,
                    compressedByte = currentEncoding,
                    compressedBits = depth
                )
            )
            return // Technically not needed, since we have value only in leaf nodes
        }

        node.left?.let {
            addSubtreeConversions(
                node = it,
                currentEncoding = currentEncoding, // Adding 0 at the 'depth' position wouldn't change anything
                depth = depth + 1,
                results = results
            )
        }
        node.right?.let {
            addSubtreeConversions(
                node = it,
                currentEncoding = currentEncoding or (1 shl depth),
                depth = depth + 1,
                results = results,
            )
        }
    }

    private fun compressBodyToStream(conversions: List<CompressionConversion>, file: File, outputStream: ObjectOutputStream) {
        val byteConversion = conversions.associateBy { it.originalByte }

        var writtenBits = 0
        var writtenValue = 0

        // Must be buffered because we read Byte by Byte.
        file.inputStream().buffered().use { inputStream ->
            while (true) {
                val readByte = inputStream.read()
                if (readByte == -1) {
                    break
                }

                val convertedAs = byteConversion[readByte] ?: throw RuntimeException("File contains byte $readByte that was not found when calculating byte frequency table. Is the file being modified?")

                writtenBits += convertedAs.compressedBits
                writtenValue = (writtenValue shl convertedAs.compressedBits) or convertedAs.compressedByte

                if (writtenBits >= 8) {
                    writtenBits -= 8
                    outputStream.writeByte(writtenValue ushr writtenBits)
                }
            }
        }

        if (writtenBits > 0) {
            outputStream.writeByte(writtenValue)
        }
    }
}
