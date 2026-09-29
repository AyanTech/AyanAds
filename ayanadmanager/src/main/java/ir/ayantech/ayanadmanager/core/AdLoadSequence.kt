package ir.ayantech.ayanadmanager.core

/** Owns fallback and ignores events from released attempts. All calls are made on the main thread. */
internal class AdLoadSequence(
    private val count: Int,
    private val createProvider: (Int) -> AdProvider,
    private val load: (AdProvider, Int, AdCallback) -> Unit,
    private val callback: AdCallback?,
    private val onProviderChanged: (AdProvider?) -> Unit = {},
    private val onFinished: () -> Unit = {},
    private val onAttemptFailed: (Int, String) -> Unit = { _, _ -> },
) {
    private var index = -1
    private var provider: AdProvider? = null
    private var closed = false
    private var advancing = false
    private var advanceRequested = false
    private var lastError = "No available ad providers."

    fun start() {
        if (index >= 0 || closed) return
        advance()
    }

    private fun advance() {
        advanceRequested = true
        if (advancing) return
        advancing = true
        try {
            while (advanceRequested && !closed) {
                advanceRequested = false
                releaseProvider()
                val attempt = ++index
                if (attempt >= count) {
                    closed = true
                    onFinished()
                    callback?.onAdFailed(lastError)
                    return
                }
                var failed = false
                val events = object : AdCallback {
                    override fun onAdLoaded() {
                        if (!closed && !failed && index == attempt) callback?.onAdLoaded()
                    }
                    override fun onAdClicked() {
                        if (!closed && !failed && index == attempt) callback?.onAdClicked()
                    }
                    override fun onAdFailed(error: String) {
                        if (closed || failed || index != attempt) return
                        failed = true
                        lastError = error
                        onAttemptFailed(attempt, error)
                        advance()
                    }
                }
                val next = createProvider(attempt)
                provider = next
                onProviderChanged(next)
                load(next, attempt, events)
            }
        } finally {
            advancing = false
        }
    }

    fun cancel() {
        if (closed) return
        closed = true
        releaseProvider()
        onFinished()
    }

    private fun releaseProvider() {
        val previous = provider ?: return
        provider = null
        // Clear the current attempt before disposal in case the SDK emits a callback from destroy().
        onProviderChanged(null)
        previous.destroy()
    }
}
