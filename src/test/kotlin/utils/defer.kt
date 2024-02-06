package utils

import java.util.Stack

inline fun <T> withDefer(block: DeferContext.() -> T): T {
    val context = DeferContext()

    try {
        return context.block()
    } finally {
        context.cleanup()
    }
}

class DeferContext {

    private val deferredActions: Stack<() -> Unit> = Stack()

    fun defer(action: () -> Unit) {
        deferredActions.push(action)
    }

    fun cleanup() {
        var exception: Exception? = null

        deferredActions.forEach {
            try {
                it()
            } catch (e: Exception) {
                exception = e
                // We want to try running all the cleanup actions, so we would rather just
                // log failed defers, than fail the whole cleanup.
                System.err.println(e.stackTraceToString())
            }
        }

        deferredActions.clear()
        exception?.let { throw it }
    }
}
