package ir.ayantech.ayanadmanager.networks.hamrahAds

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import ir.ayantech.ayanadmanager.R
import ir.ayantech.ayanadmanager.core.AdCallback
import ir.ayantech.ayanadmanager.core.AdProvider
import ir.ayantech.ayanadmanager.core.sendStatistics
import ir.ayantech.ayanadmanager.core.submitClick
import ir.ayantech.ayanadmanager.databinding.NativeLayoutBinding
import ir.ayantech.ayanadmanager.model.AdRequestConfig
import ir.ayantech.ayanadmanager.model.HamrahAdConfig
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

    private val idMapping = mapOf(
        R.id.ad_title to R.id.hamrah_ad_native_title,
        R.id.ad_description to R.id.hamrah_ad_native_description,
        R.id.ad_banner to R.id.hamrah_ad_native_banner,
        R.id.ad_icon to R.id.hamrah_ad_native_logo,
        R.id.ad_cta_view to R.id.hamrah_ad_native_cta_view,
        R.id.ad_cta to R.id.hamrah_ad_native_cta
    )

    override fun loadAd(config: AdRequestConfig) {

        (config as? HamrahAdConfig)?.apply {
            if (addStatisticsInput.AdUnitId.isNullOrEmpty()) {
                callback.onAdFailed("AdUnitId cannot be empty")
                return
            }

            when (containerType) {
                ContainerType.Banner -> showBannerAd(
                    activity = activity as AppCompatActivity,
                    viewGroup = viewGroup,
                    adSize = adSize,
                    statistics = addStatisticsInput,
                    callback = callback
                )

                ContainerType.Interstitial -> showInterstitialAd(
                    activity = activity,
                    statistics = addStatisticsInput,
                    callback = callback
                )

                ContainerType.Native -> showNativeAd(
                    activity = activity,
                    viewGroup = viewGroup,
                    statistics = addStatisticsInput,
                    nativeAdAttributes = nativeAdAttributes,
                    useDefaultNativeAdView = useDefaultNativeAdView,
                    callback = callback
                )
            }
        }

    }

    fun updateViewIds(viewGroup: ViewGroup, idMapping: Map<Int, Int>) {
        for (i in 0 until viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)
            idMapping[child.id]?.let { newId ->
                child.id = newId
            }
            if (child is ViewGroup) {
                updateViewIds(child, idMapping)
            }
        }
    }

    private fun showBannerAd(
        activity: Activity,
        viewGroup: ViewGroup?,
        adSize: HamrahAdsBannerType?,
        statistics: AddStatisticsInputParameters,
        callback: AdCallback
    ) {
        destroyBannerAdIfExist()

        viewGroup?.let { vg ->
            requestBanner = HamrahAds.RequestBannerAds()
                .setContext(activity)
                .initId(statistics.AdUnitId!!)
                .initListener(object : HamrahAdsInitListener {
                    override fun onSuccess() {
                        showBannerAds =
                            createShowBannerAds(activity, viewGroup, adSize, statistics, callback)
                    }

                    override fun onError(error: NetworkError) {
                        handleAdError(error, callback, statistics)
                    }
                }).build()
        } ?: callback.onAdFailed("ViewGroup is null")

    }

    private fun showInterstitialAd(
        activity: Activity,
        statistics: AddStatisticsInputParameters,
        callback: AdCallback
    ) {
        destroyInterstitialAdIfExist()

        requestInterstitial = HamrahAds.RequestInterstitialAds()
            .setContext(activity)
            .initId(statistics.AdUnitId!!)
            .initListener(object : HamrahAdsInitListener {
                override fun onSuccess() {
                    showInterstitialAds = createShowInterstitialAd(activity, callback, statistics)
                }

                override fun onError(error: NetworkError) {
                    handleAdError(error, callback, statistics)
                }
            }).build()
    }

    private fun showNativeAd(
        activity: Activity,
        viewGroup: ViewGroup?,
        statistics: AddStatisticsInputParameters,
        nativeAdAttributes: NativeAdAttributes,
        useDefaultNativeAdView: Boolean,
        callback: AdCallback
    ) {
        destroyNativeAdIfExist()

        viewGroup?.let {

            if (useDefaultNativeAdView) bindingDefaultView(
                viewGroup,
                activity,
                nativeAdAttributes
            ) else updateViewIds(viewGroup, idMapping)

            requestNative = HamrahAds.RequestNativeAds()
                .setContext(activity)
                .initId(statistics.AdUnitId!!)
                .initListener(object : HamrahAdsInitListener {
                    override fun onSuccess() {
                        showNativeAds =
                            createShowNativeAd(activity, callback, viewGroup, statistics)
                    }

                    override fun onError(error: NetworkError) {
                        handleAdError(error, callback, statistics)
                    }
                }).build()
        } ?: callback.onAdFailed("ViewGroup is null")

    }

    private fun bindingDefaultView(
        viewGroup: ViewGroup,
        activity: Activity,
        nativeAdAttributes: NativeAdAttributes
    ) {
        Handler(Looper.getMainLooper()).post {
            val inflater = LayoutInflater.from(activity)
            val binding = NativeLayoutBinding.inflate(inflater)
            binding.init(nativeAdAttributes)
            viewGroup.addView(binding.root)
        }
    }

    private fun createShowNativeAd(
        activity: Activity,
        callback: AdCallback,
        viewGroup: ViewGroup,
        statistics: AddStatisticsInputParameters
    ): ShowNativeAds? {
        return HamrahAds.ShowNativeAds()
            .setViewGroup(viewGroup)
            .setContext(activity)
            .initListener(createAdListener(callback, statistics)).build()
    }

    private fun createShowBannerAds(
        activity: Activity,
        viewGroup: ViewGroup,
        adSize: HamrahAdsBannerType?,
        statistics: AddStatisticsInputParameters,
        callback: AdCallback
    ): ShowBannerAds? {
        return HamrahAds.ShowBannerAds()
            .setContext(activity)
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
                    callback.onAdClicked()
                }

                override fun onError(error: NetworkError) {
                    handleAdError(error, callback, statistics)
                }
            }).build()
    }

    private fun createShowInterstitialAd(
        activity: Activity,
        callback: AdCallback,
        statistics: AddStatisticsInputParameters
    ): ShowInterstitialAds? {
        return HamrahAds.ShowInterstitialAds()
            .setContext(activity as AppCompatActivity)
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
                callback.onAdClicked()
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