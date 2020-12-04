package dev.amaro.sonic

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.amaro.sonic.app.samples.notes.*
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.TestCoroutineDispatcher
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.test.KoinTest


@RunWith(AndroidJUnit4::class)
class NoteTest : KoinTest {

    private val dispatcher = TestCoroutineDispatcher()
    private val modules = mutableListOf<Module>()
    private val oneTodo = listOf(Note("Task 1", false))
    private val oneTodoAndOneClosed = listOf(
        Note("Task 1", false),
        Note("Task 2", true)
    )

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `Load saved notes on start`() {
        setNotesOnStorage(oneTodoAndOneClosed)
        startKoin { modules(modules) }
        val renderer: IRenderer<NoteState> = mockk(relaxed = true)
        NoteScreen(renderer, dispatcher = dispatcher)
        renderer.verifyState(NoteState(oneTodoAndOneClosed))
    }

    @Test
    fun `Toggle closed notes`() {
        setNotesOnStorage(oneTodoAndOneClosed)
        startKoin { modules(modules) }
        val renderer: IRenderer<NoteState> = mockk(relaxed = true)
        val screen = NoteScreen(renderer, dispatcher = dispatcher)
        screen.perform(Action.ToggleClosedNotes)
        renderer.verifyState(NoteState(oneTodo, true))
    }

    @Test
    fun `Add new note`() {
        startKoin {
            androidContext(ApplicationProvider.getApplicationContext())
            modules(NoteModule.Instance)
        }
        val renderer: IRenderer<NoteState> = mockk(relaxed = true)
        val screen = NoteScreen(renderer, dispatcher = dispatcher)
        screen.perform(Action.AddNote(Note("Task 1", false)))
        renderer.verifyState(NoteState(oneTodo, false))
    }

    @Test
    fun `Add two new notes`() {
        startKoin {
            androidContext(ApplicationProvider.getApplicationContext())
            modules(NoteModule.Instance)
        }
        val renderer: IRenderer<NoteState> = mockk(relaxed = true)
        val screen = NoteScreen(renderer, dispatcher = dispatcher)
        screen.perform(Action.AddNote(Note("Task 1", false)))
        screen.perform(Action.AddNote(Note("Task 2", true)))
        renderer.verifyState(NoteState(oneTodoAndOneClosed, false))
    }

    @Test
    fun `Toggle note`() {
        startKoin {
            androidContext(ApplicationProvider.getApplicationContext())
            modules(NoteModule.Instance)
        }
        val renderer: IRenderer<NoteState> = mockk(relaxed = true)
        val screen = NoteScreen(renderer, dispatcher = dispatcher)
        screen.perform(Action.AddNote(Note("Task 1", false)))
        screen.perform(Action.ToggleNote(Note("Task 1", false)))
        renderer.verifyState(NoteState(listOf(Note("Task 1", true)), false))
    }

    private fun <T> IRenderer<T>.verifyState(state: T) {
        verify { render(state, any()) }
    }

    private fun setNotesOnStorage(notes: List<Note>) {
        val storage: IStorage = mockk(relaxed = true)
        every { storage.list() } returns notes
        modules.add(module {
            single<IStorage> { storage }
        })
    }
}