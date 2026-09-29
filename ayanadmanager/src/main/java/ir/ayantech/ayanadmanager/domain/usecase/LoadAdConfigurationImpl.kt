package ir.ayantech.ayanadmanager.domain.usecase

import ir.ayantech.ayanadmanager.domain.model.AdConfiguration

class LoadAdConfigurationImpl(private val load: suspend (String) -> AdConfiguration) : LoadAdConfiguration {
    override suspend operator fun invoke(appKey: String): AdConfiguration {
        require(appKey.isNotBlank()) { "appKey is blank." }
        return load(appKey)
    }
}
