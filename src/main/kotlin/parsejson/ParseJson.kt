package parsejson

class JsonParsingException : RuntimeException()

/**
 * Parser compatible with RFC-8259 JSON standard.
 *
 * @throws JsonParsingException if invalid JSON is provided.
 *
 * @return Object of one of the types:
 *  - Map<String, Any> (object)
 *  - List<Any> (array)
 *  - String (string)
 *  - BigDecimal (number)
 *  - Boolean (boolean)
 *  - <null> (null)
 */
fun parseJson(value: String): Any? {
    return null
}

