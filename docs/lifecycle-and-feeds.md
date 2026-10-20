# Fragment and RecyclerView integration (2.1)

Use `bindAd()` for a single placement and `ScreenAdSession` for a scrolling feed.
Call these APIs on the main thread after initialization succeeds.

## Single placement

```kotlin
private var adHandle: AdHandle? = null

override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    adHandle = AyanAdManager.bindAd(
        containerKey = placementKey,
        appCompatActivity = requireActivity() as AppCompatActivity,
        lifecycleOwner = viewLifecycleOwner,
        adContainerId = binding.adContainer,
    )
}

override fun onDestroyView() {
    adHandle?.dispose()
    adHandle = null
    super.onDestroyView()
}
```

Disposal also happens automatically when the supplied owner or host Activity is destroyed.
The handle releases its own provider, pending fallback, timer, registry entry, and observers.
`showAd()` remains available with its Activity-scoped behavior. Do not use the global
`adProvider` property to clean up multiple placements.

## Feed

Create one session per fragment view:

```kotlin
import kotlin.time.Duration.Companion.seconds

val ads = AyanAdManager.createScreenSession(
    activity = requireActivity() as AppCompatActivity,
    lifecycleOwner = viewLifecycleOwner,
    maxSlots = 4,
    maxConcurrentLoads = 2,
    attemptTimeout = 15.seconds,
)
```

The screen controller calls `prepare()` when its data or visible range changes:

```kotlin
ads.prepare(
    listOf(
        AdSlot(id = "feed-ad-42", containerKey = nativePlacement),
        AdSlot(id = "feed-ad-73", containerKey = nativePlacement),
    )
)
```

`attemptTimeout` takes a Kotlin `Duration` (for example, `15.seconds`).

This is the entire desired working set, not an append operation. Slots removed from it are
cancelled and destroyed. An unchanged slot keeps its load/ad; `retry(id)` explicitly retries
a failed preparation. IDs must identify logical ad rows, independently of adapter position.
Do not reuse one ID for multiple visible ads. `prepare()` rejects a set larger than `maxSlots`.

In the adapter:

```kotlin
// onBindViewHolder, after this slot has been included in prepare():
holder.adBinding = ads.bind(item.slotId, holder.adContainer)

// onViewAttachedToWindow:
holder.adBinding?.attach()

// onViewDetachedFromWindow:
holder.adBinding?.detach()

// onViewRecycled:
holder.adBinding?.dispose()
holder.adBinding = null
```

Binding does not request a new ad or access DataStore. It subscribes the container to the
slot, then renders when the ad is prepared and the view is attached to a started owner.
Images can still load asynchronously on first display; subsequent bindings use Coil caches.
Repeated binding of the same slot/container returns the same binding. A new container takes
exclusive ownership of that slot's display. Temporary detachment retains the prepared ad;
recycling releases view resources while the slot stays in the working set.

Collect `ads.states` with `viewLifecycleOwner` to update loading/error UI. If a row is bound
before the viewport controller has prepared its slot, show a placeholder until the slot
appears in `states`. Post adapter notifications when a state change happens during a
RecyclerView layout or scroll callback. Reserve a suitable row height to avoid layout jumps.

Close the session and detach the adapter in `onDestroyView()`. The lifecycle observer is a
second cleanup path. Do not keep sessions, bindings, Activities, views, or provider objects
in a ViewModel. Keep only logical slot IDs and ordinary screen data there.

The feed API supports banner and native placements using the SDK templates. Custom layouts
and interstitials continue to use `bindAd()`/`showAd()`. Prepared slots expire after five
minutes and are refreshed on the next `prepare()` call. Keep calling `prepare()` as the
visible range changes; do not cache a whole feed's worth of ads.

The runnable example is `FeedDemoActivity.kt`. Open **RecyclerView demo** in the sample app
after SDK initialization. It uses the sample's configured placement; use a test placement
when running provider integration tests.

## Tracking and compatibility

Each provider attempt/loaded ad has a separate local tracking identity. It is not added to
the server payload. AdMob statistics use impression callbacks, so preparation does not count
as an impression. Clicks wait for the matching statistics response. Releasing or replacing a
row cannot redirect an already observed click to another row's tracker.

Hamrah passes its exact `LoadedAd` response into rendering. Its callbacks describe local
display/click events; telemetry delivery continues independently of view disposal.

Upgrade both SDKs together and rebuild consuming applications. Ayan 2.1 depends on Hamrah
2.1. These local changes have not been published. Publish Hamrah first, then Ayan.

When a sibling `../HamrahAds` checkout exists, Gradle substitutes that source project.
Override its location with `-PhamrahAdsProject=/absolute/path/to/HamrahAds`. Without a source
checkout, Gradle resolves the declared published Hamrah version.

## Verification

```sh
bash gradlew :ayanadmanager:testDebugUnitTest :app:assembleDebug
```

Regression coverage includes unchanged-slot request reuse, temporary detach/reattach,
recycling during a pending load, view-owner destruction with a live Activity, queued-load
cancellation, provider timeouts, and separate trackers for concurrent same-unit displays.

Before rollout, profile a release/profileable consuming app on a representative device:

1. Compare a deterministic ad provider against a no-ad feed to isolate UI cost.
2. Scroll down/up repeatedly and navigate away/back while loads are pending.
3. Record frame timing, bind duration, allocations, active ad count, requests, image cache
   hits, and retained fragment views. Inspect a Perfetto trace and heap dump.
4. Repeat with provider test ads and slow/offline responses.

Unit tests validate ownership and request behavior; they do not establish a device-level
frame-time or memory improvement.
