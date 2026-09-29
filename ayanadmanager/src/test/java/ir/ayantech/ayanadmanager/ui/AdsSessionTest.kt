package ir.ayantech.ayanadmanager.ui

import ir.ayantech.ayanadmanager.domain.model.AdConfiguration
import ir.ayantech.ayanadmanager.domain.model.AdStatistics
import ir.ayantech.ayanadmanager.domain.model.AdsException
import ir.ayantech.ayanadmanager.domain.usecase.LoadAdConfiguration
import ir.ayantech.ayanadmanager.domain.usecase.RecordAdStatistics
import ir.ayantech.ayanadmanager.domain.usecase.TrackAdClick
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AdsSessionTest {
    @Test
    fun `configuration updates state and failures allow retry`() = runTest {
        var shouldFail = true
        val config = AdConfiguration("demo", emptyList(), emptyList())
        val session = AdsSession(
            LoadAdConfiguration { if (shouldFail) throw AdsException("offline") else config },
            RecordAdStatistics { null }, TrackAdClick {}, scope = backgroundScope,
        )
        try {
            session.load("key"); fail("Expected failure")
        } catch (expected: AdsException) {
        }
        assertEquals(AdsUiState.Failed("offline"), session.state.value)
        shouldFail = false
        assertEquals(config, session.load("key"))
        assertEquals(AdsUiState.Ready(config), session.state.value)
    }

    @Test
    fun `click uses tracker for matching ad and ignores missing or blank trackers`() = runTest {
        val clicks = mutableListOf<String>()
        var response: String? = "tracker"
        val session = AdsSession(
            LoadAdConfiguration { AdConfiguration("demo", emptyList(), emptyList()) },
            RecordAdStatistics { response }, TrackAdClick { clicks += it }, scope = backgroundScope,
        )
        val statistics =
            AdStatistics("container", "unit", "AdMob", "market", 1, null, "sdk", "android", 37)
        session.click(null)
        session.click("missing")
        session.record(statistics)
        runCurrent()
        session.click("unit")
        runCurrent()
        response = " "
        session.record(statistics)
        runCurrent()
        session.click("unit")
        runCurrent()
        assertEquals(listOf("tracker", "tracker"), clicks)
    }

    @Test
    fun `click waits for pending tracker and an older response cannot overwrite the latest`() = runTest {
        val first = kotlinx.coroutines.CompletableDeferred<String?>()
        val second = kotlinx.coroutines.CompletableDeferred<String?>()
        var requests = 0
        val clicks = mutableListOf<String>()
        val session = AdsSession(
            LoadAdConfiguration { AdConfiguration("demo", emptyList(), emptyList()) },
            RecordAdStatistics { if (requests++ == 0) first.await() else second.await() },
            TrackAdClick { clicks += it }, scope = backgroundScope,
        )
        val statistics = AdStatistics("container", "unit", "AdMob", "market", 1, null, "sdk", "android", 37)
        session.record(statistics)
        runCurrent()
        session.record(statistics)
        session.click("unit")
        runCurrent()
        assertEquals(emptyList<String>(), clicks)
        second.complete("latest")
        runCurrent()
        first.complete("older")
        runCurrent()
        session.click("unit")
        runCurrent()
        assertEquals(listOf("latest", "latest"), clicks)
    }

    @Test
    fun `close is idempotent and ignores new tracking work`() = runTest {
        var closed = 0
        var recorded = 0
        val session = AdsSession(
            LoadAdConfiguration { AdConfiguration("demo", emptyList(), emptyList()) },
            RecordAdStatistics { recorded++; null }, TrackAdClick {},
            closeClient = { closed++ }, scope = backgroundScope,
        )
        session.close()
        session.close()
        session.record(AdStatistics("container", "unit", "AdMob", "market", 1, null, "sdk", "android", 37))
        runCurrent()
        assertEquals(1, closed)
        assertEquals(0, recorded)
    }
}
