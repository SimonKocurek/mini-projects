package parsejson

import java.math.BigDecimal
import java.nio.CharBuffer
import java.util.*

class JsonParsingException(message: String) : RuntimeException(message)

/**
 * Parser compatible with RFC-8259 JSON standard.
 *
 * @throws JsonParsingException if invalid JSON is provided.
 * @throws StackOverflowError if JSON nesting is too deep
 *  (max depth depends on call-stack size. Should not happen unless JSON has thousands of layers of nesting).
 *
 * @return Object of one of the types:
 *  - Map<String, Any?> (object)
 *  - List<Any?> (array)
 *  - String (string)
 *  - BigDecimal (number)
 *  - Boolean (boolean)
 *  - <null> (null)
 */
fun parseJson(value: String): Any? {
    // For possible future improvements, we parse the JSON in a single pass using buffer.
    // This implementation could be then easily improved to read an input stream instead of string.
    val buffer = CharBuffer.wrap(value)
    val result = ParsingInstance(buffer).parseValue()

    if (buffer.hasRemaining()) {
        throw JsonParsingException(
            "Expected end of input, but got extra character '${buffer[buffer.position()]}' at position ${buffer.position()}.",
        )
    }

    return result
}

private class ParsingInstance(private val buffer: CharBuffer) {

    /**
     * Keystack is useful for debugging and is part of the error message.
     * It shows nesting inside objects and arrays.
     */
    private val keyStack = Stack<String>()

    /**
     * Moves buffer forward while parsing any valid JSON value.
     */
    fun parseValue(): Any? {
        parseWhitespace()

        // The recursive nesting here introduces the option of StackOverflowError.
        // We could fix that by separating streaming token reader with parser that
        // interprets those tokens (e.g., OBJECT_START, STRING, ARRAY_START, ARRAY_END, OBJECT_END)
        val starter = buffer.peek(onEndOfInput = "Expected JSON value")
        val result: Any? = when {
            starter == '{' -> parseObject()
            starter == '[' -> parseArray()
            starter == '"' -> parseString()
            starter == 't' -> parseTrue()
            starter == 'f' -> parseFalse()
            starter == 'n' -> parseNull()
            starter.isDigit() || starter == '-' -> parseNumber()
            else -> throw error("Expected start of a JSON value, but got '${buffer.peek()}'")
        }

        parseWhitespace()
        return result
    }

    /**
     * Moves buffer forward while parsing null value.
     */
    private fun parseNull(): Void? {
        "null".forEach { expectCharacter(it) }
        return null
    }

    /**
     * Moves buffer forward while parsing false value.
     */
    private fun parseFalse(): Boolean {
        "false".forEach { expectCharacter(it) }
        return false
    }

    /**
     * Moves buffer forward while parsing true value.
     */
    private fun parseTrue(): Boolean {
        "true".forEach { expectCharacter(it) }
        return true
    }

    /**
     * Moves buffer forward while parsing [<values>] array.
     */
    private fun parseArray(): List<Any?> {
        expectCharacter('[')
        parseWhitespace()

        val result = buildList {
            while (buffer.peek(onEndOfInput = "Expected end of array ']'") != ']') {
                if (size > 0) {
                    expectCharacter(',')
                }

                keyStack.push(size.toString())
                add(parseValue())
                keyStack.pop()
            }
        }

        expectCharacter(']')

        return result
    }

    /**
     * Moves buffer forward while parsing { <values> } object.
     */
    private fun parseObject(): Map<String, Any?> {
        expectCharacter('{')
        parseWhitespace()

        val result = buildMap {
            while (buffer.peek(onEndOfInput = "Expected end of object '}'") != '}') {
                if (isNotEmpty()) {
                    expectCharacter(',')
                    parseWhitespace()
                }

                val key = parseString()
                parseWhitespace()
                expectCharacter(':')

                keyStack.push(key)
                put(key, parseValue())
                keyStack.pop()
            }
        }

        expectCharacter('}')

        return result
    }

    /**
     * Moves buffer forward while parsing number.
     */
    private fun parseNumber(): BigDecimal {
        val result = buildString {
            parseNumberSign(this)
            parseNumberBase(this)
            parseNumberFraction(this)
            parseNumberExponent(this)
        }

        return BigDecimal(result)
    }

    private fun parseNumberSign(result: StringBuilder) {
        if (buffer.peek(onEndOfInput = "Expected number or minus sign") == '-') {
            result.append(buffer.get())
        }
    }

    private fun parseNumberBase(result: StringBuilder) {
        when (buffer.peek(onEndOfInput = "Expected digit")) {
            '0' -> result.append(buffer.get())
            '1', '2', '3', '4', '5', '6', '7', '8', '9' -> {
                while (buffer.hasRemaining() && buffer.peek().isDigit()) {
                    result.append(buffer.get())
                }
            }

            else -> throw error("Expected digit, but got '${buffer.peek()}'")
        }
    }

    private fun parseNumberFraction(result: StringBuilder) {
        if (buffer.hasRemaining() && buffer.peek() == '.') {
            result.append(buffer.get())

            if (!buffer.peek(onEndOfInput = "Expected fraction to contain digits").isDigit()) {
                throw error("Expected fraction to contain digits, but got '${buffer.peek()}'")
            }

            while (buffer.hasRemaining() && buffer.peek().isDigit()) {
                result.append(buffer.get())
            }
        }
    }

    private fun parseNumberExponent(result: StringBuilder) {
        if (buffer.hasRemaining() && (buffer.peek() == 'e' || buffer.peek() == 'E')) {
            result.append(buffer.get())

            when (buffer.peek(onEndOfInput = "Expected exponent to contain digits")) {
                '-', '+' -> result.append(buffer.get())
            }

            if (!buffer.peek(onEndOfInput = "Expected exponent to contain digits").isDigit()) {
                throw error("Expected exponent to contain digits, but got '${buffer.peek()}'")
            }

            while (buffer.hasRemaining() && buffer.peek().isDigit()) {
                result.append(buffer.get())
            }
        }
    }

    /**
     * Moves buffer forward while parsing "<characters>" string.
     */
    private fun parseString(): String {
        expectCharacter('"')

        val result = StringBuilder()
        while (true) {
            val currentCharacter = buffer.peek(onEndOfInput = "Expected string content")
            when {
                // String ended
                currentCharacter == '"' -> break

                // Escaped character
                currentCharacter == '\\' -> {
                    buffer.get()
                    result.append(readEscapedCharacter())
                }

                // Invalid
                currentCharacter.isISOControl() -> throw error("ISO control characters are not allowed, but got character with code ${currentCharacter.code}")

                // Regular character
                else -> result.append(buffer.get())
            }
        }

        expectCharacter('"')

        return result.toString()
    }

    private fun readEscapedCharacter(): Char {
        return when (buffer.peek(onEndOfInput = "Expected escaped character")) {
            in escapedCharacterMapping -> {
                escapedCharacterMapping[buffer.get()]
                    ?: throw RuntimeException("After checking if character is in escaped character mapping, it should always be present.")
            }

            'u' -> {
                buffer.get()

                val characterCode = "${getHexCharacter()}${getHexCharacter()}${getHexCharacter()}${getHexCharacter()}"
                Char(characterCode.toInt(16))
            }

            else -> throw error("Expected one of ${escapedCharacterMapping.keys.joinToString()}, u to be escaped, but got '${buffer.peek()}'")
        }
    }

    private fun getHexCharacter(): Char {
        if (!buffer.hasRemaining()) {
            throw error("Expected 4 characters after unicode identifier: \\u but reached end of input")
        }

        val character = buffer.peek()
        if (!character.isHexDigit()) {
            throw error("Expected a hex digit (a-f, A-F, 0-9) after unicode identifier, but got '$character'")
        }
        buffer.get()

        return character
    }

    private val escapedCharacterMapping = mapOf(
        '"' to '"',
        '\\' to '\\',
        '/' to '/',
        'b' to '\b', // backspace
        'f' to Char(0xC),  // formfeed (new page)
        'n' to '\n', // linefeed (new line)
        'r' to '\r', // carriage return (start of line)
        't' to '\t' // horizontal tab
    )

    /**
     * Checks if character is at the current buffer position and advances buffer.
     * @throws JsonParsingException with human friendly explanation what failed
     */
    private fun expectCharacter(character: Char) {
        if (buffer.peek(onEndOfInput = "Expected '$character'") != character) {
            throw error("Expected '\"' but got '${buffer.peek()}'")
        }

        buffer.get() // Advance buffer after we know the character is at current position
    }

    /**
     * Moves buffer forward while there is whitespace.
     */
    private fun parseWhitespace() {
        while (buffer.hasRemaining()) {
            when (buffer.peek()) {
                ' ', '\n', '\r', '\t' -> buffer.get() // Move to next character
                else -> break
            }
        }
    }

    /**
     * Returns current character without advancing position
     * @throws JsonParsingException If buffer is exhausted
     */
    private fun CharBuffer.peek(onEndOfInput: String = ""): Char {
        if (!hasRemaining()) {
            throw error("$onEndOfInput but reached end of input".trimStart())
        }

        return get(position())
    }

    private fun Char.isHexDigit() = isDigit() || when (this) {
        'a', 'A', 'b', 'B', 'c', 'C', 'd', 'D', 'e', 'E', 'f', 'F' -> true
        else -> false
    }

    private fun error(message: String): JsonParsingException {
        return JsonParsingException("$message at index ${buffer.position()}. Path: ${keyStack.joinToString("->")}.")
    }

}
