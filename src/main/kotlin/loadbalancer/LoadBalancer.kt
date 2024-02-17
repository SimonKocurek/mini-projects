package loadbalancer

import java.net.URI
import java.time.OffsetDateTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.atomic.AtomicInteger

internal interface LoadBalancer {
    /**
     * @return Downstream server that should be used to make the next request.
     *      null if there is no downstream server that is alive.
     */
    fun getDownstreamServer(): URI?

    /**
     * When connection fails, we won't connect to the particular server for some time.
     */
    fun reportConnectionFailed(downstreamServer: URI)
}

private val logger = LoggerFactory.create(RoundRobinLoadBalancer::class.simpleName!!)

internal class RoundRobinLoadBalancer(
    private val downstreamServers: List<URI>,
    private val recoveryTimeoutSeconds: Long,
    private val timedOutServers: ConcurrentMap<URI, OffsetDateTime> = ConcurrentHashMap()
) : LoadBalancer {

    private val currentServer = AtomicInteger(0)

    override fun getDownstreamServer(): URI? {
        // Some downstream servers might be timed out due to being unhealthy,
        // so we need to iterate until we find the next healthy server, instead
        // of just returning the next one (which might be unhealthy).
        for (i in 1..downstreamServers.size) {
            val nextServerIndex =
                currentServer.accumulateAndGet(1) { previous, increment -> (previous + increment) % downstreamServers.size }
            val serverUri = downstreamServers[nextServerIndex]

            val serverTimeout = timedOutServers[serverUri]
            if (serverTimeout != null) {
                // The server is still unhealthy, and we skip it for now
                if (serverTimeout.isAfter(OffsetDateTime.now())) {
                    continue
                }


                logger.log(
                    "marking server as heatlhy", mapOf(
                        "server" to serverUri,
                        "reason" to "unhealthy timeout reached"
                    )
                )
                timedOutServers.remove(serverUri)
            }

            return serverUri
        }

        // This will happen if all downstream servers are unhealthy at the moment
        return null
    }

    override fun reportConnectionFailed(downstreamServer: URI) {
        logger.log("marking server as unhealthy", mapOf("server" to downstreamServer))
        timedOutServers[downstreamServer] = OffsetDateTime.now().plusSeconds(recoveryTimeoutSeconds)
    }

}
