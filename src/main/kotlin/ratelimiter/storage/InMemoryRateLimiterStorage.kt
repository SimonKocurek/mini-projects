package ratelimiter.storage

/**
 * In-Memory implementation is suitable only for single-instance
 * backend services. With more complicated infrastructure, it is
 * recommended to do rate-limiting either at reverse-proxy level,
 * or use shared cache server (like Redis) for rate limiting.
 */
class InMemoryRateLimiterStorage: RateLimiterStorage {
    override fun store(key: String, bucket: Int) {
        TODO("Not yet implemented")
    }

    override fun get(key: String, bucket: Int) {
        TODO("Not yet implemented")
    }

    override fun reset() {
        TODO("Not yet implemented")
    }

}
