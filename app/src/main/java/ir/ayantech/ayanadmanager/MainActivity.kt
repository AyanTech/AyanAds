package ir.ayantech.ayanadmanager

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import ir.ayantech.ayanadmanager.core.AdCallback
import ir.ayantech.ayanadmanager.core.AyanAdManager
import ir.ayantech.ayanadmanager.networks.hamrahAds.HamrahAdProvider
import ir.ayantech.ayanadmanager.networks.hamrahAds.components.NativeAdAttributes
import ir.ayantech.ayanadmanager.utils.constant.AppMarket

class MainActivity : AppCompatActivity() {
    val appKey = "c89ce51c1c6686ac560580b28aba0642b6fc639fa93d7543bf0833c5ca9c965c"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        AyanAdManager.initialize(
            context = this,
            appKey = appKey,
            appMarket = AppMarket.CafeBazaar
        )


        findViewById<Button>(R.id.ad1).setOnClickListener {

            AyanAdManager.showAd(
                containerKey = "6ec7f088-4800-4fda-ac03-b7ec88e9a829",
                context = this,
                adContainerId = findViewById(R.id.nativeView),
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
            AyanAdManager.showAd(
                containerKey = "2ec0d99d-baae-40b6-a6aa-cb5f8f41f1a3",
                context = this,
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
        findViewById<Button>(R.id.ad3).setOnClickListener {
            AyanAdManager.showAd(
                useDefaultNativeAdView = true,
                containerKey = "aa76b03d-2cde-49ef-9c2a-6853e343da60",
                context = this,
                adContainerId = findViewById(R.id.banner),
                adSize = null,
                nativeAdAttributes = NativeAdAttributes(
                    titleColor = ContextCompat.getColor(
                        this,
                        R.color.black
                    ),
                    buttonTextColor = Color.parseColor("#000000"),
                    buttonBackgroundTint = Color.parseColor("#ffffff"),
                    typeface = ResourcesCompat.getFont(this, R.font.medium)
                ),
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
        HamrahAdProvider().destroy()
    }
}