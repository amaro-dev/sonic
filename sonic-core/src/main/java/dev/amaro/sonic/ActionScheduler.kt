package dev.amaro.sonic

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Internal utility that manages a queue of scheduled actions.
 *
 * Actions are queued via [schedule] and executed in FIFO order when [drain] is called.
 * Thread-safe for concurrent scheduling from multiple middleware.
 */
internal class ActionScheduler {
    private val queue = ConcurrentLinkedQueue<IAction>()
    private val mutex = Mutex()

    /**
     * Add an action to the queue for later execution.
     */
    fun schedule(action: IAction) {
        queue.offer(action)
    }

    /**
     * Execute all queued actions in FIFO order using the provided executor.
     * Thread-safe: only one drain operation runs at a time.
     */
    suspend fun drain(executor: suspend (IAction) -> Unit) {
        mutex.withLock {
            while (queue.isNotEmpty()) {
                val action = queue.poll() ?: break
                executor(action)
            }
        }
    }
}
