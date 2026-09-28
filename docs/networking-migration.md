# Networking and architecture migration

Verified against upstream tags on 2026-09-28:

| Library | Previous | Current | Relevant changes |
| --- | --- | --- | --- |
| [Generator](https://github.com/AyanTech/Generator/tree/2.0.4) | `com.github.shadowalker77:generator:0.3.0` | `com.github.AyanTech:Generator:2.0.4` | KSP category-based APIs; generated remote and repository implementations; generated mocks with flows, captured arguments, and invocation counts. The latest tag also refactors processor validation and the architecture sample. |
| [Networking](https://github.com/AyanTech/Networking/tree/2.0.5) | `com.github.shadowalker77:networking:1.6.7` | `com.github.AyanTech:Networking:2.0.5` | v2 uses Ktor, Kotlin serialization, suspend/Flow calls, and typed result events. The 2.0.5 commit makes Ktor JSON and logging implementation dependencies while retaining the core client API dependencies. |

KSP `2.3.10` matches Generator's tagged build. The serialization plugin uses the project's Kotlin `2.4.0`. AGP `9.3.0`, Gradle `9.6.0`, compile SDK 37, target SDK 37, and min SDK 21 are retained.

## Dependency direction

```text
Sample Activity → AdsDemoViewModel state / SDK facade
SDK facade → UI AdsSession → Domain use cases and models
                                  ↑ operation functions
                              DI composition root
                                  ↓
                 KSP-generated AdsRepositoryImpl
                                  ↓
                 KSP-generated AdsRemoteDataSourceImpl
                                  ↓
                            Networking v2
```

- **Data**: `data/api/AdsApis.kt` declares the three `@AyanAPI` endpoints, their nested serializable request/response DTOs, and camelCase properties with explicit `@SerialName` annotations preserving the existing wire names. `data/mapper` translates DTOs and terminal Flow events into domain values/errors.
- **Domain**: plain Kotlin configuration/statistics models, configuration loading, statistics/click use cases, and provider selection. Each use case has a separate interface and `Impl` class exposing one `suspend operator fun invoke` method. No Android, Ktor, Generator, or Networking imports. Dependencies are supplied as suspend operation functions rather than another handwritten repository interface.
- **UI**: `ui/AdsSession` owns configuration state and tracking jobs. The SDK facade manages Activity-dependent ad providers with lifecycle cancellation. The sample ViewModel holds immutable UI state; the Activity collects it while started and disables ad buttons until initialization succeeds.
- **DI**: `di/AdsGraph.kt` creates a client with application context, constructs the generated classes, and supplies mapped operations to the domain use cases. No DI framework is required.

There are **no handwritten remote data sources or repositories**. KSP generates `AdsRepository`, `AdsRepositoryImpl`, `AdsRepositoryMock`, `AdsRemoteDataSource`, `AdsRemoteDataSourceImpl`, and `AdsRemoteDataSourceMock` beneath `ayanadmanager/build/generated/ksp/<variant>/kotlin`. Do not check these files in or edit them.

## Behavior and compatibility

The public `initialize`, `showAd`, `isInitialized`, `sendStatistics`, and `submitClick` entry points remain. Configuration loading now completes before initialization succeeds, and initialization reports success after provider initialization finishes, keeping successfully initialized providers available. Calling `showAd` before initialization reports an error instead of accessing uninitialized fields. Provider initialization follows the Activity lifecycle; statistics use a separate SDK-owned scope. Call `AyanAdManager.shutdown()` when the SDK is no longer needed to cancel requests and close its client.

The old publicly mutable `ayanAdApi` and `clickTrackers` implementation details have been removed. App key and market remain readable but are set through initialization. `AdProviderManager` now consumes domain ad units. The old `model/api` value classes remain for provider integrations; they are not v2 transport DTOs. Their properties are now camelCase with explicit `@SerialName` wire names as well, so callers using their former PascalCase properties or named constructor arguments must update those references. Mutable statistics are copied into an immutable domain event before asynchronous submission.

Networking 2.0.5's `setCustomHeaders` does not actually apply headers. `data/AdsHttpHeaders.kt` adds `X-APP-Key` to configuration, statistics, and tracker requests through the existing Ktor request pipeline. This does not replace the generated transport. Configuration also sends the app key in its request body; the backend requires the HTTP header for initialization as well. The header is restricted to the configured backend origin and known SDK endpoint paths. Client payload logging is disabled. The request pipeline also removes Networking v2's `Identity` envelope field for these endpoints: the deployed ads backend accepts a `Parameters`-only envelope and rejects the v2 identity envelope. This compatibility step runs after serialization, preserving the generated repository and DTO serializers.

The tagged source uses `ir.ayantech.networking.v2`, despite older `ir.ayantech.ayannetworking.v2` imports in the upstream README. v2 requires a non-null `Parameters` object and a successful status envelope. Unit tests use fixtures and a mock engine. A separate emulator smoke test validated configuration and initialization against the configured backend after fixing the envelope/header incompatibilities. Live banner display remains unverified because provider SDKs returned errors in the emulator; see the module review.

## Validation

```sh
bash gradlew :ayanadmanager:testDebugUnitTest :app:assembleDebug :app:assembleRelease :ayanadmanager:generatePomFileForReleasePublication
```

Tests cover DTO wire names and mapping, generated repository delegation, terminal result/error handling, cancellation propagation, request headers, provider ordering, state retry, and per-ad click tracking.
