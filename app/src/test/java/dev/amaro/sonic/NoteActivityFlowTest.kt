package dev.amaro.sonic

import android.os.Looper
import android.widget.Button
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import dev.amaro.sonic.app.R
import dev.amaro.sonic.app.samples.notes.NoteActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.Robolectric
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class NoteActivityFlowTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        stopKoin()
    }

    @Test
    fun `clicking add and saving note updates list`() {
        val controller = Robolectric.buildActivity(NoteActivity::class.java).setup()
        val activity = controller.get()
        val navController = activity.findNavController(R.id.main_navigation_host)

        val addButton = activity.findViewById<FloatingActionButton>(R.id.buttonNew)
        checkNotNull(addButton)
        addButton.performClick()
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        assertEquals(R.id.createNoteScreen, navController.currentDestination?.id)

        val input = activity.findViewById<TextInputEditText>(R.id.editNote)
        val saveButton = activity.findViewById<Button>(R.id.buttonSave)
        assertNotNull(input)
        assertNotNull(saveButton)
        input!!.setText("Test note")
        saveButton!!.performClick()
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        assertEquals(R.id.noteListScreen, navController.currentDestination?.id)
        val list = activity.findViewById<RecyclerView>(R.id.listNotes)
        assertEquals(1, list.adapter?.itemCount)
    }
}
