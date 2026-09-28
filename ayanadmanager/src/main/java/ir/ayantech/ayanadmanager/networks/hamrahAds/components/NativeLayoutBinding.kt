package ir.ayantech.ayanadmanager.networks.hamrahAds.components

import android.annotation.SuppressLint
import android.graphics.Typeface
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import ir.ayantech.ayanadmanager.R
import ir.ayantech.ayanadmanager.databinding.NativeLayoutBinding
import ir.ayantech.ayanadmanager.utils.toPx

/** Color properties accept color resource IDs (R.color.*), resolved using the ad view context. */
data class NativeAdAttributes(
    @ColorRes val titleColor: Int = R.color.ayan_ad_title,
    @ColorRes val descriptionColor: Int = R.color.ayan_ad_description,
    @ColorRes val buttonBackgroundTint: Int = R.color.ayan_ad_button_background,
    @ColorRes val buttonTextColor: Int = R.color.ayan_ad_button_text,
    val buttonWidth: Int = 80,
    val buttonHeight: Int = 35,
    @ColorRes val backgroundColor: Int = R.color.ayan_ad_background,
    val typeface: Typeface? = null
)

@SuppressLint("NewApi")
fun NativeLayoutBinding.init(
    nativeAdAttributes: NativeAdAttributes,
) {
    val context = hamrahAdNativeLogo.context
    hamrahAdNativeTitle.apply {
        isSelected = true
        setTextColor(ContextCompat.getColorStateList(context, nativeAdAttributes.titleColor))
        setTypeface(nativeAdAttributes.typeface)
    }
    hamrahAdNativeDescription.apply {
        isSelected = true
        setTextColor(ContextCompat.getColorStateList(context, nativeAdAttributes.descriptionColor))
        setTypeface(nativeAdAttributes.typeface)
    }
    hamrahAdNativeCta.apply {
        layoutParams = layoutParams.apply {
            width = nativeAdAttributes.buttonWidth.toPx(context)
            height = nativeAdAttributes.buttonHeight.toPx(context)
        }
        setTypeface(nativeAdAttributes.typeface)
        setTextColor(ContextCompat.getColorStateList(context, nativeAdAttributes.buttonTextColor))
        background = ContextCompat.getDrawable(this.context, R.drawable.button_background)
        backgroundTintList =
            ContextCompat.getColorStateList(context, nativeAdAttributes.buttonBackgroundTint)
    }
    hamrahAdNativeBanner.apply {
        setBackgroundResource(nativeAdAttributes.backgroundColor)
    }
}