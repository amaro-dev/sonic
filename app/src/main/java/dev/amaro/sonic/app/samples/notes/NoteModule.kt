package dev.amaro.sonic.app.samples.notes

import dev.amaro.sonic.IRenderer
import org.koin.dsl.module

object NoteModule {
    val Instance = module {
        single<IStorage> { PrefsStorage(get()) }
        single { listOf(Navigator(get())) }
        single { NoteStateManager(NoteState(), get()) }
        factory { (renderer: IRenderer<NoteState>) -> NoteScreen(renderer, get()) }
        factory { (renderer: IRenderer<NoteState>) -> NewNoteScreen(renderer, get()) }
    }
}