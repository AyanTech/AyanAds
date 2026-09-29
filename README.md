# **AyanAdManager SDK**

## **Introduction**
The **AyanAdManager SDK** is a robust solution for managing advertisements in Android applications. It seamlessly integrates with multiple ad providers (e.g., AdMob, HamrahAds, Adivery), enabling developers to display ads efficiently while tracking essential statistics.

### Important Note for Google Ads:
To use Google Ads (AdMob) in your application, you must add the following metadata tag to your `AndroidManifest.xml` file. This tag specifies your AdMob `appId`, which is required for initializing Google Ads.
```
<manifest>
    <application>
        <meta-data
            android:name="com.google.android.gms.ads.APPLICATION_ID"
            android:value="YOUR_ADMOB_APP_ID"/>
    </application>
</manifest>
```
Replace `YOUR_ADMOB_APP_ID` with your actual AdMob app ID. This step is mandatory for Google Ads to function correctly.

---

## **Installation**

Requires Android API 21 or higher. See [dependency versions and compatibility limits](docs/dependency-updates.md) for the current dependency update.

Step 1: Add the SDK to your Project
In your project-level build.gradle file, include the following repository:

```
repositories {
    maven { url "https://jitpack.io" }
}
```
In the app-level build.gradle, add the dependency:

```
dependencies {
    implementation 'com.github.AyanTech:AyanAds:Tag'
}
```

---

## **Getting Started**
Step 1: Initialize the SDK
Before using the SDK, initialize it in your Application class:

```
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()

        AyanAdManager.initialize(
            appCompatActivity = this,
            appMarket = "YOUR_APP_MARKET",
            appKey = "YOUR_APP_KEY",
            onSuccess = { Logger.d("AyanAdManager initialized successfully.") },
            onError = { Logger.e("Failed to initialize AyanAdManager: $it") }
        )
    }
}
```
Step 2: Display an Ad
To display an ad, use the following example:
```
AyanAdManager.showAd(
    useDefaultNativeAdView = true,
    containerKey = "CONTAINER_KEY",
    appCompatActivity = this,
    adSize = BannerAdSize.SMALL, // Required for banner ad [SMALL, MEDIUM, LARGE]
    adContainerId = findViewById(R.id.view)
)
```

### **Native Ad Customization**
When displaying native ads, you can configure the behavior based on whether you want to use the default native ad view or customize it:

1. **Use the Default Native Ad View:**
   Set `useDefaultNativeAdView = true` to use the SDK's default view without customization.

   ```kotlin
   AyanAdManager.showAd(
       useDefaultNativeAdView = true,
       containerKey = "CONTAINER_KEY",
       appCompatActivity = this,
       adContainerId = findViewById(R.id.view)
   )
   ```

2. **Customize the Default Native Ad View:**
   Set `useDefaultNativeAdView = true` and provide a `nativeAdAttributes` object to customize the default view.

   ```kotlin
   AyanAdManager.showAd(
       useDefaultNativeAdView = true,
       containerKey = "CONTAINER_KEY",
       appCompatActivity = this,
       adContainerId = findViewById(R.id.view),
       nativeAdAttributes = NativeAdAttributes(
           titleColor = R.color.black,
           buttonTextColor = R.color.black,
           buttonBackgroundTint = R.color.white,
           typeface = ResourcesCompat.getFont(this, R.font.medium)
       )
   )
   ```

3. **Use a Custom Native Ad View:**
   If you want to provide your custom view for displaying native ads, set `useDefaultNativeAdView = false` and define your layout.
```kotlin
   AyanAdManager.showAd(
       useDefaultNativeAdView = false,
       containerKey = "CONTAINER_KEY",
       appCompatActivity = this,
       adContainerId = findViewById(R.id.view)
   )
```
 Note: When using a custom native ad view, you must use the following specific view IDs in your layout to ensure the ad is displayed correctly:

| ID Name         | View Type                                     |
|-----------------|-----------------------------------------------|
| `ad_title`      | `TextView`                                    |
| `ad_media`      | `com.google.android.gms.ads.nativead.MediaView` |
| `ad_price`      | `TextView`                                    |
| `ad_store`      | `TextView`                                    |
| `ad_cta`        | `Button`                                      |
| `ad_banner`     | `ImageView`                                   |
| `ad_stars`      | `RatingBar`                                   |
| `ad_icon`       | `ImageView`                                   |
| `ad_description`| `TextView`                                    |
| `ad_cta_view`   | `Button`                                      |

These IDs are required regardless of the ad type being displayed. They ensure that the SDK can properly bind the ad content to your custom view.

### **Custom Layout Requirements**
When using a custom native ad view (`useDefaultNativeAdView = false`), **all layout containers** — including the parent and all nested views — must use **LinearLayout**.

Avoid using **RelativeLayout**, **ConstraintLayout**, or any other layout types anywhere in the hierarchy. These layouts are not supported and may lead to rendering issues or SDK integration problems.

### Consent Management
For Google Ads, it is necessary to obtain user consent for personalized advertising, especially for users in specific regions like the European Union (EU). The AyanAdManager SDK handles this automatically. However, if you must manually request consent or display a consent dialog at a specific point in your app, you can use the ```ConsentManager.requestConsent()``` function.


### Destroy Ad
It's crucial to handle the lifecycle of ads when the activity or fragment is destroyed to ensure proper resource management and avoid memory leaks. Use the destroy method to release resources associated with the ad when the activity is destroyed.

``` Kotlin
override fun onDestroy() {
    super.onDestroy()
    AyanAdManager.adProvider.destroy()
}

```
This method ensures that all ad resources are properly released, preventing memory leaks and improving your application's overall performance.


### ProGuard Configuration
If you're using ProGuard, add the following rules to your ProGuard configuration file:

```proguard
-keep public class ir.ayantech.hamrahads.** { *; }
-keep class ir.ayantech.ayanadmanager.model.api.** { *; }
-keep class ir.ayantech.ayanadmanager.utils.constant.** { *; }
```

### **Logging**
To view SDK logs in **Logcat**, filter logs using the following tag:

**TAG:** `"AyanAdManager"`

#### Example via ADB:
```bash
adb logcat -s AyanAdManager
```

#### In Android Studio:
- Open the **Logcat** window  
- Enter `AyanAdManager` in the search/filter box to isolate SDK logs

This will help you monitor the SDK’s behavior, initialization process, and any errors or debug information provided during integration.


## Networking and architecture

The SDK uses Networking 2.0.5 and Generator 2.0.4 with KSP-generated repository and remote data source implementations. See [the migration and architecture notes](docs/networking-migration.md) for the Data, Domain, and UI boundaries, compatibility changes, and build/test commands.

## Optional callbacks and lifecycle

The callback may be omitted or passed as `null`:

```kotlin
AyanAdManager.showAd(
    containerKey = "banner",
    appCompatActivity = this,
    adContainerId = bannerContainer,
    adCallback = null,
)
```

Override only the events you need; all three methods have default no-op implementations:

```kotlin
AyanAdManager.showAd(
    containerKey = "banner",
    appCompatActivity = this,
    adContainerId = bannerContainer,
    adCallback = object : AdCallback {
        override fun onAdClicked() {
            // Handle the click.
        }
    },
)
```

`initialize` also accepts nullable or omitted `onSuccess` and `onError` callbacks. Provider fallback and statistics still run when no callback is supplied. `adSize` and `adContainerId` default to `null`; banner/native ads require a container, while interstitials do not.

Requests are replaced when the same Activity/placement is used again, and their providers are released when the Activity is destroyed. `shutdown()` releases all remaining providers and network jobs. The optional `adProvider` property is nullable when no provider is active.

For custom AdMob native layouts, put all ad assets inside `com.google.android.gms.ads.nativead.NativeAdView` and retain the documented asset IDs. The sample includes this wrapper. Passing an invalid custom layout reports an error and allows the next provider to run.

All `NativeAdAttributes` color parameters accept `@ColorRes` IDs such as `R.color.black`, including color selectors. Pass resource IDs directly; do not pass resolved ARGB values or `Color.parseColor()` results. Colors are resolved from the view context when the ad is bound.
