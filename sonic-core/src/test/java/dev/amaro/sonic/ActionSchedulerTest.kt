package dev.amaro.sonic

import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ActionSchedulerTest {

    @Test
    fun `Schedule adds action to queue`() = runTest {
        val scheduler = ActionScheduler()
        val executed = mutableListOf<IAction>()

        scheduler.schedule(TestAction1)
        scheduler.drain { executed.add(it) }

        assertEquals(listOf(TestAction1), executed)
    }

    @Test
    fun `Drain executes actions in FIFO order`() = runTest {
        val scheduler = ActionScheduler()
        val executed = mutableListOf<IAction>()

        scheduler.schedule(TestAction1)
        scheduler.schedule(TestAction2)
        scheduler.schedule(TestAction3)

        scheduler.drain { executed.add(it) }

        assertEquals(listOf(TestAction1, TestAction2, TestAction3), executed)
    }

    @Test
    fun `Drain clears queue`() = runTest {
        val scheduler = ActionScheduler()
        val executed = mutableListOf<IAction>()

        scheduler.schedule(TestAction1)
        scheduler.drain { executed.add(it) }
        scheduler.drain { executed.add(it) }  // Second drain

        assertEquals(listOf(TestAction1), executed)  // Only executed once
    }

    @Test
    fun `Concurrent scheduling is thread-safe`() = runTest {
        val scheduler = ActionScheduler()
        val executed = mutableListOf<IAction>()

        // Schedule from multiple coroutines
        val jobs = (1..100).map { i ->
            launch {
                scheduler.schedule(TestAction1)
            }
        }
        jobs.forEach { it.join() }

        scheduler.drain { executed.add(it) }

        assertEquals(100, executed.size)
    }

    object TestAction1 : IAction
    object TestAction2 : IAction
    object TestAction3 : IAction
}
