# Default ProGuard rules for this project.
# Refer to https://developer.android.com/guide/developing/tools/proguard.html

# Keep Moshi adapters
-keep class com.example.** { *; }
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}

# Keep Retrofit interfaces
-keepattributes Signature
-keepattributes *Annotation*

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
