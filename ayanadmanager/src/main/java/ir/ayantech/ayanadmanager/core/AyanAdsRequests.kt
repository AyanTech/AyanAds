package ir.ayantech.ayanadmanager.core

import ir.ayantech.ayanadmanager.core.AyanAdManager.appKey
import ir.ayantech.ayanadmanager.core.AyanAdManager.ayanAdApi
import ir.ayantech.ayanadmanager.core.AyanAdManager.clickTracker
import ir.ayantech.ayanadmanager.model.api.AddStatisticsInputParameters
import ir.ayantech.ayanadmanager.model.api.AddStatisticsOutPutParameters
import ir.ayantech.ayanadmanager.model.api.GetConfigInputParameters
import ir.ayantech.ayanadmanager.model.api.GetConfigOutputParameters
import ir.ayantech.ayanadmanager.model.api.TrackStatisticsInputParameters
import ir.ayantech.ayanadmanager.model.api.TrackStatisticsOutputParameters
import ir.ayantech.ayanadmanager.utils.Logger
import ir.ayantech.ayanadmanager.utils.constant.Config.AppKeyHeader
import ir.ayantech.ayanadmanager.utils.constant.EndPoint


fun getConfig(appKey: String, success: (GetConfigOutputParameters?) -> Unit) {
    ayanAdApi.call<GetConfigOutputParameters>(
        endPoint = EndPoint.getConfigs,
        input = GetConfigInputParameters(appKey),
    ) {
        success { res ->
            success.invoke(res)
        }
        failure {
            Logger.e("getConfig: ${it.failureMessage}")
        }
    }
}

fun sendStatistics(input: AddStatisticsInputParameters) {
    ayanAdApi.apply {
        headers = hashMapOf(AppKeyHeader to appKey)
        call<AddStatisticsOutPutParameters>(
            endPoint = EndPoint.addStatistics,
            input = input,
        ) {
            success {
                clickTracker = it?.ClickTracker ?: ""
            }
            failure {
                Logger.e("addStatistics: ${it.failureMessage}")
            }
        }
    }
}

fun submitClick() {
    ayanAdApi.apply {
        headers = hashMapOf(AppKeyHeader to appKey)
        call<TrackStatisticsOutputParameters>(
            endPoint = EndPoint.trackStatistics,
            input = TrackStatisticsInputParameters(clickTracker),
        ) {
            failure {
                Logger.e("submitClick: ${it.failureMessage}")
            }
        }
    }
}