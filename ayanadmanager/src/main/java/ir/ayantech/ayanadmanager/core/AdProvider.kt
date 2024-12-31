package com.ayanco.ayanads.core

import android.content.Context
import android.view.ViewGroup
import ir.ayantech.ayanadmanager.model.api.AddStatisticsInputParameters
import ir.ayantech.ayanadmanager.networks.hamrahAds.components.NativeAdAttributes
import ir.ayantech.ayanadmanager.utils.ContainerType
import ir.ayantech.hamrahads.domain.enums.HamrahAdsBannerType

interface AdProvider {
    fun loadAd(
        containerType: ContainerType,
        context: Context,
        addStatisticsInput: AddStatisticsInputParameters,
        viewGroup: ViewGroup?,
        adSize: HamrahAdsBannerType?,
        nativeAdAttributes: NativeAdAttributes,
        useDefaultNativeAdView: Boolean,
        callback: AdCallback
    )

    fun destroy()
}