package ir.ayantech.ayanadmanager.networks.tapsell

import android.content.Context
import android.view.ViewGroup
import ir.ayantech.ayanadmanager.core.AdCallback
import ir.ayantech.ayanadmanager.core.AdProvider
import ir.ayantech.ayanadmanager.model.api.AddStatisticsInputParameters
import ir.ayantech.ayanadmanager.networks.hamrahAds.components.NativeAdAttributes
import ir.ayantech.ayanadmanager.utils.ContainerType
import ir.ayantech.hamrahads.domain.enums.HamrahAdsBannerType

class TapsellProvider : AdProvider {
    override fun loadAd(
        containerType: ContainerType,
        context: Context,
        addStatisticsInput: AddStatisticsInputParameters,
        viewGroup: ViewGroup?,
        adSize: HamrahAdsBannerType?,
        nativeAdAttributes: NativeAdAttributes,
        useDefaultNativeAdView: Boolean,
        callback: AdCallback
    ) {
        callback.onAdFailed("Not yet implemented")
    }

    override fun destroy() {
        TODO("Not yet implemented")
    }

}