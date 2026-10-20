package ir.ayantech.ayanadmanager.core

import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import ir.ayantech.ayanadmanager.domain.model.AdUnit
import ir.ayantech.ayanadmanager.model.AdRequestConfigBuilder
import ir.ayantech.ayanadmanager.model.api.AddStatisticsInputParameters
import ir.ayantech.ayanadmanager.networks.admob.AdmobProvider
import ir.ayantech.ayanadmanager.networks.hamrahAds.HamrahAdProvider
import ir.ayantech.ayanadmanager.networks.hamrahAds.components.NativeAdAttributes
import ir.ayantech.ayanadmanager.utils.AdSizeType
import ir.ayantech.ayanadmanager.utils.Logger
import ir.ayantech.ayanadmanager.utils.constant.AdSource
import ir.ayantech.ayanadmanager.utils.constant.Config.PLATFORM
import ir.ayantech.ayanadmanager.utils.getAppVersion
import ir.ayantech.ayanadmanager.utils.getOsVersion
import kotlin.time.Duration.Companion.seconds

/** Owns one request per placement, including fallback providers and lifecycle cleanup. */
class AdProviderManager {
    private data class Placement(val activity: AppCompatActivity, val target: Any)
    private val requests = mutableMapOf<Placement, AdLoadSequence>()
    private var appVersion: Long? = null

    fun loadAndShowAd(
        containerKey: String,
        adUnits: List<AdUnit>,
        callback: AdCallback? = null,
        appCompatActivity: AppCompatActivity,
        viewGroup: ViewGroup? = null,
        nativeAdAttributes: NativeAdAttributes = NativeAdAttributes(),
        useDefaultNativeAdView: Boolean = true,
        adSize: AdSizeType? = null,
    ) {
        bindAd(containerKey, adUnits, callback, appCompatActivity, viewGroup, nativeAdAttributes, useDefaultNativeAdView, adSize, appCompatActivity)
    }

    internal fun bindAd(
        containerKey: String,
        adUnits: List<AdUnit>,
        callback: AdCallback?,
        appCompatActivity: AppCompatActivity,
        viewGroup: ViewGroup?,
        nativeAdAttributes: NativeAdAttributes,
        useDefaultNativeAdView: Boolean,
        adSize: AdSizeType?,
        lifecycleOwner: LifecycleOwner,
    ): AdHandle {
        val handle = DisposableAdHandle()
        if (lifecycleOwner.lifecycle.currentState == Lifecycle.State.DESTROYED ||
            appCompatActivity.lifecycle.currentState == Lifecycle.State.DESTROYED || appCompatActivity.isFinishing) {
            callback?.onAdFailed("The ad Activity is no longer active.")
            return handle
        }
        if (adUnits.isEmpty()) {
            callback?.onAdFailed("No ad found for containerKey: $containerKey")
            return handle
        }
        val placement = Placement(appCompatActivity, viewGroup ?: containerKey)
        requests.remove(placement)?.cancel()
        val version = appVersion ?: getAppVersion(appCompatActivity.applicationContext).also { appVersion = it }
        lateinit var sequence: AdLoadSequence
        val observer = object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) { handle.dispose() }
        }
        var currentProvider: AdProvider? = null
        sequence = AdLoadSequence(
            count = adUnits.size,
            createProvider = { index ->
                when (adUnits[index].adSource) {
                    AdSource.AdMob -> AdmobProvider()
                    AdSource.HamrahAd -> HamrahAdProvider()
                }
            },
            load = { provider, index, events ->
                val unit = adUnits[index]
                // SDK callbacks may arrive off the main thread; serialize fallback and view work.
                val mainEvents = object : AdCallback {
                    override fun onAdLoaded() = appCompatActivity.runOnUiThread { events.onAdLoaded() }
                    override fun onAdClicked() = appCompatActivity.runOnUiThread { events.onAdClicked() }
                    override fun onAdFailed(error: String) = appCompatActivity.runOnUiThread { events.onAdFailed(error) }
                }
                val statistics = AddStatisticsInputParameters(
                    containerKey = unit.containerKey,
                    adSource = unit.adSource.name,
                    adUnitId = unit.adUnitId,
                    appMarket = AyanAdManager.appMarket.value,
                    appVersion = version,
                    failureCause = null,
                    osName = PLATFORM,
                    osVersion = getOsVersion(),
                )
                val builder = AdRequestConfigBuilder(unit.containerType, appCompatActivity, statistics, mainEvents)
                when (unit.adSource) {
                    AdSource.AdMob -> builder.setAdMobConfig(
                        unit.adUnitId, viewGroup, useDefaultNativeAdView, nativeAdAttributes, adSize,
                    )
                    AdSource.HamrahAd -> builder.setHamrahAdConfig(
                        adSize, viewGroup, nativeAdAttributes, useDefaultNativeAdView,
                    )
                }
                provider.loadAd(builder.build(unit.adSource))
            },
            callback = callback,
            onAttemptFailed = { index, error ->
                val unit = adUnits[index]
                Logger.e("${unit.adSource} ${unit.containerType} attempt ${index + 1}/${adUnits.size} failed: $error")
            },
            onProviderChanged = { next ->
                if (next != null || AyanAdManager.adProvider === currentProvider) {
                    AyanAdManager.adProvider = next
                }
                currentProvider = next
            },
            onFinished = {
                if (requests[placement] === sequence) requests.remove(placement)
                appCompatActivity.lifecycle.removeObserver(observer)
                lifecycleOwner.lifecycle.removeObserver(observer)
                handle.action = null
            },
            scheduleTimeout = { timeout ->
                val handler = Handler(Looper.getMainLooper())
                val task = Runnable(timeout)
                handler.postDelayed(task, 15.seconds.inWholeMilliseconds)
                val cancel: () -> Unit = { handler.removeCallbacks(task) }
                cancel
            },
        )
        requests[placement] = sequence
        handle.action = sequence::cancel
        appCompatActivity.lifecycle.addObserver(observer)
        lifecycleOwner.lifecycle.addObserver(observer)
        sequence.start()
        return handle
    }

    fun destroyAll() {
        val active = requests.values.toList()
        requests.clear()
        active.forEach { it.cancel() }
        appVersion = null
    }
}
