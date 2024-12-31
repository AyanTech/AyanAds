package ir.ayantech.ayanadmanager.model.api

data class AddStatisticsOutPutParameters(val ClickTracker: String)
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