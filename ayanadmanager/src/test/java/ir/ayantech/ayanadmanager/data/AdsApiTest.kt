package ir.ayantech.ayanadmanager.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.content.OutgoingContent
import ir.ayantech.networking.v2.model.AyanRequest
import ir.ayantech.networking.v2.model.Identity
import io.ktor.http.HttpStatusCode
import ir.ayantech.ayanadmanager.data.api.GetConfig
import ir.ayantech.ayanadmanager.data.mapper.awaitValue
import ir.ayantech.ayanadmanager.data.mapper.toDomain
import ir.ayantech.ayanadmanager.data.mapper.toRequest
import ir.ayantech.ayanadmanager.domain.model.AdStatistics
import ir.ayantech.ayanadmanager.domain.model.AdsException
import ir.ayantech.ayanadmanager.utils.constant.AdSource
import ir.ayantech.ayanadmanager.utils.constant.Config
import ir.ayantech.ayanadmanager.utils.constant.EndPoint
import ir.ayantech.networking.datasource.mock.AdsRemoteDataSourceMock
import ir.ayantech.networking.repository.impl.AdsRepositoryImpl
import ir.ayantech.networking.v2.api.AyanAPIResult
import ir.ayantech.networking.v2.model.ApiCallStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertTrue
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.fail
import org.junit.Test

class AdsApiTest {
    @Test
    fun `configuration wire format maps to domain`() {
        val response = Json.decodeFromString<GetConfig.GetConfigResponseModel>(
            """
            {"Name":"Demo","AdSourcePriority":[{"AdSource":"AdMob","AppId":null,"SharePercent":100}],
             "AdUnits":[{"ContainerKey":"banner","ContainerType":"Banner","AdSource":"AdMob","AdUnitId":"unit"}]}
        """
        )
        val config = response.toDomain()
        assertEquals("Demo", config.name)
        assertEquals(AdSource.AdMob, config.providers.single().adSource)
        assertEquals("unit", config.adUnits.single().adUnitId)
    }

    @Test
    fun `statistics retain server field names and SDK version`() {
        val statistics = AdStatistics(
            "container",
            "unit",
            "AdMob",
            "market",
            42,
            null,
            "sdk-test",
            "android",
            37
        )
        val wire = Json.encodeToJsonElement(statistics.toRequest()).jsonObject
        assertEquals("sdk-test", wire.getValue("SdkVersion").jsonPrimitive.content)
        assertEquals(42L, wire.getValue("AppVersion").jsonPrimitive.long)
        assertEquals("unit", wire.getValue("AdUnitId").jsonPrimitive.content)
        assertFalse(wire.containsKey("adUnitId"))
    }

    @Test
    fun `generated repository delegates configuration to generated remote`() = runTest {
        val expected = GetConfig.GetConfigResponseModel("demo", emptyList(), emptyList())
        val remote = AdsRemoteDataSourceMock().apply {
            getConfigResult = flowOf(
                AyanAPIResult.changeState(ApiCallStatus.LOADING),
                AyanAPIResult.success(expected)
            )
        }
        val request = GetConfig.GetConfigRequestBody("key")
        val actual = AdsRepositoryImpl(remote).getConfig(request).awaitValue()
        assertEquals(expected, actual)
        assertEquals(1, remote.getConfigCallCount)
        assertEquals(request, remote.lastGetConfigRequestBody)
    }

    @Test
    fun `failure is mapped and cancellation is propagated`() = runTest {
        try {
            flowOf(AyanAPIResult.error<String>(IllegalStateException("service failure"))).awaitValue()
            fail("Expected failure")
        } catch (error: AdsException) {
            assertEquals("service failure", error.message)
        }
        try {
            flow<AyanAPIResult<String, ApiCallStatus, Exception>> { throw CancellationException("cancelled") }.awaitValue()
            fail("Expected cancellation")
        } catch (expected: CancellationException) {
            assertEquals("cancelled", expected.message)
        }
    }

    @Test
    fun `app key authenticates every SDK endpoint but never another origin`() = runTest {
        val seen = mutableListOf<String?>()
        val client = HttpClient(MockEngine { request ->
            seen += request.headers[Config.APP_KEY_HEADER]
            respond("{}", HttpStatusCode.OK)
        })
        try {
            client.installAdsHeaders("app-key")
            client.post(Config.AYAN_AD_BASE_URL + EndPoint.GET_CONFIG)
            client.post(Config.AYAN_AD_BASE_URL + EndPoint.ADD_STATISTICS)
            client.post(Config.AYAN_AD_BASE_URL + EndPoint.TRACK_STATISTICS)
            client.post("https://example.test/sdk/api/" + EndPoint.GET_CONFIG)
            client.post(Config.AYAN_AD_BASE_URL + "unrelated/")
            assertEquals(listOf("app-key", "app-key", "app-key", null, null), seen)
        } finally {
            client.close()
        }
    }

    @Test
    fun `generated v2 body is adapted to backend envelope without Identity`() = runTest {
        val bodies = mutableListOf<JsonObject>()
        val client = HttpClient(MockEngine { request ->
            val content = request.body as OutgoingContent.ByteArrayContent
            bodies += Json.parseToJsonElement(content.bytes().decodeToString()).jsonObject
            respond("{}", HttpStatusCode.OK)
        }) {
            install(ContentNegotiation) { json(Json { explicitNulls = false }) }
        }
        try {
            client.installAdsHeaders("app-key")
            val request = AyanRequest(Identity(null), GetConfig.GetConfigRequestBody("app-key"))
            client.post(Config.AYAN_AD_BASE_URL + EndPoint.GET_CONFIG) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            client.post("https://example.test/sdk/api/" + EndPoint.GET_CONFIG) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            assertEquals(setOf("Parameters"), bodies[0].keys)
            assertEquals("app-key", bodies[0].getValue("Parameters").jsonObject.getValue("AppKey").jsonPrimitive.content)
            assertTrue("Identity" in bodies[1])
        } finally { client.close() }
    }
}
