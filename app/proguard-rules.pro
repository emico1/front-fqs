# Reglas para la versión release (minify activado).
# Retrofit, OkHttp y kotlinx.serialization ya incluyen sus propias reglas, aquí solo
# se protege lo propio de la app.

# Clases serializables de la capa remota y sus serializadores generados
-keepattributes *Annotation*, InnerClasses, Signature, Exceptions
-keep,includedescriptorclasses class mx.familiasquesuman.app.data.remote.**$$serializer { *; }
-keepclassmembers class mx.familiasquesuman.app.data.remote.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}

# Envíos guardados sin conexión (clase sellada serializable)
-keep,includedescriptorclasses class mx.familiasquesuman.app.data.local.**$$serializer { *; }
-keepclassmembers class mx.familiasquesuman.app.data.local.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}

# Interfaz de Retrofit (los métodos suspend necesitan sus firmas genéricas)
-keep,allowobfuscation interface mx.familiasquesuman.app.data.remote.FqsApi
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# Quita los registros de depuración del APK publicado
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
