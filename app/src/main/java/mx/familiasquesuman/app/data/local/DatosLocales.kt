package mx.familiasquesuman.app.data.local

import android.content.Context
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import mx.familiasquesuman.app.data.remote.jsonFqs

/**
 * Todo lo que la app guarda en el teléfono para funcionar sin conexión.
 *
 * Respuestas del backend. Cada consulta correcta se guarda cifrada con su clave (por ejemplo
 * actividades o actividad/{id}). Si después no hay red, la pantalla muestra la última copia
 * y un aviso con la hora en que se guardó.
 *
 * Envíos pendientes. Las escrituras que fallan por falta de red se guardan en orden y
 * SincronizarEnvios las manda en cuanto hay conexión. Las fotos de esos envíos se guardan
 * cifradas en una carpeta que no se respalda.
 *
 * Al cerrar sesión, al expirar la sesión o al pedir la eliminación de la cuenta se borra todo.
 */
class DatosLocales(context: Context) {

    private val dao = BaseLocal.crear(context).dao()
    private val cifrado = CifradoLocal()
    private val carpetaFotos = File(context.noBackupFilesDir, "envios")

    val pendientes: Flow<Int> = dao.pendientes()
    val rechazados: Flow<List<EnvioPendiente>> = dao.rechazados()

    // ---------------- Respuestas guardadas ----------------

    suspend fun <T> guardar(clave: String, serializador: KSerializer<T>, valor: T) {
        val texto = jsonFqs.encodeToString(serializador, valor)
        val contenido = withContext(Dispatchers.Default) { cifrado.cifrarTexto(texto, "respuesta/$clave") }
        dao.guardarRespuesta(RespuestaGuardada(clave, contenido, System.currentTimeMillis()))
    }

    /** La copia guardada y la hora en que se guardó, o null si no hay o ya no se puede leer. */
    suspend fun <T> leer(clave: String, serializador: KSerializer<T>): Pair<T, Long>? {
        val fila = dao.respuesta(clave) ?: return null
        return try {
            val texto = withContext(Dispatchers.Default) { cifrado.descifrarTexto(fila.contenido, "respuesta/$clave") }
            jsonFqs.decodeFromString(serializador, texto) to fila.guardadaEn
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Llave perdida, datos alterados o un formato viejo, se descarta la copia
            dao.borrarRespuesta(clave)
            null
        }
    }

    /** Las copias de más de 30 días ya no sirven para decidir nada, se borran al abrir la app. */
    suspend fun podar() {
        dao.borrarRespuestasViejas(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30))
    }

    // ---------------- Envíos pendientes ----------------

    suspend fun encolar(envio: Envio, idempotencia: String) {
        dao.encolar(
            EnvioPendiente(
                idempotencia = idempotencia,
                tipo = envio.tipo(),
                contenido = cifrarEnvio(envio, idempotencia),
                creadoEn = System.currentTimeMillis()
            )
        )
    }

    /** Siguiente envío pendiente en el orden en que se hicieron. Los ilegibles se descartan. */
    suspend fun siguiente(): Pair<EnvioPendiente, Envio>? {
        while (true) {
            val fila = dao.siguiente() ?: return null
            try {
                val texto = withContext(Dispatchers.Default) { cifrado.descifrarTexto(fila.contenido, "envio/${fila.idempotencia}") }
                return fila to jsonFqs.decodeFromString(Envio.serializer(), texto)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                dao.quitar(fila.id)
            }
        }
    }

    /** Guarda el envío con la foto ya subida, así un reintento no la vuelve a subir. */
    suspend fun actualizar(fila: EnvioPendiente, envio: Envio, intentos: Int) {
        dao.actualizar(fila.id, cifrarEnvio(envio, fila.idempotencia), intentos)
    }

    suspend fun rechazar(fila: EnvioPendiente, motivo: String) = dao.rechazar(fila.id, motivo.take(300))

    suspend fun quitar(fila: EnvioPendiente) = dao.quitar(fila.id)

    suspend fun descartarRechazados() = dao.borrarRechazados()

    private suspend fun cifrarEnvio(envio: Envio, idempotencia: String): String {
        val texto = jsonFqs.encodeToString(Envio.serializer(), envio)
        return withContext(Dispatchers.Default) { cifrado.cifrarTexto(texto, "envio/$idempotencia") }
    }

    // ---------------- Fotos de envíos pendientes ----------------

    /** Guarda el JPEG cifrado y devuelve el nombre del archivo. */
    suspend fun guardarFoto(jpeg: ByteArray): String = withContext(Dispatchers.IO) {
        val nombre = UUID.randomUUID().toString()
        carpetaFotos.mkdirs()
        File(carpetaFotos, nombre).writeBytes(cifrado.cifrar(jpeg, "foto/$nombre"))
        nombre
    }

    suspend fun leerFoto(nombre: String): ByteArray? = withContext(Dispatchers.IO) {
        val archivo = archivoFoto(nombre) ?: return@withContext null
        runCatching { cifrado.descifrar(archivo.readBytes(), "foto/$nombre") }.getOrNull()
    }

    suspend fun borrarFoto(nombre: String) {
        withContext(Dispatchers.IO) { archivoFoto(nombre)?.delete() }
    }

    /** Solo nombres generados por guardarFoto, nunca una ruta que salga de la carpeta. */
    private fun archivoFoto(nombre: String): File? {
        if (!Regex("^[0-9a-f-]{36}$").matches(nombre)) return null
        return File(carpetaFotos, nombre).takeIf { it.isFile }
    }

    // ---------------- Limpieza ----------------

    suspend fun borrarTodo() {
        dao.borrarRespuestas()
        dao.borrarEnvios()
        withContext(Dispatchers.IO) { carpetaFotos.deleteRecursively() }
    }
}
