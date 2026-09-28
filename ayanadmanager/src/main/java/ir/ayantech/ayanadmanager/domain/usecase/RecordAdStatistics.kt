package ir.ayantech.ayanadmanager.domain.usecase

import ir.ayantech.ayanadmanager.domain.model.AdStatistics

fun interface RecordAdStatistics {
    suspend operator fun invoke(statistics: AdStatistics): String?
}
