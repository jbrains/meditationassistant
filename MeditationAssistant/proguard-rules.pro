-verbose

#ACRA specifics
# Restore some Source file names and restore approximate line numbers in the stack traces,
# otherwise the stack traces are pretty useless
-keepattributes SourceFile,LineNumberTable

# ACRA needs "annotations" so add this...
# Note: This may already be defined in the default "proguard-android-optimize.txt"
# file in the SDK. If it is, then you don't need to duplicate it. See your
# "project.properties" file to get the path to the default "proguard-android-optimize.txt".
-keepattributes *Annotation*

# ACRA loads Plugins using reflection, so we need to keep all Plugin classes
-keep class * extends @android.support.annotation.Keep org.acra.** {*;}

# ACRA uses enum fields in annotations, so we have to keep those
-keep enum org.acra.** {*;}

-dontwarn android.support.**

# From https://github.com/square/okhttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
# A resource is loaded with a relative path so the package of this class must be preserved.
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# Begin optimizeproguard-optimize.txt:

-optimizations !code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 25
-allowaccessmodification
-dontpreverify

-dontwarn android.webkit.**

-keep class com.google.android.gms.analytics.**
-keep class com.google.analytics.tracking.**
-dontwarn com.google.android.gms.analytics.**
-dontwarn com.google.analytics.tracking.**

-dontwarn com.google.android.gms.**

-keep class android.support.** { *; }

-keep interface android.support.** { *; }

-keep class com.actionbarsherlock.** { *; }

-keep interface com.actionbarsherlock.** { *; }

-keep class com.actionbarsherlock.** {*;}
-keep class org.holoeverywhere.** {*;}

-keep class * extends java.util.ListResourceBundle {
    protected Object[][] getContents();
}

-keep public class com.google.android.gms.common.internal.safeparcel.SafeParcelable {
    public static final *** NULL;
}

-keepnames @com.google.android.gms.common.annotation.KeepName class *
-keepclassmembernames class * {
    @com.google.android.gms.common.annotation.KeepName *;
}

-keepnames class * implements android.os.Parcelable {
    public static final ** CREATOR;
}
-keepclassmembers class * extends com.actionbarsherlock.ActionBarSherlock {
    <init>(android.app.Activity, int);
}



# Keep line numbers to alleviate debugging stack traces

# -renamesourcefileattribute SourceFile

-keepattributes SourceFile,LineNumberTable

# The remainder of this file is identical to the non-optimized version
# of the Proguard configuration file (except that the other file has
# flags to turn off optimization).

-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses

-keepattributes *Annotation*
-keep public class com.google.vending.licensing.ILicensingService
-keep public class com.android.vending.licensing.ILicensingService

-keep class org.acra.** { *; }

# For native methods, see http://proguard.sourceforge.net/manual/examples.html#native
-keepclasseswithmembernames class * {
    native <methods>;
}

# keep setters in Views so that animations can still work.
# see http://proguard.sourceforge.net/manual/examples.html#beans
-keepclassmembers public class * extends android.view.View {
   void set*(***);
   *** get*();
}

# We want to keep methods in Activity that could be used in the XML attribute onClick
-keepclassmembers class * extends android.app.Activity {
   public void *(android.view.View);
}

# For enumeration classes, see http://proguard.sourceforge.net/manual/examples.html#enumerations
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}

-keepclassmembers class **.R$* {
    public static <fields>;
}

# The support library contains references to newer platform versions.
# Don't warn about those in case this app is linking against an older
# platform version.  We know about them, and they are safe.
-dontwarn android.support.**


# End optimize


-keepattributes JavascriptInterface
-overloadaggressively
-allowaccessmodification

-keep public class sh.ftp.rocketninelabs.meditationassistant.JavascriptCallback
-keep public class * implements sh.ftp.rocketninelabs.meditationassistant.JavascriptCallback
-keepclassmembers class * implements sh.ftp.rocketninelabs.meditationassistant.JavascriptCallback {
    <methods>;
}

-keep class android.support.v4.app.** { *; }
-keep interface android.support.v4.app.** { *; }
-keep class com.actionbarsherlock.** { *; }
-keep interface com.actionbarsherlock.** { *; }

# HoloEverywhere
-keep,allowshrinking,allowoptimization public class * extends ListView

-keep,allowoptimization class org.holoeverywhere.** {
    public *;
    protected *;
}

-keepclasseswithmembernames class org.holoeverywhere.internal.** {
    public *;
    protected *;
}

-keepclasseswithmembernames class org.holoeverywhere.internal.AlertController
-keep public class org.holoeverywhere.internal.AlertController.RecycleListView { *; }

-keep,allowshrinking,allowoptimization class org.holoeverywhere.** { *; }
-keep,allowshrinking,allowoptimization interface org.holoeverywhere.** { *; }




-keep,allowshrinking,allowoptimization public class * extends android.app.Application
-keep,allowshrinking,allowoptimization public class * extends android.app.Service
-keep,allowshrinking,allowoptimization public class * extends android.content.BroadcastReceiver
-keep,allowshrinking,allowoptimization public class * extends android.content.ContentProvider
-keep,allowshrinking,allowoptimization public class * extends android.app.backup.BackupAgentHelper
-keep,allowshrinking,allowoptimization public class * extends android.preference.Preference
-keep,allowshrinking,allowoptimization public class com.android.vending.licensing.ILicensingService

-keepclasseswithmembernames class * {
    native <methods>;
}

-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}

-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

-keepclassmembers class * extends android.app.Activity {
   public void *(android.view.View);
}

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}

-keep,allowshrinking,allowoptimization class * { <methods>; }

-keepclasseswithmembers,allowshrinking class * {
    public <init>(android.content.Context,android.util.AttributeSet);
    public <init>(android.content.Context,android.util.AttributeSet,int);
}

-keepclassmembers class * {
    public <init>(android.content.Context,android.util.AttributeSet);
    public <init>(android.content.Context,android.util.AttributeSet,int);
}



# Google API Client

-keepattributes Signature,RuntimeVisibleAnnotations,AnnotationDefault

-keepclassmembers class * {
  @com.google.api.client.util.Key <fields>;
}

# Needed by google-http-client-android when linking against an older platform version

-dontwarn com.google.api.client.extensions.android.**

# Needed by google-api-client-android when linking against an older platform version

-dontwarn com.google.api.client.googleapis.extensions.android.**

# End Google API Client