package ir.ayantech.ayanadmanager.model.api

import ir.ayantech.ayanadmanager.BuildConfig
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AddStatisticsOutPutParameters(@SerialName("ClickTracker") val clickTracker: String?)

@Serializable
data class AddStatisticsInputParameters(
    @SerialName("ContainerKey") val containerKey: String?,
    @SerialName("AdUnitId") val adUnitId: String?,
    @SerialName("AdSource") val adSource: String?,
    @SerialName("AppMarket") val appMarket: String,
    @SerialName("AppVersion") val appVersion: Long,
    @SerialName("FailureCause") var failureCause: String?,
    @SerialName("SdkVersion") val sdkVersion: String = BuildConfig.SDK_VERSION,
    @SerialName("OsName") val osName: String,
    @SerialName("OsVersion") val osVersion: Int,
)
