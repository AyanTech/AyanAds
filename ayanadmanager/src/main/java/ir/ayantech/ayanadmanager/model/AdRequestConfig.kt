package ir.ayantech.ayanadmanager.model

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import ir.ayantech.ayanadmanager.core.AdCallback
import ir.ayantech.ayanadmanager.model.api.AddStatisticsInputParameters
import ir.ayantech.ayanadmanager.utils.ContainerType


abstract class AdRequestConfig(
    private val containerType: ContainerType,
    private val activity: Activity,
    private val addStatisticsInput: AddStatisticsInputParameters,
    private val callback: AdCallback
) {
    abstract fun getAdView(parentView: ViewGroup): View
}