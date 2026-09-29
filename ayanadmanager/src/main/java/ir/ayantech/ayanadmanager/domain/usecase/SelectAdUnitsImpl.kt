package ir.ayantech.ayanadmanager.domain.usecase

import ir.ayantech.ayanadmanager.domain.model.AdConfiguration
import ir.ayantech.ayanadmanager.domain.model.AdUnit
import ir.ayantech.ayanadmanager.utils.constant.AdSource

class SelectAdUnitsImpl : SelectAdUnits {
    override suspend operator fun invoke(config: AdConfiguration): Map<String, List<AdUnit>> {
        val priorities = mutableMapOf<AdSource, Int>()
        config.providers.forEachIndexed { index, provider ->
            if (provider.adSource !in priorities) priorities[provider.adSource] = index
        }
        return config.adUnits.groupBy { it.containerKey }.mapValues { (_, units) ->
            units.sortedBy { priorities[it.adSource] ?: Int.MAX_VALUE }
        }
    }
}
