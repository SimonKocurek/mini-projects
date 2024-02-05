package loadbalancer

import java.time.OffsetDateTime
import kotlin.concurrent.getOrSet

internal object LoggerFactory {
    fun create(name: String): Logger = ConsoleLogger(name)
}

internal interface Logger {
    fun log(eventName: String, data: Map<String, Any?> = emptyMap())

    fun useContext(key: String, value: String, block: () -> Unit)
}

private class ConsoleLogger(private val name: String) : Logger {

    private companion object {
        private val context = ThreadLocal<MutableMap<String, String>>()
    }

    override fun log(eventName: String, data: Map<String, Any?>) {
        // For better performance, the logs could be buffered. And printed in batches.
        val logData = mutableMapOf<String, Any?>("logger" to name).apply {
            putAll(data)
            putAll(context.get() ?: emptyMap())
        }
        println("${OffsetDateTime.now()} - $eventName - $logData")
    }

    override fun useContext(key: String, value: String, block: () -> Unit) {
        val contextValues = context.getOrSet { mutableMapOf() }

        try {
            contextValues[key] = value
            block()
        } finally {
            contextValues.remove(key)

            if (contextValues.isEmpty()) {
                context.remove()
            }
        }
    }
}

