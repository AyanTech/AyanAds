package ir.ayantech.ayanadmanager.core

import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import ir.ayantech.ayanadmanager.core.AyanAdManager.appMarket
import ir.ayantech.ayanadmanager.model.AdRequestConfigBuilder
import ir.ayantech.ayanadmanager.model.api.AdUnit
import ir.ayantech.ayanadmanager.model.api.AddStatisticsInputParameters
import ir.ayantech.ayanadmanager.networks.admob.AdmobProvider
import ir.ayantech.ayanadmanager.networks.hamrahAds.HamrahAdProvider
import ir.ayantech.ayanadmanager.networks.hamrahAds.components.NativeAdAttributes
import ir.ayantech.ayanadmanager.utils.AdSizeType
import ir.ayantech.ayanadmanager.utils.ContainerType
import ir.ayantech.ayanadmanager.utils.Logger
import ir.ayantech.ayanadmanager.utils.constant.AdSource
import ir.ayantech.ayanadmanager.utils.constant.Config.PLATFORM
import ir.ayantech.ayanadmanager.utils.getAppVersion
import ir.ayantech.ayanadmanager.utils.getOsVersion


class AdProviderManager {

    /**
     * Initiates the process of loading and showing ads using the available providers.
     *
     * @param adUnits The list of available ad units.
     * @param callback The callback to handle ad loading events.
     * @param appCompatActivity, The activity for ad operations.
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
        appCompatActivity: AppCompatActivity,
        viewGroup: ViewGroup? = null,
        nativeAdAttributes: NativeAdAttributes,
        useDefaultNativeAdView: Boolean,
        adSize: AdSizeType?
    ) {
        tryNextProvider(
            containerKey = containerKey,
            adUnits = adUnits,
            index = 0,
            containerType = adUnits.first().ContainerType,
            callback = callback,
            appCompatActivity = appCompatActivity,
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
        appCompatActivity: AppCompatActivity,
        viewGroup: ViewGroup?,
        nativeAdAttributes: NativeAdAttributes,
        useDefaultNativeAdView: Boolean,
        adSize: AdSizeType?,
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
                AppVersion = getAppVersion(appCompatActivity),
                FailureCause = null,
                OsName = PLATFORM,
                OsVersion = getOsVersion()
            )

            val provider = when (it.AdSource) {
                AdSource.AdMob -> AdmobProvider()
                AdSource.HamrahAd -> HamrahAdProvider()
            }

            AyanAdManager.adProvider = provider

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
                        appCompatActivity = appCompatActivity,
                        viewGroup = viewGroup,
                        nativeAdAttributes = nativeAdAttributes,
                        useDefaultNativeAdView = useDefaultNativeAdView,
                        adSize = adSize
                    )
                }
            }

            val adConfig =
                AdRequestConfigBuilder(containerType, appCompatActivity, addStatistics, callbackObj).apply {
                    when (it.AdSource) {
                        AdSource.AdMob -> setAdMobConfig(
                            it.AdUnitId,
                            viewGroup,
                            useDefaultNativeAdView,
                            nativeAdAttributes,
                        )

                        AdSource.HamrahAd -> setHamrahAdConfig(
                            adSize,
                            viewGroup,
                            nativeAdAttributes,
                            useDefaultNativeAdView
                        )
                    }
                }.build(it.AdSource)

            provider.loadAd(adConfig)
        }

    }
}

