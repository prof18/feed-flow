# Okhttp

# JSR 305 annotations are for embedding nullability information.
-dontwarn javax.annotation.**

# A resource is loaded with a relative path so the package of this class must be preserved.
-adaptresourcefilenames okhttp3/internal/publicsuffix/PublicSuffixDatabase.gz

# Animal Sniffer compileOnly dependency to ensure APIs are compatible with older versions of Java.
-dontwarn org.codehaus.mojo.animal_sniffer.*

# OkHttp platform used only on JVM and when Conscrypt and other security providers are available.
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Okio

# Animal Sniffer compileOnly dependency to ensure APIs are compatible with older versions of Java.
-dontwarn org.codehaus.mojo.animal_sniffer.*
-dontwarn org.slf4j.impl.StaticLoggerBinder

# Keep LinkOpeningPreference enum and its serialization
-keep class com.prof18.feedflow.core.model.LinkOpeningPreference { *; }
-keepclassmembers class com.prof18.feedflow.core.model.LinkOpeningPreference { *; }
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}

# Keep the DateTimeComponents class and all its members
# TODO: remove when https://github.com/Kotlin/kotlinx-datetime/issues/519 is closed
-keep class kotlinx.datetime.format.DateTimeComponents { *; }