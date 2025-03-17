package ir.ayantech.ayanadmanager.networks.admob

import android.app.Activity
import android.graphics.drawable.Drawable
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MediaContent
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import ir.ayantech.ayanadmanager.R
import ir.ayantech.ayanadmanager.core.AdCallback
import ir.ayantech.ayanadmanager.core.AdProvider
import ir.ayantech.ayanadmanager.core.sendStatistics
import ir.ayantech.ayanadmanager.core.submitClick
import ir.ayantech.ayanadmanager.databinding.AdmobNativeLayoutBinding
import ir.ayantech.ayanadmanager.model.AdMobConfig
import ir.ayantech.ayanadmanager.model.AdRequestConfig
import ir.ayantech.ayanadmanager.model.api.AddStatisticsInputParameters
import ir.ayantech.ayanadmanager.utils.ContainerType
import ir.ayantech.ayanadmanager.utils.Logger
import ir.ayantech.ayanadmanager.utils.constant.Config.GOOGLE_AD_VIEW
import ir.ayantech.ayanadmanager.utils.makeGone
import ir.ayantech.ayanadmanager.utils.makeVisible
import ir.ayantech.ayanadmanager.utils.trying

class AdmobProvider : AdProvider {

    private var mInterstitialAd: InterstitialAd? = null
    private var currentNativeAd: NativeAd? = null
    private var adView: AdView? = null

    override fun loadAd(
        config: AdRequestConfig
    ) {
        (config as AdMobConfig).apply {
            if (addStatisticsInput.AdUnitId.isNullOrEmpty()) {
                callback.onAdFailed("AdUnitId cannot be empty")
                return
            }

            when (containerType) {
                ContainerType.Banner -> {
                    viewGroup?.let {
                        adView = getAdView(viewGroup).findViewWithTag(GOOGLE_AD_VIEW)
                        showBannerAd(
                            adView = adView,
                            statistics = addStatisticsInput,
                            callback = callback
                        )
                    } ?: {
                        Logger.e("ViewGroup can not be null !")
                        callback.onAdFailed("ViewGroup can not be null !")
                    }
                }

                ContainerType.Interstitial -> {
                    showInterstitialAd(
                        activity = activity,
                        statistics = addStatisticsInput,
                        callback = callback
                    )
                }

                ContainerType.Native -> {
                    viewGroup?.let {
                        showNativeAd(
                            activity = activity,
                            statistics = addStatisticsInput,
                            viewGroup = it,
                            useDefaultNativeView = useDefaultNativeAdView,
                            callback = callback
                        )
                    } ?: {
                        Logger.e("ViewGroup can not be null !")
                        callback.onAdFailed("ViewGroup can not be null !")
                    }
                }
            }
        }
    }

    private fun showBannerAd(
        adView: AdView?,
        statistics: AddStatisticsInputParameters,
        callback: AdCallback
    ) {
        adView?.let { view ->
            val adRequest = AdRequest.Builder().build()
            view.apply {
                loadAd(adRequest)
                adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        super.onAdLoaded()
                        callback.onAdLoaded()
                        sendStatistics(input = statistics)
                    }

                    override fun onAdClicked() {
                        super.onAdClicked()
                        submitClick()
                        callback.onAdClicked()
                    }

                    override fun onAdFailedToLoad(p0: LoadAdError) {
                        super.onAdFailedToLoad(p0)
                        handleAdError(error = p0.toString(), callback, statistics)
                    }
                }
            }
        } ?: callback.onAdFailed("Google AdView is Null")
    }

    private fun showInterstitialAd(
        activity: Activity,
        statistics: AddStatisticsInputParameters,
        callback: AdCallback
    ) {
        mInterstitialAd = null
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            activity,
            statistics.AdUnitId ?: "",
            adRequest,
            object : InterstitialAdLoadCallback() {

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    callback.onAdFailed(adError.toString())
                    mInterstitialAd = null
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    callback.onAdLoaded()
                    sendStatistics(statistics)
                    mInterstitialAd = interstitialAd
                    mInterstitialAd?.fullScreenContentCallback =
                        object : FullScreenContentCallback() {
                            override fun onAdClicked() {
                                super.onAdClicked()
                                callback.onAdClicked()
                                submitClick()
                            }

                            override fun onAdDismissedFullScreenContent() {
                                super.onAdDismissedFullScreenContent()
                                mInterstitialAd = null
                            }
                        }
                    mInterstitialAd?.show(activity)
                }

            })
    }

    private fun showNativeAd(
        activity: Activity,
        statistics: AddStatisticsInputParameters,
        useDefaultNativeView: Boolean,
        viewGroup: ViewGroup,
        callback: AdCallback
    ) {
        currentNativeAd?.destroy()

        val builder = AdLoader.Builder(activity, statistics.AdUnitId ?: "")

        builder.forNativeAd { nativeAd ->

            /** If this callback occurs after the activity is destroyed, must call
            destroy and return or you may get a memory leak. **/

            var activityDestroyed = activity.isDestroyed
            if (activityDestroyed || activity.isFinishing || activity.isChangingConfigurations) {
                nativeAd.destroy()
                return@forNativeAd
            }
            currentNativeAd?.destroy()
            currentNativeAd = nativeAd

            if (useDefaultNativeView) {
                val defaultNativeBinding = AdmobNativeLayoutBinding.inflate(activity.layoutInflater)
                populateDefaultNativeAdView(nativeAd, defaultNativeBinding)
                viewGroup.removeAllViews()
                viewGroup.addView(defaultNativeBinding.root)
            } else {
                populateCustomNativeAdView(nativeAd, viewGroup)
            }

        }

        val adLoader = builder.withAdListener(object : AdListener() {
            override fun onAdFailedToLoad(p0: LoadAdError) {
                super.onAdFailedToLoad(p0)
                handleAdError(error = p0.toString(), callback, statistics)
            }

            override fun onAdClicked() {
                super.onAdClicked()
                callback.onAdClicked()
                submitClick()
            }

            override fun onAdLoaded() {
                super.onAdLoaded()
                callback.onAdLoaded()
                sendStatistics(statistics)
            }
        }).build()

        adLoader.loadAd(AdRequest.Builder().build())
    }

    private fun populateCustomNativeAdView(nativeAd: NativeAd, viewGroup: ViewGroup) {
        val context = viewGroup.context
        val existingAdView = viewGroup.findViewWithTag<NativeAdView>("NativeAdView")
        val nativeAdView = existingAdView ?: NativeAdView(context).apply {
            tag = "NativeAdView"
            viewGroup.addView(this, 0)
        }

        val title = nativeAd.headline
        val description = nativeAd.body
        val media = nativeAd.mediaContent
        val price = nativeAd.price
        val store = nativeAd.store
        val stars = nativeAd.starRating
        val banner = nativeAd.images.firstOrNull()?.drawable
        val icon = nativeAd.icon?.drawable
        val cta = nativeAd.callToAction

        val idMapping = mapOf(
            R.id.ad_title to title,
            R.id.ad_banner to banner,
            R.id.ad_icon to icon,
            R.id.ad_cta to cta,
            R.id.ad_description to description,
            R.id.ad_cta_view to cta,
            R.id.ad_media to media,
            R.id.ad_price to price,
            R.id.ad_store to store,
            R.id.ad_stars to stars,
        )

        fun traverseViews(parent: ViewGroup) {
            for (i in 0 until parent.childCount) {
                val child = parent.getChildAt(i)

                idMapping[child.id]?.let { value ->
                    when {
                        child is TextView && value is String -> child.apply {
                            text =
                                value.takeIf { it.isNotBlank() } ?: run {
                                    makeGone()
                                    return@apply
                                }
                        }

                        child is ImageView && value is Drawable -> child.apply {
                            setImageDrawable(value)
                        }

                        child is Button && value is String -> child.apply {
                            text = value.takeIf {
                                it.isNotBlank()
                            } ?: run {
                                makeGone()
                                return@apply
                            }
                        }

                        child is MediaView && value is MediaContent -> child.apply {
                            nativeAdView.mediaView = child
                            mediaContent = value
                        }

                        child is RatingBar && value is Double -> child.apply {
                            rating = value.toFloat()
                        }

                    }
                }

                if (child is ViewGroup) traverseViews(child)
            }
        }

        traverseViews(viewGroup)
        nativeAdView.callToActionView = viewGroup.findViewById(R.id.ad_cta)
        nativeAdView.setNativeAd(nativeAd)

    }

    private fun populateDefaultNativeAdView(nativeAd: NativeAd, binding: AdmobNativeLayoutBinding) {

        val nativeAdView = binding.root

        with(binding.nativeLayout) {

            nativeAdView.apply {
                mediaView = binding.mediaView
                headlineView = hamrahAdNativeTitle
                bodyView = hamrahAdNativeDescription
                callToActionView = hamrahAdNativeCta
                iconView = hamrahAdNativeLogo
            }

            hamrahAdNativeTitle.text = nativeAd.headline
            nativeAd.mediaContent?.let { binding.mediaView.mediaContent = it }

            if (nativeAd.body.isNullOrBlank()) {
                hamrahAdNativeDescription.makeGone()
            } else {
                hamrahAdNativeDescription.apply {
                    makeVisible()
                    text = nativeAd.body
                }
            }

            if (nativeAd.icon == null) {
                hamrahAdNativeLogo.makeGone()
            } else {
                hamrahAdNativeLogo.setImageDrawable(nativeAd.icon?.drawable)
                hamrahAdNativeLogo.makeVisible()
            }

            if (nativeAd.callToAction.isNullOrBlank()) {
                hamrahAdNativeCta.makeGone()
            } else {
                hamrahAdNativeCta.apply {
                    makeVisible()
                    text = nativeAd.callToAction
                }
            }

            /** This method tells the Google Mobile Ads SDK that you have finished populating your
            native ad view with this native ad. **/

            nativeAdView.setNativeAd(nativeAd)

        }

    }

    private fun handleAdError(
        error: String,
        callback: AdCallback,
        statistics: AddStatisticsInputParameters
    ) {
        statistics.FailureCause = error
        callback.onAdFailed(statistics.FailureCause!!)
        sendStatistics(statistics)
    }

    private fun destroyBannerAd() {
        adView?.destroy()
    }

    private fun destroyNativeAd() {
        currentNativeAd?.destroy()
    }

    private fun destroyInterstitialAd() {
        mInterstitialAd = null
    }

    override fun destroy() {
        trying {
            destroyInterstitialAd()
            destroyBannerAd()
            destroyNativeAd()
        }
    }
}