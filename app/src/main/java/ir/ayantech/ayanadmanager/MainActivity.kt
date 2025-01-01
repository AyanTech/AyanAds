package ir.ayantech.ayanadmanager

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
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
            )
        }
        findViewById<Button>(R.id.ad2).setOnClickListener {
            AyanAdManager.showAd(
                containerKey = "fh652bn7u56nb",
                context = this,
                adContainerId = findViewById(R.id.banner),
                adSize = null,
            )
        }
        findViewById<Button>(R.id.ad3).setOnClickListener {
            AyanAdManager.showAd(
                useDefaultNativeAdView = true,
                containerKey = "29a6cefce668",
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
                )
            )
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        HamrahAdProvider().destroy()
    }
}