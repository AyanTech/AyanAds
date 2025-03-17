package ir.ayantech.ayanadmanager.networks.admob

import android.app.Activity
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import ir.ayantech.ayanadmanager.utils.Logger

object ConsentManager {

    private var consentInformation: ConsentInformation? = null

    fun initialize(activity: Activity, onConsentUpdated: ((Boolean) -> Unit)? = null) {
        val params = ConsentRequestParameters.Builder().build()
        consentInformation = UserMessagingPlatform.getConsentInformation(activity)

        consentInformation?.requestConsentInfoUpdate(activity, params, {
            if (consentInformation?.isConsentFormAvailable == true) {
                loadAndShowConsentForm(activity, onConsentUpdated)
            } else {
                onConsentUpdated?.invoke(consentInformation?.canRequestAds() != false)
            }
        }, { error ->
            Logger.e("Consent Info Error: ${error.message}")
            onConsentUpdated?.invoke(true) // display ads even has errors.
        })
    }

    private fun loadAndShowConsentForm(
        activity: Activity,
        onConsentUpdated: ((Boolean) -> Unit)? = null
    ) {
        UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { error ->
            if (error != null) {
                Logger.e("Consent Form Error: ${error.message}")
            }
            onConsentUpdated?.invoke(consentInformation?.canRequestAds() != false)
        }
    }

    fun requestConsent(activity: Activity) {
        val consentInformation = UserMessagingPlatform.getConsentInformation(activity)

        val params = ConsentRequestParameters.Builder().build()
        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                if (consentInformation.isConsentFormAvailable) {
                    loadAndShowConsentForm(activity)
                }
            },
            { error ->
                Logger.e("request Consent has error, Message : ${error.message} | Error Code : ${error.errorCode} ")
            }
        )
    }

    fun canShowAds(): Boolean {
        return consentInformation?.canRequestAds() != false
    }
}