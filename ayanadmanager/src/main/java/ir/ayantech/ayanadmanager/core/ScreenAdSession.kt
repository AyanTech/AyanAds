package ir.ayantech.ayanadmanager.core

import android.view.View
import android.view.ViewGroup
import androidx.annotation.MainThread
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import ir.ayantech.ayanadmanager.networks.hamrahAds.components.NativeAdAttributes
import ir.ayantech.ayanadmanager.utils.AdSizeType
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/** Stable logical identity, independent of adapter position. Supports native and banner placements. */
data class AdSlot(
    val id: String,
    val containerKey: String,
    val adSize: AdSizeType? = null,
    val nativeAdAttributes: NativeAdAttributes = NativeAdAttributes(),
)

sealed interface AdSlotState {
    data object Loading : AdSlotState
    data object Ready : AdSlotState
    data class Failed(val message: String) : AdSlotState
}

interface AdBinding : AdHandle {
    fun attach()
    fun detach()
}

/**
 * Create in onViewCreated with viewLifecycleOwner. prepare() defines a small working set;
 * bind() only renders that state and never starts an ad request. All methods run on Main.
 */
@MainThread
class ScreenAdSession internal constructor(
    activity: AppCompatActivity,
    lifecycleOwner: LifecycleOwner,
    private val maxSlots: Int = 4,
    maxConcurrentLoads: Int = 2,
    private val lifetime: Duration = 5.minutes,
    private val onClosed: (ScreenAdSession) -> Unit = {},
    prepareAd: suspend (AdSlot) -> PreparedInlineAd,
) : AutoCloseable, DefaultLifecycleObserver {
    private var activity: AppCompatActivity? = activity
    private var owner: LifecycleOwner? = lifecycleOwner
    private var loader: (suspend (AdSlot) -> PreparedInlineAd)? = prepareAd
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val permits = Semaphore(maxConcurrentLoads)
    private val entries = linkedMapOf<String, Entry>()
    private val bindings = mutableMapOf<ViewGroup, Binding>()
    private val mutableStates = MutableStateFlow<Map<String, AdSlotState>>(emptyMap())
    val states = mutableStates.asStateFlow()
    private var closed = false

    private class Entry(val slot: AdSlot) {
        var job: Job? = null
        var ad: PreparedInlineAd? = null
        var binding: Binding? = null
        var readyAt = 0L
        var state: AdSlotState = AdSlotState.Loading
    }

    init {
        require(maxSlots > 0 && maxConcurrentLoads > 0 && lifetime > Duration.ZERO)
        if (lifecycleOwner.lifecycle.currentState == Lifecycle.State.DESTROYED || activity.isDestroyed) {
            close()
        } else {
            lifecycleOwner.lifecycle.addObserver(this)
            activity.lifecycle.addObserver(this)
        }
    }

    fun prepare(slots: List<AdSlot>) {
        if (closed) return
        require(slots.size <= maxSlots) { "Prepare at most $maxSlots visible/nearby ad slots." }
        require(slots.map { it.id }.distinct().size == slots.size) { "Ad slot IDs must be unique." }
        require(slots.all { it.id.isNotBlank() && it.containerKey.isNotBlank() })
        val desired = slots.associateBy { it.id }
        entries.values.toList().forEach { entry ->
            if (desired[entry.slot.id] != entry.slot ||
                (entry.ad != null && (now() - entry.readyAt).milliseconds >= lifetime)) {
                remove(entry)
            }
        }
        slots.forEach { slot ->
            if (slot.id !in entries) start(slot)
        }
        publish()
    }

    /** Explicit retry prevents repeated binds/viewport updates from creating request storms. */
    fun retry(slotId: String) {
        val entry = entries[slotId] ?: return
        if (entry.state is AdSlotState.Failed) {
            val slot = entry.slot
            remove(entry)
            start(slot)
        }
    }

    private fun start(slot: AdSlot) {
        if (closed) return
        val entry = Entry(slot)
        entries[slot.id] = entry
        publish()
        entry.job = scope.launch {
            try {
                val ad = permits.withPermit { checkNotNull(loader).invoke(slot) }
                if (closed || entries[slot.id] !== entry) {
                    ad.close()
                    return@launch
                }
                entry.ad = ad
                entry.readyAt = now()
                entry.state = AdSlotState.Ready
                publish()
                entry.binding?.render()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (closed || entries[slot.id] !== entry) return@launch
                entry.state = AdSlotState.Failed(error.message ?: "Unable to prepare ad.")
                publish()
                entry.binding?.reportFailure((entry.state as AdSlotState.Failed).message)
            }
        }
    }

    fun bind(slotId: String, container: ViewGroup, callback: AdCallback = object : AdCallback {}): AdBinding {
        check(!closed) { "Ad session is closed." }
        val entry = checkNotNull(entries[slotId]) { "Call prepare() for this slot before binding." }
        bindings[container]?.takeIf { it.entry === entry }?.let { it.callback = callback; return it }
        bindings[container]?.dispose()
        entry.binding?.dispose()
        return Binding(entry, container, callback).also {
            bindings[container] = it
            entry.binding = it
            container.addOnAttachStateChangeListener(it)
            it.render()
        }
    }

    private inner class Binding(
        val entry: Entry,
        private var container: ViewGroup?,
        var callback: AdCallback?,
    ) : AdBinding, View.OnAttachStateChangeListener {
        private var display: AdHandle? = null
        private var attached = container?.isAttachedToWindow == true
        private var failed = false

        fun render() {
            val target = container ?: return
            if (entry.state is AdSlotState.Failed) {
                reportFailure((entry.state as AdSlotState.Failed).message)
                return
            }
            if (!attached || !isStarted()) return
            val ad = entry.ad ?: return
            if (display == null) {
                try {
                    val next = ad.bind(target, object : AdCallback {
                        override fun onAdLoaded() { if (container != null) callback?.onAdLoaded() }
                        override fun onAdClicked() { if (container != null) callback?.onAdClicked() }
                        override fun onAdFailed(error: String) = reportFailure(error)
                    })
                    if (container === target && entry.ad === ad) display = next else next.dispose()
                } catch (error: Exception) {
                    reportFailure(error.message ?: "Unable to display ad.")
                    return
                }
            }
            if (container != null) ad.setActive(true)
        }

        fun reportFailure(message: String) {
            if (!failed && container != null) {
                failed = true
                callback?.onAdFailed(message)
            }
        }

        override fun attach() { attached = true; render() }
        override fun detach() { attached = false; entry.ad?.setActive(false) }
        override fun onViewAttachedToWindow(v: View) = attach()
        override fun onViewDetachedFromWindow(v: View) = detach()
        override fun dispose() {
            val target = container ?: return
            container = null
            callback = null
            target.removeOnAttachStateChangeListener(this)
            if (bindings[target] === this) bindings.remove(target)
            if (entry.binding === this) entry.binding = null
            entry.ad?.setActive(false)
            display?.dispose()
            display = null
        }
    }

    private fun remove(entry: Entry) {
        entries.remove(entry.slot.id)
        entry.job?.cancel()
        entry.binding?.dispose()
        entry.ad?.close()
        entry.ad = null
    }

    private fun publish() { mutableStates.value = entries.mapValues { it.value.state } }
    private fun now() = System.nanoTime() / 1_000_000
    private fun isStarted() = owner?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.STARTED) == true
    override fun onStart(owner: LifecycleOwner) { bindings.values.toList().forEach { it.render() } }
    override fun onStop(owner: LifecycleOwner) { entries.values.forEach { it.ad?.setActive(false) } }
    override fun onDestroy(owner: LifecycleOwner) = close()
    override fun close() {
        if (closed) return
        closed = true
        scope.cancel()
        entries.values.toList().forEach(::remove)
        bindings.values.toList().forEach { it.dispose() }
        owner?.lifecycle?.removeObserver(this)
        activity?.lifecycle?.removeObserver(this)
        owner = null
        activity = null
        loader = null
        publish()
        onClosed(this)
    }
}
