package dev.amaro.sonic.app.samples.notes

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckedTextView
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.setPadding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dev.amaro.sonic.IPerformer
import dev.amaro.sonic.IRenderer
import dev.amaro.sonic.Screen
import dev.amaro.sonic.app.R
import dev.amaro.sonic.app.clicks
import dev.amaro.sonic.collectOn
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.onEach
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class NoteScreen(
    renderer: IRenderer<NoteState>,
    stateManager: NoteStateManager,
    dispatcher: CoroutineDispatcher = Dispatchers.Main
) : Screen<NoteState>(stateManager, renderer, dispatcher) {
    init {
        perform(Action.Load)
    }
}

class NoteListScreen @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = -1
) : ConstraintLayout(context, attrs, defStyleAttr), IRenderer<NoteState>, KoinComponent {

    private val buttonNew: FloatingActionButton
    private val listNotes: RecyclerView

    init {
        LayoutInflater.from(context).inflate(R.layout.screen_note_list, this)
        buttonNew = findViewById(R.id.buttonNew)
        listNotes = findViewById(R.id.listNotes)
        NoteScreen(this, get())

    }

    override fun render(state: NoteState, performer: IPerformer<NoteState>) {
        println(state.notes)
        buttonNew.clicks()
            .onEach { performer.perform(Action.NewNote) }
            .collectOn(Dispatchers.Main) {}
        listNotes.adapter = NoteAdapter(state.notes).apply {
            onClick()
                .onEach { performer.perform(Action.ToggleNote(it)) }
                .collectOn(Dispatchers.Main) {}
        }
        listNotes.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
    }
}

class NoteAdapter(private val items: List<Note>) : RecyclerView.Adapter<NoteAdapter.NoteItem>() {

    private val publisher: BroadcastChannel<Note> = BroadcastChannel<Note>(1)

    fun onClick(): Flow<Note> {
        return publisher.asFlow()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteItem {
        val checkedTextView = CheckedTextView(parent.context)
        checkedTextView.checkMarkDrawable =
            parent.context.resources.getDrawable(R.drawable.ic_check)
        checkedTextView.setPadding(24)
        checkedTextView.gravity = Gravity.CENTER
        checkedTextView.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        return NoteItem(checkedTextView)
    }

    override fun onBindViewHolder(holder: NoteItem, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class NoteItem(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(note: Note) {
            (itemView as CheckedTextView).apply {
                text = note.title
                isChecked = note.done
                setOnClickListener { publisher.offer(note) }

            }

        }
    }

}