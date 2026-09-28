package ir.ayantech.ayanadmanager.domain.usecase

import ir.ayantech.ayanadmanager.domain.model.AdConfiguration

fun interface LoadAdConfiguration {
    suspend operator fun invoke(appKey: String): AdConfiguration
}
