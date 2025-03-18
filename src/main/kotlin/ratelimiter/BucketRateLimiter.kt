package ratelimiter

/**
 * Uses buckets to............
 *
 * The implementation is storage mechanism agnostic and thread safe.
 */
class BucketRateLimiter: RateLimiter {

    override fun countHit(key: String, increment: Int) {
    }

}
