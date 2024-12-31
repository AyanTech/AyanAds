package ir.ayantech.ayanadmanager

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.ayanco.ayanads.core.AyanAdManager
import ir.ayantech.ayanadmanager.networks.hamrahAds.HamrahAdProvider
import ir.ayantech.ayanadmanager.networks.hamrahAds.components.NativeAdAttributes
import ir.ayantech.ayanadmanager.utils.constant.AppMarket

class MainActivity : AppCompatActivity() {
    val apiKey = "7b4b488a40a0c1dfe0ff73688766d79cab88274d21b16ff3f3af7157fabc692c"
    val appKey = "590608eaa7f9d7352e97c35a398bd9cda4c5aa5df8b9796fe8cb483a977d29a7"
    val interId = "20521bed-d9dc-4198-bc22-b026b6e696d3"
    val nativeId = "00ea8b15-eb29-40f9-80ab-3bd92a631a89"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        AyanAdManager.initialize(
            context = this,
            apiKey = apiKey,
            appKey = appKey,
            appMarket = AppMarket.CafeBazaar
        )

        findViewById<Button>(R.id.ad1).setOnClickListener {
            AyanAdManager.showAd(
                containerKey = "bu74f563nhl35f45",
                context = this,
                bannerAdContainer = findViewById(R.id.banner),
                adSize = null,
            )
        }
        findViewById<Button>(R.id.ad2).setOnClickListener {
            AyanAdManager.showAd(
                containerKey = "fh652bn7u56nb",
                context = this,
                bannerAdContainer = findViewById(R.id.banner),
                adSize = null,
            )
        }
        findViewById<Button>(R.id.ad3).setOnClickListener {
            AyanAdManager.showAd(
                useDefaultNativeAdView = true,
                containerKey = "29a6cefce668",
                context = this,
                bannerAdContainer = findViewById(R.id.banner),
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