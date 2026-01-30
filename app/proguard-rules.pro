# Ultimate Player - Git Freeze Protection
-keep class com.playermaxai.** { *; }
-keep class com.playermaxai.core.** { *; }
-dontwarn org.videolan.libvlc.**
-keep class org.videolan.libvlc.** { *; }

# No-Lite Policy - usuń dead code
-dontoptimize
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Native LibVLC protection
-keepclasseswithmembernames class * {
    native <methods>;
}
-dontwarn **.R$*
-dontwarn **.BuildConfig*

# Build Hash Verification (#80) - keep version strings
-keepclassmembers class * {
    public static final java.lang.String VERSION = "<init>";
}
