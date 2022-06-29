package dev.amaro.sonic

import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class DirectMiddlewareTest {

    private val middleware = DirectMiddleware<String>()
    private val someString = ""

    @Test
    fun `Perform any actions`() {
        val processor: Processor<String> = mockk(relaxed = true)
        middleware.process(SampleActions.Action1, someString, processor)
        middleware.process(SampleActions.Action2, someString, processor)
        middleware.process(SampleActions.Action3, someString, processor)
        verify {
            processor.reduce(SampleActions.Action1)
            processor.reduce(SampleActions.Action2)
            processor.reduce(SampleActions.Action3)
        }
    }

    sealed class SampleActions : IAction {
        object Action1 : SampleActions()
        object Action2 : SampleActions()
        object Action3 : SampleActions()
    }

}