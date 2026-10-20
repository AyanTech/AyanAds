package ir.ayantech.ayanadmanager.core

import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import ir.ayantech.ayanadmanager.databinding.AdmobNativeLayoutBinding
import ir.ayantech.ayanadmanager.databinding.NativeLayoutBinding
import ir.ayantech.ayanadmanager.domain.model.AdUnit
import ir.ayantech.ayanadmanager.model.api.AddStatisticsInputParameters
import ir.ayantech.ayanadmanager.networks.admob.AdmobProvider
import ir.ayantech.ayanadmanager.networks.admob.ConsentManager
import ir.ayantech.ayanadmanager.networks.hamrahAds.components.init
import ir.ayantech.ayanadmanager.utils.AdSizeType
import ir.ayantech.ayanadmanager.utils.ContainerType
import ir.ayantech.ayanadmanager.utils.constant.AdSource
import ir.ayantech.ayanadmanager.utils.getAppVersion
import ir.ayantech.ayanadmanager.utils.getOsVersion
import ir.ayantech.hamrahads.HamrahAds
import ir.ayantech.hamrahads.ads.LoadedAd
import ir.ayantech.hamrahads.domain.model.AdResult
import ir.ayantech.hamrahads.domain.model.BannerAd
import ir.ayantech.hamrahads.domain.model.NativeAd as HamrahNativeAd
import ir.ayantech.hamrahads.listener.AdDisplayListener
import ir.ayantech.hamrahads.model.enums.BannerSize
import ir.ayantech.hamrahads.model.error.HamrahAdsError
import java.util.UUID
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine

/** Owned by a screen slot, never by the application singleton or a ViewModel. */
internal interface PreparedInlineAd : AutoCloseable {
    fun bind(container: ViewGroup, callback: AdCallback): AdHandle
    fun setActive(active: Boolean) {}
}

internal suspend fun prepareInlineAd(
    activity: AppCompatActivity,
    owner: LifecycleOwner,
    slot: AdSlot,
    unit: AdUnit,
): PreparedInlineAd {
    require(unit.containerType != ContainerType.Interstitial) { "Interstitials cannot be prepared as list rows." }
    val statistics = AddStatisticsInputParameters(
        unit.containerKey, unit.adUnitId, unit.adSource.name, AyanAdManager.appMarket.value,
        getAppVersion(activity.applicationContext), null, osName = "android", osVersion = getOsVersion(),
    )
    return try { when (unit.adSource) {
        AdSource.HamrahAd -> {
            val loaded = when (unit.containerType) {
                ContainerType.Native -> HamrahAds.loadNative(activity.applicationContext, unit.adUnitId).value()
                ContainerType.Banner -> HamrahAds.loadBanner(activity.applicationContext, unit.adUnitId).value()
            }
            HamrahPreparedAd(activity, owner, slot, loaded, statistics)
        }
        AdSource.AdMob -> {
            check(ConsentManager.canShowAds()) { "Ads are currently unavailable due to privacy settings." }
            val events = GoogleEvents(statistics)
            if (unit.containerType == ContainerType.Native) {
                GoogleNativeAd(activity, slot, awaitNative(activity, unit.adUnitId, events), events)
            } else {
                GoogleBannerAd(awaitBanner(activity, unit.adUnitId, slot.adSize, events), events)
            }
        }
    } } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        sendStatistics(statistics.copy(failureCause = error.message ?: "Ad preparation failed."))
        throw error
    }
}

private fun <T> AdResult<T>.value(): T = when (this) {
    is AdResult.Success -> data
    is AdResult.Error -> error(errorResponse.description ?: "Ad request failed.")
}

private class HamrahPreparedAd(
    private val activity: AppCompatActivity,
    private val owner: LifecycleOwner,
    private val slot: AdSlot,
    private val loaded: LoadedAd<*>,
    private val statistics: AddStatisticsInputParameters,
) : PreparedInlineAd {
    override fun bind(container: ViewGroup, callback: AdCallback): AdHandle {
        val events = object : AdDisplayListener {
            override fun onLoaded() = callback.onAdLoaded()
            override fun onDisplayed() = sendStatistics(statistics)
            override fun onClick() { submitClick(statistics); callback.onAdClicked() }
            override fun onError(error: HamrahAdsError) = callback.onAdFailed(error.description ?: "Ad display failed.")
        }
        val handle = DisposableAdHandle()
        when (loaded.ad) {
            is HamrahNativeAd -> {
                // Feed templates use explicit references/SDK IDs, without rewriting host IDs.
                val view = NativeLayoutBinding.inflate(activity.layoutInflater, container, false)
                view.init(slot.nativeAdAttributes)
                container.addView(view.root)
                @Suppress("UNCHECKED_CAST")
                val display = HamrahAds.ShowNativeAds()
                    .setContext(activity).setLifecycleOwner(owner).setViewGroup(view.root)
                    .setAd(loaded as LoadedAd<HamrahNativeAd>)
                    .initListener(events).build()
                handle.action = {
                    display?.destroyAds()
                    container.removeView(view.root)
                }
            }
            is BannerAd -> {
                @Suppress("UNCHECKED_CAST")
                val display = HamrahAds.ShowBannerAds()
                    .setContext(activity).setLifecycleOwner(owner).setViewGroup(container)
                    .setAd(loaded as LoadedAd<BannerAd>)
                    .setSize(when (slot.adSize) {
                        AdSizeType.MEDIUM -> BannerSize.BANNER_640x1136
                        AdSizeType.LARGE -> BannerSize.BANNER_1136x640
                        else -> BannerSize.BANNER_320x50
                    }).initListener(events).build()
                handle.action = { display?.destroyAds() }
            }
        }
        return handle
    }

    override fun close() {
        loaded.close()
        AyanAdManager.session?.release(statistics.trackingId)
    }
}

private class GoogleEvents(private var statistics: AddStatisticsInputParameters) : AdListener() {
    var callback: AdCallback? = null
    private var impressed = false
    override fun onAdLoaded() {
        // AdView can refresh itself; each new banner response needs its own tracker.
        AyanAdManager.session?.release(statistics.trackingId)
        statistics = statistics.copy(trackingId = UUID.randomUUID().toString())
        impressed = false
    }
    override fun onAdClicked() {
        submitClick(statistics)
        callback?.onAdClicked()
    }
    override fun onAdImpression() {
        if (!impressed) {
            impressed = true
            sendStatistics(statistics)
        }
    }
    fun close() {
        callback = null
        AyanAdManager.session?.release(statistics.trackingId)
    }
}

private suspend fun awaitNative(activity: AppCompatActivity, unitId: String, events: GoogleEvents): NativeAd =
    suspendCancellableCoroutine { continuation ->
        val main = Handler(Looper.getMainLooper())
        AdLoader.Builder(activity.applicationContext, unitId).forNativeAd { ad ->
            if (continuation.isActive) {
                continuation.resume(ad) { _, discarded, _ -> main.post { discarded.destroy() } }
            } else ad.destroy()
        }.withAdListener(object : AdListener() {
            override fun onAdFailedToLoad(error: LoadAdError) {
                if (continuation.isActive) continuation.resumeWithException(IllegalStateException(error.message))
            }
            override fun onAdClicked() = events.onAdClicked()
            override fun onAdImpression() = events.onAdImpression()
        }).build().loadAd(AdRequest.Builder().build())
    }

private suspend fun awaitBanner(
    activity: AppCompatActivity, unitId: String, size: AdSizeType?, events: GoogleEvents,
): AdView = suspendCancellableCoroutine { continuation ->
    val view = AdView(activity).apply {
        adUnitId = unitId
        setAdSize(when (size) {
            AdSizeType.MEDIUM -> AdSize.LARGE_BANNER
            AdSizeType.LARGE -> AdSize.MEDIUM_RECTANGLE
            else -> AdSize.BANNER
        })
    }
    continuation.invokeOnCancellation { activity.runOnUiThread { view.destroy() } }
    view.adListener = object : AdListener() {
        override fun onAdLoaded() {
            view.adListener = events
            view.pause()
            if (continuation.isActive) continuation.resume(view) { _, discarded, _ ->
                activity.runOnUiThread { discarded.destroy() }
            } else view.destroy()
        }
        override fun onAdFailedToLoad(error: LoadAdError) {
            view.destroy()
            if (continuation.isActive) continuation.resumeWithException(IllegalStateException(error.message))
        }
    }
    view.loadAd(AdRequest.Builder().build())
}

private class GoogleNativeAd(
    private val activity: AppCompatActivity,
    private val slot: AdSlot,
    private val ad: NativeAd,
    private val events: GoogleEvents,
) : PreparedInlineAd {
    override fun bind(container: ViewGroup, callback: AdCallback): AdHandle {
        val binding = AdmobNativeLayoutBinding.inflate(activity.layoutInflater, container, false)
        AdmobProvider().populateDefaultNativeAdView(ad, binding, slot.nativeAdAttributes)
        events.callback = callback
        container.addView(binding.root)
        // Post so the owner has registered the returned handle before client callbacks run.
        binding.root.post { if (events.callback === callback) callback.onAdLoaded() }
        return DisposableAdHandle().apply {
            action = {
                events.callback = null
                container.removeView(binding.root)
                binding.root.destroy()
            }
        }
    }
    override fun close() { events.close(); ad.destroy() }
}

private class GoogleBannerAd(private val view: AdView, private val events: GoogleEvents) : PreparedInlineAd {
    override fun bind(container: ViewGroup, callback: AdCallback): AdHandle {
        events.callback = callback
        container.addView(view)
        view.post { if (events.callback === callback) callback.onAdLoaded() }
        return DisposableAdHandle().apply {
            action = {
                events.callback = null
                view.pause()
                (view.parent as? ViewGroup)?.removeView(view)
            }
        }
    }
    override fun setActive(active: Boolean) { if (active) view.resume() else view.pause() }
    override fun close() { events.close(); view.destroy() }
}
