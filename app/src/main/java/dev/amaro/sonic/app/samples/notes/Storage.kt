package dev.amaro.sonic.app.samples.notes

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken


interface IStorage {
    fun list(): List<Note>
    fun save(note: Note)
    fun update(note: Note)
    fun delete(note: Note)
}

class PrefsStorage(context: Context) : IStorage {
    private val sharedPrefs = context.getSharedPreferences("STORAGE", Context.MODE_PRIVATE)
    private val serializer = Gson()

    override fun list(): List<Note> {
        return sharedPrefs.getString("ITEMS", null)
            ?.let { serializer.fromJson<List<Note>>(it) } ?: emptyList()
    }

    override fun save(note: Note) {
        val list = list().toMutableList().plus(note)
        val json = serializer.toJson(list)
        sharedPrefs.edit().putString("ITEMS", json).apply()
    }

    override fun update(note: Note) {
        val list = list().toMutableList().minus(note).plus(note.copy(done = !note.done))
        val json = serializer.toJson(list)
        sharedPrefs.edit().putString("ITEMS", json).apply()
    }

    override fun delete(note: Note) {
        val list = list().toMutableList().minus(note)
        val json = serializer.toJson(list)
        sharedPrefs.edit().putString("ITEMS", json).apply()
    }

    private inline fun <reified T> Gson.fromJson(json: String) =
        fromJson<T>(json, object : TypeToken<T>() {}.type)

}