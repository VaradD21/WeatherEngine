# Proguard rules for WeatherEngine Android

# ── Kotlinx Serialization ───────────────────────────────────────────────────
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.SerializationKt

-keepclassmembers class * {
    *** Companion;
}

-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,allowobfuscation,allowshrinking class * {
    <init>(...);
}

# Keep all API and internal data transfer models
-keep class com.weatherengine.app.data.model.** { *; }
-keep class com.weatherengine.app.data.openmeteo.** { *; }
-keep class com.weatherengine.app.persona.** { *; }

# ── Retrofit 2 & OkHttp ────────────────────────────────────────────────────
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepclasseswithmembers interface * {
    @retrofit2.http.* <methods>;
}

-dontwarn okhttp3.**
-dontwarn okio.**

# ── Coroutines ─────────────────────────────────────────────────────────────
-dontwarn kotlinx.coroutines.**
