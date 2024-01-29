package base64coding

class Base64DecodingException(message: String) : IllegalArgumentException(message)

/**
 * Encode bytes into a text form.
 */
fun encodeBase64(encoded: ByteArray) = buildString {
    val byteIterator = encoded.iterator()
    // Since base64 needs only 6 bits per character, we can simplify serialization by
    // using batches of 3 bytes encoded as 4 characters.
    while (byteIterator.hasNext()) {
        appendBatchOf4Characters(byteIterator)
    }
}

private fun StringBuilder.appendBatchOf4Characters(byteIterator: ByteIterator) {
    val byte1End = appendNextCharacter(
        byte = byteIterator.nextByte().toInt(), leftoverBits = 0, remainingBitCount = 2
    )
    if (!byteIterator.hasNext()) {
        append(base64Alphabet[byte1End])
        append("==") // Padding so that decoder can read batches of 4 characters
        return
    }

    val byte2End = appendNextCharacter(
        byte = byteIterator.nextByte().toInt(), leftoverBits = byte1End, remainingBitCount = 4
    )
    if (!byteIterator.hasNext()) {
        append(base64Alphabet[byte2End])
        append("=") // Padding so that decoder can read batches of 4 characters
        return
    }

    val byte3End = appendNextCharacter(
        byte = byteIterator.nextByte().toInt(), leftoverBits = byte2End, remainingBitCount = 6
    )
    append(base64Alphabet[byte3End])
}

/**
 * @return remaining bits.
 */
private fun StringBuilder.appendNextCharacter(byte: Int, leftoverBits: Int, remainingBitCount: Int): Int {
    val byteStart = byte ushr remainingBitCount
    append(base64Alphabet[leftoverBits or byteStart])

    val endBitsMask = (1 shl remainingBitCount) - 1
    val byteEnd = (byte and endBitsMask) shl (6 - remainingBitCount)

    return byteEnd
}

/**
 * Decode Base64 encoded string (with optional padding) to bytes that were used to generate it.
 * @throws Base64DecodingException if decoding fails due to encountering unsupported character.
 */
fun decodeBase64(decoded: String): ByteArray = buildList {
    var index = 0

    // Since base64 needs only 6 bits per character, we can simplify serialization by
    // using batches of 4 characters decoded as 3 bytes.
    while (index < decoded.length) {
        val isFinalChunk = index + 4 >= decoded.length

        val char1 = decoded[index++]
        val bits1 = base64CharacterToBits[char1]
            ?: throw Base64DecodingException("Got unexpected character $char1 (index ${index - 1}) at the start of byte ${size + 1}. Valid characters are: '$base64Alphabet'.")

        val char2 = if (index < decoded.length) {
            decoded[index++]
        } else throw Base64DecodingException("Expected 2 characters at the start of each 4 character chunk. No second character found when decoding byte ${size + 1}.")
        val bits2 = base64CharacterToBits[char2]
            ?: throw Base64DecodingException("Got unexpected character $char2 (index ${index - 1}) at the end of byte ${size + 1}. Valid characters are: '$base64Alphabet'.")

        val byte1 = (bits1 shl 2) or (bits2 ushr 4)
        add(byte1.toByte())

        if (isFinalChunk && (index >= decoded.length || decoded[index] == '=')) {
            // We have reached the end of the (non-padded) input.
            break
        }

        val char3 = decoded[index++]
        val bits3 = base64CharacterToBits[char3]
            ?: throw Base64DecodingException("Got unexpected character $char3 (index ${index - 1}) at the start of byte ${size + 1}. Valid characters are: '$base64Alphabet'.")

        val byte2 = ((bits2 and 0b1111) shl 4) or (bits3 ushr 2)
        add(byte2.toByte())

        if (isFinalChunk && (index >= decoded.length || decoded[index] == '=')) {
            // We have reached the end of the (non-padded) input.
            break
        }

        val char4 = decoded[index++]
        val bits4 = base64CharacterToBits[char4]
            ?: throw Base64DecodingException("Got unexpected character $char4 (index ${index - 1}) at the end of byte ${size + 1}. Valid characters are: '$base64Alphabet'.")

        val byte3 = ((bits3 and 0b11) shl 6) or bits4
        add(byte3.toByte())
    }
}.toByteArray()

// RFC 4648 §4 variant of the alphabet
private const val base64Alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
private val base64CharacterToBits = base64Alphabet.withIndex().associateBy({ it.value }, { it.index })
