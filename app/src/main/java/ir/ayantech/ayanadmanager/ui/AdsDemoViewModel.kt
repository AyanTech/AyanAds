package ir.ayantech.ayanadmanager.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AdsDemoUiState(val ready: Boolean = false, val message: String = "Initializing ads…")

/** Keeps rendering state across Activity recreation without retaining an Activity. */
class AdsDemoViewModel : ViewModel() {
    private val mutableState = MutableStateFlow(AdsDemoUiState())
    val state = mutableState.asStateFlow()

    fun onInitializing() { mutableState.value = AdsDemoUiState() }
    fun onInitialized() { mutableState.value = AdsDemoUiState(ready = true, message = "Ads ready") }
    fun onInitializationFailed(message: String) {
        mutableState.value = AdsDemoUiState(message = message)
    }
}
