package ir.ayantech.ayanadmanager.model

import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import ir.ayantech.ayanadmanager.core.AdCallback
import ir.ayantech.ayanadmanager.model.api.AddStatisticsInputParameters
import ir.ayantech.ayanadmanager.networks.hamrahAds.components.NativeAdAttributes
import ir.ayantech.ayanadmanager.utils.AdSizeType
import ir.ayantech.ayanadmanager.utils.ContainerType
import ir.ayantech.ayanadmanager.utils.constant.AdSource

class AdRequestConfigBuilder(
    private val containerType: ContainerType,
    private val appCompatActivity: AppCompatActivity,
    private val addStatisticsInput: AddStatisticsInputParameters,
    private val callback: AdCallback
) {

    private var adSize: AdSizeType? = null
    private var viewGroup: ViewGroup? = null
    private var nativeAdAttributes: NativeAdAttributes = NativeAdAttributes()
    private var useDefaultNativeAdView: Boolean = false
    private var adUnitId: String? = null

    fun setHamrahAdConfig(
        adSize: AdSizeType?,
        viewGroup: ViewGroup?,
        nativeAdAttributes: NativeAdAttributes,
        useDefaultNativeAdView: Boolean
    ): AdRequestConfigBuilder {
        this.adSize = adSize
        this.viewGroup = viewGroup
        this.nativeAdAttributes = nativeAdAttributes
        this.useDefaultNativeAdView = useDefaultNativeAdView
        return this
    }

    fun setAdMobConfig(
        adUnitId: String,
        viewGroup: ViewGroup?,
        useDefaultNativeAdView: Boolean,
        nativeAdAttributes: NativeAdAttributes
    ): AdRequestConfigBuilder {
        this.viewGroup = viewGroup
        this.useDefaultNativeAdView = useDefaultNativeAdView
        this.nativeAdAttributes = nativeAdAttributes
        this.adUnitId = adUnitId
        return this
    }

    fun build(adSource: AdSource): AdRequestConfig {
        return when (adSource) {
            AdSource.HamrahAd -> HamrahAdConfig(
                adSize = adSize,
                viewGroup = viewGroup,
                nativeAdAttributes = nativeAdAttributes,
                useDefaultNativeAdView = useDefaultNativeAdView,
                containerType = containerType,
                appCompatActivity = appCompatActivity,
                addStatisticsInput = addStatisticsInput,
                callback = callback
            )

            AdSource.AdMob -> AdMobConfig(
                containerType = containerType,
                appCompatActivity = appCompatActivity,
                viewGroup = viewGroup,
                addStatisticsInput = addStatisticsInput,
                adUnitId = adUnitId ?: "",
                callback = callback,
                adSize = adSize,
                nativeAdAttributes = nativeAdAttributes,
                useDefaultNativeAdView = useDefaultNativeAdView
            )

        }
    }

}