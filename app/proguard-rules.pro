# NewPipeExtractor
-keep class org.schabi.newpipe.extractor.timeago.patterns.** { *; }
-keep class org.mozilla.javascript.** { *; }
-keep class org.mozilla.classfile.ClassFileWriter
-dontwarn org.mozilla.javascript.**
-dontwarn org.mozilla.classfile.**
-dontwarn java.beans.**
-dontwarn javax.script.**
-dontwarn jdk.dynalink.**
-dontwarn com.google.re2j.**

# Conscrypt
-keep class org.conscrypt.** { *; }
-dontwarn org.conscrypt.**
-dontwarn com.android.org.conscrypt.**
-dontwarn org.apache.harmony.xnet.provider.jsse.**

# NanoHTTPD / ZXing
-keep class fi.iki.elonen.** { *; }
-dontwarn fi.iki.elonen.**

# Dùng reflection để lấy User-Agent của YouTube
-keep class org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper { *; }

-dontwarn org.jetbrains.annotations.**
-dontwarn javax.annotation.**
