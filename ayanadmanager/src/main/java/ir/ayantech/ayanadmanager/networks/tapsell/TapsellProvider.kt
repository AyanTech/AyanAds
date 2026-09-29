package ir.ayantech.ayanadmanager.networks.tapsell

import ir.ayantech.ayanadmanager.core.AdProvider
import ir.ayantech.ayanadmanager.model.AdRequestConfig

class TapsellProvider : AdProvider {
    override fun loadAd(
        config: AdRequestConfig
    ) {
        config.callback?.onAdFailed("This ad provider is not supported yet.")
    }

    override fun destroy() {
        // No resources are allocated by this provider.
    }

}