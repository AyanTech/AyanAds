package ir.ayantech.ayanadmanager.core

import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import androidx.annotation.MainThread
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.MobileAds
import ir.ayantech.ayanadmanager.BuildConfig
import ir.ayantech.ayanadmanager.di.createAdsSession
import ir.ayantech.ayanadmanager.domain.model.AdConfiguration
import ir.ayantech.ayanadmanager.domain.model.AdUnit
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
import ir.ayantech.ayanadmanager.utils.constant.Config
import ir.ayantech.hamrahads.HamrahAds
import ir.ayantech.hamrahads.listener.InitializationListener
import ir.ayantech.hamrahads.model.error.HamrahAdsError
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull

object AyanAdManager {
    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }
    private val selectAdUnits: SelectAdUnits = SelectAdUnitsImpl()
    private var initialized = false
    private var initializationJob: Job? = null
    var adProvider: AdProvider? = null
    private val adManager = AdProviderManager()
    private val screenSessions = mutableSetOf<ScreenAdSession>()
    internal var session: AdsSession? = null
        private set
    private var configuration: AdConfiguration? = null
    private var adUnitsByContainer = emptyMap<String, List<AdUnit>>()
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
                        withTimeout(Config.TIMEOUT) {
                            when (provider.adSource) {
                                AdSource.HamrahAd -> initializeHamrahAds(appCompatActivity, provider.appId.orEmpty())
                                AdSource.AdMob -> initializeMobileAds(appCompatActivity)
                            }
                        }
                        true
                    } catch (timeout: TimeoutCancellationException) {
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
            val initializer = HamrahAds.Initializer().setContext(activity.applicationContext).initId(appId)
                .initListener(object : InitializationListener {
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
            if (initializer == null) {
                if (continuation.isActive) continuation.resumeWithException(
                    IllegalStateException("Unable to create HamrahAds initializer.")
                )
            } else {
                continuation.invokeOnCancellation { initializer.cancelRequest() }
            }
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
        screenSessions.toList().forEach { it.close() }
        screenSessions.clear()
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
        bindAd(containerKey, appCompatActivity, appCompatActivity, adSize, adContainerId,
            nativeAdAttributes, useDefaultNativeAdView, adCallback)
    }

    /** Owns one placement; use viewLifecycleOwner for fragments and dispose on recycling. */
    @MainThread
    fun bindAd(
        containerKey: String,
        appCompatActivity: AppCompatActivity,
        lifecycleOwner: LifecycleOwner,
        adSize: AdSizeType? = null,
        adContainerId: ViewGroup? = null,
        nativeAdAttributes: NativeAdAttributes = NativeAdAttributes(),
        useDefaultNativeAdView: Boolean = true,
        adCallback: AdCallback? = null,
    ): AdHandle {
        check(Looper.myLooper() == Looper.getMainLooper()) { "bindAd must run on the main thread." }
        val config = configuration
        if (!initialized || config == null) {
            adCallback?.onAdFailed("SDK is not initialized.")
            return DisposableAdHandle()
        }
        adUnitsByContainer[containerKey]?.takeIf { it.isNotEmpty() }
            ?.let { filteredAdUnits ->
                return adManager.bindAd(
                    containerKey = containerKey,
                    adUnits = filteredAdUnits,
                    callback = createAdCallBack(adCallback),
                    appCompatActivity = appCompatActivity,
                    viewGroup = adContainerId,
                    nativeAdAttributes = nativeAdAttributes,
                    useDefaultNativeAdView = useDefaultNativeAdView,
                    adSize = adSize,
                    lifecycleOwner = lifecycleOwner,
                )
            } ?: run {
            val message = "No ad found for containerKey: $containerKey"
            Logger.w(message)
            adCallback?.onAdFailed(error = message)
        }
        return DisposableAdHandle()
    }

    internal fun unitsFor(containerKey: String) = if (initialized) adUnitsByContainer[containerKey].orEmpty() else emptyList()

    /** A bounded native/banner working set, owned by the fragment's view lifecycle. */
    @MainThread
    fun createScreenSession(
        activity: AppCompatActivity,
        lifecycleOwner: LifecycleOwner,
        maxSlots: Int = 4,
        maxConcurrentLoads: Int = 2,
        attemptTimeout: Duration = 15.seconds,
    ): ScreenAdSession {
        check(Looper.myLooper() == Looper.getMainLooper())
        require(attemptTimeout > Duration.ZERO)
        val session = ScreenAdSession(activity, lifecycleOwner, maxSlots, maxConcurrentLoads,
            onClosed = { screenSessions.remove(it) }) { slot ->
            val units = unitsFor(slot.containerKey)
            check(units.isNotEmpty()) { "Initialize the SDK before preparing a configured placement." }
            var lastError = "No available provider."
            var prepared: PreparedInlineAd? = null
            for (unit in units) {
                try {
                    prepared = withTimeoutOrNull(attemptTimeout) {
                        prepareInlineAd(activity, lifecycleOwner, slot, unit)
                    }
                    if (prepared != null) break
                    lastError = "${unit.adSource} preparation timed out."
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    lastError = error.message ?: "Ad preparation failed."
                }
            }
            prepared ?: error(lastError)
        }
        if (lifecycleOwner.lifecycle.currentState != Lifecycle.State.DESTROYED && !activity.isDestroyed) {
            screenSessions += session
        }
        return session
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
