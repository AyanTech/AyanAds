package ir.ayantech.ayanadmanager.model

import android.app.Activity
import android.view.ViewGroup
import com.google.android.gms.ads.AdSize
import ir.ayantech.ayanadmanager.core.AdCallback
import ir.ayantech.ayanadmanager.model.api.AddStatisticsInputParameters
import ir.ayantech.ayanadmanager.networks.hamrahAds.components.NativeAdAttributes
import ir.ayantech.ayanadmanager.utils.ContainerType
import ir.ayantech.ayanadmanager.utils.constant.AdSource
import ir.ayantech.hamrahads.domain.enums.HamrahAdsBannerType

class AdRequestConfigBuilder(
    private val containerType: ContainerType,
    private val activity: Activity,
    private val addStatisticsInput: AddStatisticsInputParameters,
    private val callback: AdCallback
) {

    private var hamrahAdSize: HamrahAdsBannerType? = null
    private var googleAdSize: AdSize = AdSize.BANNER
    private var viewGroup: ViewGroup? = null
    private var nativeAdAttributes: NativeAdAttributes = NativeAdAttributes()
    private var useDefaultNativeAdView: Boolean = false
    private var adUnitId: String? = null

    fun setHamrahAdConfig(
        adSize: HamrahAdsBannerType?,
        viewGroup: ViewGroup?,
        nativeAdAttributes: NativeAdAttributes,
        useDefaultNativeAdView: Boolean
    ): AdRequestConfigBuilder {
        this.hamrahAdSize = adSize
        this.viewGroup = viewGroup
        this.nativeAdAttributes = nativeAdAttributes
        this.useDefaultNativeAdView = useDefaultNativeAdView
        return this
    }

    fun setAdMobConfig(adUnitId: String, viewGroup: ViewGroup?): AdRequestConfigBuilder {
        this.viewGroup = viewGroup
        this.adUnitId = adUnitId
        return this
    }

    fun build(adSource: AdSource): AdRequestConfig {
        return when (adSource) {
            AdSource.HamrahAd -> HamrahAdConfig(
                adSize = hamrahAdSize,
                viewGroup = viewGroup,
                nativeAdAttributes = nativeAdAttributes,
                useDefaultNativeAdView = useDefaultNativeAdView,
                containerType = containerType,
                activity = activity,
                addStatisticsInput = addStatisticsInput,
                callback = callback
            )

            AdSource.AdMob -> AdMobConfig(
                containerType = containerType,
                activity = activity,
                viewGroup = viewGroup,
                addStatisticsInput = addStatisticsInput,
                adUnitId = adUnitId ?: "",
                callback = callback,
                adSize = googleAdSize,
                useDefaultNativeAdView = useDefaultNativeAdView
            )

        }
    }

}