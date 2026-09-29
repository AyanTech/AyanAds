package ir.ayantech.ayanadmanager.model.api

import ir.ayantech.ayanadmanager.utils.constant.AdSource
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AdProviderPriority(
    @SerialName("adSource") val adSource: AdSource,
    @SerialName("priority") val appId: String?
)
