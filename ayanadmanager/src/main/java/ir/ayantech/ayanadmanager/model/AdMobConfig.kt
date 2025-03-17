package ir.ayantech.ayanadmanager.model

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import ir.ayantech.ayanadmanager.core.AdCallback
import ir.ayantech.ayanadmanager.model.api.AddStatisticsInputParameters
import ir.ayantech.ayanadmanager.utils.ContainerType
import ir.ayantech.ayanadmanager.utils.constant.Config.GOOGLE_AD_VIEW

data class AdMobConfig(
    val containerType: ContainerType,
    val adUnitId: String,
    val activity: Activity,
    val adSize: AdSize,
    val viewGroup: ViewGroup?,
    val useDefaultNativeAdView: Boolean,
    val addStatisticsInput: AddStatisticsInputParameters,
    val callback: AdCallback
) : AdRequestConfig(containerType, activity, addStatisticsInput, callback) {


    override fun getAdView(parentView: ViewGroup): View {
        val adView = AdView(activity)
        adView.apply {
            tag = GOOGLE_AD_VIEW
            adUnitId = this@AdMobConfig.adUnitId
            setAdSize(this@AdMobConfig.adSize)
        }
        parentView.apply {
            removeAllViews()
            addView(adView)
        }
        return adView
    }
}
