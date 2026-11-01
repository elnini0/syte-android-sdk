# Applied to apps that shrink their release build with R8.

# Gson maps API responses onto these classes by field name, so they must keep their names.
-keep class com.syte.ai.android_sdk.data.** { *; }
-keep class com.syte.ai.android_sdk.events.** { *; }
-keepattributes Signature, *Annotation*, InnerClasses, EnclosingMethod

# Optional classes referenced by the networking and image libraries, not needed at runtime.
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
-dontwarn io.reactivex.**
-dontwarn com.google.errorprone.annotations.**
