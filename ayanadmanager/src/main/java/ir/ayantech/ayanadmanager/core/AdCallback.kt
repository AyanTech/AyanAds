package ir.ayantech.ayanadmanager.core


interface AdCallback {
    fun onAdLoaded()
    fun onAdFailed(error: String)
}
