package com.ayanco.ayanads.core

import android.content.Context
import android.view.ViewGroup
import com.ayanco.ayanads.core.AyanAdManager.appMarket
import ir.ayantech.ayanadmanager.model.api.AdUnit
import ir.ayantech.ayanadmanager.model.api.AddStatisticsInputParameters
import ir.ayantech.ayanadmanager.networks.adivery.AdiveryProvider
import ir.ayantech.ayanadmanager.networks.admob.AdmobProvider
import ir.ayantech.ayanadmanager.networks.hamrahAds.HamrahAdProvider
import ir.ayantech.ayanadmanager.networks.hamrahAds.components.NativeAdAttributes
import ir.ayantech.ayanadmanager.networks.tapsell.TapsellProvider
import ir.ayantech.ayanadmanager.utils.ContainerType
import ir.ayantech.ayanadmanager.utils.constant.AdSource
import ir.ayantech.ayanadmanager.utils.constant.Config.Platform
import ir.ayantech.ayanadmanager.utils.getAppVersion
import ir.ayantech.ayanadmanager.utils.getOsVersion
import ir.ayantech.hamrahads.domain.enums.HamrahAdsBannerType


class AdProviderManager {

    /**
     * Initiates the process of loading and showing ads using the available providers.
     *
     * @param adUnits The list of available ad units.
     * @param callback The callback to handle ad loading events.
     * @param context The context for ad operations.
     * @param containerKey The key of containerAd.
     * @param viewGroup The view group to display the ad.
     * @param nativeAdAttributes Attributes like (textSize ,...) to customize DefaultNativeAd views.
     * @param useDefaultNativeAdView Default value is False, use it when needed.
     * @param adSize The size of the ad (optional).
     */
    fun loadAndShowAd(
        containerKey: String,
        adUnits: List<AdUnit>,
        callback: AdCallback,
        context: Context,
        viewGroup: ViewGroup? = null,
        nativeAdAttributes: NativeAdAttributes,
        useDefaultNativeAdView: Boolean,
        adSize: HamrahAdsBannerType?
    ) {
        tryNextProvider(
            containerKey = containerKey,
            adUnits = adUnits,
            index = 0,
            containerType = adUnits.first().ContainerType,
            callback = callback,
            context = context,
            viewGroup = viewGroup,
            nativeAdAttributes = nativeAdAttributes,
            useDefaultNativeAdView = useDefaultNativeAdView,
            adSize = adSize
        )
    }

    /**
     * Tries to load ads using the next available provider in the list.
     */
    private fun tryNextProvider(
        containerKey: String,
        adUnits: List<AdUnit>,
        index: Int,
        containerType: ContainerType,
        callback: AdCallback,
        context: Context,
        viewGroup: ViewGroup?,
        nativeAdAttributes: NativeAdAttributes,
        useDefaultNativeAdView: Boolean,
        adSize: HamrahAdsBannerType?,
    ) {

        if (index >= adUnits.size) {
            callback.onAdFailed("No available ad providers for $containerType")
            return
        }

        adUnits[index].let {
            val addStatistics = AddStatisticsInputParameters(
                ContainerKey = it.ContainerKey,
                AdSource = it.AdSource.name,
                AdUnitId = it.AdUnitId,
                AppMarket = appMarket.value,
                AppVersion = getAppVersion(context),
                FailureCause = null,
                OsName = Platform,
                OsVersion = getOsVersion()
            )

            val provider = when (it.AdSource) {
                AdSource.Adivery -> AdiveryProvider()
                AdSource.AdMob -> AdmobProvider()
                AdSource.HamrahAd -> HamrahAdProvider()
                AdSource.Tapsell -> TapsellProvider()
            }

            provider.loadAd(
                containerType = containerType,
                context = context,
                addStatisticsInput = addStatistics,
                viewGroup = viewGroup,
                adSize = adSize,
                nativeAdAttributes = nativeAdAttributes,
                useDefaultNativeAdView = useDefaultNativeAdView,
                callback = object : AdCallback {
                    override fun onAdLoaded() {
                        callback.onAdLoaded()
                    }

                    override fun onAdFailed(error: String) {
                        // Try the next provider if this one fails
                        tryNextProvider(
                            containerKey = containerKey,
                            adUnits = adUnits,
                            index = index + 1,
                            containerType = containerType,
                            callback = callback,
                            context = context,
                            viewGroup = viewGroup,
                            nativeAdAttributes = nativeAdAttributes,
                            useDefaultNativeAdView = useDefaultNativeAdView,
                            adSize = adSize
                        )
                    }
                })
        }

    }
}

