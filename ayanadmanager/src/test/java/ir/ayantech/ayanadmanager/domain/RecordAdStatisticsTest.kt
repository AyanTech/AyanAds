package ir.ayantech.ayanadmanager.domain

import ir.ayantech.ayanadmanager.domain.model.AdStatistics
import ir.ayantech.ayanadmanager.domain.usecase.RecordAdStatistics
import ir.ayantech.ayanadmanager.domain.usecase.RecordAdStatisticsImpl
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class RecordAdStatisticsTest {
    private val statistics = AdStatistics("container", "unit", "AdMob", "market", 1, "no fill", "sdk", "android", 37)

    @Test fun `passes immutable event unchanged and returns tracker`() = runTest {
        var captured: AdStatistics? = null
        val useCase: RecordAdStatistics = RecordAdStatisticsImpl { captured = it; "tracker" }
        assertEquals("tracker", useCase(statistics))
        assertSame(statistics, captured)
    }

    @Test fun `response without tracker is allowed`() = runTest {
        assertNull(RecordAdStatisticsImpl { null }(statistics))
    }

    @Test fun `preserves failures and cancellation`() = runTest {
        for (expected in listOf(IllegalStateException("network"), CancellationException("cancelled"))) {
            try {
                RecordAdStatisticsImpl { throw expected }(statistics)
                fail("Expected failure")
            } catch (actual: Exception) { assertSame(expected, actual) }
        }
    }
}
