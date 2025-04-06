package ir.ayantech.ayanadmanager.model

import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import ir.ayantech.ayanadmanager.core.AdCallback
import ir.ayantech.ayanadmanager.model.api.AddStatisticsInputParameters
import ir.ayantech.ayanadmanager.networks.hamrahAds.components.NativeAdAttributes
import ir.ayantech.ayanadmanager.utils.AdSizeType
import ir.ayantech.ayanadmanager.utils.ContainerType
import ir.ayantech.hamrahads.domain.enums.HamrahAdsBannerType

data class HamrahAdConfig(
    val adSize: AdSizeType?,
    val viewGroup: ViewGroup?,
    val nativeAdAttributes: NativeAdAttributes,
    val useDefaultNativeAdView: Boolean,
    val containerType: ContainerType,
    val appCompatActivity: AppCompatActivity,
    val addStatisticsInput: AddStatisticsInputParameters,
    val callback: AdCallback
) : AdRequestConfig(containerType, appCompatActivity, addStatisticsInput, callback) {

    fun getAdSize(): HamrahAdsBannerType {
        return when (adSize) {
            AdSizeType.SMALL -> HamrahAdsBannerType.BANNER_320x50
            AdSizeType.MEDIUM -> HamrahAdsBannerType.BANNER_640x1136
            AdSizeType.LARGE -> HamrahAdsBannerType.BANNER_1136x640
            null -> HamrahAdsBannerType.BANNER_320x50
        }
    }

    override fun getAdView(parentView: ViewGroup): View {
        return parentView
    }
}