# FullTextSearch

The point of this engine is to provide a simple in-memory library for fuzzy search in text content 
and to improve my understanding of Full-Text indexing.

## Interface
```kotlin
data class Entry(
    /** Unique ID of the entry. */
    val id: UUID = UUID.randomUUID(),
    /** Text that will be indexed and used for searching. */
    val indexedText: String,
    /** Additional data that should be stored under the index. */
    val document: Map<String, Any?>
)

/**
 * @param entry Inserted data that should be searchable by indexed text.
 * @throws IllegalArgumentException Entry with such ID already exists.
 */
fun insert(entry: Entry)

/**
 * @param searchText Text to search by.
 * @return Found entries that contain searched text. Entries are sorted by relevance.
 */
fun find(searchText: String): List<Entry>

/**
 * Removes entry with particular ID from the index.
 * @throws IllegalArgumentException Entry with such ID does not exist.
 */
fun delete(id: UUID)
```

## Example usage

```kotlin
import fulltextsearch.FullTextSearch
import fulltextsearch.InMemoryFullTextSearch
import fulltextsearch.WordTokenizer

val searchEngine = InMemoryFullTextSearch(
    tokenizer = WordTokenizer(
        stopWords = setOf("a", "and", "be", "have", "i", "in", "of", "that", "the", "to")
    )
)

// Add data to the search:
dataset.forEach { entry ->
    // Example dataset entry: 
    // { 
    //      "createdAt": "2023-01-02",
    //      "author": "Jozko",
    //      "threadId": "algorithms",
    //      "textContent": "I would like to point out that ..." 
    // }
    searchEngine.insert(FullTextSearch.Entry(
        indexedText = "${entry["threadId"]} ${entry["textContent"]}",
        document = entry
    ))
}

// Find entries that contain the input text:
val results = searchEngine.find("algorithms point")
// Example results:
// [
//      { "id": "...", "indexedText": "algorithms I would like to ...", "document": { "createdAt": "2023-01-02", "author": "Jozko", ... },
//      { "id": "...", "indexedText": "AI The point of using AI algorithms is ...", "document": { "createdAt": ... },
//      { "id": "...", "indexedText": "...", "document": { "createdAt": ... }
// ]
val firstResult = results[0]

// Remove entry from dataset:
searchEngine.delete(firstResult.id)
```