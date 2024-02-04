package loadbalancer

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.long
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

fun main(args: Array<String>) {
    LoadBalancerCli().main(args)
}

class LoadBalancerCli : CliktCommand(
    help = """
    """.trimIndent(),
    name = "loadbalancer",
) {

    private val port by option(
        "-p",
        "--port",
        help = "Port the server will be listening on. Default: 8000."
    ).int().default(8000)

    private val gracefulShutdownSeconds by option(
        "--gracefulShutdownSeconds",
        help = "After termination signal is received, the server will stop receiving new requests. " +
                "Before forcefully terminating existing connections, it will give the existing connections " +
                "this specified time to finish. Default: 3."
    ).int().default(3)

    private val minThreadPoolSize by option(
        "--minThreadPoolSize",
        help = "Minimum number of threads handling requests. Default: 10."
    ).int().default(10)

    private val maxThreadPoolSize by option(
        "--maxThreadPoolSize",
        help = "Maximum number of threads handling requests. Default: 10_000."
    ).int().default(10_000)

    private val threadPoolKeepAliveSeconds by option(
        "--threadPoolKeepAliveSeconds",
        help = "Number of seconds after which idle threads will be released from thread pool. Default: 60."
    ).long().default(60)

    override fun run() {
        val loadBalancer = RoundRobinLoadBalancer(
            servers = emptyList()
        )

        val threadPool = ThreadPoolExecutor(
            minThreadPoolSize,
            maxThreadPoolSize,
            threadPoolKeepAliveSeconds,
            TimeUnit.SECONDS,
            SynchronousQueue()
        )

        val gracefulShutdownServer = GracefulShutdownServer(
            requestHandler = loadBalancer,
            threadPool = threadPool,
            port = port,
            gracefulShutdownSeconds = gracefulShutdownSeconds,
        )
        gracefulShutdownServer.start()
    }
}
