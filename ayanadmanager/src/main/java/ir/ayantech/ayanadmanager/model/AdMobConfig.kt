package ir.ayantech.ayanadmanager.model

import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import ir.ayantech.ayanadmanager.core.AdCallback
import ir.ayantech.ayanadmanager.model.api.AddStatisticsInputParameters
import ir.ayantech.ayanadmanager.networks.hamrahAds.components.NativeAdAttributes
import ir.ayantech.ayanadmanager.utils.AdSizeType
import ir.ayantech.ayanadmanager.utils.ContainerType
import ir.ayantech.ayanadmanager.utils.constant.Config.GOOGLE_AD_VIEW

data class AdMobConfig(
    val containerType: ContainerType,
    val adUnitId: String,
    val appCompatActivity: AppCompatActivity,
    val adSize: AdSizeType?,
    val viewGroup: ViewGroup?,
    val useDefaultNativeAdView: Boolean,
    val nativeAdAttributes: NativeAdAttributes,
    val addStatisticsInput: AddStatisticsInputParameters,
    val callback: AdCallback
) : AdRequestConfig(containerType, appCompatActivity, addStatisticsInput, callback) {


    fun getAdSize(): AdSize {
        return when (adSize) {
            AdSizeType.SMALL -> AdSize.BANNER
            AdSizeType.MEDIUM -> AdSize.LARGE_BANNER
            AdSizeType.LARGE -> AdSize.MEDIUM_RECTANGLE
            null -> AdSize.BANNER
        }
    }

    override fun getAdView(parentView: ViewGroup): View {
        val adView = AdView(appCompatActivity)
        adView.apply {
            tag = GOOGLE_AD_VIEW
            adUnitId = this@AdMobConfig.adUnitId
            setAdSize(this@AdMobConfig.getAdSize())
        }
        parentView.apply {
            removeAllViews()
            addView(adView)
        }
        return adView
    }
}
