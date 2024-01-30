# Base64 Coding

[RFC 4648](https://datatracker.ietf.org/doc/html/rfc4648#section-4) compatible Base64 decoder and encoder.

Base64 is a group of binary-to-text encoding schemes that transforms binary data into a sequence of printable
characters, limited to a set of 64 unique characters.

Base64 encoding causes an overhead of 33–37%, so it should be used only when transferring text is the only option.

> Throws Base64DecodingException if decoding fails due to encountering unsupported character.

Example usage:

```kotlin
import base64coding.Base64DecodingException
import base64coding.decodeBase64ToBytes
import base64coding.encodeToBase64String

val encoded = File("img.jpg").readBytes().encodeToBase64String() // "/9j/4VHrRXhpZgA...f/9k="

// ... transfer image as base64 text ...

val decodedData = try {
    encoded.decodeBase64ToBytes()
} catch (e: Base64DecodingException) {
    log.exception("Decoding base64 image payload failed", e)
    throw e
}
File("received.jpg").writeBytes(decodedData)
```
