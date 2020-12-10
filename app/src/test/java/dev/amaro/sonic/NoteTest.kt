package dev.amaro.sonic

import androidx.navigation.NavController
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.amaro.sonic.app.samples.notes.*
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.TestCoroutineDispatcher
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.get


@RunWith(AndroidJUnit4::class)
class NoteTest : KoinTest {

    private val dispatcher = TestCoroutineDispatcher()

    private val oneTodo = listOf(Note("Task 1", false))
    private val oneTodoAndOneClosed = listOf(
        Note("Task 1", false),
        Note("Task 2", true)
    )

    @Before
    fun setUp() {
        startKoin {
            androidContext(ApplicationProvider.getApplicationContext())
            modules(
                module { factory { mockk<NavController>(relaxed = true) } },
                NoteModule.Instance
            )
        }
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `Load saved notes on start`() {
        setNotesOnStorage(oneTodoAndOneClosed)
        val renderer: IRenderer<NoteState> = mockk(relaxed = true)
        NoteScreen(renderer, get(), dispatcher = dispatcher)
        renderer.verifyState(NoteState(oneTodoAndOneClosed))
    }

    @Test
    fun `Toggle closed notes`() {
        setNotesOnStorage(oneTodoAndOneClosed)
        val renderer: IRenderer<NoteState> = mockk(relaxed = true)
        val screen = NoteScreen(renderer, get(), dispatcher = dispatcher)
        screen.perform(Action.ToggleClosedNotes)
        renderer.verifyState(NoteState(oneTodo, true))
    }

    @Test
    fun `Add new note`() {
        val renderer: IRenderer<NoteState> = mockk(relaxed = true)
        val screen = NoteScreen(renderer, get(), dispatcher = dispatcher)
        screen.perform(Action.AddNote(Note("Task 1", false)))
        renderer.verifyState(NoteState(oneTodo, false))
    }

    @Test
    fun `Add two new notes`() {
        val renderer: IRenderer<NoteState> = mockk(relaxed = true)
        val screen = NoteScreen(renderer, get(), dispatcher = dispatcher)
        screen.perform(Action.AddNote(Note("Task 1", false)))
        screen.perform(Action.AddNote(Note("Task 2", true)))
        renderer.verifyState(NoteState(oneTodoAndOneClosed, false))
    }

    @Test
    fun `Toggle note`() {
        val renderer: IRenderer<NoteState> = mockk(relaxed = true)
        val screen = NoteScreen(renderer, get(), dispatcher = dispatcher)
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
        loadKoinModules(
            module {
                single<IStorage>(override = true) { storage }
            }
        )

    }
}