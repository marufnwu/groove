-keep class androidx.appcompat.widget.** { *; }
-keep class com.arthenica.mobileffmpeg.Config {
    native <methods>;
    void log(int, byte[]);
    void statistics(int, float, float, long , int, double, double);
}
-keep class com.arthenica.mobileffmpeg.AbiDetect {
    native <methods>;
}
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule {
    <init>(...);
}
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
    **[] $VALUES;
    public *;
}
-keep class com.bumptech.glide.load.data.ParcelFileDescriptorRewinder$InternalRewinder {
    *** rewind();
}
-keep class com.coremedia.iso.** { *; }
-keep class com.googlecode.mp4parser.** { *; }
-keep class com.mp4parser.** { *; }
-keep class * extends com.stfalcon.chatkit.messages.MessageHolders$OutcomingTextMessageViewHolder {
    public <init>(android.view.View, java.lang.Object);
    public <init>(android.view.View);
}
-keep class * extends com.stfalcon.chatkit.messages.MessageHolders$IncomingTextMessageViewHolder {
    public <init>(android.view.View, java.lang.Object);
    public <init>(android.view.View);
}
-keep class * extends com.stfalcon.chatkit.messages.MessageHolders$IncomingImageMessageViewHolder {
    public <init>(android.view.View, java.lang.Object);
    public <init>(android.view.View);
}
-keep class * extends com.stfalcon.chatkit.messages.MessageHolders$OutcomingImageMessageViewHolder {
    public <init>(android.view.View, java.lang.Object);
    public <init>(android.view.View);
}
-keepattributes *Annotation*
-keepclassmembers class * {
    @org.greenrobot.eventbus.Subscribe <methods>;
}
-keep enum org.greenrobot.eventbus.ThreadMode { *; }
-keepclassmembers class * extends org.greenrobot.eventbus.util.ThrowableFailureEvent {
    <init>(java.lang.Throwable);
}
-keep class com.groove.megaapp.data.models.* { *; }
-keep class com.daimajia.easing** { *; }
-keep interface com.daimajia.easing** { *; }
-keepnames class * implements android.os.Parcelable {
    public static final ** CREATOR;
}
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int d(...);
    public static int w(...);
    public static int v(...);
    public static int i(...);
    public static int e(...);
}
