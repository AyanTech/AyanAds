# **AyanAdManager SDK**

## **Introduction**
The **AyanAdManager SDK** is a robust solution for managing advertisements in Android applications. It seamlessly integrates with multiple ad providers (e.g., AdMob, HamrahAds, Adivery), enabling developers to display ads efficiently while tracking essential statistics.

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
    implementation 'com...'
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

        AdManager.initialize(
            context = this,
            appMarket = "YOUR_APP_MARKET",
            apiKey = "YOUR_API_KEY",
            appKey = "YOUR_APP_KEY",
            onSuccess = { Logger.d("AdManager initialized successfully.") },
            onError = { Logger.e("Failed to initialize AdManager: $it") }
        )
    }
}
```
Step 2: Display an Ad
To display an ad, use the following example:
```
AdManager.showAd(
    useDefaultNativeAdView = true,
    containerKey = "BANNER_CONTAINER_KEY",
    context = this,
    adSize = BannerAdSize.BANNER_320x50, // Required for banner ad, has own default value (optional)
    bannerAdContainer = findViewById(R.id.ad_container)
)
```

### **Native Ad Customization**
When displaying native ads, you can configure the behavior based on whether you want to use the default native ad view or customize it:

1. **Use the Default Native Ad View:**
   Set `useDefaultNativeAdView = true` to use the SDK's default view without customization.

   ```kotlin
   AdManager.showAd(
       useDefaultNativeAdView = true,
       containerKey = "NATIVE_CONTAINER_KEY",
       context = this,
       bannerAdContainer = findViewById(R.id.native_ad_container)
   )
   ```

2. **Customize the Default Native Ad View:**
   Set `useDefaultNativeAdView = true` and provide a `nativeAdAttributes` object to customize the default view.

   ```kotlin
   AdManager.showAd(
       useDefaultNativeAdView = true,
       containerKey = "NATIVE_CONTAINER_KEY",
       context = this,
       bannerAdContainer = findViewById(R.id.native_ad_container),
       nativeAdAttributes = NativeAdAttributes(
           titleColor = ContextCompat.getColor(this, R.color.black),
           buttonTextColor = Color.parseColor("#000000"),
           buttonBackgroundTint = Color.parseColor("#ffffff"),
           typeface = ResourcesCompat.getFont(this, R.font.medium)
       )
   )
   ```

3. **Use a Custom Native Ad View:**
   If you want to provide your own custom view for displaying native ads, set `useDefaultNativeAdView = false` and define your own layout.

   ```kotlin
   AdManager.showAd(
       useDefaultNativeAdView = false,
       containerKey = "NATIVE_CONTAINER_KEY",
       context = this,
       bannerAdContainer = findViewById(R.id.native_ad_container)
   )
   ```

---
