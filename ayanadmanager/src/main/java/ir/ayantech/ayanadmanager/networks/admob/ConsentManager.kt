package ir.ayantech.ayanadmanager.networks.admob

import androidx.appcompat.app.AppCompatActivity
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import ir.ayantech.ayanadmanager.utils.Logger

object ConsentManager {

    private var consentInformation: ConsentInformation? = null

    fun initialize(
        appCompatActivity: AppCompatActivity,
        onConsentUpdated: ((Boolean) -> Unit)? = null
    ) {
        val params = ConsentRequestParameters.Builder().build()
        consentInformation = UserMessagingPlatform.getConsentInformation(appCompatActivity)

        consentInformation?.requestConsentInfoUpdate(appCompatActivity, params, {
            if (consentInformation?.isConsentFormAvailable == true) {
                loadAndShowConsentForm(appCompatActivity = appCompatActivity, onConsentUpdated)
            } else {
                onConsentUpdated?.invoke(consentInformation?.canRequestAds() != false)
            }
        }, { error ->
            Logger.e("Consent Info Error: ${error.message}")
            onConsentUpdated?.invoke(true) // display ads even has errors.
        })
    }

    private fun loadAndShowConsentForm(
        appCompatActivity: AppCompatActivity,
        onConsentUpdated: ((Boolean) -> Unit)? = null
    ) {
        UserMessagingPlatform.loadAndShowConsentFormIfRequired(appCompatActivity) { error ->
            if (error != null) {
                Logger.e("Consent Form Error: ${error.message}")
            }
            onConsentUpdated?.invoke(consentInformation?.canRequestAds() != false)
        }
    }

    fun requestConsent(appCompatActivity: AppCompatActivity) {
        val consentInformation = UserMessagingPlatform.getConsentInformation(appCompatActivity)

        val params = ConsentRequestParameters.Builder().build()
        consentInformation.requestConsentInfoUpdate(
            appCompatActivity,
            params,
            {
                if (consentInformation.isConsentFormAvailable) {
                    loadAndShowConsentForm(appCompatActivity)
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