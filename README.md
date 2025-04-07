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
            context = this,
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
    activity = this,
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
       context = this,
       adContainerId = findViewById(R.id.view)
   )
   ```

2. **Customize the Default Native Ad View:**
   Set `useDefaultNativeAdView = true` and provide a `nativeAdAttributes` object to customize the default view.

   ```kotlin
   AyanAdManager.showAd(
       useDefaultNativeAdView = true,
       containerKey = "CONTAINER_KEY",
       context = this,
       adContainerId = findViewById(R.id.view),
       nativeAdAttributes = NativeAdAttributes(
           titleColor = ContextCompat.getColor(this, R.color.black),
           buttonTextColor = Color.parseColor("#000000"),
           buttonBackgroundTint = Color.parseColor("#ffffff"),
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
       context = this,
       adContainerId = findViewById(R.id.view)
   )
```
 Note: When using a custom native ad view, you must use the following specific view IDs in your layout to ensure the ad is displayed correctly:

``` XML
<item name="ad_title" type="id" />
<item name="ad_media" type="id" />
<item name="ad_price" type="id" />
<item name="ad_store" type="id" />
<item name="ad_cta" type="id" />
<item name="ad_banner" type="id" />
<item name="ad_stars" type="id" />
<item name="ad_icon" type="id" />
<item name="ad_description" type="id" />
<item name="ad_cta_view" type="id" />
```
These IDs are required regardless of the ad type being displayed. They ensure that the SDK can properly bind the ad content to your custom view.


### Consent Management
For Google Ads, it is necessary to obtain user consent for personalized advertising, especially for users in specific regions like the European Union (EU). The AyanAdManager SDK handles this automatically. However, if you need to manually request consent or display a consent dialog at a specific point in your app, you can use the ```ConsentManager.requestConsent()``` function.


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
