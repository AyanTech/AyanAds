package ir.ayantech.ayanadmanager.domain.usecase

import ir.ayantech.ayanadmanager.domain.model.AdConfiguration
import ir.ayantech.ayanadmanager.domain.model.AdUnit

/** Builds the ordered placement lookup once when configuration is loaded. */
fun interface SelectAdUnits {
    suspend operator fun invoke(config: AdConfiguration): Map<String, List<AdUnit>>
}
