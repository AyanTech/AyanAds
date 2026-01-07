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
import ir.ayantech.hamrahads.listener.InitListener
import ir.ayantech.hamrahads.model.error.HamrahAdsError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object AyanAdManager {
    private var isInitialized = false
    lateinit var adProvider: AdProvider
    lateinit var ayanAdApi: AyanApi
    private lateinit var adManager: AdProviderManager
    var clickTrackers = mutableMapOf<String, String>()
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
        if (appKey.isNotBlank()) {
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
                        if (it.appId.isNullOrEmpty()) {
                            Logger.e("${it.adSource} appId is not valid.")
                        }

                        when (it.adSource) {
                            AdSource.HamrahAd -> {
                                initializeHamrahAds(
                                    appCompatActivity = appCompatActivity,
                                    appId = it.appId ?: "",
                                    onSuccess = onSuccess,
                                    onError = onError
                                )
                            }

                            AdSource.AdMob -> {
                                initializeMobileAds(appCompatActivity)
                            }
                        }
                    }
                },
                onFailed = { failure ->
                    isInitialized = false
                    onError.invoke(failure.failureMessage)
                }
            )
        } else {
            onError.invoke("appKey is blank.")
        }
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
        appId: String,
        onSuccess: SimpleCallBack,
        onError: StringCallBack
    ) {
        HamrahAds.Initializer()
            .setContext(appCompatActivity)
            .initId(appId)
            .initListener(object : InitListener {
                override fun onSuccess() {
                    onSuccess.invoke()
                }

                override fun onError(error: HamrahAdsError) {
                    super.onError(error)
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
            } ?: run {
            val message = "No ad found for containerKey: $containerKey"
            Logger.w(message)
            adCallback.onAdFailed(error = message)
        }

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