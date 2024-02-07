import com.sun.net.httpserver.HttpServer
import loadbalancer.LoadBalancerCli
import org.junit.jupiter.api.Test
import utils.withDefer
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.URL
import kotlin.test.assertEquals

class LoadBalancer {

    @Test
    fun test() {
        // Given
        withDefer {
            val loadBalancer = LoadBalancerCli()
            defer { loadBalancer.stop() }
            loadBalancer.main(listOf(
                "--port", "8000",
                "--gracefulShutdownSeconds", "0", // We don't need to slow down tests for this
                "http://localhost:9000",
                "http://localhost:9001",
                "http://localhost:9002",
                "http://localhost:9003",
            ))

            val server = HttpServer.create(InetSocketAddress(9000), 0)
            server.createContext("/") { exchange ->
                exchange.sendResponseHeaders(200, 4)
                exchange.responseBody.bufferedWriter().use { it.write("Test") }
                exchange.close()
            }
            defer { server.stop(0) }
            server.start()

            // When
            val connection = URL("http://localhost:8000").openConnection() as HttpURLConnection

            // Then
            assertEquals(200, connection.responseCode)
            assertEquals("Test", connection.inputStream.bufferedReader().readText())
        }
    }

    // Test URLs with and without path prefix
    // Test URLs with and without fragment
    // Test URLs with and without query params
    // Test URLs with and without port
    // Test URLs that should be URL-encoded
    // Test exceptions

}