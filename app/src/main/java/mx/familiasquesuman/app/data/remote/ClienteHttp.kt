package mx.familiasquesuman.app.data.remote

import java.util.concurrent.TimeUnit
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import mx.familiasquesuman.app.data.sesion.AlmacenSesion
import okhttp3.Authenticator
import okhttp3.CertificatePinner
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@OptIn(ExperimentalSerializationApi::class)
val jsonFqs = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    encodeDefaults = false
    coerceInputValues = true
}

private val TIPO_JSON = "application/json".toMediaType()

/**
 * Cliente HTTP de la app.
 * El token solo se agrega a peticiones dirigidas al host del backend, nunca a otro
 * dominio (Coil usa este mismo cliente para las imágenes).
 * Cuando el backend responde 401 se intenta renovar la sesión una sola vez con el
 * refresh token. Si varias peticiones fallan al mismo tiempo, solo una renueva y las
 * demás reutilizan el token nuevo.
 *
 * Con pines configurados (fqs.api.pins en gradle.properties) la conexión al backend solo
 * se acepta si alguna llave pública de la cadena coincide, aunque un tercero logre que el
 * teléfono confíe en otro certificado. Coil y los cupos en vivo usan este mismo cliente.
 */
class ClienteHttp(
    baseUrl: String,
    private val sesion: AlmacenSesion,
    private val agente: String,
    pines: List<String> = emptyList()
) {

    private val base: HttpUrl = baseUrl.toHttpUrl()
    private val candadoRenovacion = Any()

    private val fijacion: CertificatePinner? =
        if (pines.isEmpty() || base.scheme != "https") null
        else CertificatePinner.Builder().apply { pines.forEach { add(base.host, it) } }.build()

    private fun OkHttpClient.Builder.conFijacion() = apply { fijacion?.let { certificatePinner(it) } }

    private fun esDelBackend(url: HttpUrl) =
        url.scheme == base.scheme && url.host == base.host && url.port == base.port

    // Login, registro, refresh y recuperación no llevan token. Así un 401 por contraseña
    // incorrecta nunca dispara una renovación de sesión.
    private fun esRutaDeAuth(url: HttpUrl) = url.encodedPath.startsWith(base.encodedPath + "v1/auth/")

    private val agregarToken = Interceptor { chain ->
        val original = chain.request()
        val peticion = original.newBuilder().header("User-Agent", agente)
        val token = sesion.tokens()?.accessToken
        if (token != null && esDelBackend(original.url) && !esRutaDeAuth(original.url) && original.header("Authorization") == null) {
            peticion.header("Authorization", "Bearer $token")
        }
        chain.proceed(peticion.build())
    }

    // Cliente sin autenticador, solo para llamar a /v1/auth/refresh
    private val clienteRenovacion = OkHttpClient.Builder()
        .conFijacion()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val renovarSesion = object : Authenticator {
        override fun authenticate(route: Route?, response: Response): Request? {
            val peticion = response.request
            if (!esDelBackend(peticion.url) || esRutaDeAuth(peticion.url)) return null
            // Ya se reintentó una vez, no entrar en ciclo
            if (response.priorResponse != null) return null
            val usado = peticion.header("Authorization")?.removePrefix("Bearer ") ?: return null

            synchronized(candadoRenovacion) {
                val actuales = sesion.tokens() ?: return null
                // Otra petición ya renovó mientras esta esperaba
                if (actuales.accessToken != usado) {
                    return peticion.newBuilder().header("Authorization", "Bearer ${actuales.accessToken}").build()
                }
                val nuevos = renovar(actuales.refreshToken) ?: return null
                sesion.guardar(nuevos)
                return peticion.newBuilder().header("Authorization", "Bearer ${nuevos.accessToken}").build()
            }
        }
    }

    /** Devuelve tokens nuevos, o null. Si el backend rechaza el refresh se cierra la sesión. */
    private fun renovar(refresh: String): TokensDto? {
        val cuerpo = jsonFqs.encodeToString(RefreshDto.serializer(), RefreshDto(refresh)).toRequestBody(TIPO_JSON)
        val peticion = Request.Builder()
            .url(base.newBuilder().addPathSegments("v1/auth/refresh").build())
            .header("User-Agent", agente)
            .post(cuerpo)
            .build()
        return try {
            clienteRenovacion.newCall(peticion).execute().use { r ->
                when {
                    r.isSuccessful -> {
                        val texto = r.body?.string() ?: return null
                        jsonFqs.decodeFromString(SoloTokensDto.serializer(), texto).tokens
                    }
                    r.code == 401 -> {
                        sesion.avisarExpiracion()
                        null
                    }
                    else -> null
                }
            }
        } catch (e: Exception) {
            // Sin red, se conserva la sesión y la petición original falla normalmente
            null
        }
    }

    val okHttp: OkHttpClient = OkHttpClient.Builder()
        .conFijacion()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(agregarToken)
        .authenticator(renovarSesion)
        .build()

    val api: FqsApi = Retrofit.Builder()
        .baseUrl(base)
        .client(okHttp)
        .addConverterFactory(jsonFqs.asConverterFactory(TIPO_JSON))
        .build()
        .create(FqsApi::class.java)

    /** Flujo de cupos en tiempo real. */
    val urlCuposEnVivo: String = base.newBuilder().addPathSegments("v1/cupos/stream").build().toString()

    /** URL de una imagen guardada en el backend, o null si no hay imagen. */
    fun urlImagen(clave: String?): String? =
        clave?.let { base.newBuilder().addPathSegments("v1/archivos").addPathSegment(it).build().toString() }
}
