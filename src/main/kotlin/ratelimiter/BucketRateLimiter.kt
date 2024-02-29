package ratelimiter

/**
 * Uses buckets to............
 *
 * The implementation is storage mechanism agnostic and thread safe.
 */
class BucketRateLimiter: RateLimiter {

    time
    buckets

    override fun countHit(key: String, increment: Int) {
        TODO("Not yet implemented")
    }

}
