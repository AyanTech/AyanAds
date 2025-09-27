package ir.ayantech.ayanadmanager.core

import ir.ayantech.ayanadmanager.core.AyanAdManager.appKey
import ir.ayantech.ayanadmanager.core.AyanAdManager.ayanAdApi
import ir.ayantech.ayanadmanager.core.AyanAdManager.clickTrackers
import ir.ayantech.ayanadmanager.model.api.AddStatisticsInputParameters
import ir.ayantech.ayanadmanager.model.api.AddStatisticsOutPutParameters
import ir.ayantech.ayanadmanager.model.api.GetConfigAdInputParameters
import ir.ayantech.ayanadmanager.model.api.GetConfigAdOutputParameters
import ir.ayantech.ayanadmanager.model.api.TrackStatisticsInputParameters
import ir.ayantech.ayanadmanager.model.api.TrackStatisticsOutputParameters
import ir.ayantech.ayanadmanager.utils.Logger
import ir.ayantech.ayanadmanager.utils.constant.Config.APP_KEY_HEADER
import ir.ayantech.ayanadmanager.utils.constant.EndPoint
import ir.ayantech.ayannetworking.ayanModel.Failure


fun getConfig(
    appKey: String,
    onSuccess: (GetConfigAdOutputParameters?) -> Unit,
    onFailed: (Failure) -> Unit
) {
    ayanAdApi.call<GetConfigAdOutputParameters>(
        endPoint = EndPoint.GET_CONFIG,
        input = GetConfigAdInputParameters(appKey),
    ) {
        success { res ->
            onSuccess.invoke(res)
            Logger.d("getConfig success: $res")
        }
        failure {
            onFailed.invoke(it)
            Logger.e("getConfig failure: ${it.failureMessage}")
        }
    }
}

fun sendStatistics(input: AddStatisticsInputParameters) {

    val adUnitId = input.AdUnitId?.takeIf { it.isNotBlank() }
    with(ayanAdApi) {
        headers = hashMapOf(APP_KEY_HEADER to appKey)
        call<AddStatisticsOutPutParameters>(
            endPoint = EndPoint.ADD_STATISTICS,
            input = input,
        ) {
            success { response ->
                Logger.d("sendStatistics success: $response")

                val clickTracker = response?.ClickTracker?.takeIf { it.isNotBlank() }

                if (adUnitId != null && clickTracker != null) {
                    clickTrackers[adUnitId] = clickTracker
                } else {
                    Logger.w(
                        "sendStatistics: skip updating clickTracker (adUnitId=$adUnitId, tracker=${response?.ClickTracker})"
                    )
                }
            }
            failure { err -> Logger.e("sendStatistics failure: ${err.failureMessage}") }
        }
    }
}

fun submitClick(adUnitId: String?) {

    val tracker = adUnitId
        ?.takeIf { it.isNotBlank() }
        ?.let { id -> clickTrackers[id] }
        ?.takeIf { it.isNotBlank() }

    if (tracker == null) {
        Logger.w("submitClick: skipped (null/blank adUnitId or missing/blank tracker)")
        return
    }

    with(ayanAdApi) {
        headers = hashMapOf(APP_KEY_HEADER to appKey)
        call<TrackStatisticsOutputParameters>(
            endPoint = EndPoint.TRACK_STATISTICS,
            input = TrackStatisticsInputParameters(tracker),
        ) {
            success { Logger.d("submitClick success: $it") }
            failure { Logger.e("submitClick failure: ${it.failureMessage}") }
        }
    }

}