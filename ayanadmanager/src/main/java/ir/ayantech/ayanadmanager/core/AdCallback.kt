package ir.ayantech.ayanadmanager.core


interface AdCallback {
    fun onAdLoaded()
    fun onAdClicked()
    fun onAdFailed(error: String)
}
