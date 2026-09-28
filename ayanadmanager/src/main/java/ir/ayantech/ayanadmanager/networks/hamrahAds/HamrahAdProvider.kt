package ir.ayantech.ayanadmanager.networks.hamrahAds

import android.app.Activity
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
import ir.ayantech.hamrahads.ads.banner.BannerAdLoader
import ir.ayantech.hamrahads.ads.banner.BannerAdView
import ir.ayantech.hamrahads.ads.interstitial.InterstitialAdLoader
import ir.ayantech.hamrahads.ads.interstitial.InterstitialAdView
import ir.ayantech.hamrahads.ads.native.NativeAdLoader
import ir.ayantech.hamrahads.ads.native.NativeAdView
import ir.ayantech.hamrahads.listener.RequestListener
import ir.ayantech.hamrahads.listener.ShowListener
import ir.ayantech.hamrahads.model.enums.BannerSize
import ir.ayantech.hamrahads.model.error.HamrahAdsError

class HamrahAdProvider : AdProvider {

    private var generation = 0
    private val originalViewIds = java.util.IdentityHashMap<android.view.View, Int>()

    private var showBannerAds: BannerAdView? = null
    private var requestBanner: BannerAdLoader? = null

    private var showInterstitialAds: InterstitialAdView? = null
    private var requestInterstitial: InterstitialAdLoader? = null

    private var showNativeAds: NativeAdView? = null
    private var requestNative: NativeAdLoader? = null

    private val idMapping = mapOf(
        R.id.ad_title to R.id.hamrah_ad_native_title,
        R.id.ad_description to R.id.hamrah_ad_native_description,
        R.id.ad_banner to R.id.hamrah_ad_native_banner,
        R.id.ad_icon to R.id.hamrah_ad_native_logo,
        R.id.ad_cta_view to R.id.hamrah_ad_native_cta_view,
        R.id.ad_cta to R.id.hamrah_ad_native_cta
    )

    override fun loadAd(config: AdRequestConfig) {
        destroy()

        (config as? HamrahAdConfig)?.apply {
            if (addStatisticsInput.adUnitId.isNullOrEmpty()) {
                callback?.onAdFailed("AdUnitId cannot be empty")
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
                if (child !in originalViewIds) originalViewIds[child] = child.id
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
        adSize: BannerSize?,
        statistics: AddStatisticsInputParameters,
        callback: AdCallback?
    ) {
        destroyBannerAdIfExist()

        val requestGeneration = generation
        viewGroup?.let { vg ->
            requestBanner = HamrahAds.RequestBannerAds()
                .setContext(appCompatActivity)
                .initId(statistics.adUnitId.orEmpty())
                .initListener(object : RequestListener {
                    override fun onSuccess() {
                        if (requestGeneration != generation) return
                        showBannerAds =
                            createShowBannerAds(
                                appCompatActivity,
                                viewGroup,
                                adSize,
                                statistics,
                                callback
                            )
                    }

                    override fun onError(error: HamrahAdsError) {
                        if (requestGeneration != generation) return
                        handleAdError(
                            error, callback, statistics
                        )
                    }
                }).build()
        } ?: callback?.onAdFailed("ViewGroup is null")

    }

    private fun showInterstitialAd(
        appCompatActivity: AppCompatActivity,
        statistics: AddStatisticsInputParameters,
        callback: AdCallback?
    ) {
        destroyInterstitialAdIfExist()

        val requestGeneration = generation
        requestInterstitial = HamrahAds.RequestInterstitialAds()
            .setContext(appCompatActivity)
            .initId(statistics.adUnitId ?: "")
            .initListener(requestListener = object : RequestListener {
                override fun onSuccess() {
                    if (requestGeneration != generation) return
                    showInterstitialAds =
                        createShowInterstitialAd(appCompatActivity, callback, statistics)
                }

                override fun onError(error: HamrahAdsError) {
                    if (requestGeneration != generation) return
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
        callback: AdCallback?
    ) {
        destroyNativeAdIfExist()

        val requestGeneration = generation
        viewGroup?.let {

            if (useDefaultNativeAdView) bindingDefaultView(
                viewGroup,
                appCompatActivity,
                nativeAdAttributes
            ) else updateViewIds(viewGroup, idMapping)

            requestNative = HamrahAds.RequestNativeAds()
                .setContext(appCompatActivity)
                .initId(statistics.adUnitId ?: "")
                .initListener(requestListener = object : RequestListener {
                    override fun onSuccess() {
                        if (requestGeneration != generation) return
                        showNativeAds =
                            createShowNativeAd(appCompatActivity, callback, viewGroup, statistics)
                    }

                    override fun onError(error: HamrahAdsError) {
                        if (requestGeneration != generation) return
                        handleAdError(error, callback, statistics)
                    }
                }).build()
        } ?: callback?.onAdFailed("ViewGroup is null")

    }

    private fun bindingDefaultView(
        viewGroup: ViewGroup,
        appCompatActivity: AppCompatActivity,
        nativeAdAttributes: NativeAdAttributes
    ) {
        run {
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
        callback: AdCallback?,
        viewGroup: ViewGroup,
        statistics: AddStatisticsInputParameters
    ): NativeAdView? {
        return HamrahAds.ShowNativeAds()
            .setViewGroup(viewGroup)
            .initId(statistics.adUnitId ?: "")
            .setContext(appCompatActivity)
            .initListener(createAdListener(callback, statistics)).build()
    }

    private fun createShowBannerAds(
        appCompatActivity: AppCompatActivity,
        viewGroup: ViewGroup,
        adSize: BannerSize?,
        statistics: AddStatisticsInputParameters,
        callback: AdCallback?
    ): BannerAdView? {
        val requestGeneration = generation
        return HamrahAds.ShowBannerAds()
            .setContext(appCompatActivity)
            .setViewGroup(viewGroup)
            .initId(statistics.adUnitId ?: "")
            .setSize(adSize ?: BannerSize.BANNER_320x50)
            .initListener(showListener = object : ShowListener {

                override fun onLoaded() {
                    if (requestGeneration != generation) return
                    super.onLoaded()
                    callback?.onAdLoaded()
                }

                override fun onDisplayed() {
                    if (requestGeneration != generation) return
                    super.onDisplayed()
                    sendStatistics(input = statistics)
                }

                override fun onClick() {
                    if (requestGeneration != generation) return
                    super.onClick()
                    submitClick(adUnitId = statistics.adUnitId)
                    callback?.onAdClicked()
                }

                override fun onError(error: HamrahAdsError) {
                    if (requestGeneration != generation) return
                    handleAdError(error, callback, statistics)
                }
            }).build()
    }

    private fun createShowInterstitialAd(
        activity: Activity,
        callback: AdCallback?,
        statistics: AddStatisticsInputParameters
    ): InterstitialAdView? {
        return HamrahAds.ShowInterstitialAds()
            .setContext(activity as AppCompatActivity)
            .initId(statistics.adUnitId.orEmpty())
            .initListener(
                createAdListener(
                    callback = callback,
                    statistics = statistics
                )
            ).build()
    }

    private fun handleAdError(
        error: HamrahAdsError,
        callback: AdCallback?,
        statistics: AddStatisticsInputParameters
    ) {
        val message = error.description ?: "Unknown Error"
        sendStatistics(statistics.copy(failureCause = message))
        callback?.onAdFailed(message)
    }

    private fun createAdListener(callback: AdCallback?, statistics: AddStatisticsInputParameters): ShowListener {
        val requestGeneration = generation
        return object : ShowListener {

            override fun onLoaded() {
                if (requestGeneration != generation) return
                callback?.onAdLoaded()
                super.onLoaded()
            }

            override fun onDisplayed() {
                if (requestGeneration != generation) return
                super.onDisplayed()
                sendStatistics(statistics)
            }

            override fun onError(error: HamrahAdsError) {
                if (requestGeneration != generation) return
                handleAdError(error, callback, statistics)
            }

            override fun onClick() {
                if (requestGeneration != generation) return
                super.onClick()
                callback?.onAdClicked()
                submitClick(adUnitId = statistics.adUnitId)
            }

        }
    }

    private fun destroyBannerAdIfExist() {
        showBannerAds?.destroyAds()
        requestBanner?.cancelRequest()
        showBannerAds = null
        requestBanner = null
    }

    private fun destroyInterstitialAdIfExist() {
        showInterstitialAds?.destroyAds()
        requestInterstitial?.cancelRequest()
        showInterstitialAds = null
        requestInterstitial = null
    }

    private fun destroyNativeAdIfExist() {
        showNativeAds?.destroyAds()
        requestNative?.cancelRequest()
        showNativeAds = null
        requestNative = null
    }

    override fun destroy() {
        generation++
        originalViewIds.forEach { (view, id) -> view.id = id }
        originalViewIds.clear()
        trying { destroyBannerAdIfExist() }
        trying { destroyInterstitialAdIfExist() }
        trying { destroyNativeAdIfExist() }
    }
}
