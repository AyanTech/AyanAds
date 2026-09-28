package ir.ayantech.ayanadmanager.domain.usecase


class TrackAdClickImpl(private val track: suspend (String) -> Unit) : TrackAdClick {
    override suspend operator fun invoke(tracker: String): Unit {
        require(tracker.isNotBlank()) { "Click tracker is blank." }
        track(tracker)
    }
}
