package com.ayanco.ayanads.core


interface AdCallback {
    fun onAdLoaded()
    fun onAdFailed(error: String)
}
