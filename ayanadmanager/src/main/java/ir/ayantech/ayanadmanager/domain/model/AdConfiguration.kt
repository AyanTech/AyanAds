package ir.ayantech.ayanadmanager.domain.model

import ir.ayantech.ayanadmanager.utils.ContainerType
import ir.ayantech.ayanadmanager.utils.constant.AdSource

// Domain models are independent of the transport and serialization libraries.
data class AdConfiguration(
    val name: String,
    val providers: List<ProviderPriority>,
    val adUnits: List<AdUnit>,
)

data class ProviderPriority(val adSource: AdSource, val appId: String?, val sharePercent: Long)

data class AdUnit(
    val containerKey: String,
    val containerType: ContainerType,
    val adSource: AdSource,
    val adUnitId: String,
)

data class AdStatistics(
    val containerKey: String?,
    val adUnitId: String?,
    val adSource: String?,
    val appMarket: String,
    val appVersion: Long,
    val failureCause: String?,
    val sdkVersion: String,
    val osName: String,
    val osVersion: Int,
)

class AdsException(message: String, cause: Throwable? = null) : Exception(message, cause)
