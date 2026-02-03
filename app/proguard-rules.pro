# YTMusic Pro ProGuard Rules - Production Ready

# WebView JavaScript Interface
-keep class com.ytmusic.pro.MainActivity$WebAppInterface { *; }
-keepclassmembers class com.ytmusic.pro.MainActivity$WebAppInterface {
    public *;
}
-keepattributes JavascriptInterface
-keepattributes *Annotation*

# WebView Support
-keepclassmembers class * extends android.webkit.WebViewClient {
    public void *(android.webkit.WebView, java.lang.String, android.graphics.Bitmap);
    public boolean *(android.webkit.WebView, java.lang.String);
}
-keepclassmembers class * extends android.webkit.WebChromeClient {
    public void *(android.webkit.WebView, java.lang.String);
}

# Media session support - Critical for notifications
-keep class androidx.media.** { *; }
-keep class android.support.v4.media.** { *; }
-keep class androidx.core.app.** { *; }

# AndroidX Core
-keep class androidx.core.** { *; }
-dontwarn androidx.core.**

# Keep Service classes
-keep class com.ytmusic.pro.ForegroundService { *; }
-keep class com.ytmusic.pro.YTMusicWebview { *; }

# General Android
-keep class android.webkit.** { *; }
-dontwarn android.webkit.**
-keep class androidx.appcompat.** { *; }
-dontwarn androidx.appcompat.**

# Optimization
-optimizationpasses 5
-dontusemixedcaseclassnames
-verbose
