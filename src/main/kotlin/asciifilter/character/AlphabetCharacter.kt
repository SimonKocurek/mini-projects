package asciifilter.character

data class AlphabetCharacter(
    /** UTF-8 character (ideally with monospace font) */
    val character: Char,
    /** Brightness between 0 and 255. */
    val brightness: Double
)
