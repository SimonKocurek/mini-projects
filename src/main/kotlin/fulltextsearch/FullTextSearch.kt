package fulltextsearch

/**
 *
 */
class FullTextSearch {

    /**
     * @param entry
     */
    fun insert(entry: Entry) {
        // Cleaning
            // Remove diacritics

            // Remove filler words

            // Remove non-alphanums

            // Stem (correct for spelling mistakes)

            // Normalize (Make uppercase)

        // Indexing
        // Store original in dictionary with references from each word
    }

    /**
     * @param searchText
     * @return
     */
    fun find(searchText: String): List<Entry> {
        // Clean value

        // Look up in index

        // Cleanup results
        return emptyList()
    }

    data class Entry(
        /**
         *
         */
        val indexedText: String,
        /**
         *
         */
        val document: Map<String, Any?>
    )

}
