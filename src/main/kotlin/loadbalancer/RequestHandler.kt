package loadbalancer

import sun.net.www.protocol.http.HttpURLConnection
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.net.URL

internal interface RequestHandler {

    /**
     * @throws IOException if connecting, sending data or reading data from downstream server fails.
     * @return Result from downstream server. Response with 502 status if no downstream server is available.
     */
    fun handleRequest(requestURI: URI, method: String, headers: Map<String, List<String>>, body: InputStream): Result

    data class Result(
        val statusCode: Int,
        val contentSize: Long,
        /** When some header key is specified multiple times, each value would be grouped in a list here. */
        val headers: Map<String, List<String>>,
        val body: InputStream
    )
}

private val logger = LoggerFactory.create(LoadBalancedRequestHandler::class.simpleName!!)

internal class LoadBalancedRequestHandler(private val loadBalancer: LoadBalancer) : RequestHandler {

    override fun handleRequest(
        requestURI: URI,
        method: String,
        headers: Map<String, List<String>>,
        body: InputStream
    ): RequestHandler.Result {
        val downstreamServer = loadBalancer.getDownstreamServer()

        // Handling case with no downstream servers also serves as a
        // simple circuit breaker implementation.
        if (downstreamServer === null) {
            logger.log("no available downstream")

            val message = "No downstream server available.".encodeToByteArray()
            return RequestHandler.Result(
                statusCode = 502,
                contentSize = message.size.toLong(),
                headers = emptyMap(),
                body = message.inputStream()
            )
        }

        try {
            val url = URL("$downstreamServer$requestURI")
            logger.log("forwarding request", mapOf("downstream" to url))

            val connection = url.openConnection() as HttpURLConnection
            connection.sendRequest(method, headers, body)

            return RequestHandler.Result(
                statusCode = connection.responseCode,
                contentSize = connection.contentLengthLong,
                // Sometimes the headers contain `null` key, which would later on cause NullPointerException.
                headers = connection.headerFields.filterKeys { it != null },
                // The API is old and very clunky... on some status codes,
                // it returns error stream instead of input stream.
                body = connection.errorStream ?: connection.inputStream
            )
        } catch (e: IOException) {
            loadBalancer.reportConnectionFailed(downstreamServer)
            throw e
        }
    }

    private fun HttpURLConnection.sendRequest(method: String, headers: Map<String, List<String>>, body: InputStream) {
        requestMethod = method

        headers.forEach { (key, values) ->
            values.forEach { value ->
                addRequestProperty(key, value)
            }
        }

        if (method != "GET") {
            doOutput = true
            body.copyTo(outputStream)
        } else {
            val bodyBytes = body.readBytes().size
            if (bodyBytes > 0) {
                logger.log(
                    "skipped body",
                    mapOf("reason" to "GET request should have no body. $bodyBytes bytes of data are not forwarded.")
                )
            }
        }

        connect()
    }

}
