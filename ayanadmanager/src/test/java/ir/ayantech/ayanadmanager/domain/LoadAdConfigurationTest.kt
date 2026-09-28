package ir.ayantech.ayanadmanager.domain

import ir.ayantech.ayanadmanager.domain.model.AdConfiguration
import ir.ayantech.ayanadmanager.domain.usecase.LoadAdConfiguration
import ir.ayantech.ayanadmanager.domain.usecase.LoadAdConfigurationImpl
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class LoadAdConfigurationTest {
    @Test fun `passes key unchanged and returns configuration`() = runTest {
        val expected = AdConfiguration("demo", emptyList(), emptyList())
        var key: String? = null
        val useCase: LoadAdConfiguration = LoadAdConfigurationImpl { key = it; expected }
        assertSame(expected, useCase("app-key"))
        assertEquals("app-key", key)
    }

    @Test fun `rejects blank key before calling data layer`() = runTest {
        var calls = 0
        val useCase = LoadAdConfigurationImpl { calls++; error("Must not be called") }
        for (key in listOf("", " \t")) {
            try { useCase(key); fail("Expected validation error") } catch (_: IllegalArgumentException) { }
        }
        assertEquals(0, calls)
    }

    @Test fun `preserves failures and cancellation`() = runTest {
        for (expected in listOf(IllegalStateException("network"), CancellationException("cancelled"))) {
            try {
                LoadAdConfigurationImpl { throw expected }("key")
                fail("Expected failure")
            } catch (actual: Exception) { assertSame(expected, actual) }
        }
    }
}
