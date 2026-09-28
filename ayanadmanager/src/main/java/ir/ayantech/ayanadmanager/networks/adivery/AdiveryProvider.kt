package ir.ayantech.ayanadmanager.networks.adivery

import ir.ayantech.ayanadmanager.core.AdProvider
import ir.ayantech.ayanadmanager.model.AdRequestConfig

class AdiveryProvider : AdProvider {
    override fun loadAd(
        config: AdRequestConfig
    ) {
//        callback.onAdFailed("Not yet implemented")
    }

    override fun destroy() {
        TODO("Not yet implemented")
    }
}