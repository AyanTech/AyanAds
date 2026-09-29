package ir.ayantech.ayanadmanager.core

/** Optional ad notifications. Override only the events your application needs. */
interface AdCallback {
    fun onAdLoaded() = Unit
    fun onAdClicked() = Unit
    fun onAdFailed(error: String) = Unit
}
