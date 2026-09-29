package ir.ayantech.ayanadmanager.model.api

import ir.ayantech.ayanadmanager.utils.constant.ErrorCode
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Status(
    @SerialName("Code") val code: ErrorCode,
    @SerialName("Description") val description: String
)
