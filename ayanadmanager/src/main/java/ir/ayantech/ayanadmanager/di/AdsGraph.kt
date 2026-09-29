package ir.ayantech.ayanadmanager.di

import android.content.Context
import ir.ayantech.ayanadmanager.data.api.GetConfig
import ir.ayantech.ayanadmanager.data.api.TrackStatistics
import ir.ayantech.ayanadmanager.data.installAdsHeaders
import ir.ayantech.ayanadmanager.data.mapper.awaitValue
import ir.ayantech.ayanadmanager.data.mapper.toDomain
import ir.ayantech.ayanadmanager.data.mapper.toRequest
import ir.ayantech.ayanadmanager.domain.usecase.LoadAdConfigurationImpl
import ir.ayantech.ayanadmanager.domain.usecase.RecordAdStatisticsImpl
import ir.ayantech.ayanadmanager.domain.usecase.TrackAdClickImpl
import ir.ayantech.ayanadmanager.ui.AdsSession
import ir.ayantech.ayanadmanager.utils.constant.Config
import ir.ayantech.networking.ayanModel.LogLevel
import ir.ayantech.networking.datasource.impl.AdsRemoteDataSourceImpl
import ir.ayantech.networking.repository.impl.AdsRepositoryImpl
import ir.ayantech.networking.v2.AyanApi
import kotlin.time.Duration.Companion.seconds

internal fun createAdsSession(context: Context, appKey: String): AdsSession {
    val api = AyanApi.Builder(context.applicationContext, Config.AYAN_AD_BASE_URL)
        .setTimeOutDuration(Config.TIMEOUT.seconds)
        // Avoid logging app keys and tracking payloads in the SDK.
        .setLogLevel(LogLevel.LOG_ALL)
        .build()
    api.apiCall.httpClient.installAdsHeaders(appKey)
    val repository = AdsRepositoryImpl(AdsRemoteDataSourceImpl(api))
    return AdsSession(
        loadConfiguration = LoadAdConfigurationImpl { key ->
            repository.getConfig(GetConfig.GetConfigRequestBody(key)).awaitValue().toDomain()
        },
        recordStatistics = RecordAdStatisticsImpl { statistics ->
            repository.addStatistics(statistics.toRequest()).awaitValue().clickTracker
        },
        trackClick = TrackAdClickImpl { tracker ->
            repository.trackStatistics(TrackStatistics.TrackStatisticsRequestBody(tracker))
                .awaitValue()
        },
        closeClient = { api.apiCall.httpClient.close() },
    )
}
