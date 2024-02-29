package ratelimiter.storage

/**
 *
 */
interface RateLimiterStorage {

    /**
     *
     */
    fun store(key: String, bucket: Int)

    /**
     *
     */
    fun get(key: String, bucket: Int)

    /**
     *
     */
    fun reset()
}

