package dev.amaro.sonic

import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

import org.junit.Assert.*

class ConditionedDirectMiddlewareTest {

    private val middleware = ConditionedDirectMiddleware<String>(
        SampleActions.Action1::class,
        SampleActions.Action2::class
    )

    private val someString = ""

    @Test
    fun `Perform actions on the list`() {
        val processor: Processor<String> = mockk(relaxed = true)
        middleware.process(SampleActions.Action1, someString, processor)
        middleware.process(SampleActions.Action2, someString, processor)
        verify {
            processor.reduce(SampleActions.Action1)
            processor.reduce(SampleActions.Action2)
        }
    }


    @Test
    fun `Do not perform actions out of the list`() {
        val processor: Processor<String> = mockk(relaxed = true)
        middleware.process(SampleActions.Action3, someString, processor)
        verify(exactly = 0) {
            processor.reduce(SampleActions.Action3)
        }
    }

    sealed class SampleActions : IAction {
        object Action1 : SampleActions()
        object Action2 : SampleActions()
        object Action3 : SampleActions()
    }

}