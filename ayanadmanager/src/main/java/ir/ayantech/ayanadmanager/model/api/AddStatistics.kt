package ir.ayantech.ayanadmanager.model.api

import ir.ayantech.ayanadmanager.BuildConfig

data class AddStatisticsOutPutParameters(val ClickTracker: String?, val SdkVersion: String = BuildConfig.SDK_VERSION)
data class AddStatisticsInputParameters(
    val ContainerKey: String?,
    val AdUnitId: String?,
    val AdSource: String?,
    val AppMarket: String,
    val AppVersion: Long,
    var FailureCause: String?,
    val OsName: String,
    val OsVersion: Int,
)