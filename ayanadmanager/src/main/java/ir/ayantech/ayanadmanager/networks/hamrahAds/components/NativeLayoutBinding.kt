package ir.ayantech.ayanadmanager.networks.hamrahAds.components

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import androidx.core.content.ContextCompat
import ir.ayantech.ayanadmanager.utils.toPx
import ir.ayantech.ayanadmanager.R
import ir.ayantech.ayanadmanager.databinding.NativeLayoutBinding

data class NativeAdAttributes(
    val titleColor: Int = Color.parseColor("#353535"),
    val descriptionColor: Int = Color.parseColor("#424242"),
    val buttonBackgroundTint: Int = Color.parseColor("#931FA8"),
    val buttonTextColor: Int = Color.parseColor("#000000"),
    val buttonWidth: Int = 80,
    val buttonHeight: Int = 35,
    val backgroundColor: Int = Color.parseColor("#000000"),
    val typeface: Typeface? = null
)

@SuppressLint("NewApi")
fun NativeLayoutBinding.init(
    nativeAdAttributes: NativeAdAttributes,
) {
    val context = hamrahAdNativeLogo.context
    hamrahAdNativeTitle.apply {
        isSelected = true
        setTextColor(nativeAdAttributes.titleColor)
        setTypeface(nativeAdAttributes.typeface)
    }
    hamrahAdNativeDescription.apply {
        isSelected = true
        setTextColor(nativeAdAttributes.descriptionColor)
        setTypeface(nativeAdAttributes.typeface)
    }
    hamrahAdNativeCta.apply {
        layoutParams = layoutParams.apply {
            width = nativeAdAttributes.buttonWidth.toPx(context)
            height = nativeAdAttributes.buttonHeight.toPx(context)
        }
        setTypeface(nativeAdAttributes.typeface)
        setTextColor(nativeAdAttributes.buttonTextColor)
        background = ContextCompat.getDrawable(this.context, R.drawable.button_background)
        backgroundTintList =
            ColorStateList.valueOf(nativeAdAttributes.buttonBackgroundTint)
    }
    hamrahAdNativeBanner.apply {
        setBackgroundColor(nativeAdAttributes.backgroundColor)
    }
}