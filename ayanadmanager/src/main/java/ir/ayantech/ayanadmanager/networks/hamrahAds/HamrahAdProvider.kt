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
import ir.ayantech.ayanadmanager.utils.trying
import ir.ayantech.hamrahads.HamrahAds
import ir.ayantech.hamrahads.core.RequestBannerAds
import ir.ayantech.hamrahads.core.RequestInterstitialAds
import ir.ayantech.hamrahads.core.RequestNativeAds
import ir.ayantech.hamrahads.core.ShowBannerAds
import ir.ayantech.hamrahads.core.ShowInterstitialAds
import ir.ayantech.hamrahads.core.ShowNativeAds
import ir.ayantech.hamrahads.domain.enums.HamrahAdsBannerType
import ir.ayantech.hamrahads.listener.RequestListener
import ir.ayantech.hamrahads.listener.ShowListener
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
                    appCompatActivity = appCompatActivity,
                    viewGroup = viewGroup,
                    adSize = getAdSize(),
                    statistics = addStatisticsInput,
                    callback = callback
                )

                ContainerType.Interstitial -> showInterstitialAd(
                    appCompatActivity = appCompatActivity,
                    statistics = addStatisticsInput,
                    callback = callback
                )

                ContainerType.Native -> showNativeAd(
                    appCompatActivity = appCompatActivity,
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
        appCompatActivity: AppCompatActivity,
        viewGroup: ViewGroup?,
        adSize: HamrahAdsBannerType?,
        statistics: AddStatisticsInputParameters,
        callback: AdCallback
    ) {
        destroyBannerAdIfExist()

        viewGroup?.let { vg ->
            requestBanner = HamrahAds.RequestBannerAds()
                .setContext(appCompatActivity)
                .initId(statistics.AdUnitId ?: "")
                .initListener(object : RequestListener {
                    override fun onSuccess() {
                        showBannerAds =
                            createShowBannerAds(
                                appCompatActivity,
                                viewGroup,
                                adSize,
                                statistics,
                                callback
                            )
                    }

                    override fun onError(error: NetworkError) {
                        handleAdError(error, callback, statistics)
                    }
                }).build()
        } ?: callback.onAdFailed("ViewGroup is null")

    }

    private fun showInterstitialAd(
        appCompatActivity: AppCompatActivity,
        statistics: AddStatisticsInputParameters,
        callback: AdCallback
    ) {
        destroyInterstitialAdIfExist()

        requestInterstitial = HamrahAds.RequestInterstitialAds()
            .setContext(appCompatActivity)
            .initId(statistics.AdUnitId ?: "")
            .initListener(requestListener = object : RequestListener {
                override fun onSuccess() {
                    showInterstitialAds =
                        createShowInterstitialAd(appCompatActivity, callback, statistics)
                }

                override fun onError(error: NetworkError) {
                    handleAdError(error, callback, statistics)
                }
            }).build()
    }

    private fun showNativeAd(
        appCompatActivity: AppCompatActivity,
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
                appCompatActivity,
                nativeAdAttributes
            ) else updateViewIds(viewGroup, idMapping)

            requestNative = HamrahAds.RequestNativeAds()
                .setContext(appCompatActivity)
                .initId(statistics.AdUnitId ?: "")
                .initListener(requestListener = object : RequestListener {
                    override fun onSuccess() {
                        showNativeAds =
                            createShowNativeAd(appCompatActivity, callback, viewGroup, statistics)
                    }

                    override fun onError(error: NetworkError) {
                        handleAdError(error, callback, statistics)
                    }
                }).build()
        } ?: callback.onAdFailed("ViewGroup is null")

    }

    private fun bindingDefaultView(
        viewGroup: ViewGroup,
        appCompatActivity: AppCompatActivity,
        nativeAdAttributes: NativeAdAttributes
    ) {
        Handler(Looper.getMainLooper()).post {
            val inflater = LayoutInflater.from(appCompatActivity)
            val binding = NativeLayoutBinding.inflate(inflater)
            binding.init(nativeAdAttributes)
            viewGroup.apply {
                removeAllViews()
                addView(binding.root, 0)
            }
        }
    }

    private fun createShowNativeAd(
        appCompatActivity: AppCompatActivity,
        callback: AdCallback,
        viewGroup: ViewGroup,
        statistics: AddStatisticsInputParameters
    ): ShowNativeAds? {
        return HamrahAds.ShowNativeAds()
            .setViewGroup(viewGroup)
            .initId(statistics.AdUnitId ?: "")
            .setContext(appCompatActivity)
            .initListener(createAdListener(callback, statistics)).build()
    }

    private fun createShowBannerAds(
        appCompatActivity: AppCompatActivity,
        viewGroup: ViewGroup,
        adSize: HamrahAdsBannerType?,
        statistics: AddStatisticsInputParameters,
        callback: AdCallback
    ): ShowBannerAds? {
        return HamrahAds.ShowBannerAds()
            .setContext(appCompatActivity)
            .setViewGroup(viewGroup)
            .initId(statistics.AdUnitId ?: "")
            .setSize(adSize ?: HamrahAdsBannerType.BANNER_320x50)
            .initListener(showListener = object : ShowListener {

                override fun onLoaded() {
                    super.onLoaded()
                    callback.onAdLoaded()
                }

                override fun onDisplayed() {
                    super.onDisplayed()
                    sendStatistics(input = statistics)
                }

                override fun onClick() {
                    super.onClick()
                    submitClick(adUnitId = statistics.AdUnitId)
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
            .initId(statistics.AdUnitId ?: "")
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
        object : ShowListener {

            override fun onLoaded() {
                callback.onAdLoaded()
                super.onLoaded()
            }

            override fun onDisplayed() {
                super.onDisplayed()
                sendStatistics(statistics)
            }

            override fun onError(error: NetworkError) {
                handleAdError(error, callback, statistics)
            }

            override fun onClick() {
                super.onClick()
                callback.onAdClicked()
                submitClick(adUnitId = statistics.AdUnitId)
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
        trying {
            destroyBannerAdIfExist()
            destroyInterstitialAdIfExist()
            destroyNativeAdIfExist()
        }
    }
}