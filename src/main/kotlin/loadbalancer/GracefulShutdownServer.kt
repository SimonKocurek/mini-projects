package loadbalancer

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.util.UUID
import java.util.concurrent.ThreadPoolExecutor

private val logger = LoggerFactory.create(GracefulShutdownServer::class.simpleName!!)

internal class GracefulShutdownServer(
    private val requestHandler: RequestHandler,
    private val threadPool: ThreadPoolExecutor,
    private val port: Int,
    private val gracefulShutdownSeconds: Int,
) {

    fun start() {
        // By default, the address is set to 0.0.0.0
        val server = HttpServer.create(InetSocketAddress(port), 0)
        server.executor = threadPool

        // To support graceful shutdowns, we need to propagate the request
        // to stop to the server on SIGTERM signal.
        Runtime.getRuntime().addShutdownHook(Thread {
            logger.log("Stopping with graceful shutdown", mapOf("gracefulShutdownSeconds" to gracefulShutdownSeconds))
            server.stop(gracefulShutdownSeconds)

            // Without also stopping the thread pool, the threads would keep running in the background
            // preventing the program from stopping.
            threadPool.shutdown()
        })

        server.createContext("/") { exchange ->
            logger.useContext("contextId", UUID.randomUUID().toString()) {
                logger.log(
                    "request", mapOf(
                        "clientAddress" to exchange.remoteAddress,
                        "method" to exchange.requestMethod,
                        "uri" to exchange.requestURI
                    )
                )

                val result = requestHandler.handleRequest(
                    requestURI = exchange.requestURI,
                    method = exchange.requestMethod,
                    headers = exchange.requestHeaders,
                    body = exchange.requestBody
                )

                // Headers need to be specified **before** the body.
                exchange.responseHeaders.putAll(result.headers)
                exchange.sendResponseHeaders(result.statusCode, result.contentSize)

                result.body.copyTo(exchange.responseBody)
                exchange.close()
                logger.log(
                    "response", mapOf(
                        "statusCode" to result.statusCode,
                        "contentSize" to result.contentSize,
                    )
                )
            }
        }

        server.start()
        logger.log("Server started", mapOf("port" to port))
    }
}