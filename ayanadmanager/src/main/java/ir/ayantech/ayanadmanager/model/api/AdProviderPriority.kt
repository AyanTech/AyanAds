package ir.ayantech.ayanadmanager.model.api

import com.google.gson.annotations.SerializedName
import ir.ayantech.ayanadmanager.utils.constant.AdSource

data class AdProviderPriority(
    @SerializedName("adSource")
    val adSource: AdSource,
    @SerializedName("priority")
    val appId: String?
)