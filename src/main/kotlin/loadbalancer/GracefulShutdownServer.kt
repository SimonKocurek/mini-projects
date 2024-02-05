package loadbalancer

import com.sun.net.httpserver.HttpExchange
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

    private lateinit var server: HttpServer

    fun start() {
        // By default, the address is set to 0.0.0.0
        server = HttpServer.create(InetSocketAddress(port), 0)
        server.executor = threadPool

        // To support graceful shutdowns, we need to propagate the request
        // to stop to the server on SIGTERM signal.
        Runtime.getRuntime().addShutdownHook(Thread { stop() })

        server.createContext("/") { exchange -> onRequest(exchange) }

        server.start()
        logger.log("Server started", mapOf("port" to port))
    }

    private fun onRequest(exchange: HttpExchange) {
        logger.useContext("contextId", UUID.randomUUID().toString()) {
            logger.log(
                "request", mapOf(
                    "clientAddress" to exchange.remoteAddress,
                    "method" to exchange.requestMethod,
                    "uri" to exchange.requestURI
                )
            )

            try {
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

            } catch (e: Exception) {
                logger.log("error", mapOf("exception" to e.stackTraceToString()))

                // If possible, we also want to indicate to the client that
                // forwarding the request failed.
                exchange.sendResponseHeaders(502, 0)
                exchange.close()

                throw e
            }
        }
    }

    fun stop() {
        logger.log("Stopping with graceful shutdown", mapOf("gracefulShutdownSeconds" to gracefulShutdownSeconds))
        server.stop(gracefulShutdownSeconds)

        // Without also stopping the thread pool, the threads would keep running in the background
        // preventing the program from stopping.
        threadPool.shutdown()
    }

}
