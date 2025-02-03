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

# ---------------- Basic Configuration ----------------
# Optimize code aggressively
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontpreverify
-verbose
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*,!code/allocation/variable

# ---------------- Security Enhancements ----------------
# Remove all debug and verbose logging
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

# Remove System.out/println calls
-assumenosideeffects class java.io.PrintStream {
    public void println(...);
    public void print(...);
}

# Additional string and code obfuscation
-adaptclassstrings
-adaptresourcefilecontents **.properties,META-INF/MANIFEST.MF,META-INF/*.version
-classobfuscationdictionary dictionary.txt
-packageobfuscationdictionary dictionary.txt
-obfuscationdictionary dictionary.txt

# ---------------- Crash Reporting Protection ----------------
# Keep source file names and line numbers for stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep crash reporting and analytics working
-keepattributes *Annotation*
-keepattributes Exception
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# ---------------- Database Protection ----------------
# Keep Room database (no obfuscation for database)
-keep class com.redn.farm.data.entity.** { *; }
-keep class com.redn.farm.data.dao.** { *; }
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# ---------------- Framework Components ----------------
# Keep Compose UI
-keep class androidx.compose.** { *; }
-keepclassmembers class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Keep Hilt DI
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.lifecycle.HiltViewModel
-dontwarn dagger.hilt.**

# Keep Kotlin Coroutines
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# ---------------- Data Classes Protection ----------------
# Keep model classes but allow obfuscation of their members
-keepclassmembers class com.redn.farm.data.model.** {
    <fields>;
    <methods>;
}

# Keep Kotlin data classes
-keepclassmembers class * {
    @kotlin.Metadata <methods>;
    @kotlin.Metadata <fields>;
}

# ---------------- Third Party Libraries ----------------
# Keep CSV functionality
-keep class com.opencsv.** { *; }
-dontwarn com.opencsv.**

# ---------------- Android Framework ----------------
# Keep required Android components
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.preference.Preference
-keep public class * extends android.view.View
-keep public class * extends android.app.Fragment

# Keep the app's entry points
-keep public class com.redn.farm.MainActivity
-keep public class com.redn.farm.FarmApplication

# ---------------- View Components ----------------
# Keep custom view constructors
-keepclasseswithmembers class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# Keep onClick handlers
-keepclassmembers class * extends android.content.Context {
    public void *(android.view.View);
    public void *(android.view.MenuItem);
}

# ---------------- Serialization ----------------
# Keep Parcelable implementations
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep Serializable implementations
-keepnames class * implements java.io.Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ---------------- Enums ----------------
# Keep enums (used for state management)
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ---------------- Resources ----------------
# Keep R classes
-keepclassmembers class **.R$* {
    public static <fields>;
}

# ---------------- Native Methods ----------------
# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# ---------------- WebView & JavaScript ----------------
# Keep JavaScript interface methods
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# ---------------- Additional Security ----------------
# Remove all debugging related code
-assumevalues class android.os.Build$VERSION {
    int SDK_INT return 21..2147483647;
}

# ---------------- Performance Optimization ----------------
# Remove unused code in release
-dontwarn android.support.**
-dontwarn com.google.android.material.**
-dontwarn androidx.**
-keep class androidx.** { *; }
-keep interface androidx.** { *; }

# Aggressive optimization
-allowaccessmodification
-repackageclasses ''