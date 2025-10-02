# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Keep the Unity bridge class and its members (called from Unity via reflection)
-keep class com.prog7314.IdleLoadsheddingMobile.core.UnityBridge { *; }
-keep class com.prog7314.IdleLoadsheddingMobile.core.UnityBridge$Listener { *; }

# Keep Unity integration classes
-keep class com.prog7314.IdleLoadsheddingMobile.unity.** { *; }

# Keep data models used for JSON serialization
-keep class com.prog7314.IdleLoadsheddingMobile.data.models.** { *; }

# Gson specific rules
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
