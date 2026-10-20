package ir.ayantech.ayanadmanager.ui

import ir.ayantech.ayanadmanager.domain.model.AdConfiguration
import ir.ayantech.ayanadmanager.domain.model.AdStatistics
import ir.ayantech.ayanadmanager.domain.usecase.LoadAdConfiguration
import ir.ayantech.ayanadmanager.domain.usecase.RecordAdStatistics
import ir.ayantech.ayanadmanager.domain.usecase.TrackAdClick
import ir.ayantech.ayanadmanager.utils.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal sealed interface AdsUiState {
    data object Idle : AdsUiState
    data object Loading : AdsUiState
    data class Ready(val configuration: AdConfiguration) : AdsUiState
    data class Failed(val message: String) : AdsUiState
}

/** SDK UI state holder; owns tracking jobs, but never retains an Activity. */
internal class AdsSession(
    private val loadConfiguration: LoadAdConfiguration,
    private val recordStatistics: RecordAdStatistics,
    private val trackClick: TrackAdClick,
    private val closeClient: () -> Unit = {},
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
) {
    private val mutableState = MutableStateFlow<AdsUiState>(AdsUiState.Idle)
    val state = mutableState.asStateFlow()
    private class TrackingEntry(var tracker: String? = null, var pending: Job? = null)
    private val tracking = LinkedHashMap<String, TrackingEntry>()
    @Volatile private var closed = false

    suspend fun load(appKey: String): AdConfiguration {
        mutableState.value = AdsUiState.Loading
        try {
            return loadConfiguration(appKey).also { mutableState.value = AdsUiState.Ready(it) }
        } catch (cancelled: CancellationException) {
            mutableState.value = AdsUiState.Idle
            throw cancelled
        } catch (error: Exception) {
            mutableState.value = AdsUiState.Failed(error.message ?: "Unable to load ads.")
            throw error
        }
    }

    fun record(statistics: AdStatistics, trackingId: String? = statistics.adUnitId) {
        if (closed) return
        val key = trackingId?.takeIf(String::isNotBlank)
        val entry = TrackingEntry(key?.let { tracking[it]?.tracker })
        if (key != null) {
            tracking.remove(key)
            tracking[key] = entry
            while (tracking.size > 256) tracking.remove(tracking.keys.first())
        }
        val pending = scope.launch(start = CoroutineStart.LAZY) {
            try {
                recordStatistics(statistics)?.takeIf(String::isNotBlank)?.let { entry.tracker = it }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Logger.e("sendStatistics failure: ${error.message}")
            }
        }
        entry.pending = pending
        pending.start()
    }

    fun click(trackingId: String?) {
        if (closed || trackingId.isNullOrBlank()) return
        // Snapshot the display identity before suspension. Eviction/disposal must not redirect
        // an already observed click to the tracker of a newer ad using the same unit.
        val entry = tracking[trackingId] ?: return
        scope.launch {
            entry.pending?.join()
            val tracker = entry.tracker ?: return@launch
            try {
                trackClick(tracker)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Logger.e("submitClick failure: ${error.message}")
            }
        }
    }

    fun release(trackingId: String) { tracking.remove(trackingId) }

    fun close() {
        if (closed) return
        closed = true
        scope.cancel()
        tracking.clear()
        closeClient()
    }
}
