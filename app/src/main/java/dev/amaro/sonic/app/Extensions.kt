package dev.amaro.sonic.app

import android.view.View
import android.widget.AdapterView
import android.widget.EditText
import android.widget.Spinner
import androidx.core.widget.doAfterTextChanged
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*

fun EditText.toFlow(): Flow<CharSequence> {
    return callbackFlow {
        doAfterTextChanged {
            it?.run {
                offer(this@run)
            }
        }
        awaitClose { this@toFlow.setOnClickListener(null) }
    }
}

fun View.clicks(): Flow<Unit> {
    return callbackFlow {
        setOnClickListener {
            offer(Unit)
        }
        awaitClose { this@clicks.setOnClickListener(null) }
    }
}

inline fun <reified T> Spinner.selection(): Flow<T> {
    return callbackFlow {
        onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>, p1: View?, index: Int, p3: Long) {
                offer(p0.adapter.getItem(index) as T)
            }

            override fun onNothingSelected(p0: AdapterView<*>?) = Unit

        }
        awaitClose { this@selection.onItemSelectedListener = null }
    }
}