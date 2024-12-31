package ir.ayantech.ayanadmanager.model.api

import ir.ayantech.ayanadmanager.utils.ContainerType
import ir.ayantech.ayanadmanager.utils.constant.AdSource


data class GetConfigInputParameters(val AppKey: String)

data class GetConfigOutputParameters(
    val Name: String,
    val AdSourcePriority: List<AdSourcePriority>,
    val AdUnits: List<AdUnit>
)

data class AdSourcePriority(
    val AdSource: AdSource,
    val AppId: String,
    val SharePercent: Long,
)

data class AdUnit(
    val ContainerKey: String,
    val ContainerType: ContainerType,
    val AdSource: AdSource,
    val AdUnitId: String
)

