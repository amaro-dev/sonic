package dev.amaro.sonic.app.samples.notes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.CompositionLocalProvider
import dev.amaro.sonic.app.compose.LocalStateManager
import dev.amaro.sonic.app.compose.StateManagerContext
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin

/**
 * Hosts the NoteComposeRoot so the sample can showcase Compose selectors
 * and rich result handling without touching the legacy navigation stack.
 */
class NoteComposeActivity : ComponentActivity() {

    private val noteStateManager: NoteStateManager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (GlobalContext.getOrNull() == null) {
            startKoin {
                androidContext(this@NoteComposeActivity)
                modules(NoteModule.Instance)
            }
        }
        setContent {
            MaterialTheme {
                Surface {
                    CompositionLocalProvider(
                        LocalStateManager provides StateManagerContext(noteStateManager)
                    ) {
                        NoteComposeRoot(noteStateManager)
                    }
                }
            }
        }
    }
}
