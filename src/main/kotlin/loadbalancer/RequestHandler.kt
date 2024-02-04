package loadbalancer

import java.io.InputStream
import java.net.URI

internal interface RequestHandler {

    fun handleRequest(requestURI: URI, method: String, headers: Map<String, List<String>>, body: InputStream): Result

    data class Result(
        val statusCode: Int,
        val contentSize: Long,
        /** When some header key is specified multiple times, each value would be grouped in a list here. */
        val headers: Map<String, List<String>>,
        val body: InputStream
    )
}

internal class RoundRobinLoadBalancer(
    private val servers: List<String>,
): RequestHandler {

    // TODO healthcheck

    // TODO redirect requests to one of the servers

    override fun handleRequest(
        requestURI: URI,
        method: String,
        headers: Map<String, List<String>>,
        body: InputStream
    ): RequestHandler.Result {
        val mockedResponse = "Hello World!".encodeToByteArray()

        return RequestHandler.Result(
            statusCode = 201,
            // TODO take content size from previous response headers
            contentSize = mockedResponse.size.toLong(),
            headers = headers,
            body = mockedResponse.inputStream()
        )
    }

}
