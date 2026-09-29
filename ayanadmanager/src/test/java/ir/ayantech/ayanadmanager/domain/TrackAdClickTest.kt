package ir.ayantech.ayanadmanager.domain

import ir.ayantech.ayanadmanager.domain.usecase.TrackAdClick
import ir.ayantech.ayanadmanager.domain.usecase.TrackAdClickImpl
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class TrackAdClickTest {
    @Test fun `submits tracker once`() = runTest {
        val submitted = mutableListOf<String>()
        val useCase: TrackAdClick = TrackAdClickImpl { submitted += it }
        useCase("tracker")
        assertEquals(listOf("tracker"), submitted)
    }

    @Test fun `rejects blank tracker without submitting`() = runTest {
        var calls = 0
        val useCase = TrackAdClickImpl { calls++ }
        for (tracker in listOf("", " \t")) {
            try { useCase(tracker); fail("Expected validation error") } catch (_: IllegalArgumentException) { }
        }
        assertEquals(0, calls)
    }

    @Test fun `preserves failures and cancellation`() = runTest {
        for (expected in listOf(IllegalStateException("network"), CancellationException("cancelled"))) {
            try {
                TrackAdClickImpl { throw expected }("tracker")
                fail("Expected failure")
            } catch (actual: Exception) { assertSame(expected, actual) }
        }
    }
}
