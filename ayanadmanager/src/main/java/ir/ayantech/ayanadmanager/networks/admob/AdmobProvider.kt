package ir.ayantech.ayanadmanager.networks.admob

import android.graphics.drawable.Drawable
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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
import ir.ayantech.ayanadmanager.networks.hamrahAds.components.NativeAdAttributes
import ir.ayantech.ayanadmanager.utils.ContainerType
import ir.ayantech.ayanadmanager.utils.Logger
import ir.ayantech.ayanadmanager.utils.constant.Config.GOOGLE_AD_VIEW
import ir.ayantech.ayanadmanager.utils.makeGone
import ir.ayantech.ayanadmanager.utils.makeVisible
import ir.ayantech.ayanadmanager.utils.toPx

class AdmobProvider : AdProvider {

    private var generation = 0
    private var ownedNativeView: NativeAdView? = null
    private var mInterstitialAd: InterstitialAd? = null
    private var currentNativeAd: NativeAd? = null
    private var adView: AdView? = null

    override fun loadAd(
        config: AdRequestConfig
    ) {
        destroy()
        val adConfig = config as? AdMobConfig ?: return
        if (ConsentManager.canShowAds()) {
            adConfig.apply {
                if (addStatisticsInput.adUnitId.isNullOrEmpty()) {
                    callback?.onAdFailed("AdUnitId cannot be empty")
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
                        } ?: run {
                            Logger.e("ViewGroup can not be null !")
                            callback?.onAdFailed("ViewGroup can not be null !")
                        }
                    }

                    ContainerType.Interstitial -> {
                        showInterstitialAd(
                            appCompatActivity = appCompatActivity,
                            statistics = addStatisticsInput,
                            callback = callback
                        )
                    }

                    ContainerType.Native -> {
                        viewGroup?.let {
                            if (!useDefaultNativeAdView && findNativeAdView(it) == null) {
                                callback?.onAdFailed("Custom AdMob assets must be inside a NativeAdView.")
                                return
                            }
                            showNativeAd(
                                appCompatActivity = appCompatActivity,
                                statistics = addStatisticsInput,
                                viewGroup = it,
                                useDefaultNativeView = useDefaultNativeAdView,
                                nativeAdAttributes = nativeAdAttributes,
                                callback = callback
                            )
                        } ?: run {
                            Logger.e("ViewGroup can not be null !")
                            callback?.onAdFailed("ViewGroup can not be null !")
                        }
                    }
                }
            }
        } else {
            adConfig.callback?.onAdFailed("Ads are currently unavailable due to privacy settings")
        }

    }

    private fun showBannerAd(
        adView: AdView?,
        statistics: AddStatisticsInputParameters,
        callback: AdCallback?
    ) {
        val requestGeneration = generation
        adView?.let { view ->
            val adRequest = AdRequest.Builder().build()
            view.apply {
                adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        if (requestGeneration != generation) return
                        super.onAdLoaded()
                        sendStatistics(input = statistics)
                        callback?.onAdLoaded()
                    }

                    override fun onAdClicked() {
                        if (requestGeneration != generation) return
                        super.onAdClicked()
                        submitClick(adUnitId = statistics.adUnitId)
                        callback?.onAdClicked()
                    }

                    override fun onAdFailedToLoad(p0: LoadAdError) {
                        if (requestGeneration != generation) return
                        super.onAdFailedToLoad(p0)
                        handleAdError(error = p0.toString(), callback, statistics)
                    }
                }
                loadAd(adRequest)
            }
        } ?: callback?.onAdFailed("Google AdView is Null")
    }

    private fun showInterstitialAd(
        appCompatActivity: AppCompatActivity,
        statistics: AddStatisticsInputParameters,
        callback: AdCallback?
    ) {
        val requestGeneration = generation
        mInterstitialAd = null
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            appCompatActivity,
            statistics.adUnitId ?: "",
            adRequest,
            object : InterstitialAdLoadCallback() {

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    if (requestGeneration != generation) return
                    mInterstitialAd = null
                    handleAdError(adError.toString(), callback, statistics)
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    if (requestGeneration != generation) return
                    if (appCompatActivity.isDestroyed || appCompatActivity.isFinishing) return
                    mInterstitialAd = interstitialAd
                    mInterstitialAd?.fullScreenContentCallback =
                        object : FullScreenContentCallback() {
                            override fun onAdClicked() {
                                if (requestGeneration != generation) return
                                super.onAdClicked()
                                callback?.onAdClicked()
                                submitClick(adUnitId = statistics.adUnitId)
                            }

                            override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                                if (requestGeneration != generation) return
                                mInterstitialAd = null
                                handleAdError(error.toString(), callback, statistics)
                            }

                            override fun onAdDismissedFullScreenContent() {
                                if (requestGeneration != generation) return
                                super.onAdDismissedFullScreenContent()
                                mInterstitialAd = null
                            }
                        }
                    sendStatistics(statistics)
                    callback?.onAdLoaded()
                    if (requestGeneration == generation) mInterstitialAd?.show(appCompatActivity)
                }

            })
    }

    private fun showNativeAd(
        appCompatActivity: AppCompatActivity,
        statistics: AddStatisticsInputParameters,
        useDefaultNativeView: Boolean,
        nativeAdAttributes: NativeAdAttributes,
        viewGroup: ViewGroup,
        callback: AdCallback?
    ) {
        val requestGeneration = generation
        currentNativeAd?.destroy()

        val builder = AdLoader.Builder(appCompatActivity, statistics.adUnitId ?: "")

        builder.forNativeAd { nativeAd ->

            /** If this callback occurs after the activity is destroyed, must call
            destroy and return or you may get a memory leak. **/

            if (requestGeneration != generation || appCompatActivity.isDestroyed || appCompatActivity.isFinishing || appCompatActivity.isChangingConfigurations) {
                nativeAd.destroy()
                return@forNativeAd
            }
            currentNativeAd?.destroy()
            currentNativeAd = nativeAd

            if (useDefaultNativeView) {
                val defaultNativeBinding =
                    AdmobNativeLayoutBinding.inflate(appCompatActivity.layoutInflater)
                populateDefaultNativeAdView(nativeAd, defaultNativeBinding, nativeAdAttributes)
                ownedNativeView = defaultNativeBinding.root
                viewGroup.removeAllViews()
                viewGroup.addView(defaultNativeBinding.root)
            } else {
                populateCustomNativeAdView(nativeAd, viewGroup)
            }

        }

        val adLoader = builder.withAdListener(object : AdListener() {
            override fun onAdFailedToLoad(p0: LoadAdError) {
                if (requestGeneration != generation) return
                super.onAdFailedToLoad(p0)
                handleAdError(error = p0.toString(), callback, statistics)
            }

            override fun onAdClicked() {
                if (requestGeneration != generation) return
                super.onAdClicked()
                callback?.onAdClicked()
                submitClick(adUnitId = statistics.adUnitId)
            }

            override fun onAdLoaded() {
                if (requestGeneration != generation) return
                super.onAdLoaded()
                sendStatistics(statistics)
                callback?.onAdLoaded()
            }
        }).build()

        adLoader.loadAd(AdRequest.Builder().build())
    }

    private fun findNativeAdView(parent: ViewGroup): NativeAdView? {
        if (parent is NativeAdView) return parent
        for (index in 0 until parent.childCount) {
            val child = parent.getChildAt(index)
            if (child is ViewGroup) findNativeAdView(child)?.let { return it }
        }
        return null
    }

    private fun populateCustomNativeAdView(nativeAd: NativeAd, viewGroup: ViewGroup) {
        val nativeAdView = requireNotNull(findNativeAdView(viewGroup))

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

        traverseViews(nativeAdView)
        nativeAdView.headlineView = nativeAdView.findViewById(R.id.ad_title)
        nativeAdView.bodyView = nativeAdView.findViewById(R.id.ad_description)
        nativeAdView.iconView = nativeAdView.findViewById(R.id.ad_icon)
        nativeAdView.imageView = nativeAdView.findViewById(R.id.ad_banner)
        nativeAdView.priceView = nativeAdView.findViewById(R.id.ad_price)
        nativeAdView.storeView = nativeAdView.findViewById(R.id.ad_store)
        nativeAdView.starRatingView = nativeAdView.findViewById(R.id.ad_stars)
        nativeAdView.callToActionView = nativeAdView.findViewById(R.id.ad_cta)
        nativeAdView.setNativeAd(nativeAd)

    }

    private fun populateDefaultNativeAdView(
        nativeAd: NativeAd,
        binding: AdmobNativeLayoutBinding,
        nativeAdAttributes: NativeAdAttributes
    ) {

        val nativeAdView = binding.root

        with(binding.nativeLayout) {
            hamrahAdNativeBanner.setBackgroundResource(nativeAdAttributes.backgroundColor)

            nativeAdView.apply {
                mediaView = binding.mediaView
                headlineView = hamrahAdNativeTitle
                bodyView = hamrahAdNativeDescription
                callToActionView = hamrahAdNativeCta
                iconView = hamrahAdNativeLogo
            }

            hamrahAdNativeTitle.apply {
                text = nativeAd.headline
                setTextColor(ContextCompat.getColorStateList(context, nativeAdAttributes.titleColor))
                setTypeface(nativeAdAttributes.typeface)
            }

            nativeAd.mediaContent?.let { binding.mediaView.mediaContent = it }

            if (nativeAd.body.isNullOrBlank()) {
                hamrahAdNativeDescription.makeGone()
            } else {
                hamrahAdNativeDescription.apply {
                    makeVisible()
                    text = nativeAd.body
                    setTextColor(ContextCompat.getColorStateList(context, nativeAdAttributes.descriptionColor))
                    setTypeface(nativeAdAttributes.typeface)
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
                    layoutParams = layoutParams.apply {
                        width = nativeAdAttributes.buttonWidth.toPx(context)
                        height = nativeAdAttributes.buttonHeight.toPx(context)
                    }
                    setTypeface(nativeAdAttributes.typeface)
                    setTextColor(ContextCompat.getColorStateList(context, nativeAdAttributes.buttonTextColor))
                    background =
                        ContextCompat.getDrawable(this.context, R.drawable.button_background)
                    backgroundTintList =
                        ContextCompat.getColorStateList(context, nativeAdAttributes.buttonBackgroundTint)
                }
            }

            /** This method tells the Google Mobile Ads SDK that you have finished populating your
            native ad view with this native ad. **/

            nativeAdView.setNativeAd(nativeAd)

        }

    }

    private fun handleAdError(
        error: String,
        callback: AdCallback?,
        statistics: AddStatisticsInputParameters
    ) {
        sendStatistics(statistics.copy(failureCause = error))
        callback?.onAdFailed(error)
    }

    override fun destroy() {
        generation++
        mInterstitialAd?.fullScreenContentCallback = null
        mInterstitialAd = null
        adView?.let { view ->
            view.adListener = object : AdListener() {}
            (view.parent as? ViewGroup)?.removeView(view)
            view.destroy()
        }
        adView = null
        currentNativeAd?.destroy()
        currentNativeAd = null
        ownedNativeView?.let { view ->
            (view.parent as? ViewGroup)?.removeView(view)
            view.destroy()
        }
        ownedNativeView = null
    }
}
