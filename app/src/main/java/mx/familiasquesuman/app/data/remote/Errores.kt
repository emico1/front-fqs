package mx.familiasquesuman.app.data.remote

import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.HttpException

/**
 * Convierte cualquier falla de red o del backend en un mensaje que se puede mostrar.
 * El backend ya responde mensajes en español pensados para la persona usuaria.
 */
class ErrorApi(
    val status: Int,
    val codigo: String,
    override val message: String,
    /** Datos extra que manda el backend con algunos errores, por ejemplo la asociación repetida. */
    val detalles: JsonElement? = null
) : Exception(message) {
    /** Lee un texto de los detalles, por ejemplo detalle("asociacionId"). */
    fun detalle(clave: String): String? =
        runCatching { (detalles as? JsonObject)?.get(clave)?.jsonPrimitive?.contentOrNull }.getOrNull()
}

fun mensajeDeError(e: Throwable): String = when (e) {
    is ErrorApi -> e.message
    is IOException -> "No hay conexión con el servidor. Revisa tu internet e intenta de nuevo."
    else -> "Ocurrió un error inesperado."
}

/**
 * Ejecuta una llamada y la envuelve en Result. Las cancelaciones de corrutina se
 * propagan, no se convierten en error.
 */
suspend fun <T> llamar(bloque: suspend () -> T): Result<T> = try {
    Result.success(bloque())
} catch (e: CancellationException) {
    throw e
} catch (e: HttpException) {
    val texto = runCatching { e.response()?.errorBody()?.string() }.getOrNull()
    val detalle = texto?.let { runCatching { jsonFqs.decodeFromString(RespuestaErrorDto.serializer(), it).error }.getOrNull() }
    val mensaje = detalle?.mensaje ?: when (e.code()) {
        401 -> "Tu sesión expiró, inicia sesión de nuevo."
        403 -> "No tienes permiso para esta acción."
        404 -> "No encontramos lo que buscabas."
        429 -> "Demasiados intentos, espera un momento."
        in 500..599 -> "El servidor tuvo un problema, intenta más tarde."
        else -> "No se pudo completar la solicitud."
    }
    Result.failure(ErrorApi(e.code(), detalle?.codigo ?: "http_${e.code()}", mensaje, detalle?.detalles))
} catch (e: IOException) {
    Result.failure(e)
} catch (e: Exception) {
    Result.failure(e)
}
