package ir.ayantech.ayanadmanager.core

import ir.ayantech.ayanadmanager.core.AyanAdManager.appKey
import ir.ayantech.ayanadmanager.core.AyanAdManager.ayanAdApi
import ir.ayantech.ayanadmanager.core.AyanAdManager.clickTracker
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
            Logger.d("getConfig: $res")
            onSuccess.invoke(res)
        }
        failure {
            onFailed.invoke(it)
            Logger.e("getConfig: ${it.failureMessage}")
        }
    }
}

fun sendStatistics(input: AddStatisticsInputParameters) {
    ayanAdApi.apply {
        headers = hashMapOf(APP_KEY_HEADER to appKey)
        call<AddStatisticsOutPutParameters>(
            endPoint = EndPoint.ADD_STATISTICS,
            input = input,
        ) {
            success {
                Logger.d("sendStatistics: $it")
                clickTracker = it?.ClickTracker ?: ""
            }
            failure {
                Logger.e("sendStatistics: ${it.failureMessage}")
            }
        }
    }
}

fun submitClick() {
    ayanAdApi.apply {
        headers = hashMapOf(APP_KEY_HEADER to appKey)
        call<TrackStatisticsOutputParameters>(
            endPoint = EndPoint.TRACK_STATISTICS,
            input = TrackStatisticsInputParameters(clickTracker),
        ) {
            success {
                Logger.d("submitClick: $it")
            }
            failure {
                Logger.e("submitClick: ${it.failureMessage}")
            }
        }
    }
}