package ratelimiter

/**
 * Limit the number of executions within a time period.
 *
 * Example use-case is limiting API usage for each user,
 * so that they cannot spam API non-cachable calls.
 */
interface RateLimiter {

    /**
     *
     *
     * @param key
     * @param increment
     * @return
     */
    fun countHit(key: String, increment: Int = 1)

}

