package dev.amaro.sonic.app.samples.notes

import org.koin.dsl.module

object NoteModule {
    val Instance = module {
        single<IStorage> { PrefsStorage(get()) }
    }
}