package ir.ayantech.ayanadmanager.core

import ir.ayantech.ayanadmanager.model.AdRequestConfig

interface AdProvider {
    fun loadAd(config: AdRequestConfig)
    fun destroy()
}