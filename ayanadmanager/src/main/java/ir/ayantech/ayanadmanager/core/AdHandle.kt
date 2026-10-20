package ir.ayantech.ayanadmanager.core

import androidx.annotation.MainThread

/** Releases this placement only. Dispose from onViewRecycled or onDestroyView on the main thread. */
interface AdHandle : AutoCloseable {
    @MainThread fun dispose()
    override fun close() = dispose()
}

internal class DisposableAdHandle : AdHandle {
    var action: (() -> Unit)? = null
    override fun dispose() {
        val dispose = action
        action = null
        dispose?.invoke()
    }
}
