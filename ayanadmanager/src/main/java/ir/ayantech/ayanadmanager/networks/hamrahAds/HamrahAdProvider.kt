package ir.ayantech.ayanadmanager.networks.hamrahAds

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import ir.ayantech.ayanadmanager.core.AdCallback
import ir.ayantech.ayanadmanager.core.AdProvider
import ir.ayantech.ayanadmanager.core.sendStatistics
import ir.ayantech.ayanadmanager.core.submitClick
import ir.ayantech.ayanadmanager.databinding.NativeLayoutBinding
import ir.ayantech.ayanadmanager.model.api.AddStatisticsInputParameters
import ir.ayantech.ayanadmanager.networks.hamrahAds.components.NativeAdAttributes
import ir.ayantech.ayanadmanager.networks.hamrahAds.components.init
import ir.ayantech.ayanadmanager.utils.ContainerType
import ir.ayantech.hamrahads.HamrahAds
import ir.ayantech.hamrahads.core.RequestBannerAds
import ir.ayantech.hamrahads.core.RequestInterstitialAds
import ir.ayantech.hamrahads.core.RequestNativeAds
import ir.ayantech.hamrahads.core.ShowBannerAds
import ir.ayantech.hamrahads.core.ShowInterstitialAds
import ir.ayantech.hamrahads.core.ShowNativeAds
import ir.ayantech.hamrahads.domain.enums.HamrahAdsBannerType
import ir.ayantech.hamrahads.listener.HamrahAdsInitListener
import ir.ayantech.hamrahads.network.model.NetworkError

class HamrahAdProvider : AdProvider {

    private var showBannerAds: ShowBannerAds? = null
    private var requestBanner: RequestBannerAds? = null

    private var showInterstitialAds: ShowInterstitialAds? = null
    private var requestInterstitial: RequestInterstitialAds? = null

    private var showNativeAds: ShowNativeAds? = null
    private var requestNative: RequestNativeAds? = null

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
        if (addStatisticsInput.AdUnitId.isNullOrEmpty()) {
            callback.onAdFailed("AdUnitId cannot be empty")
            return
        }

        when (containerType) {
            ContainerType.Banner -> showBannerAd(
                context = context,
                viewGroup = viewGroup,
                adSize = adSize,
                statistics = addStatisticsInput,
                callback = callback
            )

            ContainerType.Interstitial -> showInterstitialAd(
                context = context,
                statistics = addStatisticsInput,
                callback = callback
            )
            ContainerType.Native -> showNativeAd(
                context = context,
                viewGroup = viewGroup,
                statistics = addStatisticsInput,
                nativeAdAttributes = nativeAdAttributes,
                useDefaultNativeAdView = useDefaultNativeAdView,
                callback = callback
            )
        }
    }

    private fun showBannerAd(
        context: Context,
        viewGroup: ViewGroup?,
        adSize: HamrahAdsBannerType?,
        statistics: AddStatisticsInputParameters,
        callback: AdCallback
    ) {
        destroyBannerAdIfExist()

        viewGroup?.let { vg ->
            requestBanner = HamrahAds.RequestBannerAds()
                .setContext(context)
                .initId(statistics.AdUnitId!!)
                .initListener(object : HamrahAdsInitListener {
                    override fun onSuccess() {
                        showBannerAds =
                            createShowBannerAds(context, viewGroup, adSize, statistics, callback)
                    }

                    override fun onError(error: NetworkError) {
                        handleAdError(error, callback, statistics)
                    }
                }).build()
        } ?: callback.onAdFailed("ViewGroup is null")

    }

    private fun showInterstitialAd(
        context: Context,
        statistics: AddStatisticsInputParameters,
        callback: AdCallback
    ) {
        destroyInterstitialAdIfExist()

        requestInterstitial = HamrahAds.RequestInterstitialAds()
            .setContext(context)
            .initId(statistics.AdUnitId!!)
            .initListener(object : HamrahAdsInitListener {
                override fun onSuccess() {
                    showInterstitialAds = createShowInterstitialAd(context, callback, statistics)
                }

                override fun onError(error: NetworkError) {
                    handleAdError(error, callback, statistics)
                }
            }).build()
    }

    private fun showNativeAd(
        context: Context,
        viewGroup: ViewGroup?,
        statistics: AddStatisticsInputParameters,
        nativeAdAttributes: NativeAdAttributes,
        useDefaultNativeAdView: Boolean,
        callback: AdCallback
    ) {
        destroyNativeAdIfExist()

        viewGroup?.let {

            if (useDefaultNativeAdView) bindingDefaultView(viewGroup, context, nativeAdAttributes)

            requestNative = HamrahAds.RequestNativeAds()
                .setContext(context)
                .initId(statistics.AdUnitId!!)
                .initListener(object : HamrahAdsInitListener {
                    override fun onSuccess() {
                        showNativeAds =
                            createShowNativeAd(context, callback, viewGroup, statistics)
                    }

                    override fun onError(error: NetworkError) {
                        handleAdError(error, callback, statistics)
                    }
                }).build()
        } ?: callback.onAdFailed("ViewGroup is null")

    }

    private fun bindingDefaultView(
        viewGroup: ViewGroup,
        context: Context,
        nativeAdAttributes: NativeAdAttributes
    ) {

        Handler(Looper.getMainLooper()).post {
            val inflater = LayoutInflater.from(context)
            val binding = NativeLayoutBinding.inflate(inflater)
            binding.init(nativeAdAttributes)
            viewGroup.addView(binding.root)
        }
    }

    private fun createShowNativeAd(
        context: Context,
        callback: AdCallback,
        viewGroup: ViewGroup,
        statistics: AddStatisticsInputParameters
    ): ShowNativeAds? {
        return HamrahAds.ShowNativeAds()
            .setViewGroup(viewGroup)
            .setContext(context as AppCompatActivity)
            .initListener(createAdListener(callback, statistics)).build()
    }

    private fun createShowBannerAds(
        context: Context,
        viewGroup: ViewGroup,
        adSize: HamrahAdsBannerType?,
        statistics: AddStatisticsInputParameters,
        callback: AdCallback
    ): ShowBannerAds? {
        return HamrahAds.ShowBannerAds()
            .setContext(context as Activity)
            .setViewGroup(viewGroup)
            .setSize(adSize ?: HamrahAdsBannerType.BANNER_320x50)
            .initListener(object : HamrahAdsInitListener {
                override fun onSuccess() {
                    callback.onAdLoaded()
                    sendStatistics(input = statistics)
                }

                override fun onClick() {
                    super.onClick()
                    submitClick()
                }

                override fun onError(error: NetworkError) {
                    handleAdError(error, callback, statistics)
                }
            }).build()
    }

    private fun createShowInterstitialAd(
        context: Context,
        callback: AdCallback,
        statistics: AddStatisticsInputParameters
    ): ShowInterstitialAds? {
        return HamrahAds.ShowInterstitialAds()
            .setContext(context as AppCompatActivity)
            .initListener(
                createAdListener(
                    callback = callback,
                    statistics = statistics
                )
            ).build()
    }

    private fun handleAdError(
        error: NetworkError,
        callback: AdCallback,
        statistics: AddStatisticsInputParameters
    ) {
        statistics.FailureCause = error.description ?: "Unknown Error"
        callback.onAdFailed(statistics.FailureCause!!)
        sendStatistics(statistics)
    }

    private fun createAdListener(callback: AdCallback, statistics: AddStatisticsInputParameters) =
        object : HamrahAdsInitListener {
            override fun onSuccess() {
                callback.onAdLoaded()
                sendStatistics(statistics)
            }

            override fun onError(error: NetworkError) {
                handleAdError(error, callback, statistics)
            }

            override fun onClick() {
                super.onClick()
                submitClick()
            }

        }

    private fun destroyBannerAdIfExist() {
        showBannerAds?.destroyAds()
        requestBanner?.cancelRequest()
    }

    private fun destroyInterstitialAdIfExist() {
        showInterstitialAds?.destroyAds()
        requestInterstitial?.cancelRequest()
    }

    private fun destroyNativeAdIfExist() {
        showNativeAds?.destroyAds()
        requestNative?.cancelRequest()
    }

    override fun destroy() {
        destroyBannerAdIfExist()
        destroyInterstitialAdIfExist()
        destroyNativeAdIfExist()
    }
}