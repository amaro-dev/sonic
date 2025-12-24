package dev.amaro.sonic

import androidx.navigation.NavController
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.amaro.sonic.app.samples.notes.*
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.get


@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class NoteTest : KoinTest {

    private lateinit var storage: FakeStorage
    private val oneTodo = listOf(Note("Task 1", false))
    private val oneTodoAndOneClosed = listOf(
        Note("Task 1", false),
        Note("Task 2", true)
    )

    @Before
    fun setUp() {
        storage = FakeStorage()
        startKoin {
            androidContext(ApplicationProvider.getApplicationContext())
            modules(
                module {
                    factory { mockk<NavController>(relaxed = true) }
                    single<IStorage> { storage }
                    single { NoteStateManager() }
                }
            )
        }
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `Load saved notes on start`() = runMainTest {
        setNotesOnStorage(oneTodoAndOneClosed)
        val renderer: IRenderer<NoteState> = mockk(relaxed = true)
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + Job())
        try {
            NoteScreen(renderer, get(), scope)
            advanceUntilIdle()
            renderer.verifyState(NoteState(oneTodoAndOneClosed))
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun `Toggle closed notes`() = runMainTest {
        setNotesOnStorage(oneTodoAndOneClosed)
        val renderer: IRenderer<NoteState> = mockk(relaxed = true)
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + Job())
        try {
            val screen = NoteScreen(renderer, get(), scope)
            advanceUntilIdle()
            screen.perform(Action.ToggleClosedNotes)
            advanceUntilIdle()
            renderer.verifyState(NoteState(oneTodo, true))
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun `Add new note`() = runMainTest {
        val renderer: IRenderer<NoteState> = mockk(relaxed = true)
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + Job())
        try {
            val screen = NoteScreen(renderer, get(), scope)
            advanceUntilIdle()
            screen.perform(Action.AddNote(Note("Task 1", false)))
            advanceUntilIdle()
            renderer.verifyState(NoteState(oneTodo, false))
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun `Add two new notes`() = runMainTest {
        val renderer: IRenderer<NoteState> = mockk(relaxed = true)
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + Job())
        try {
            val screen = NoteScreen(renderer, get(), scope)
            advanceUntilIdle()
            screen.perform(Action.AddNote(Note("Task 1", false)))
            advanceUntilIdle()
            screen.perform(Action.AddNote(Note("Task 2", true)))
            advanceUntilIdle()
            renderer.verifyState(NoteState(oneTodoAndOneClosed, false))
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun `Toggle note`() = runMainTest {
        val renderer: IRenderer<NoteState> = mockk(relaxed = true)
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + Job())
        try {
            val screen = NoteScreen(renderer, get(), scope)
            advanceUntilIdle()
            screen.perform(Action.AddNote(Note("Task 1", false)))
            advanceUntilIdle()
            screen.perform(Action.ToggleNote(Note("Task 1", false)))
            advanceUntilIdle()
            renderer.verifyState(NoteState(listOf(Note("Task 1", true)), false))
        } finally {
            scope.cancel()
        }
    }

    private fun runMainTest(block: suspend TestScope.() -> Unit) = kotlinx.coroutines.test.runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            block()
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun IRenderer<NoteState>.verifyState(expected: NoteState) {
        verify {
            render(match { it.notes == expected.notes && it.showOnlyOpen == expected.showOnlyOpen }, any())
        }
    }

    private fun setNotesOnStorage(notes: List<Note>) {
        storage.replaceAll(notes)
    }

    private class FakeStorage : IStorage {
        private val items = mutableListOf<Note>()

        override fun list(): List<Note> = items.toList()

        override fun save(note: Note) {
            items.add(note)
        }

        override fun update(note: Note) {
            items.remove(note)
            items.add(note.copy(done = !note.done))
        }

        override fun delete(note: Note) {
            items.remove(note)
        }

        fun replaceAll(notes: List<Note>) {
            items.clear()
            items.addAll(notes)
        }
    }
}
