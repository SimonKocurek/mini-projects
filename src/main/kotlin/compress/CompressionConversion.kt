package compress

import java.io.ObjectInputStream
import java.io.ObjectOutputStream

data class CompressionConversion(
    /** Byte before compression */
    val originalByte: Int,
    /** Byte after compression */
    val compressedByte: Int,
    /** Number of bits from currentEncoding to use */
    val compressedBits: Int,
) {

    fun write(objectStream: ObjectOutputStream) {
        objectStream.writeByte(originalByte)
        objectStream.writeByte(compressedByte)
        objectStream.writeByte(compressedBits)
    }

    companion object {

        /**
         * @throws EOFException If end of stream was reached while trying to read the object.
         */
        fun read(objectStream: ObjectInputStream) = CompressionConversion(
            originalByte = objectStream.readUnsignedByte(),
            bits = objectStream.readInt(),
            bitCount = objectStream.readUnsignedByte(),
        )
    }
}
