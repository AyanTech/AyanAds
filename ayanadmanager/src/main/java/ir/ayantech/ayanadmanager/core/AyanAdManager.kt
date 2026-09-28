package ir.ayantech.ayanadmanager.core

import android.view.ViewGroup
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.withTimeout
import ir.ayantech.ayanadmanager.utils.constant.Config
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.MobileAds
import ir.ayantech.ayanadmanager.BuildConfig
import ir.ayantech.ayanadmanager.di.createAdsSession
import ir.ayantech.ayanadmanager.domain.model.AdConfiguration
import ir.ayantech.ayanadmanager.domain.usecase.SelectAdUnits
import ir.ayantech.ayanadmanager.domain.usecase.SelectAdUnitsImpl
import ir.ayantech.ayanadmanager.networks.admob.ConsentManager
import ir.ayantech.ayanadmanager.networks.hamrahAds.components.NativeAdAttributes
import ir.ayantech.ayanadmanager.ui.AdsSession
import ir.ayantech.ayanadmanager.utils.AdSizeType
import ir.ayantech.ayanadmanager.utils.Logger
import ir.ayantech.ayanadmanager.utils.SimpleCallBack
import ir.ayantech.ayanadmanager.utils.StringCallBack
import ir.ayantech.ayanadmanager.utils.constant.AdSource
import ir.ayantech.ayanadmanager.utils.constant.AppMarket
import ir.ayantech.hamrahads.HamrahAds
import ir.ayantech.hamrahads.listener.InitListener
import ir.ayantech.hamrahads.model.error.HamrahAdsError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException


object AyanAdManager {
    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }
    private val selectAdUnits: SelectAdUnits = SelectAdUnitsImpl()
    private var initialized = false
    private var initializationJob: Job? = null
    var adProvider: AdProvider? = null
    private val adManager = AdProviderManager()
    internal var session: AdsSession? = null
        private set
    private var configuration: AdConfiguration? = null
    private var adUnitsByContainer = emptyMap<String, List<ir.ayantech.ayanadmanager.domain.model.AdUnit>>()
    var appKey = ""
        private set
    lateinit var appMarket: AppMarket
        private set

    fun initialize(
        appCompatActivity: AppCompatActivity,
        appKey: String,
        appMarket: AppMarket,
        onSuccess: SimpleCallBack? = null,
        onError: StringCallBack? = null
    ) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { initialize(appCompatActivity, appKey, appMarket, onSuccess, onError) }
            return
        }
        if (appCompatActivity.isDestroyed || appCompatActivity.isFinishing) {
            onError?.invoke("The ad Activity is no longer active.")
            return
        }
        if (appKey.isBlank()) {
            onError?.invoke("appKey is blank.")
            return
        }
        if (initialized) {
            if (this.appKey == appKey && this.appMarket == appMarket) onSuccess?.invoke()
            else onError?.invoke("SDK is already initialized with a different app key or market.")
            return
        }
        if (initializationJob?.isActive == true) {
            onError?.invoke("SDK initialization is already in progress.")
            return
        }
        this.appKey = appKey
        this.appMarket = appMarket
        Logger.setDebugMode(BuildConfig.DEBUG)
        session?.close()
        val currentSession = try {
            createAdsSession(appCompatActivity.applicationContext, appKey)
        } catch (error: Exception) {
            session = null
            onError?.invoke(error.message ?: "Unable to create the ad client.")
            return
        }
        session = currentSession
        initializationJob = appCompatActivity.lifecycleScope.launch {
            try {
                val config = currentSession.load(appKey)
                val available = config.providers.distinctBy { it.adSource }.filter { provider ->
                    try {
                        withTimeout(Config.TIMEOUT * 1_000L) {
                            when (provider.adSource) {
                                AdSource.HamrahAd -> initializeHamrahAds(appCompatActivity, provider.appId.orEmpty())
                                AdSource.AdMob -> initializeMobileAds(appCompatActivity)
                            }
                        }
                        true
                    } catch (timeout: kotlinx.coroutines.TimeoutCancellationException) {
                        Logger.w("${provider.adSource} initialization timed out.")
                        false
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        Logger.w("${provider.adSource} initialization failed: ${error.message}")
                        false
                    }
                }
                check(available.isNotEmpty()) { "No ad provider could be initialized." }
                val sources = available.mapTo(mutableSetOf()) { it.adSource }
                val readyConfig = config.copy(providers = available, adUnits = config.adUnits.filter { it.adSource in sources })
                configuration = readyConfig
                adUnitsByContainer = selectAdUnits(readyConfig)
                initialized = true
            } catch (cancelled: CancellationException) {
                currentSession.close()
                if (session === currentSession) session = null
                throw cancelled
            } catch (error: Exception) {
                initialized = false
                currentSession.close()
                if (session === currentSession) session = null
                onError?.invoke(error.message ?: "Unable to initialize ads.")
                return@launch
            }
            onSuccess?.invoke()
        }
    }

    private suspend fun initializeMobileAds(activity: AppCompatActivity) {
        val allowed = suspendCancellableCoroutine<Boolean> { continuation ->
            ConsentManager.initialize(activity) { allowed ->
                if (continuation.isActive) continuation.resume(allowed)
            }
        }
        check(allowed) { "Ads are currently unavailable due to privacy settings." }
        withContext(Dispatchers.IO) {
            suspendCancellableCoroutine<Unit> { continuation ->
                MobileAds.initialize(activity.applicationContext) {
                    if (continuation.isActive) continuation.resume(Unit)
                }
            }
        }
    }

    private suspend fun initializeHamrahAds(activity: AppCompatActivity, appId: String) {
        require(appId.isNotBlank()) { "HamrahAd appId is not valid." }
        suspendCancellableCoroutine<Unit> { continuation ->
            HamrahAds.Initializer().setContext(activity).initId(appId)
                .initListener(object : InitListener {
                    override fun onSuccess() {
                        if (continuation.isActive) continuation.resume(Unit)
                    }

                    override fun onError(error: HamrahAdsError) {
                        if (continuation.isActive) continuation.resumeWithException(
                            IllegalStateException(
                                error.description ?: "Unable to initialize HamrahAds."
                            )
                        )
                    }
                }).build()
        }
    }

    /** Cancel SDK requests and release the HTTP client when the SDK is no longer needed. */
    fun shutdown() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { shutdown() }
            return
        }
        initializationJob?.cancel()
        initializationJob = null
        session?.close()
        session = null
        adManager.destroyAll()
        adProvider = null
        configuration = null
        adUnitsByContainer = emptyMap()
        initialized = false
    }

    /**
     * Displays an advertisement based on the provided containerKey.
     */
    fun showAd(
        containerKey: String,
        appCompatActivity: AppCompatActivity,
        adSize: AdSizeType? = null,
        adContainerId: ViewGroup? = null,
        nativeAdAttributes: NativeAdAttributes = NativeAdAttributes(),
        useDefaultNativeAdView: Boolean = true,
        adCallback: AdCallback? = null
    ) {

        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post {
                showAd(containerKey, appCompatActivity, adSize, adContainerId, nativeAdAttributes, useDefaultNativeAdView, adCallback)
            }
            return
        }
        val config = configuration
        if (!initialized || config == null) {
            adCallback?.onAdFailed("SDK is not initialized.")
            return
        }
        adUnitsByContainer[containerKey]?.takeIf { it.isNotEmpty() }
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
            adCallback?.onAdFailed(error = message)
        }

    }

    private fun createAdCallBack(adCallback: AdCallback?) = object : AdCallback {
        override fun onAdLoaded() {
            Logger.d("Ad loaded successfully!")
            adCallback?.onAdLoaded()
        }

        override fun onAdClicked() {
            adCallback?.onAdClicked()
        }

        override fun onAdFailed(error: String) {
            Logger.e("Failed to load ad: $error")
            adCallback?.onAdFailed(error)
        }
    }

    fun isInitialized(): Boolean = initialized

}
