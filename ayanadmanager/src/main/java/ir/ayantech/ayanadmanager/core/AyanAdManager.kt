package ir.ayantech.ayanadmanager.core

import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.MobileAds
import ir.ayantech.ayanadmanager.model.api.AdProviderPriority
import ir.ayantech.ayanadmanager.model.api.AdUnit
import ir.ayantech.ayanadmanager.networks.admob.ConsentManager
import ir.ayantech.ayanadmanager.networks.hamrahAds.components.NativeAdAttributes
import ir.ayantech.ayanadmanager.utils.AdSizeType
import ir.ayantech.ayanadmanager.utils.Logger
import ir.ayantech.ayanadmanager.utils.SimpleCallBack
import ir.ayantech.ayanadmanager.utils.StringCallBack
import ir.ayantech.ayanadmanager.utils.constant.AdSource
import ir.ayantech.ayanadmanager.utils.constant.AppMarket
import ir.ayantech.ayanadmanager.utils.constant.Config
import ir.ayantech.ayanadmanager.utils.constant.Config.TIMEOUT
import ir.ayantech.ayannetworking.BuildConfig
import ir.ayantech.ayannetworking.api.AyanApi
import ir.ayantech.ayannetworking.ayanModel.LogLevel
import ir.ayantech.hamrahads.HamrahAds
import ir.ayantech.hamrahads.listener.HamrahAdsInitListener
import ir.ayantech.hamrahads.network.model.NetworkError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object AyanAdManager {
    private var isInitialized = false
    lateinit var adProvider: AdProvider
    lateinit var ayanAdApi: AyanApi
    private lateinit var adManager: AdProviderManager
    var clickTracker = ""
    var appKey = ""
    private val adUnits = arrayListOf<AdUnit>()
    private val adProvidersPriority = arrayListOf<AdProviderPriority>()
    lateinit var appMarket: AppMarket

    fun initialize(
        appCompatActivity: AppCompatActivity,
        appKey: String,
        appMarket: AppMarket,
        onSuccess: SimpleCallBack = { Logger.d("Initialization successful.") },
        onError: StringCallBack = { Logger.e(it) }
    ) {
        this.appKey = appKey
        this.appMarket = appMarket

        if (BuildConfig.DEBUG.not()) {
            Logger.setDebugMode(false)
        }

        if (isInitialized) {
            Logger.w("SDK is already initialized.")
            return
        }

        createAyanAdApi(appCompatActivity)

        getConfig(
            appKey = appKey,
            onSuccess = { response ->
                isInitialized = true
                response?.let {
                    it.AdSourcePriority.map { AdProviderPriority(it.AdSource, it.AppId) }
                        .let { adProvidersPriority.addAll(it) }
                    adManager = AdProviderManager()
                    adUnits.addAll(it.AdUnits)
                }
                adProvidersPriority.forEach {
                    when (it.adSource) {
                        AdSource.HamrahAd -> {
                            if (it.priority.isNullOrEmpty().not()) {
                                initializeHamrahAds(
                                    appCompatActivity,
                                    it.priority,
                                    onSuccess,
                                    onError
                                )
                            } else {
                                isInitialized = false
                                Logger.e("HamrahAd is not initialize, appID is not valid.")
                                return@getConfig
                            }
                        }

                        AdSource.AdMob -> {
                            if (it.priority.isNullOrEmpty().not()) {
                                initializeMobileAds(appCompatActivity)
                                isInitialized = false
                                return@getConfig
                            }
                        }
                    }
                }
            },
            onFailed = { failure ->
                isInitialized = false
            }
        )
    }

    private fun initializeMobileAds(appCompatActivity: AppCompatActivity) {
        CoroutineScope(Dispatchers.IO).launch {
            MobileAds.initialize(appCompatActivity) {}
            ConsentManager.initialize(appCompatActivity)
        }
    }

    private fun createAyanAdApi(appCompatActivity: AppCompatActivity) {
        ayanAdApi = AyanApi(
            context = appCompatActivity,
            defaultBaseUrl = Config.AYAN_AD_BASE_URL,
            timeout = TIMEOUT.toLong(),
            headers = hashMapOf("Accept-Language" to "fa"),
            logLevel = if (BuildConfig.DEBUG) LogLevel.LOG_ALL else LogLevel.DO_NOT_LOG
        )
    }

    private fun initializeHamrahAds(
        appCompatActivity: AppCompatActivity,
        apiKey: String,
        onSuccess: SimpleCallBack,
        onError: StringCallBack
    ) {
        HamrahAds.Initializer()
            .setContext(appCompatActivity)
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
        appCompatActivity: AppCompatActivity,
        adSize: AdSizeType?,
        adContainerId: ViewGroup?,
        nativeAdAttributes: NativeAdAttributes = NativeAdAttributes(),
        useDefaultNativeAdView: Boolean = true,
        adCallback: AdCallback
    ) {

        adUnits.filter { it.ContainerKey == containerKey }
            .sortedByPriority(adProvidersPriority.map { it.adSource })
            ?.let { filteredAdUnits ->
                adManager.loadAndShowAd(
                    containerKey = containerKey,
                    adUnits = filteredAdUnits,
                    callback = createAdCallBack(adCallback),
                    appCompatActivity = appCompatActivity,
                    viewGroup = adContainerId,
                    nativeAdAttributes = nativeAdAttributes,
                    useDefaultNativeAdView = useDefaultNativeAdView,
                    adSize = adSize
                )
            } ?: Logger.w("No ad found for containerKey: $containerKey")

    }

    private fun createAdCallBack(adCallback: AdCallback) = object : AdCallback {
        override fun onAdLoaded() {
            Logger.d("Ad loaded successfully!")
            adCallback.onAdLoaded()
        }

        override fun onAdClicked() {
            adCallback.onAdClicked()
        }

        override fun onAdFailed(error: String) {
            Logger.e("Failed to load ad: $error")
            adCallback.onAdFailed(error)
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