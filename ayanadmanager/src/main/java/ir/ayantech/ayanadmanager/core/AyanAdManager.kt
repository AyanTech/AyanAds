package ir.ayantech.ayanadmanager.core

import android.content.Context
import android.view.ViewGroup
import ir.ayantech.ayanadmanager.model.api.AdUnit
import ir.ayantech.ayanadmanager.networks.hamrahAds.components.NativeAdAttributes
import ir.ayantech.ayanadmanager.utils.BannerAdSize
import ir.ayantech.ayanadmanager.utils.Logger
import ir.ayantech.ayanadmanager.utils.constant.AdSource
import ir.ayantech.ayanadmanager.utils.constant.AppMarket
import ir.ayantech.ayanadmanager.utils.constant.Config
import ir.ayantech.ayanadmanager.utils.constant.Config.Timeout
import ir.ayantech.ayanadmanager.utils.simpleCallBack
import ir.ayantech.ayanadmanager.utils.stringCallBack
import ir.ayantech.ayannetworking.BuildConfig
import ir.ayantech.ayannetworking.api.AyanApi
import ir.ayantech.ayannetworking.ayanModel.LogLevel
import ir.ayantech.hamrahads.HamrahAds
import ir.ayantech.hamrahads.domain.enums.HamrahAdsBannerType
import ir.ayantech.hamrahads.listener.HamrahAdsInitListener
import ir.ayantech.hamrahads.network.model.NetworkError

object AyanAdManager {
    private var isInitialized = false
    lateinit var ayanAdApi: AyanApi
    lateinit var adManager: AdProviderManager
    var clickTracker = ""
    var appKey = ""
    val adUnits = arrayListOf<AdUnit>()
    val adProvidersPriority = arrayListOf<AdSource>()
    lateinit var appMarket: AppMarket

    fun initialize(
        context: Context,
        apiKey: String,
        appKey: String,
        appMarket: AppMarket,
        onSuccess: simpleCallBack = { Logger.d("Initialization successful.") },
        onError: stringCallBack = { Logger.e(it) }
    ) {
        AyanAdManager.appKey = appKey
        AyanAdManager.appMarket = appMarket

        if (!BuildConfig.DEBUG) {
            Logger.setDebugMode(false)
        }

        if (isInitialized) {
            Logger.w("SDK is already initialized.")
            return
        }

        createAyanAdApi(context)
        getConfig(appKey = appKey)
        initializeHamrahAds(context, apiKey, onSuccess, onError)
        isInitialized = true
    }

    private fun createAyanAdApi(context: Context) {
        ayanAdApi = AyanApi(
            context = context,
            defaultBaseUrl = Config.AdSdkBaseUrl,
            timeout = Timeout.toLong(),
            headers = hashMapOf("Accept-Language" to "fa"),
            logLevel = if (BuildConfig.DEBUG) LogLevel.LOG_ALL else LogLevel.DO_NOT_LOG
        )
    }

    private fun initializeHamrahAds(
        context: Context,
        apiKey: String,
        onSuccess: simpleCallBack,
        onError: stringCallBack
    ) {
        HamrahAds.Initializer()
            .setContext(context)
            .initId(apiKey)
            .initListener(object : HamrahAdsInitListener {
                override fun onSuccess() {
                    onSuccess.invoke()
                }

                override fun onError(error: NetworkError) {
                    onError.invoke(
                        error.description ?: "Unknown error occurred while initializing HamrahAds."
                    )
                }
            }).build()
    }

    /**
     * Displays an advertisement based on the provided containerKey.
     */
    fun showAd(
        containerKey: String,
        context: Context,
        adSize: BannerAdSize?,
        adContainerId: ViewGroup?,
        nativeAdAttributes: NativeAdAttributes = NativeAdAttributes(),
        useDefaultNativeAdView: Boolean = true,
    ) {

        val convertedAdSize: HamrahAdsBannerType? = when (adSize) {
            BannerAdSize.BANNER_320x50 -> HamrahAdsBannerType.BANNER_320x50
            BannerAdSize.BANNER_640x1136 -> HamrahAdsBannerType.BANNER_640x1136
            BannerAdSize.BANNER_1136x640 -> HamrahAdsBannerType.BANNER_1136x640
            null -> null
        }

        adUnits.filter { it.ContainerKey == containerKey }
            .sortedByPriority(adProvidersPriority)
            ?.let { filteredAdUnits ->
                adManager.loadAndShowAd(
                    containerKey = containerKey,
                    adUnits = filteredAdUnits,
                    callback = createAdCallBack(),
                    context = context,
                    viewGroup = adContainerId,
                    nativeAdAttributes = nativeAdAttributes,
                    useDefaultNativeAdView = useDefaultNativeAdView,
                    adSize = convertedAdSize
                )
            } ?: Logger.w("No ad found for containerKey: $containerKey")

    }

    private fun createAdCallBack() = object : AdCallback {
        override fun onAdLoaded() {
            Logger.d("Ad loaded successfully!")
        }

        override fun onAdFailed(error: String) {
            Logger.e("Failed to load ad: $error")
        }
    }

    private fun List<AdUnit>.sortedByPriority(priorityList: List<AdSource>): List<AdUnit>? {
        return this.takeIf { it.isNotEmpty() }
            ?.sortedBy { ad ->
                priorityList.indexOf(ad.AdSource).takeIf { it >= 0 } ?: Int.MAX_VALUE
            }
    }

    fun isInitialized(): Boolean = isInitialized
}