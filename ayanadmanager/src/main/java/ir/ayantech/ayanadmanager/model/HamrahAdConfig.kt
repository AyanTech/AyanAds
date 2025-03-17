package ir.ayantech.ayanadmanager.model

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import ir.ayantech.ayanadmanager.core.AdCallback
import ir.ayantech.ayanadmanager.model.api.AddStatisticsInputParameters
import ir.ayantech.ayanadmanager.networks.hamrahAds.components.NativeAdAttributes
import ir.ayantech.ayanadmanager.utils.ContainerType
import ir.ayantech.hamrahads.domain.enums.HamrahAdsBannerType

data class HamrahAdConfig(
    val adSize: HamrahAdsBannerType?,
    val viewGroup: ViewGroup?,
    val nativeAdAttributes: NativeAdAttributes,
    val useDefaultNativeAdView: Boolean,
    val containerType: ContainerType,
    val activity: Activity,
    val addStatisticsInput: AddStatisticsInputParameters,
    val callback: AdCallback
) : AdRequestConfig(containerType, activity, addStatisticsInput, callback) {
    override fun getAdView(parentView: ViewGroup): View {
        return parentView
    }
}