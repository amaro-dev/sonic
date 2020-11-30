package dev.amaro.sonic

import android.widget.EditText
import androidx.core.widget.doAfterTextChanged
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

fun EditText.toFlow(): Flow<CharSequence> {
    return callbackFlow {
        doAfterTextChanged {
            it?.run {
                offer(this)
            }
        }
    }
}