package ir.ayantech.ayanadmanager.domain.usecase


fun interface TrackAdClick {
    suspend operator fun invoke(tracker: String): Unit
}
