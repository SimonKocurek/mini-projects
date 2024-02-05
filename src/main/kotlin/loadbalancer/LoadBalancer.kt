package loadbalancer

import java.net.URL
import java.time.OffsetDateTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.atomic.AtomicInteger

internal interface LoadBalancer {
    /**
     * @return Downstream server that should be used to make the next request.
     *      null if there is no downstream server that is alive.
     */
    fun getDownstreamServer(): URL?

    /**
     * When connection fails, we won't connect to the particular server for some time.
     */
    fun reportConnectionFailed(downstreamServer: URL)
}

internal class RoundRobinLoadBalancer(
    private val downstreamServers: List<URL>,
    private val recoveryTimeoutSeconds: Int,
    private val timedOutServers: ConcurrentMap<Int, OffsetDateTime> = ConcurrentHashMap()
) : LoadBalancer {

    private val currentServer = AtomicInteger(0)

    override fun getDownstreamServer(): URL? {
//        val downstreamServer = currentServer.getAndAccumulate(1) { previous, increment ->
//            val newIndex = (previous + increment) % downstreamServers.size
//
//            // We limit the number of retries, so that we are never stuck in an endless loop
//            for (i in 0..<downstreamServers.size) {
//                val timedOutServer = timedOutServers[newIndex]
//                if (timedOutServer == null) {
//                    return@getAndAccumulate newIndex
//                }
//            }
//        }

        return downstreamServers.first()
    }

    override fun reportConnectionFailed(downstreamServer: URL) {
//        timedOutServers
    }

}
