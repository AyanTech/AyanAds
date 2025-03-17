package ir.ayantech.ayanadmanager.core

import android.app.Activity
import android.view.ViewGroup
import ir.ayantech.ayanadmanager.core.AyanAdManager.appMarket
import ir.ayantech.ayanadmanager.model.AdRequestConfigBuilder
import ir.ayantech.ayanadmanager.model.api.AdUnit
import ir.ayantech.ayanadmanager.model.api.AddStatisticsInputParameters
import ir.ayantech.ayanadmanager.networks.adivery.AdiveryProvider
import ir.ayantech.ayanadmanager.networks.admob.AdmobProvider
import ir.ayantech.ayanadmanager.networks.hamrahAds.HamrahAdProvider
import ir.ayantech.ayanadmanager.networks.hamrahAds.components.NativeAdAttributes
import ir.ayantech.ayanadmanager.networks.tapsell.TapsellProvider
import ir.ayantech.ayanadmanager.utils.ContainerType
import ir.ayantech.ayanadmanager.utils.Logger
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
     * @param activity The activity for ad operations.
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
        activity: Activity,
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
            activity = activity,
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
        activity: Activity,
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
                AppVersion = getAppVersion(activity),
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

            val callbackObj = object : AdCallback {
                override fun onAdLoaded() {
                    callback.onAdLoaded()
                }

                override fun onAdClicked() {
                    callback.onAdClicked()
                }

                override fun onAdFailed(error: String) {
                    Logger.e(error)
                    // Try the next provider if this one fails
                    tryNextProvider(
                        containerKey = containerKey,
                        adUnits = adUnits,
                        index = index + 1,
                        containerType = containerType,
                        callback = callback,
                        activity = activity,
                        viewGroup = viewGroup,
                        nativeAdAttributes = nativeAdAttributes,
                        useDefaultNativeAdView = useDefaultNativeAdView,
                        adSize = adSize
                    )
                }
            }

            val adConfig =
                AdRequestConfigBuilder(containerType, activity, addStatistics, callbackObj).apply {
                    when (it.AdSource) {
                        AdSource.Adivery -> {}
                        AdSource.AdMob -> setAdMobConfig(it.AdUnitId, viewGroup)
                        AdSource.HamrahAd -> setHamrahAdConfig(
                            adSize,
                            viewGroup,
                            nativeAdAttributes,
                            useDefaultNativeAdView
                        )

                        AdSource.Tapsell -> {}
                    }
                }.build(it.AdSource)

            provider.loadAd(adConfig)
        }

    }
}

