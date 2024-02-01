import base64coding.Base64DecodingException
import base64coding.decodeBase64ToBytes
import base64coding.encodeToBase64String
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class Base64 {

    @Test
    fun canEncodeEmptyValue() {
        // Given

        // When
        val base64 = byteArrayOf().encodeToBase64String()

        // Then
        assertEquals("", base64)
    }

    @Test
    fun canEncodeValueWithoutPadding() {
        // Given

        // When
        val base64 = "Many hands make light work.".encodeToByteArray().encodeToBase64String()
        val result2 = "foo".encodeToByteArray().encodeToBase64String()
        val result3 = "foobar".encodeToByteArray().encodeToBase64String()

        // Then
        assertEquals("TWFueSBoYW5kcyBtYWtlIGxpZ2h0IHdvcmsu", base64)
        assertEquals("Zm9v", result2)
        assertEquals("Zm9vYmFy", result3)
    }

    @Test
    fun canEncodeValueWithOnePaddedCharacter() {
        // Given

        // When
        val base64 = "light work.".encodeToByteArray().encodeToBase64String()
        val result2 = "fooba".encodeToByteArray().encodeToBase64String()
        val result3 = "fo".encodeToByteArray().encodeToBase64String()

        // Then
        assertEquals("bGlnaHQgd29yay4=", base64)
        assertEquals("Zm9vYmE=", result2)
        assertEquals("Zm8=", result3)
    }

    @Test
    fun canEncodeValueWithTwoPaddedCharacters() {
        // Given

        // When
        val base64 = "light work".encodeToByteArray().encodeToBase64String()
        val result2 = "f".encodeToByteArray().encodeToBase64String()
        val result3 = "foob".encodeToByteArray().encodeToBase64String()

        // Then
        assertEquals("bGlnaHQgd29yaw==", base64)
        assertEquals("Zg==", result2)
        assertEquals("Zm9vYg==", result3)
    }

    @Test
    fun canDecodeEmptyValue() {
        // Given

        // When
        val bytes = "".decodeBase64ToBytes()

        // Then
        assertEquals("", bytes.decodeToString())
    }

    @Test
    fun canDecodeValueWithNoPadding() {
        // Given

        // When
        val bytes = "bGlnaHQgd29y".decodeBase64ToBytes()
        val result2 = "Zm9v".decodeBase64ToBytes()
        val result3 = "Zm9vYmFy".decodeBase64ToBytes()

        // Then
        assertEquals("light wor", bytes.decodeToString())
        assertEquals("foo", result2.decodeToString())
        assertEquals("foobar", result3.decodeToString())
    }

    @Test
    fun canDecodeValueWithOnePaddedCharacter() {
        // Given

        // When
        val bytes = "bGlnaHQgd28=".decodeBase64ToBytes()
        val result2 = "Zm8=".decodeBase64ToBytes()
        val result3 = "Zm9vYmE=".decodeBase64ToBytes()

        // Then
        assertEquals("light wo", bytes.decodeToString())
        assertEquals("fo", result2.decodeToString())
        assertEquals("fooba", result3.decodeToString())
    }

    @Test
    fun canDecodeValueWithTwoPaddedCharacters() {
        // Given

        // When
        val bytes = "bGlnaHQgdw==".decodeBase64ToBytes()
        val result2 = "Zg==".decodeBase64ToBytes()
        val result3 = "Zm9vYg==".decodeBase64ToBytes()

        // Then
        assertEquals("light w", bytes.decodeToString())
        assertEquals("f", result2.decodeToString())
        assertEquals("foob", result3.decodeToString())
    }

    @Test
    fun canDecodeValueWithMissingOptionalPaddingCharacters() {
        // Given

        // When
        val bytes = "bGlnaHQgdw".decodeBase64ToBytes()
        val result2 = "Zm8=".decodeBase64ToBytes()

        // Then
        assertEquals("light w", bytes.decodeToString())
        assertEquals("fo", result2.decodeToString())
    }

    @Test
    fun failsDecodingWithPaddingInside() {
        // Given

        // When, Then
        assertThrows<Base64DecodingException> {
            "bGlna=HQgdw".decodeBase64ToBytes()
        }
    }

    @Test
    fun failsDecodingUnexpectedCharacter() {
        // Given

        // When, Then
        assertThrows<Base64DecodingException> {
            "bGlna,HQgdw".decodeBase64ToBytes()
        }
    }

    @Test
    fun failsDecodingWithTooMuchPadding() {
        // Given

        // When, Then
        assertThrows<Base64DecodingException> {
            "T===".decodeBase64ToBytes()
        }
    }
}