package ir.ayantech.ayanadmanager.utils

typealias SimpleCallBack = () -> Unit
typealias StringCallBack = (String) -> Unit

fun trying(block: () -> Unit) {
    try {
        block()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}