# NotiOff release R8 / ProGuard rules (expanded in PR 9 for full serialization keeps).

# Keep kotlinx.serialization generated serializers for app models
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keep,includedescriptorclasses class com.ilusons.notioff.**$$serializer { *; }
-keepclassmembers class com.ilusons.notioff.** {
    *** Companion;
}
-keepclasseswithmembers class com.ilusons.notioff.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep @kotlinx.serialization.Serializable class com.ilusons.notioff.** { *; }
