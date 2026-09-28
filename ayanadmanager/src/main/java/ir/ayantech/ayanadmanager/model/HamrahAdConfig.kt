package ir.ayantech.ayanadmanager.model

import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import ir.ayantech.ayanadmanager.core.AdCallback
import ir.ayantech.ayanadmanager.model.api.AddStatisticsInputParameters
import ir.ayantech.ayanadmanager.networks.hamrahAds.components.NativeAdAttributes
import ir.ayantech.ayanadmanager.utils.AdSizeType
import ir.ayantech.ayanadmanager.utils.ContainerType
import ir.ayantech.hamrahads.model.enums.BannerSize

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

    fun getAdSize(): BannerSize {
        return when (adSize) {
            AdSizeType.SMALL -> BannerSize.BANNER_320x50
            AdSizeType.MEDIUM -> BannerSize.BANNER_640x1136
            AdSizeType.LARGE -> BannerSize.BANNER_1136x640
            null -> BannerSize.BANNER_320x50
        }
    }

    override fun getAdView(parentView: ViewGroup): View {
        return parentView
    }
}