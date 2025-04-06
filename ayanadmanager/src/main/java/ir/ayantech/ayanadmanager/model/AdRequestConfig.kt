package ir.ayantech.ayanadmanager.model

import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import ir.ayantech.ayanadmanager.core.AdCallback
import ir.ayantech.ayanadmanager.model.api.AddStatisticsInputParameters
import ir.ayantech.ayanadmanager.utils.ContainerType


abstract class AdRequestConfig(
    private val containerType: ContainerType,
    private val appCompatActivity: AppCompatActivity,
    private val addStatisticsInput: AddStatisticsInputParameters,
    private val callback: AdCallback
) {
    abstract fun getAdView(parentView: ViewGroup): View
}