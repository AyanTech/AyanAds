package ir.ayantech.ayanadmanager

import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.toColorInt
import ir.ayantech.ayanadmanager.core.AdCallback
import ir.ayantech.ayanadmanager.core.AdProvider
import ir.ayantech.ayanadmanager.core.AyanAdManager
import ir.ayantech.ayanadmanager.core.AyanAdManager.adProvider
import ir.ayantech.ayanadmanager.networks.hamrahAds.HamrahAdProvider
import ir.ayantech.ayanadmanager.networks.hamrahAds.components.NativeAdAttributes
import ir.ayantech.ayanadmanager.utils.constant.AppMarket

class MainActivity : AppCompatActivity() {
    val appKey = "2a886212e7bdbb444a05a1649b2aa67f6aa0c1766410b9049fe83ad14c5392d1"

    val hamrahAdNativeContainerKey = "e60e4754-9b02-4af2-ab70-303afb873729"
    val hamrahAdBannerContainerKey = "1602e211-75cc-4da9-90f4-0fcca27dfe3e"
    val hamrahAdInterstitialContainerKey = "ff9d1067-bce0-463d-8697-024c35003d5c"
    val admobInterstitialContainerKey = "ff9d1067-bce0-463d-8697-024c35003d5c"
    val admobBannerContainerKey = "1602e211-75cc-4da9-90f4-0fcca27dfe3e"
    val admobNativeContainerKey = "e60e4754-9b02-4af2-ab70-303afb873729"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        AyanAdManager.initialize(
            appCompatActivity = this,
            appKey = appKey,
            appMarket = AppMarket.XiaomiStore
        )

        findViewById<Button>(R.id.ad1).setOnClickListener {
            AyanAdManager.showAd(
                containerKey = hamrahAdBannerContainerKey,
                appCompatActivity = this,
                adContainerId = findViewById(R.id.banner),
                adSize = null,
                adCallback = object : AdCallback {
                    override fun onAdLoaded() {
                        Log.d("mjmjmj", "onAdLoaded: ")
                    }

                    override fun onAdClicked() {
                        Log.d("mjmjmj", "onAdClicked: ")
                    }

                    override fun onAdFailed(error: String) {
                        Log.d("mjmjmj", "onAdFailed: $error ")
                    }

                }
            )
        }
        findViewById<Button>(R.id.ad2).setOnClickListener {
//            adProvider.destroy()
            AyanAdManager.showAd(
                useDefaultNativeAdView = true,
                containerKey = admobNativeContainerKey,
                appCompatActivity = this,
                adContainerId = findViewById(R.id.nativeAdMob),
                nativeAdAttributes = NativeAdAttributes(
                    titleColor = ContextCompat.getColor(
                        this,
                        R.color.black
                    ),
                    buttonTextColor = "#000000".toColorInt(),
                    buttonBackgroundTint = "#ffffff".toColorInt(),
                    typeface = ResourcesCompat.getFont(this, R.font.medium)
                ),
                adSize = null,
                adCallback = object : AdCallback {
                    override fun onAdLoaded() {
                        Log.d("mjmjmj", "onAdLoaded: ")
                    }

                    override fun onAdClicked() {
                        Log.d("mjmjmj", "onAdClicked: ")
                    }

                    override fun onAdFailed(error: String) {
                        Log.d("mjmjmj", "onAdFailed: $error ")
                    }

                }
            )
        }
        findViewById<Button>(R.id.ad3).setOnClickListener {
            AyanAdManager.showAd(
                useDefaultNativeAdView = false,
                containerKey = admobNativeContainerKey,
                appCompatActivity = this,
                adContainerId = findViewById(R.id.nativeAdMob),
                adSize = null,

                adCallback = object : AdCallback {
                    override fun onAdLoaded() {
                        Log.d("mjmjmj", "onAdLoaded: ")
                    }

                    override fun onAdClicked() {
                        Log.d("mjmjmj", "onAdClicked: ")
                    }

                    override fun onAdFailed(error: String) {
                        Log.d("mjmjmj", "onAdFailed: $error ")
                    }

                }
            )
        }

    }

    override fun onDestroy() {
        super.onDestroy()

    }
}