package ir.ayantech.ayanadmanager.utils

import android.content.Context
import android.view.View

fun Int.toPx(context: Context): Int {
    return (this * context.resources.displayMetrics.density).toInt()
}

fun View.makeVisible() {
    this.visibility = View.VISIBLE
}

fun View.makeGone() {
    this.visibility = View.GONE
}