package dev.amaro.sonic.app.samples.notes

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.Button
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.textfield.TextInputLayout
import dev.amaro.sonic.*
import dev.amaro.sonic.app.R
import dev.amaro.sonic.app.clicks
import dev.amaro.sonic.app.inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.onEach
import org.koin.core.parameter.parametersOf

class NewNoteScreen(
    renderer: IRenderer<NoteState>,
    stateManager: NoteStateManager,
    scope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
) {
    private val manager = stateManager

    init {
        bindState(manager, renderer, scope, Dispatchers.Main.immediate)
    }

    fun perform(action: IAction) {
        manager.perform(action)
    }
}

class CreateNoteScreen @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = -1
) : ConstraintLayout(context, attrs, defStyleAttr), IRenderer<NoteState> {

    private val buttonSave: Button
    private val buttonCancel: Button
    private val textNoteDescription: TextInputLayout
    private val screen: NewNoteScreen by inject { parametersOf(this) }
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    init {
        LayoutInflater.from(context).inflate(R.layout.screen_new_note, this)
        buttonSave = findViewById(R.id.buttonSave)
        buttonCancel = findViewById(R.id.buttonCancel)
        textNoteDescription = findViewById(R.id.fieldNote)
        buttonSave.clicks()
            .onEach { screen.perform(Action.AddNote(Note(textNoteDescription.text, false))) }
            .collectOn(scope, Dispatchers.Main) {}
        buttonCancel.clicks()
            .collectOn(scope, Dispatchers.Main) { screen.perform(Action.Cancel) }
    }

    override fun render(state: NoteState, performer: IPerformer<NoteState>) {

    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        scope.cancel()
    }
}

val TextInputLayout.text: String
    get() = editText?.text.toString()