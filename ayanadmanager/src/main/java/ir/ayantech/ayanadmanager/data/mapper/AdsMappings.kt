package ir.ayantech.ayanadmanager.data.mapper

import ir.ayantech.ayanadmanager.data.api.AddStatistics
import ir.ayantech.ayanadmanager.data.api.GetConfig
import ir.ayantech.ayanadmanager.domain.model.AdConfiguration
import ir.ayantech.ayanadmanager.domain.model.AdStatistics
import ir.ayantech.ayanadmanager.domain.model.AdUnit
import ir.ayantech.ayanadmanager.domain.model.AdsException
import ir.ayantech.ayanadmanager.domain.model.ProviderPriority
import ir.ayantech.ayanadmanager.utils.ContainerType
import ir.ayantech.ayanadmanager.utils.constant.AdSource
import ir.ayantech.networking.v2.api.AyanAPIResult
import ir.ayantech.networking.v2.helpers.Failure
import ir.ayantech.networking.v2.model.ApiCallStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

internal fun GetConfig.GetConfigResponseModel.toDomain() = AdConfiguration(
    name = name,
    providers = adSourcePriority.map {
        ProviderPriority(
            AdSource.valueOf(it.adSource),
            it.appId,
            it.sharePercent
        )
    },
    adUnits = adUnits.map {
        AdUnit(
            it.containerKey,
            ContainerType.valueOf(it.containerType),
            AdSource.valueOf(it.adSource),
            it.adUnitId
        )
    },
)

internal fun AdStatistics.toRequest() = AddStatistics.AddStatisticsRequestBody(
    containerKey,
    adUnitId,
    adSource,
    appMarket,
    appVersion,
    failureCause,
    sdkVersion,
    osName,
    osVersion,
)

/** Ignore progress events and consume exactly one terminal result. Cancellation propagates. */
internal suspend fun <T> Flow<AyanAPIResult<T, ApiCallStatus, Exception>>.awaitValue(): T =
    when (val result = first { it !is AyanAPIResult.ChangeState }) {
        is AyanAPIResult.Success -> result.value
        is AyanAPIResult.Error -> throw AdsException(
            (result.ayanFailure as? Failure)?.failureMessage
                ?: result.ayanFailure.message ?: "The ad service request failed.",
            result.ayanFailure,
        )

        is AyanAPIResult.ChangeState -> error("Expected a terminal result")
    }
