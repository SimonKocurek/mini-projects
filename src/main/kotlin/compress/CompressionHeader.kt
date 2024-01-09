package compress

import java.io.ObjectInputStream
import java.io.ObjectOutputStream

data class CompressionHeader(
    /** Set of conversions that are used for packing/unpacking bytes */
    val conversions: List<CompressionConversion>,
    /** Number of bytes that decompressed body should contain */
    val bodyBytes: Long,
) {

    fun write(objectStream: ObjectOutputStream) {
        // Since there is at most 1 conversion for each possible byte,
        // the number of all conversions is limited to Byte.MAX_VALUE.
        objectStream.writeByte(conversions.size)

        conversions.forEach { it.write(objectStream) }

        // At the end of file, in the final byte we would not be able to tell which bits to convert
        // and which ones to ignore without this length
        objectStream.writeLong(bodyBytes)
    }

    companion object {

        /**
         * @throws EOFException If end of stream was reached while trying to read the object.
         */
        fun read(objectStream: ObjectInputStream): CompressionHeader {
            val conversionCount = objectStream.readByte()

            val conversions = buildList {
                for (i in 0..<conversionCount) {
                    add(CompressionConversion.read(objectStream))
                }
            }

            val bodyBytes = objectStream.readLong()

            return CompressionHeader(
                conversions = conversions,
                bodyBytes = bodyBytes,
            )
        }
    }
}
