package base64coding

import java.nio.CharBuffer

class Base64DecodingException(message: String) : IllegalArgumentException(message)

/**
 * Encode bytes into a text form.
 */
fun ByteArray.encodeToBase64String(): String {
    val byteIterator = iterator()

    return buildString {
        // Since base64 needs only 6 bits (octet) per character, we can simplify
        // the serialization by using batches of 3 bytes encoded as 4 characters.
        while (byteIterator.hasNext()) {
            appendBatchOf4Characters(byteIterator)
        }
    }
}

private fun StringBuilder.appendBatchOf4Characters(byteIterator: ByteIterator) {
    val byte1End = appendNextCharacter(
        byte = byteIterator.nextByte().toInt(), leftoverBits = 0, remainingBitCount = 2
    )
    if (!byteIterator.hasNext()) {
        append(octetToCharacter[byte1End])
        append("==") // Padding so that decoder can read batches of 4 characters
        return
    }

    val byte2End = appendNextCharacter(
        byte = byteIterator.nextByte().toInt(), leftoverBits = byte1End, remainingBitCount = 4
    )
    if (!byteIterator.hasNext()) {
        append(octetToCharacter[byte2End])
        append("=") // Padding so that decoder can read batches of 4 characters
        return
    }

    val byte3End = appendNextCharacter(
        byte = byteIterator.nextByte().toInt(), leftoverBits = byte2End, remainingBitCount = 6
    )
    append(octetToCharacter[byte3End])
}

/**
 * @return remaining bits.
 */
private fun StringBuilder.appendNextCharacter(byte: Int, leftoverBits: Int, remainingBitCount: Int): Int {
    val byteStart = byte ushr remainingBitCount
    val character = octetToCharacter[leftoverBits or byteStart]
    append(character)

    val endBitsMask = (1 shl remainingBitCount) - 1
    val byteEnd = (byte and endBitsMask) shl (6 - remainingBitCount)

    return byteEnd
}

/**
 * Decode Base64 encoded string (with optional padding) to bytes that were used to generate it.
 * @throws Base64DecodingException if decoding fails due to encountering an unexpected character.
 */
fun String.decodeBase64ToBytes(): ByteArray {
    val charBuffer = CharBuffer.wrap(this)

    return buildList {
        // Since base64 needs only 6 bits (octet) per character, we can simplify
        // the serialization by using batches of 4 characters decoded as 3 bytes.
        while (charBuffer.hasRemaining()) {
            val isFinalChunk = charBuffer.remaining() <= 4

            val octet1 = charBuffer.getNextOctet()
            val octet2 = charBuffer.getNextOctet()
            addByte(octet1, octet2, bitsFromSecondOctet = 2)

            if (isFinalChunk && charBuffer.reachedPadding()) {
                break
            }
            val octet3 = charBuffer.getNextOctet()
            addByte(octet2, octet3, bitsFromSecondOctet = 4)

            if (isFinalChunk && charBuffer.reachedPadding()) {
                break
            }
            val octet4 = charBuffer.getNextOctet()
            addByte(octet3, octet4, bitsFromSecondOctet = 6)
        }
    }.toByteArray()
}

private fun CharBuffer.getNextOctet(): Int {
    val character = if (hasRemaining()) {
        get()
    } else throw Base64DecodingException("Expected a valid base64 character at position ${position() - 1}. No character was found.")

    return characterToOctet[character]
        ?: throw Base64DecodingException("Got unexpected character $character at position ${position() - 1}. Valid characters are: '$octetToCharacter'.")
}

private fun MutableList<Byte>.addByte(octet1: Int, octet2: Int, bitsFromSecondOctet: Int) {
    val octet1Mask = (1 shl (8 - bitsFromSecondOctet)) - 1
    val byte1 = ((octet1 and octet1Mask) shl bitsFromSecondOctet) or (octet2 ushr (6 - bitsFromSecondOctet))
    add(byte1.toByte())
}

private fun CharBuffer.reachedPadding() = !hasRemaining() || this[position()] == '='

// RFC 4648 §4 variant of the alphabet
private const val octetToCharacter = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
private val characterToOctet = octetToCharacter.withIndex().associateBy({ it.value }, { it.index })
