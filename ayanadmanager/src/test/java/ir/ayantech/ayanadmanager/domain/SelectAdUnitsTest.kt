package ir.ayantech.ayanadmanager.domain

import ir.ayantech.ayanadmanager.domain.model.AdConfiguration
import ir.ayantech.ayanadmanager.domain.model.AdUnit
import ir.ayantech.ayanadmanager.domain.model.ProviderPriority
import ir.ayantech.ayanadmanager.domain.usecase.SelectAdUnitsImpl
import ir.ayantech.ayanadmanager.utils.ContainerType
import ir.ayantech.ayanadmanager.utils.constant.AdSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.coroutines.test.runTest

class SelectAdUnitsTest {
    @Test
    fun `filters container and respects provider order with unlisted providers last`() = runTest {
        val config = AdConfiguration(
            "demo", listOf(ProviderPriority(AdSource.HamrahAd, "app", 100)), listOf(
                AdUnit("banner", ContainerType.Banner, AdSource.AdMob, "google"),
                AdUnit("other", ContainerType.Native, AdSource.HamrahAd, "unrelated"),
                AdUnit("banner", ContainerType.Banner, AdSource.HamrahAd, "hamrah"),
            )
        )
        assertEquals(
            listOf("hamrah", "google"),
            SelectAdUnitsImpl()(config)["banner"].orEmpty().map { it.adUnitId })
        assertTrue(SelectAdUnitsImpl()(config)["missing"].orEmpty().isEmpty())
    }

    @Test fun `empty configuration produces no placements`() = runTest {
        assertTrue(SelectAdUnitsImpl()(AdConfiguration("empty", emptyList(), emptyList())).isEmpty())
    }

    @Test fun `duplicate provider priorities use first position and preserve ties`() = runTest {
        val first = AdUnit("banner", ContainerType.Banner, AdSource.AdMob, "first")
        val second = first.copy(adUnitId = "second")
        val other = first.copy(adSource = AdSource.HamrahAd, adUnitId = "other")
        val config = AdConfiguration("demo", listOf(
            ProviderPriority(AdSource.AdMob, null, 50),
            ProviderPriority(AdSource.HamrahAd, null, 50),
            ProviderPriority(AdSource.AdMob, null, 50),
        ), listOf(other, first, second))
        val useCase: ir.ayantech.ayanadmanager.domain.usecase.SelectAdUnits = SelectAdUnitsImpl()
        assertEquals(listOf(first, second, other), useCase(config)["banner"])
        assertEquals(listOf(other, first, second), config.adUnits)
    }
}
