# Centralized ProGuard rules for WhatsCustomReply (gamalsolutions.whatscustomreply)

# Cache / Line Numbers for easier stacktrace debugging
-keepattributes SourceFile,LineNumberTable,Signature,InnerClasses,EnclosingMethod,Deprecated,*Annotation*,*Element*

# Keep BuildConfig safe (so GEMINI_API_KEY inside BuildConfig can be read correctly at runtime Dev/Release)
-keep class gamalsolutions.whatscustomreply.BuildConfig { *; }

# Kotlin Serialization Rules
-keepattributes *Annotation*,Descriptor
-keepclassmembers class * {
    *** Companion;
    *** $serializer;
}
# Keep serializable classes and their properties
-keep,allowobfuscation,allowshrinking @kotlinx.serialization.Serializable class *
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
}

# Room Database Rules
-keep class * extends androidx.room.RoomDatabase
-keep class * extends androidx.room.Entity
-keep class * extends androidx.room.Dao
-dontwarn androidx.room.paging**

# Keep Custom Room Entities intact to avoid mismatches on DB columns
-keep class gamalsolutions.whatscustomreply.data.database.** { *; }

# Koin Dependency Injection Rules
-keep class org.koin.** { *; }
-dontwarn org.koin.**
# Keep DI entry points and generic parameters
-keepclassmembers class * {
    @org.koin.core.annotation.** *;
}
# Keep your viewmodels and their constructors for DI
-keep class gamalsolutions.whatscustomreply.ui.viewmodel.** { *; }

# OkHttp Rules
-keepattributes Signature, InnerClasses, EnclosingMethod
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
# Keep OkHttp Platform classes
-keep class okhttp3.** { *; }

# Android Notification Listener Service & Android Service
# Services declared in AndroidManifest must be preserved in name and definition
-keep class gamalsolutions.whatscustomreply.service.WhatsAppNotificationListenerService { *; }
-keep class * extends android.app.Service
-keep class * extends android.content.BroadcastReceiver

# ErrorProne Rules
-dontwarn com.google.errorprone.annotations.**
-keep class com.google.errorprone.annotations.** { *; }
