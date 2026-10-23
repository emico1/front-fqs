package mx.familiasquesuman.app.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * Última respuesta correcta de una consulta al backend, para mostrarla sin conexión.
 * El contenido va cifrado con la llave del Android Keystore (ver CifradoLocal).
 */
@Entity(tableName = "respuestas_guardadas")
data class RespuestaGuardada(
    @PrimaryKey val clave: String,
    val contenido: String,
    val guardadaEn: Long
)

/**
 * Escritura hecha sin conexión que se manda cuando vuelve la red. La clave de idempotencia
 * se genera una sola vez, así un reintento nunca crea un duplicado en el servidor.
 * tipo es un texto fijo para mostrar (Reporte, Propuesta...), sin datos de la persona.
 */
@Entity(tableName = "envios_pendientes", indices = [Index(value = ["idempotencia"], unique = true)])
data class EnvioPendiente(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val idempotencia: String,
    val tipo: String,
    val contenido: String,
    val creadoEn: Long,
    val intentos: Int = 0,
    /** Mensaje del servidor cuando rechazó el envío. Mientras sea null sigue pendiente. */
    val rechazo: String? = null
)

@Dao
interface DaoLocal {

    @Query("SELECT * FROM respuestas_guardadas WHERE clave = :clave")
    suspend fun respuesta(clave: String): RespuestaGuardada?

    @Upsert
    suspend fun guardarRespuesta(r: RespuestaGuardada)

    @Query("DELETE FROM respuestas_guardadas WHERE clave = :clave")
    suspend fun borrarRespuesta(clave: String)

    @Query("DELETE FROM respuestas_guardadas WHERE guardadaEn < :limite")
    suspend fun borrarRespuestasViejas(limite: Long)

    @Query("DELETE FROM respuestas_guardadas")
    suspend fun borrarRespuestas()

    @Insert
    suspend fun encolar(e: EnvioPendiente): Long

    @Query("SELECT * FROM envios_pendientes WHERE rechazo IS NULL ORDER BY id LIMIT 1")
    suspend fun siguiente(): EnvioPendiente?

    @Query("UPDATE envios_pendientes SET contenido = :contenido, intentos = :intentos WHERE id = :id")
    suspend fun actualizar(id: Long, contenido: String, intentos: Int)

    @Query("UPDATE envios_pendientes SET rechazo = :motivo WHERE id = :id")
    suspend fun rechazar(id: Long, motivo: String)

    @Query("DELETE FROM envios_pendientes WHERE id = :id")
    suspend fun quitar(id: Long)

    @Query("SELECT COUNT(*) FROM envios_pendientes WHERE rechazo IS NULL")
    fun pendientes(): Flow<Int>

    @Query("SELECT * FROM envios_pendientes WHERE rechazo IS NOT NULL ORDER BY id")
    fun rechazados(): Flow<List<EnvioPendiente>>

    @Query("DELETE FROM envios_pendientes WHERE rechazo IS NOT NULL")
    suspend fun borrarRechazados()

    @Query("DELETE FROM envios_pendientes")
    suspend fun borrarEnvios()
}

@Database(entities = [RespuestaGuardada::class, EnvioPendiente::class], version = 1, exportSchema = false)
abstract class BaseLocal : RoomDatabase() {

    abstract fun dao(): DaoLocal

    companion object {
        fun crear(context: Context): BaseLocal =
            Room.databaseBuilder(context.applicationContext, BaseLocal::class.java, "fqs_local.db")
                // Es solo una copia de lo que hay en el servidor, si cambia la estructura se vuelve a llenar
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
    }
}
