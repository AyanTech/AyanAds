package ir.ayantech.ayanadmanager.ui

import ir.ayantech.ayanadmanager.domain.model.AdConfiguration
import ir.ayantech.ayanadmanager.domain.model.AdStatistics
import ir.ayantech.ayanadmanager.domain.usecase.LoadAdConfiguration
import ir.ayantech.ayanadmanager.domain.usecase.RecordAdStatistics
import ir.ayantech.ayanadmanager.domain.usecase.TrackAdClick
import ir.ayantech.ayanadmanager.utils.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.job
import kotlinx.coroutines.Dispatchers
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
    private val clickTrackers = mutableMapOf<String, String>()
    private val pendingStatistics = mutableMapOf<String, Job>()
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

    fun record(statistics: AdStatistics) {
        if (closed) return
        scope.launch {
            val adUnitId = statistics.adUnitId?.takeIf(String::isNotBlank)
            val request = currentCoroutineContext().job
            if (adUnitId != null) pendingStatistics[adUnitId] = request
            try {
                val tracker = recordStatistics(statistics)?.takeIf(String::isNotBlank)
                // A slow older impression must not overwrite the tracker for a newer one.
                if (tracker != null && adUnitId != null && pendingStatistics[adUnitId] === request) {
                    clickTrackers[adUnitId] = tracker
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Logger.e("sendStatistics failure: ${error.message}")
            } finally {
                if (adUnitId != null && pendingStatistics[adUnitId] === request) {
                    pendingStatistics.remove(adUnitId)
                }
            }
        }
    }

    fun click(adUnitId: String?) {
        if (closed || adUnitId.isNullOrBlank()) return
        scope.launch {
            pendingStatistics[adUnitId]?.join()
            val tracker = clickTrackers[adUnitId] ?: return@launch
            try {
                trackClick(tracker)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Logger.e("submitClick failure: ${error.message}")
            }
        }
    }

    fun close() {
        if (closed) return
        closed = true
        scope.cancel()
        pendingStatistics.clear()
        clickTrackers.clear()
        closeClient()
    }
}
