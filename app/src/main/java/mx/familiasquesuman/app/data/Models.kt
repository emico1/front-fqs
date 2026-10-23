package mx.familiasquesuman.app.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Diversity3
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Elderly
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pets
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Modelos que usan las pantallas. Ya no hay datos de muestra, todo se llena desde el
 * backend a través de Repositorio (las conversiones están en Mapeos.kt).
 */

/** La clave coincide con categorias.clave de la base de datos. */
enum class Categoria(val clave: String, val etiqueta: String, val icono: ImageVector) {
    MEDIO_AMBIENTE("MEDIO_AMBIENTE", "Medio Ambiente", Icons.Filled.Eco),
    ADULTOS_MAYORES("ADULTOS_MAYORES", "Adultos Mayores", Icons.Filled.Elderly),
    NINOS("NINOS", "Niños", Icons.Filled.ChildCare),
    MASCOTAS("MASCOTAS", "Mascotas", Icons.Filled.Pets),
    ARTE_CULTURA("ARTE_CULTURA", "Arte y Cultura", Icons.Filled.Palette),
    COMUNIDAD("COMUNIDAD", "Comunidad", Icons.Filled.Diversity3);

    companion object {
        /** Una categoría nueva que la app aún no conoce se muestra como Comunidad. */
        fun desdeClave(clave: String?): Categoria = entries.firstOrNull { it.clave == clave } ?: COMUNIDAD
    }
}

/**
 * Tipo de actividad. Visiteo son visitas a lugares (casa hogar, refugio, comedor) donde
 * la familia va a convivir. Voluntariado es cuando la familia trabaja o ayuda.
 */
enum class TipoActividad(val api: String, val etiqueta: String) {
    VOLUNTARIADO("voluntariado", "Voluntariado"),
    VISITA("visita", "Visita");

    companion object {
        fun desdeApi(v: String?) = entries.firstOrNull { it.api == v } ?: VOLUNTARIADO
    }
}

/** Filtro inicial de Inicio, igual que en el sitio web de Familias que Suman. */
enum class FiltroInicio(val etiqueta: String) {
    PARTICIPAR("Participar"),
    DONAR("Donar"),
    VISITEO("Visiteo"),
    PROYECTOS("Proyectos")
}

data class Asociacion(
    val id: String,
    val nombre: String,
    val categoria: Categoria,
    val descripcion: String,
    val ubicacion: String,
    val verificada: Boolean,
    val familiasBeneficiadas: Int,
    val mision: String,
    val necesidades: String,
    val fotoUrl: String?,
    val telefono: String?,
    val whatsapp: String?,
    val correo: String?,
    val latitud: Double?,
    val longitud: Double?,
    val notificacionesActivas: Boolean = false,
    val meGusta: Int = 0,
    val noMeGusta: Int = 0,
    val proximasActividades: List<ActividadItem> = emptyList()
)

data class InscripcionActual(
    val id: String,
    val confirmada: Boolean,
    val titularAsiste: Boolean,
    val miembros: Set<String>
)

data class ActividadItem(
    val id: String,
    val titulo: String,
    val asociacion: String,
    val asociacionId: String,
    val categoria: Categoria,
    val dia: String,
    val mes: String,
    val fechaHoraCompleta: String,
    val ubicacion: String,
    val etiquetaAudiencia: String,
    val descripcionCorta: String,
    val descripcionLarga: String,
    val recomendaciones: String,
    val cuposDisponibles: Int,
    val cuposTotales: Int,
    val edadMinima: Int?,
    val inicioMillis: Long,
    val finMillis: Long,
    val fotoUrl: String?,
    val tipo: TipoActividad = TipoActividad.VOLUNTARIADO,
    val latitud: Double? = null,
    val longitud: Double? = null,
    val esFavorita: Boolean = false,
    val inscripcion: InscripcionActual? = null
)

data class ArticuloDonacion(
    val id: String,
    val nombre: String,
    val descripcion: String,
    val unidad: String,
    val prioridad: String,
    val urgente: Boolean,
    val progresoTexto: String,
    val progreso: Float
)

data class PuntoEntrega(
    val id: String,
    val nombre: String,
    val direccion: String,
    val horarios: String,
    val indicaciones: String?
)

data class Campana(
    val id: String,
    val titulo: String,
    val organizacion: String,
    val descripcion: String,
    val recaudadoTexto: String,
    val metaTexto: String,
    val progreso: Float,
    val urgente: Boolean = false,
    val diasRestantes: Int? = null,
    val articulos: List<ArticuloDonacion> = emptyList(),
    val puntosEntrega: List<PuntoEntrega> = emptyList()
)

data class Proyecto(
    val id: String,
    val titulo: String,
    val descripcion: String,
    val organizacion: String?,
    val periodo: String,
    val progreso: Float,
    val campanasActivas: Int,
    val fotoUrl: String?,
    val campanas: List<Campana> = emptyList()
)

data class MiembroComunidad(
    val id: String,
    val nombre: String,
    val relacion: String,
    val iniciales: String,
    val fechaNacimiento: String
)

/** Datos para crear un miembro nuevo desde Mi Comunidad. */
data class NuevoMiembro(
    val nombre: String,
    val relacion: String,
    val fechaNacimientoIso: String,
    val contacto: String
)

enum class EstadoPropuesta(val etiqueta: String) {
    EN_REVISION("En revisión"),
    NECESITA_CAMBIOS("Necesita cambios"),
    APROBADA("Aprobada"),
    RECHAZADA("No aprobada"),
    COMPLETADA("Completada");

    companion object {
        fun desdeApi(valor: String): EstadoPropuesta = when (valor) {
            "necesita_cambios" -> NECESITA_CAMBIOS
            "aprobada" -> APROBADA
            "rechazada" -> RECHAZADA
            "completada" -> COMPLETADA
            else -> EN_REVISION
        }
    }
}

data class PasoHistorial(
    val estado: EstadoPropuesta,
    val fecha: String,
    val comentario: String?
)

data class Propuesta(
    val id: String,
    val tipo: String,
    val titulo: String,
    val descripcion: String,
    val fecha: String,
    val estado: EstadoPropuesta,
    val comentarioAdmin: String? = null,
    val historial: List<PasoHistorial> = emptyList()
)

/** Tipos de propuesta que muestra la app y su valor en la API. */
enum class TipoPropuesta(val etiqueta: String, val api: String) {
    ACTIVIDAD("Actividad", "actividad"),
    INICIATIVA_VECINAL("Iniciativa vecinal", "iniciativa_vecinal"),
    ASOCIACION("Asociación", "asociacion");

    companion object {
        fun desdeApi(v: String) = entries.firstOrNull { it.api == v } ?: INICIATIVA_VECINAL
    }
}

/** Cómo terminó una participación, para el historial (RF-11). */
enum class ResultadoParticipacion(val etiqueta: String) {
    PROXIMA("Próxima"),
    ASISTIO("Asististe"),
    SIN_ASISTENCIA("Sin asistencia registrada"),
    CANCELADA("Cancelada")
}

/** Una actividad del historial del perfil (inscripción propia). */
data class Experiencia(
    val inscripcionId: String,
    val actividadId: String,
    val titulo: String,
    val fecha: String,
    val ubicacion: String,
    val asistio: Boolean,
    val estadoTestimonio: String?,
    val inicioMillis: Long = 0L,
    /** Mes y año en texto, por ejemplo "Octubre 2026", para agrupar el historial */
    val mesAnio: String = "",
    val resultado: ResultadoParticipacion = ResultadoParticipacion.PROXIMA,
    val tipo: TipoActividad = TipoActividad.VOLUNTARIADO,
    /** 1 me gusta, -1 no me gusta, null sin calificar (RF21) */
    val valoracion: Int? = null
) {
    val puedePublicarTestimonio: Boolean get() = asistio && estadoTestimonio == null
}

data class Insignia(val nombre: String, val descripcion: String)

data class Perfil(
    val id: String,
    val nombre: String,
    val username: String,
    val correo: String?,
    val ciudadId: String?,
    val ciudadNombre: String?,
    val fotoUrl: String?,
    val nivel: String?,
    val participaciones: Int,
    val insignias: List<Insignia>,
    val quiereDonar: Boolean,
    val quiereParticipar: Boolean,
    val quiereProponer: Boolean,
    val recibirRecordatorios: Boolean,
    val recibirSugerencias: Boolean,
    val categorias: Set<String>,
    val avisoPendiente: Boolean = false,
    /** Las cuentas de administración gestionan, no se inscriben a actividades */
    val esAdmin: Boolean = false
)

data class Ciudad(val id: String, val nombre: String)

data class AvisoPrivacidad(val id: String, val version: String, val contenido: String)

data class Catalogo(
    val ciudades: List<Ciudad>,
    val categorias: List<Categoria>,
    val aviso: AvisoPrivacidad?
)

/** Lo que se captura en Registro y se completa en el onboarding antes de crear la cuenta. */
data class DatosRegistro(
    val nombre: String,
    val contacto: String,
    val password: String,
    val fechaNacimientoIso: String,
    val ciudadId: String
)

/** Estado de privacidad de la cuenta (RF-15). */
data class Privacidad(
    val aviso: AvisoPrivacidad?,
    val publicadoEn: String?,
    val aceptado: Boolean,
    val aceptadoEn: String?,
    val solicitudEliminacion: String?
)

/** Cambio de cupo recibido en tiempo real. */
data class CambioCupo(val actividadId: String, val cupoTotal: Int, val cuposDisponibles: Int)

data class Notificacion(
    val id: String,
    val titulo: String,
    val cuerpo: String,
    val leida: Boolean,
    val tipo: String = "",
    val actividadId: String? = null,
    val campanaId: String? = null,
    val propuestaId: String? = null,
    val asociacionId: String? = null
)

// ---------------- Fase 2 ----------------

data class OpcionEncuesta(val id: String, val texto: String)

data class PreguntaEncuesta(
    val id: String,
    val texto: String,
    val esOpcionUnica: Boolean,
    val obligatoria: Boolean,
    val opciones: List<OpcionEncuesta>,
    val respondida: Boolean
)

data class Encuesta(
    val id: String,
    val titulo: String,
    /** true para la encuesta previa, false para la posterior */
    val esPrevia: Boolean,
    val preguntas: List<PreguntaEncuesta>
) {
    val respondida: Boolean get() = preguntas.isNotEmpty() && preguntas.all { it.respondida || !it.obligatoria }
}

/** Respuesta a una pregunta, con opción o con texto. */
data class RespuestaEncuesta(val preguntaId: String, val opcionId: String? = null, val texto: String? = null)

data class Favoritos(val asociaciones: List<Asociacion>, val actividades: List<ActividadItem>)

/** Historia publicada por una familia y aprobada por el equipo (RF-12). */
data class Testimonio(
    val id: String,
    val texto: String,
    val fotoUrl: String?,
    val autor: String,
    val actividad: String?,
    val asociacion: String?,
    val fecha: String
)

// ---------------- Seguridad ----------------

/** Resultado de escribir correo y contraseña. */
sealed interface PasoLogin {
    /** Sesión abierta */
    data object Listo : PasoLogin
    /** Cuenta de administración con segundo factor, falta el código de su app */
    data class Codigo(val desafio: String) : PasoLogin
}

data class EstadoSegundoFactor(
    val activo: Boolean,
    val codigosRespaldoRestantes: Int,
    val obligatorio: Boolean
)

/** Clave para la app de autenticación y el enlace otpauth que la abre directamente. */
data class InicioSegundoFactor(val secreto: String, val uri: String)

/** Resultado de enviar el registro. */
sealed interface ResultadoRegistro {
    data class Listo(val perfil: Perfil) : ResultadoRegistro
    /** Falta escribir el código que llegó por correo o por SMS */
    data class VerificarContacto(val registroId: String, val destino: String, val porSms: Boolean) : ResultadoRegistro
}

/** Un dispositivo con sesión abierta. */
data class SesionDispositivo(
    val id: String,
    val dispositivo: String,
    val iniciada: String,
    val ultimaActividad: String,
    val actual: Boolean
)

