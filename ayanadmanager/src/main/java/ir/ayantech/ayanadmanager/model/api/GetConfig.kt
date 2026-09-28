package ir.ayantech.ayanadmanager.model.api

import ir.ayantech.ayanadmanager.utils.ContainerType
import ir.ayantech.ayanadmanager.utils.constant.AdSource
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class GetConfigAdInputParameters(@SerialName("AppKey") val appKey: String)

@Serializable
data class GetConfigAdOutputParameters(
    @SerialName("Name") val name: String,
    @SerialName("AdSourcePriority") val adSourcePriority: List<AdSourcePriority>,
    @SerialName("AdUnits") val adUnits: List<AdUnit>
)

@Serializable
data class AdSourcePriority(
    @SerialName("AdSource") val adSource: AdSource,
    @SerialName("AppId") val appId: String?,
    @SerialName("SharePercent") val sharePercent: Long,
)

@Serializable
data class AdUnit(
    @SerialName("ContainerKey") val containerKey: String,
    @SerialName("ContainerType") val containerType: ContainerType,
    @SerialName("AdSource") val adSource: AdSource,
    @SerialName("AdUnitId") val adUnitId: String
)
