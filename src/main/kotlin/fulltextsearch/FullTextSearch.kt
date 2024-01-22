package fulltextsearch

import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.log10

interface FullTextSearch {

    data class Entry(
        /** Unique ID of the entry. */
        val id: UUID = UUID.randomUUID(),
        /** Text that will be indexed and used for searching. */
        val indexedText: String,
        /** Additional data that should be stored under the index. */
        val document: Map<String, Any?>
    ) {
        override fun equals(other: Any?) = this === other ||
                javaClass == other?.javaClass &&
                id == (other as Entry).id

        override fun hashCode() = id.hashCode()
    }

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
}

/**
 * A simple thread-safe, in-memory implementation of a language agnostic full-text search engine.
 */
class InMemoryFullTextSearch(private val tokenizer: Tokenizer) : FullTextSearch {

    /**
     * Values does not need to be concurrent, because we need to do locking on it anyway.
     */
    private val tokenToEntryIds = ConcurrentHashMap<String, HashSet<UUID>>()

    /** Mapping of entry ID to the stored entry. */
    private val entryById = ConcurrentHashMap<UUID, SavedEntry>()

    override fun insert(entry: FullTextSearch.Entry) {
        val tokens = tokenizer.tokenizeText(entry.indexedText)

        // We need to lock the entry, so that calling insert and delete at the same time would not result
        // in some entry being partially saved and partially deleted.
        synchronized(entry) {
            if (entryById.containsKey(entry.id)) {
                throw IllegalArgumentException("Entry with ID ${entry.id} already exists.")
            }

            tokens.forEach { token ->
                val tokenEntryIds = tokenToEntryIds.compute(token) { _, value -> value ?: HashSet() }
                    ?: throw RuntimeException("When adding entry under certain token, the set of entries must not be null.")

                // We have to lock entries, because they might be processed by delete at the same time.
                synchronized(tokenEntryIds) {
                    tokenEntryIds.add(entry.id)
                }
            }

            entryById[entry.id] = SavedEntry(
                entry = entry,
                tokenFrequencies = tokens
                    .groupBy { it }
                    .mapValues { (key, value) -> value.size }
            )
        }
    }

    override fun find(searchText: String): List<FullTextSearch.Entry> {
        val tokens = tokenizer.tokenizeText(searchText)

        return tokens
            .asSequence()
            .map { token -> tokenToEntryIds[token] ?: emptySet() }
            // Consider only entries that contain **all** the searched tokens.
            .reduce { result, tokenEntries -> result.intersect(tokenEntries) }
            // It can happen that some entry was deleted while we were searching it.
            // In such case entryById would now return `null`.
            .mapNotNull { entryIds -> entryById[entryIds] }
            // So that we don't need to calculate relevance score on each sorting comparison,
            // we can precalculate it:
            .map { savedEntry ->
                Pair(
                    first = savedEntry.entry,
                    second = getRelevanceScore(tokens, savedEntry)
                )
            }
            .sortedByDescending { (_, relevanceScore) -> relevanceScore }
            .map { (savedEntry, _) -> savedEntry }
    }

    private fun getRelevanceScore(tokens: List<String>, savedEntry: SavedEntry): Double {
        return tokens.sumOf { token ->
            // How many times the token is present in the index in general.
            // We use this metric to lower the weight of words that are
            // very common and increase weight of rare word matches.
            val totalTokenFrequency = tokenToEntryIds[token]?.size ?: 1
            val inverseTokenFrequency = log10(entryById.size / totalTokenFrequency.toDouble())

            // How many times the token is found in the found entry.
            // We use this metric to prioritize results that frequently
            // mention searched text.
            val tokenFrequencyInEntry = savedEntry.tokenFrequencies[token] ?: 0

            // If we have multiple entries that have same number of token
            // occurrences, we want to prioritize the shorter text.
            val wordLengthMultiplier = 1 / log10(100.0 + savedEntry.entry.indexedText.length)

            tokenFrequencyInEntry * inverseTokenFrequency * wordLengthMultiplier
        }
    }

    override fun delete(id: UUID) {
        val entry = entryById[id]?.entry ?: throw IllegalArgumentException("Entry with ID $id was not saved")
        val tokens = tokenizer.tokenizeText(entry.indexedText)

        synchronized(entry) {
            tokens.forEach { token ->
                val tokenEntryIds = tokenToEntryIds[token] ?: return@forEach

                // We need to lock the entries, in case some other thread is adding
                // in new data at the same time.
                synchronized(tokenEntryIds) {
                    tokenEntryIds.remove(entry.id)
                    if (tokenEntryIds.isEmpty()) {
                        tokenToEntryIds.remove(token)
                    }
                }
            }

            entryById.remove(id)
        }
    }

    private data class SavedEntry(
        val entry: FullTextSearch.Entry,
        /** Number of occurrences of each token, used for calculating relevance. */
        val tokenFrequencies: Map<String, Int>
    )

}
