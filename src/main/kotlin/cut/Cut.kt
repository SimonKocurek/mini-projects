package cut

import java.io.BufferedReader

internal class Cut {

    fun printFields(reader: BufferedReader, fields: List<Int>, delimiter: String) {
        reader.lineSequence().forEach outputLine@{ line ->
            val lineFields = line.split(delimiter)

            fields.forEach { field ->
                if (lineFields.size <= field) {
                    // If one of the fields is not found, we print whole line as per documentation.
                    println(line)
                    return@outputLine
                }
            }

            fields
                .joinToString(delimiter) { field -> lineFields[field] }
                .also { println(it) }
        }
    }

}
