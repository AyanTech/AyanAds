package ir.ayantech.ayanadmanager.data.api

import com.alirezabdn.generator.AyanAPI
import ir.ayantech.ayanadmanager.utils.constant.EndPoint
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@AyanAPI(endpoint = EndPoint.GET_CONFIG, methodImplName = "getConfig", separationCategory = "Ads")
class GetConfig {
    @Serializable
    data class GetConfigRequestBody(@SerialName("AppKey") val appKey: String)

    @Serializable
    data class GetConfigResponseModel(
        @SerialName("Name") val name: String,
        @SerialName("AdSourcePriority") val adSourcePriority: List<AdSourcePriorityDto>,
        @SerialName("AdUnits") val adUnits: List<AdUnitDto>,
    )
}

@Serializable
data class AdSourcePriorityDto(
    @SerialName("AdSource") val adSource: String,
    @SerialName("AppId") val appId: String?,
    @SerialName("SharePercent") val sharePercent: Long,
)

@Serializable
data class AdUnitDto(
    @SerialName("ContainerKey") val containerKey: String,
    @SerialName("ContainerType") val containerType: String,
    @SerialName("AdSource") val adSource: String,
    @SerialName("AdUnitId") val adUnitId: String,
)

@AyanAPI(
    endpoint = EndPoint.ADD_STATISTICS,
    methodImplName = "addStatistics",
    separationCategory = "Ads"
)
class AddStatistics {
    @Serializable
    data class AddStatisticsRequestBody(
        @SerialName("ContainerKey") val containerKey: String?,
        @SerialName("AdUnitId") val adUnitId: String?,
        @SerialName("AdSource") val adSource: String?,
        @SerialName("AppMarket") val appMarket: String,
        @SerialName("AppVersion") val appVersion: Long,
        @SerialName("FailureCause") val failureCause: String?,
        @SerialName("SdkVersion") val sdkVersion: String,
        @SerialName("OsName") val osName: String,
        @SerialName("OsVersion") val osVersion: Int,
    )

    @Serializable
    data class AddStatisticsResponseModel(@SerialName("ClickTracker") val clickTracker: String? = null)
}

@AyanAPI(
    endpoint = EndPoint.TRACK_STATISTICS,
    methodImplName = "trackStatistics",
    separationCategory = "Ads"
)
class TrackStatistics {
    @Serializable
    data class TrackStatisticsRequestBody(@SerialName("ClickTracker") val clickTracker: String)

    @Serializable
    data class TrackStatisticsResponseModel(@SerialName("Status") val status: StatusDto? = null)
}

@Serializable
data class StatusDto(
    @SerialName("Code") val code: String,
    @SerialName("Description") val description: String? = null,
)
