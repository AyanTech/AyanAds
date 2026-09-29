package ir.ayantech.ayanadmanager.model.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TrackStatisticsInputParameters(@SerialName("ClickTracker") val clickTracker: String)

@Serializable
data class TrackStatisticsOutputParameters(@SerialName("Status") val status: Status)
