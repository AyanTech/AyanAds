package ir.ayantech.ayanadmanager.utils.constant

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

object Config {
    const val AYAN_AD_BASE_URL = "https://ad.ayanco.com/sdk/api/"
    const val APP_KEY_HEADER = "X-APP-Key"
    const val TAG = "AyanAdManager"
    val TIMEOUT: Duration = 60.seconds
    const val PLATFORM = "android"
    const val GOOGLE_AD_VIEW = "googleAdView"
}