package dev.amaro.sonic.app.samples.notes

import android.content.Context
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
import dev.amaro.sonic.app.inject
import dev.amaro.sonic.app.loadDrawable
import dev.amaro.sonic.collectOn
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import org.koin.core.parameter.parametersOf


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
    context: Context
) : ConstraintLayout(context, null, -1), IRenderer<NoteState> {

    private val buttonNew: FloatingActionButton
    private val listNotes: RecyclerView
    private val screen: NoteScreen by inject { parametersOf(this as IRenderer<NoteState>) }
    private val adapter: NoteAdapter

    init {
        LayoutInflater.from(context).inflate(R.layout.screen_note_list, this)
        buttonNew = findViewById(R.id.buttonNew)
        listNotes = findViewById(R.id.listNotes)
        buttonNew.clicks()
            .onEach { screen.perform(Action.NewNote) }
            .collectOn(Dispatchers.Main) {}
        listNotes.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        adapter = NoteAdapter(mutableListOf()).apply {
            onClick()
                .onEach { screen.perform(it) }
                .collectOn(Dispatchers.Main) {}
        }
        listNotes.adapter = adapter
        screen.run { }
    }

    override fun render(state: NoteState, performer: IPerformer<NoteState>) {
        adapter.update(state.notes)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        screen.dispose()
    }
}

class NoteAdapter(private val items: MutableList<Note>) :
    RecyclerView.Adapter<NoteAdapter.NoteItem>() {

    private val publisher: MutableSharedFlow<Action> = MutableSharedFlow(1)

    fun update(newItems: List<Note>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun onClick(): Flow<Action> {
        return publisher
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteItem {
        val checkedTextView = CheckedTextView(parent.context)
        checkedTextView.run {
            checkMarkDrawable = loadDrawable(R.drawable.ic_check)
            setPadding(24)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
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
                setOnClickListener { runBlocking { publisher.emit(Action.ToggleNote(note)) } }
                setOnLongClickListener {
                    runBlocking { publisher.emit(Action.DeleteNote(note)) }
                    true
                }
            }

        }
    }

}