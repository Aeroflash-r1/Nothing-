package com.nothing.assistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.wear.ambient.AmbientLifecycleObserver
import com.nothing.assistant.tile.AssistantTileService

/**
 * Main activity — the single entry point for the app.
 *
 * Handles:
 * - Ambient lifecycle (pauses animations/network when the screen dims)
 * - Tile deep-link (launches with mic pre-armed)
 *
 * In interactive mode, all Compose animations and coroutines run normally.
 * In ambient mode, the OS pauses animation and shows a static low-bit frame.
 * No wake locks are ever held — screen-off cancels network via ViewModel's
 * structured concurrency.
 */
class MainActivity : ComponentActivity() {

    private lateinit var appContainer: AppContainer
    private var launchWithMic: Boolean = false

    private val ambientObserver = AmbientLifecycleObserver(
        this,
        object : AmbientLifecycleObserver.AmbientLifecycleCallback {
            override fun onEnterAmbient(ambientDetails: AmbientLifecycleObserver.AmbientDetails) {
                // Ambient mode: the OS pauses all Compose animations automatically.
                // Network requests are cancelled via ViewModel's viewModelScope
                // which is tied to the lifecycle.
                // No further action needed.
            }

            override fun onExitAmbient() {
                // Returning to interactive mode — Compose re-renders normally.
                // The UI state is preserved from the ViewModel.
            }

            override fun onUpdateAmbient() {
                // Not used — no periodic ambient updates needed.
            }
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appContainer = AppContainer(this)
        launchWithMic = intent.getBooleanExtra(
            AssistantTileService.EXTRA_LAUNCH_FROM_TILE,
            false
        )

        lifecycle.addObserver(ambientObserver)

        setContent {
            WearApp(
                appContainer = appContainer,
                launchWithMic = launchWithMic,
            )
        }
    }

    override fun onDestroy() {
        lifecycle.removeObserver(ambientObserver)
        super.onDestroy()
    }
}
