package loadbalancer

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.long
import java.net.URI
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

fun main(args: Array<String>) {
    LoadBalancerCli().main(args)
}

class LoadBalancerCli : CliktCommand(
    name = "loadbalancer",
    help = """
        Distribute traffic using Round-robin algorithm equally between multiple HTTP downstream servers.
        
        Perform a simple passive healthcheck and route traffic only to healthy downstream servers.
    """.trimIndent(),
    printHelpOnEmptyArgs = true
) {

    private val port by option(
        "-p",
        "--port",
        help = "Port the server will be listening on. Default: 8000."
    ).int().default(8000).validate { it in 1..65_534 }

    private val gracefulShutdownSeconds by option(
        "--gracefulShutdownSeconds",
        help = "After termination signal is received, the server will stop receiving new requests. " +
                "Before forcefully terminating existing connections, it will give the existing connections " +
                "this specified time to finish. Default: 3."
    ).int().default(3).validate { it >= 0 }

    private val minThreadPoolSize by option(
        "--minThreadPoolSize",
        help = "Minimum number of threads handling requests. Default: 10."
    ).int().default(10).validate { it >= 0 }

    private val maxThreadPoolSize by option(
        "--maxThreadPoolSize",
        help = "Maximum number of threads handling requests. Default: 10_000."
    ).int().default(10_000).validate { it >= minThreadPoolSize }

    private val threadPoolKeepAliveSeconds by option(
        "--threadPoolKeepAliveSeconds",
        help = "Number of seconds after which idle threads will be released from thread pool. Default: 60."
    ).long().default(60).validate { it >= 0 }

    private val loadBalancingRecoveryTimeout by option(
        "--loadBalancingRecoveryTimeout",
        help = "Number of seconds when no traffic will be sent to an unhealthy downstream server. " +
                "Downstream server is marked as unhealthy if establishing a connection to it fails. " +
                "Default: 60."
    ).long().default(60).validate { it >= 0 }

    private val downstreamServers by argument(
        name = "downstream servers",
        help = "URLs in format: <scheme>://<authority><path> without query params or fragment " +
                "of downstream servers where traffic will be forwarded."
    ).multiple(required = true)

    private lateinit var gracefulShutdownServer: GracefulShutdownServer

    override fun run() {
        val loadBalancer = RoundRobinLoadBalancer(
            downstreamServers = downstreamServers.map { URI(it) },
            recoveryTimeoutSeconds = loadBalancingRecoveryTimeout
        )
        val requestHandler = LoadBalancedRequestHandler(
            loadBalancer = loadBalancer
        )

        val threadPool = ThreadPoolExecutor(
            minThreadPoolSize,
            maxThreadPoolSize,
            threadPoolKeepAliveSeconds,
            TimeUnit.SECONDS,
            SynchronousQueue()
        )

        gracefulShutdownServer = GracefulShutdownServer(
            requestHandler = requestHandler,
            threadPool = threadPool,
            port = port,
            gracefulShutdownSeconds = gracefulShutdownSeconds,
        )
        gracefulShutdownServer.start()
    }

    fun stop() = gracefulShutdownServer.stop()
}
