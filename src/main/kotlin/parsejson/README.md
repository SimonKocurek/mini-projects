# ParseJSON

Parser compatible with RFC-8259 JSON standard.

> Throws JsonParsingException if invalid JSON is provided.

> Return Object of one of the types:
> - `Map<String, Any?>` - object
> - `List<Any?>` - array
> - `String` - string
> - `BigDecimal` - number
> - `Boolean` - boolean
> - `null` - null

Example usage:
```kotlin
import parsejson.JsonParsingException
import parsejson.parseJson

val result = parseJson("{ \"foo\": [\"bar\", -123.567, null]}") as Map<*, *>
val foo = result["foo"] as List<Any?> // [bar, -123.567, null]

try {
    parseJson("{ \"foo\": {\"bar\": [true, [], [null, {error} ] false] } }")
} catch (e: JsonParsingException) {
    val errorMessage = e.message // Expected '"' but got 'e' at index 36. Path: foo->bar->2->1.
}
```
