# Jetpack Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Room database
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keepclassmembers @androidx.room.Entity class * { *; }

# Kotlin coroutines
-keepclassmembernames class kotlinx.** { volatile <fields>; }
-keepclassmembers class kotlin.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Kotlin serialization
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-keep @kotlinx.serialization.Serializable class * { *; }
-keepclassmembers @kotlinx.serialization.Serializable class * {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}

# Coil image loading
-keep class coil.** { *; }
-dontwarn coil.**

# Lottie animations
-keep class com.airbnb.lottie.** { *; }
-dontwarn com.airbnb.lottie.**

# Rive animations
-keep class app.rive.runtime.** { *; }
-dontwarn app.rive.runtime.**

# AndroidX / general
-keep class androidx.** { *; }
-keep interface androidx.** { *; }
-dontwarn androidx.**
