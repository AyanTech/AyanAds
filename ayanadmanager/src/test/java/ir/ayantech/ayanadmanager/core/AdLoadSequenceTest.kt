package ir.ayantech.ayanadmanager.core

import ir.ayantech.ayanadmanager.model.AdRequestConfig
import org.junit.Assert.*
import org.junit.Test

class AdLoadSequenceTest {
    @Test fun `each provider failure is reported once before fallback and final error is preserved`() {
        val events = mutableListOf<AdCallback>()
        val failures = mutableListOf<Pair<Int, String>>()
        var finalError: String? = null
        val sequence = AdLoadSequence(2, { Provider() }, { _, _, callback -> events += callback },
            object : AdCallback {
                override fun onAdFailed(error: String) { finalError = error }
            }, onAttemptFailed = { index, error -> failures += index to error })
        sequence.start()
        events[0].onAdFailed("Primary provider failed")
        events[0].onAdFailed("Stale duplicate")
        assertNull(finalError)
        assertEquals(listOf(0 to "Primary provider failed"), failures)
        events[1].onAdFailed("Unable to parse TLS packet header")
        assertEquals(listOf(0 to "Primary provider failed", 1 to "Unable to parse TLS packet header"), failures)
        assertEquals("Unable to parse TLS packet header", finalError)
    }

    private class Provider : AdProvider {
        var destroyed = 0
        override fun loadAd(config: AdRequestConfig) = Unit
        override fun destroy() { destroyed++ }
    }

    @Test fun `null callback still falls back and destroys failed providers`() {
        val providers = List(2) { Provider() }
        val events = mutableListOf<AdCallback>()
        val sequence = AdLoadSequence(2, { providers[it] }, { _, _, callback -> events += callback }, null)
        sequence.start()
        events[0].onAdFailed("no fill")
        assertEquals(2, events.size)
        assertEquals(1, providers[0].destroyed)
        events[1].onAdLoaded()
        sequence.cancel()
        sequence.cancel()
        assertEquals(1, providers[1].destroyed)
    }

    @Test fun `stale callbacks cannot skip or notify the next provider`() {
        var loaded = 0
        var clicked = 0
        var failed = 0
        val events = mutableListOf<AdCallback>()
        val sequence = AdLoadSequence(2, { Provider() }, { _, _, callback -> events += callback }, object : AdCallback {
            override fun onAdLoaded() { loaded++ }
            override fun onAdClicked() { clicked++ }
            override fun onAdFailed(error: String) { failed++ }
        })
        sequence.start()
        events[0].onAdFailed("no fill")
        events[0].onAdLoaded()
        events[0].onAdClicked()
        events[0].onAdFailed("duplicate")
        events[1].onAdLoaded()
        events[1].onAdClicked()
        sequence.cancel()
        events[1].onAdFailed("late")
        assertEquals(1, loaded)
        assertEquals(1, clicked)
        assertEquals(0, failed)
        assertEquals(2, events.size)
    }

    @Test fun `all failures notify once and dispose every provider without recursive overflow`() {
        val providers = mutableListOf<Provider>()
        var failures = 0
        var finished = 0
        val sequence = AdLoadSequence(10_000, { Provider().also(providers::add) }, { _, _, callback ->
            callback.onAdFailed("no fill")
        }, object : AdCallback {
            override fun onAdFailed(error: String) { failures++ }
        }, onFinished = { finished++ })
        sequence.start()
        sequence.cancel()
        assertEquals(10_000, providers.size)
        assertTrue(providers.all { it.destroyed == 1 })
        assertEquals(1, failures)
        assertEquals(1, finished)
    }

    @Test fun `callback can override only click or no methods at all`() {
        var clicks = 0
        val callback = object : AdCallback { override fun onAdClicked() { clicks++ } }
        callback.onAdLoaded()
        callback.onAdFailed("ignored")
        callback.onAdClicked()
        assertEquals(1, clicks)
        val empty = object : AdCallback {}
        empty.onAdLoaded()
        empty.onAdClicked()
        empty.onAdFailed("ignored")
    }

    @Test fun `empty provider list fails without accessing a first element`() {
        var failure: String? = null
        AdLoadSequence(0, { error("No provider should be created") }, { _, _, _ -> }, object : AdCallback {
            override fun onAdFailed(error: String) { failure = error }
        }).start()
        assertNotNull(failure)
    }
}
