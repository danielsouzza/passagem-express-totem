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

# ============================================================================
# kotlinx.serialization
# ============================================================================
# @Serializable / @Polymorphic / @SerialName são lidos em runtime.
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault

# Mantém o campo `Companion` de classes @Serializable.
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}

# Mantém `serializer()` nos Companion de classes @Serializable.
-if @kotlinx.serialization.Serializable class ** {
    static **$Companion Companion;
}
-keepclassmembers class <1>$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}

# Mantém `INSTANCE.serializer()` de objects @Serializable.
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# DTOs da API e serializers custom (IntBooleanSerializer / PhpAssocMapSerializer / envelope).
-keep,includedescriptorclasses class com.example.passagenexpress.core.data.remote.** { *; }

# Nav payloads serializados e passados nas rotas (feature room/passenger/payment).
-keep,includedescriptorclasses class com.example.passagenexpress.feature.**.navigation.** { *; }