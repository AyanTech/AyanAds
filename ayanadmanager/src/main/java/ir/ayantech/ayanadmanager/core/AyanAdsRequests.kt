package ir.ayantech.ayanadmanager.core

import ir.ayantech.ayanadmanager.domain.model.AdStatistics
import ir.ayantech.ayanadmanager.model.api.AddStatisticsInputParameters

/** Compatibility entry points for the ad-provider callbacks; networking lives in the generated data layer. */
fun sendStatistics(input: AddStatisticsInputParameters) {
    // Snapshot mutable provider state before launching an asynchronous request.
    AyanAdManager.session?.record(
        AdStatistics(
            input.containerKey, input.adUnitId, input.adSource, input.appMarket,
            input.appVersion, input.failureCause, input.sdkVersion, input.osName, input.osVersion,
        )
    )
}

fun submitClick(adUnitId: String?) {
    AyanAdManager.session?.click(adUnitId)
}
