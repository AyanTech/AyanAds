# AyanAdManager module review

## Changes

- `AdCallback` has default no-op methods. Public callbacks and initialization lambdas accept `null`; nullable callbacks also propagate through request builders/configs and provider implementations.
- `AdLoadSequence` isolates fallback from Android UI configuration. It releases failed attempts, ignores late callbacks, sends one terminal failure, and uses an iterative loop for synchronous failures instead of recursive fallback.
- `AdProviderManager` owns a request per Activity/placement. Replacing a placement, destroying its Activity, or shutting down releases the associated providers. Package version lookup is cached rather than repeated for each fallback.
- Ad units are indexed by container and sorted once after initialization. Display requests use a map lookup.
- Native/banner/interstitial resources are cleared on disposal; generation checks stop late SDK callbacks from recreating released views or emitting events.
- AdMob banner listeners are registered before loading. Missing containers and consent unavailability report failure. Requested banner size is passed to AdMob. Full-screen show failures enter fallback.
- Native assets are registered inside a real `NativeAdView`, matching [Google's native ad guidance](https://developers.google.com/admob/android/native/advanced). The sample demonstrates the required wrapper. Hamrah's temporary custom asset ID changes are restored on disposal.
- Consent is resolved before initializing AdMob, using `canRequestAds()` as described in [Google's UMP guidance](https://developers.google.com/admob/android/privacy). Other initialized providers remain usable if one provider fails. Initialization has per-provider timeouts and duplicate sources initialize only once.
- Statistics snapshots are immutable. Clicks await a pending tracker request, and slower older responses cannot overwrite newer trackers. Tracking state is confined to the SDK scope; closing it is idempotent.
- Placeholder Adivery/Tapsell providers report unsupported use instead of silently doing nothing or throwing `TODO` during cleanup.

## API notes

The existing `initialize` and `showAd` entry points remain. `adProvider` is now nullable after cleanup. Custom AdMob layouts must contain a `NativeAdView`; invalid layouts fail explicitly. Callback methods can be overridden individually, and external callbacks do not control internal fallback or statistics.

## Validation scope

JVM tests cover callback defaults/null handling, fallback disposal, empty provider lists, late events, long synchronous fallback chains, tracker races, session cleanup, DTO mapping, generated networking delegation, and headers. Debug and minified release builds validate integration. These checks do not display live provider ads or measure device frame rates; the performance changes remove repeated work by inspection rather than relying on benchmark claims.

## Use-case contracts and runtime regression

Each of `LoadAdConfiguration`, `RecordAdStatistics`, `TrackAdClick`, and `SelectAdUnits` now has its own interface and `Impl` file. Each interface and implementation exposes exactly one `suspend operator fun invoke`. `SelectAdUnits` returns the placement lookup in one invocation, retaining the initialization-time indexing optimization. Dedicated tests exercise each implementation through its interface.

Emulator testing reproduced an initialization regression in the v2 migration. The configuration endpoint needs `X-APP-Key` even when the request body contains `AppKey`. It also rejects the additional `Identity` field from v2's request envelope. The scoped Ktor compatibility hook now supplies the header and retains a `Parameters`-only envelope for the three SDK endpoints; unrelated origins/paths are untouched. Regression tests validate the serialized request shape and header scope. Generated repositories and remote data sources remain unchanged.

Runtime smoke test on the existing Pixel 10 Pro XL API 37.1 emulator: the app reaches **Ads ready**, with all ad buttons enabled and no app crash. A banner request proceeds through provider fallback and reports a load failure. The emulator logs include Hamrah's internal `Utils.getApp`/`u should init first` warning and AdMob's `Unable to obtain a JavascriptEngine` error. Live ad display is therefore not verified. All 27 JVM tests, the debug build, and the minified release build pass after the protocol fix.
