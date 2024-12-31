package ir.ayantech.ayanadmanager.utils

enum class BannerAdSize(private val width: Int, private val height: Int) {
    BANNER_320x50(320, 50),
    BANNER_640x1136(640, 1136),
    BANNER_1136x640(1136, 640);

    fun getSize(): Pair<Int, Int> {
        return Pair(width, height)
    }
}