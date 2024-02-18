package utils

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress

fun DeferContext.withSingleEndpointServer(
    port: Int,
    endpoint: String = "/",
    expectRequest: (request: HttpExchange) -> Boolean = { true },
    returnStatusCode: Int = 200,
    returnResponse: String,
) {
    val server = HttpServer.create(InetSocketAddress(port), 0)

    server.createContext(endpoint) { exchange ->
        if (!expectRequest(exchange)) {
            val badResponse = "Endpoint expectation not met.".encodeToByteArray()
            exchange.sendResponseHeaders(400, badResponse.size.toLong())
            exchange.responseBody.buffered().use { it.write(badResponse) }
            exchange.close()
            return@createContext
        }

        val response = returnResponse.toByteArray()
        exchange.sendResponseHeaders(returnStatusCode, response.size.toLong())
        exchange.responseBody.buffered().use { it.write(response) }
        exchange.close()
    }

    defer {
        println("Stopping test server on port: $port with endpoint: $endpoint")
        server.stop(0)
    }

    println("Starting test server on port: $port with endpoint: $endpoint")
    server.start()
}
