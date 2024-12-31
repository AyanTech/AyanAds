package ir.ayantech.ayanadmanager.utils

import android.content.Context

fun Int.toPx(context: Context): Int {
    return (this * context.resources.displayMetrics.density).toInt()
}