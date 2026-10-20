package ir.ayantech.ayanadmanager.core

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.R as AppCompatR
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ScreenAdSessionTest {
    private class Owner : LifecycleOwner {
        override val lifecycle = LifecycleRegistry(this)
    }
    private class Ad : PreparedInlineAd {
        var binds = 0
        var unbinds = 0
        var closes = 0
        override fun bind(container: ViewGroup, callback: AdCallback): AdHandle {
            binds++
            return DisposableAdHandle().apply { action = { unbinds++ } }
        }
        override fun close() { closes++ }
    }

    @Test fun `rebind and temporary detach reuse the same loaded ad`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val activity = Robolectric.buildActivity(AppCompatActivity::class.java).apply {
            get().setTheme(AppCompatR.style.Theme_AppCompat); setup()
        }
        val ad = Ad()
        var requests = 0
        val session = ScreenAdSession(activity.get(), activity.get()) { requests++; ad }
        try {
            val slot = AdSlot("row-1", "same-zone")
            repeat(5) { session.prepare(listOf(slot)) }
            runCurrent()
            val container = FrameLayout(activity.get())
            val binding = session.bind(slot.id, container)
            binding.attach()
            binding.detach()
            binding.attach()
            assertSame(binding, session.bind(slot.id, container))
            assertEquals(1, requests)
            assertEquals(1, ad.binds)
            binding.dispose()
            assertEquals(1, ad.unbinds)
            assertEquals(0, ad.closes)
            session.bind(slot.id, FrameLayout(activity.get())).attach()
            assertEquals(2, ad.binds)
        } finally {
            session.close()
            assertEquals(1, ad.closes)
            activity.pause().stop().destroy()
            Dispatchers.resetMain()
        }
    }

    @Test fun `recycled target ignores a pending load and view destruction closes session`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val activity = Robolectric.buildActivity(AppCompatActivity::class.java).apply {
            get().setTheme(AppCompatR.style.Theme_AppCompat); setup()
        }
        val owner = Owner().apply { lifecycle.currentState = Lifecycle.State.STARTED }
        val pending = CompletableDeferred<PreparedInlineAd>()
        val ad = Ad()
        val session = ScreenAdSession(activity.get(), owner) { pending.await() }
        try {
            session.prepare(listOf(AdSlot("row", "zone")))
            session.bind("row", FrameLayout(activity.get())).apply { attach(); dispose() }
            runCurrent()
            pending.complete(ad)
            runCurrent()
            assertEquals(0, ad.binds)
            owner.lifecycle.currentState = Lifecycle.State.DESTROYED
            assertFalse(activity.get().isDestroyed)
            assertEquals(1, ad.closes)
            assertTrue(session.states.value.isEmpty())
        } finally {
            session.close()
            activity.pause().stop().destroy()
            Dispatchers.resetMain()
        }
    }

    @Test fun `working set removes queued loads and same-zone slots own different ads`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val activity = Robolectric.buildActivity(AppCompatActivity::class.java).apply {
            get().setTheme(AppCompatR.style.Theme_AppCompat); setup()
        }
        val pending = CompletableDeferred<PreparedInlineAd>()
        val started = mutableListOf<String>()
        val session = ScreenAdSession(activity.get(), activity.get(), maxConcurrentLoads = 1) {
            started += it.id
            if (it.id == "first") pending.await() else Ad()
        }
        try {
            session.prepare(listOf(AdSlot("first", "zone"), AdSlot("queued", "zone")))
            runCurrent()
            assertEquals(listOf("first"), started)
            session.prepare(listOf(AdSlot("replacement", "zone")))
            runCurrent()
            assertEquals(listOf("first", "replacement"), started)
            assertEquals(setOf("replacement"), session.states.value.keys)
        } finally {
            session.close()
            activity.pause().stop().destroy()
            Dispatchers.resetMain()
        }
    }
}
