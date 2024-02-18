import loadbalancer.LoadBalancerCli
import org.junit.jupiter.api.Test
import utils.withDefer
import utils.withSingleEndpointServer
import java.net.HttpURLConnection
import java.net.URL
import kotlin.test.assertEquals

class LoadBalancer {

    @Test
    fun unhealthyServerIgnored() {
        // Given
        withDefer {
            val loadBalancer = LoadBalancerCli()
            defer { loadBalancer.stop() }
            loadBalancer.main(listOf(
                "--port", "8000",
                "--gracefulShutdownSeconds", "0", // We don't need to slow down tests for this
                "http://localhost:9000/api",
                "http://localhost:9001",
                "http://localhost:9002",
            ))

            withSingleEndpointServer(port = 9000, endpoint = "/api/endpoint", returnResponse = "Test from server 1")
            withSingleEndpointServer(port = 9001, endpoint = "/endpoint", returnResponse = "Test from server 2")

            // When
            val responses = (0..5).map { URL("http://localhost:8000/endpoint").openConnection() as HttpURLConnection }

            // Then
            assertEquals(200, responses[0].responseCode)
            assertEquals("Test from server 1", responses[0].inputStream.bufferedReader().readText())
            assertEquals(200, responses[1].responseCode)
            assertEquals("Test from server 2", responses[1].inputStream.bufferedReader().readText())
            assertEquals(502, responses[2].responseCode)
            assertEquals("", responses[2].errorStream.bufferedReader().readText())

            // Now the unhealthy server should be skipped
            assertEquals(200, responses[3].responseCode)
            assertEquals("Test from server 1", responses[3].inputStream.bufferedReader().readText())
            assertEquals(200, responses[4].responseCode)
            assertEquals("Test from server 2", responses[4].inputStream.bufferedReader().readText())
            assertEquals(200, responses[5].responseCode)
            assertEquals("Test from server 1", responses[5].inputStream.bufferedReader().readText())
        }
    }

    @Test
    fun complexUrlForwardingWorks() {
        // Given
        withDefer {
            val loadBalancer = LoadBalancerCli()
            defer { loadBalancer.stop() }
            loadBalancer.main(listOf(
                "--port", "8000",
                "--gracefulShutdownSeconds", "0", // We don't need to slow down tests for this
                "http://localhost:9000/api",
            ))

            withSingleEndpointServer(port = 9000, endpoint = "/api/endpoint", returnResponse = "Test from server 1", expectRequest = { exchange ->
                // Fragments are not usually sent to the server, so we only care that it doesn't crash anything
                exchange.requestURI.query == "query=parm&value +"
            })

            // When
            val response = URL("http://localhost:8000/endpoint?query=parm&value%20+#fragment").openConnection() as HttpURLConnection

            // Then
            assertEquals(200, response.responseCode)
            assertEquals("Test from server 1", response.inputStream.bufferedReader().readText())
        }
    }

    @Test
    fun badGatewayErrorWhenNoHealthyDownstreamIsAvailable() {
        // Given
        withDefer {
            val loadBalancer = LoadBalancerCli()
            defer { loadBalancer.stop() }
            loadBalancer.main(listOf(
                "--port", "8000",
                "--gracefulShutdownSeconds", "0", // We don't need to slow down tests for this
                "http://localhost:9000",
            ))

            // When
            val response1 = URL("http://localhost:8000/").openConnection() as HttpURLConnection
            val response2 = URL("http://localhost:8000/").openConnection() as HttpURLConnection

            // Then
            assertEquals(502, response1.responseCode)
            assertEquals("", response1.errorStream.bufferedReader().readText())

            assertEquals(502, response2.responseCode)
            assertEquals("No downstream server available.", response2.errorStream.bufferedReader().readText())
        }
    }

    @Test
    fun testUnhealthyServerCanRecover() {
        // Given
        withDefer {
            val loadBalancer = LoadBalancerCli()
            defer { loadBalancer.stop() }
            loadBalancer.main(listOf(
                "--port", "8000",
                "--gracefulShutdownSeconds", "0", // We don't need to slow down tests for this
                "--loadBalancingRecoveryTimeout", "0", // We don't need to slow down tests for this
                "http://localhost:9000",
            ))

            // When
            val response1 = URL("http://localhost:8000/").openConnection() as HttpURLConnection
            response1.responseCode // This will execute the lazy request

            withSingleEndpointServer(port = 9000, returnResponse = "Test from server 1")
            val response2 = URL("http://localhost:8000/").openConnection() as HttpURLConnection

            // Then
            assertEquals(502, response1.responseCode)
            assertEquals("", response1.errorStream.bufferedReader().readText())

            // Now the server is healthy again, and because loadBalancingRecoveryTimeout is 0, we will
            // immediately consider it again.
            assertEquals(200, response2.responseCode)
            assertEquals("Test from server 1", response2.inputStream.bufferedReader().readText())
        }
    }

}
