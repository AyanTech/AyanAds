package ir.ayantech.ayanadmanager.domain.usecase

import ir.ayantech.ayanadmanager.domain.model.AdStatistics

class RecordAdStatisticsImpl(private val record: suspend (AdStatistics) -> String?) : RecordAdStatistics {
    override suspend operator fun invoke(statistics: AdStatistics): String? {
        return record(statistics)
    }
}
