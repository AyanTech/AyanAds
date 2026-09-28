package ir.ayantech.ayanadmanager.data

import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestPipeline
import io.ktor.http.ContentType
import io.ktor.http.Url
import io.ktor.http.content.OutgoingContent
import io.ktor.http.content.TextContent
import ir.ayantech.ayanadmanager.utils.constant.Config
import ir.ayantech.ayanadmanager.utils.constant.Config.APP_KEY_HEADER
import ir.ayantech.ayanadmanager.utils.constant.EndPoint
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

/** Ad endpoints use app-key authentication and a Parameters-only envelope, not v2 Identity. */
internal fun HttpClient.installAdsHeaders(appKey: String) {
    val base = Url(Config.AYAN_AD_BASE_URL)
    val paths = setOf(EndPoint.GET_CONFIG, EndPoint.ADD_STATISTICS, EndPoint.TRACK_STATISTICS)
        .mapTo(mutableSetOf()) { base.encodedPath + it }
    fun isAdEndpoint(url: Url) = url.protocol == base.protocol && url.host == base.host &&
        url.port == base.port && url.encodedPath in paths

    // Networking 2.0.5's Builder.setCustomHeaders does not apply headers to its Ktor client.
    requestPipeline.intercept(HttpRequestPipeline.State) {
        if (isAdEndpoint(context.url.build())) {
            context.headers.remove(APP_KEY_HEADER)
            context.headers.append(APP_KEY_HEADER, appKey)
        }
    }
    // Adapt the serialized envelope, retaining generated DTO serializers and request execution.
    requestPipeline.intercept(HttpRequestPipeline.Render) {
        if (isAdEndpoint(context.url.build())) {
            val content = subject as? OutgoingContent.ByteArrayContent
            if (content != null && content.contentType?.match(ContentType.Application.Json) == true) {
                val envelope = Json.parseToJsonElement(content.bytes().decodeToString()).jsonObject
                if ("Identity" in envelope) {
                    proceedWith(TextContent(JsonObject(envelope - "Identity").toString(), ContentType.Application.Json))
                    return@intercept
                }
            }
        }
    }
}
