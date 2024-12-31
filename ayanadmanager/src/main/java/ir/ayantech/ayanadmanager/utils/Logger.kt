package ir.ayantech.ayanadmanager.utils

import android.util.Log
import ir.ayantech.ayanadmanager.utils.constant.Config.TAG

object Logger {

    private var isDebug: Boolean = true


    fun setDebugMode(debug: Boolean) {
        isDebug = debug
    }


    fun d(message: String, tag: String = TAG) {
        if (isDebug) Log.d(tag, message)
    }


    fun i(message: String, tag: String = TAG) =
        Log.i(tag, message)


    fun w(message: String, tag: String = TAG) =
        Log.w(tag, message)


    fun e(message: String, throwable: Throwable? = null, tag: String = TAG) =
        Log.e(tag, message, throwable)

}
