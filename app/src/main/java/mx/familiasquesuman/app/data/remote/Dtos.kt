package mx.familiasquesuman.app.data.remote

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/*
 * Objetos tal como los envía y recibe la API (JSON en camelCase).
 * Se mantienen separados de los modelos de interfaz de data/Models.kt para que un
 * cambio en el backend no obligue a tocar las pantallas.
 */

// ---------------- Comunes ----------------

@Serializable
data class RefDto(val id: String, val nombre: String)

@Serializable
data class CategoriaDto(val id: String, val clave: String, val nombre: String, val descripcion: String? = null)

@Serializable
data class CiudadDto(val id: String, val nombre: String, val estado: String? = null, val zonaHoraria: String? = null)

@Serializable
data class ErrorDto(val codigo: String, val mensaje: String, val detalles: JsonElement? = null)

@Serializable
data class RespuestaErrorDto(val error: ErrorDto)

// ---------------- Catálogo y autenticación ----------------

@Serializable
data class AvisoPrivacidadDto(val id: String, val version: String, val contenido: String)

@Serializable
data class CatalogosDto(
    val ciudades: List<CiudadDto>,
    val categorias: List<CategoriaDto>,
    val avisoPrivacidad: AvisoPrivacidadDto? = null
)

@Serializable
data class TokensDto(
    val accessToken: String,
    val refreshToken: String,
    val accessExpiraEn: String,
    val refreshExpiraEn: String
)

@Serializable
data class PreferenciasRegistroDto(
    val quiereDonar: Boolean,
    val quiereParticipar: Boolean,
    val quiereProponer: Boolean
)

@Serializable
data class RegistroDto(
    val nombre: String,
    val username: String,
    val correo: String? = null,
    val telefono: String? = null,
    val password: String,
    val fechaNacimiento: String,
    val ciudadId: String,
    val avisoPrivacidadId: String,
    val preferencias: PreferenciasRegistroDto,
    val categorias: List<String>
)

@Serializable
data class LoginDto(val identificador: String, val password: String)

@Serializable
data class RefreshDto(val refreshToken: String)

@Serializable
data class RecuperarDto(val identificador: String)

@Serializable
data class RestablecerDto(val identificador: String, val codigo: String, val password: String)

@Serializable
data class SesionDto(val tokens: TokensDto, val usuario: PerfilDto)

/**
 * Respuesta del inicio de sesión. Una familia recibe tokens y usuario. Una cuenta de
 * administración con segundo factor recibe requiere2fa y un desafío que se canjea con
 * el código de su app de autenticación.
 */
@Serializable
data class RespuestaLoginDto(
    val tokens: TokensDto? = null,
    val usuario: PerfilDto? = null,
    val requiere2fa: Boolean = false,
    val desafio: String? = null,
    val configurar2fa: Boolean = false
)

@Serializable
data class SegundoPasoDto(val desafio: String, val codigo: String)

/**
 * Respuesta del registro. Solo con teléfono trae tokens y usuario. Con correo trae
 * registroId y el correo enmascarado, la cuenta se crea al verificar el código.
 */
@Serializable
data class RespuestaRegistroDto(
    val tokens: TokensDto? = null,
    val usuario: PerfilDto? = null,
    val registroId: String? = null,
    /** correo o sms, por dónde llegó el código */
    val canal: String? = null,
    /** Correo o teléfono enmascarado, para mostrar a dónde llegó el código */
    val destino: String? = null,
    val mensaje: String? = null
)

@Serializable
data class VerificarRegistroDto(val registroId: String, val codigo: String)

@Serializable
data class ReenviarRegistroDto(val registroId: String)

@Serializable
data class SesionDispositivoDto(
    val id: String,
    val dispositivo: String,
    val iniciada: String,
    val ultimaActividad: String,
    val vence: String,
    val segundoFactor: Boolean = false,
    val actual: Boolean = false
)

@Serializable
data class Estado2faDto(
    val activo: Boolean,
    val activadoEn: String? = null,
    val codigosRespaldoRestantes: Int = 0,
    val obligatorio: Boolean = true
)

@Serializable
data class Iniciar2faDto(val password: String)

@Serializable
data class Inicio2faDto(val secreto: String, val uri: String)

@Serializable
data class Codigo2faDto(val codigo: String)

@Serializable
data class CodigosRespaldoDto(val codigosRespaldo: List<String>)

@Serializable
data class SoloTokensDto(val tokens: TokensDto)

// ---------------- Perfil ----------------

@Serializable
data class PreferenciasDto(
    val quiereDonar: Boolean = false,
    val quiereParticipar: Boolean = false,
    val quiereProponer: Boolean = false,
    val recibirRecordatorios: Boolean = true,
    val recibirSugerencias: Boolean = true
)

@Serializable
data class NivelDto(val nombre: String, val descripcion: String? = null)

@Serializable
data class InsigniaDto(val id: String, val nombre: String, val descripcion: String, val imagenClave: String? = null)

@Serializable
data class PerfilDto(
    val id: String,
    val nombre: String,
    val username: String,
    val correo: String? = null,
    val telefono: String? = null,
    val rol: String,
    val fechaNacimiento: String? = null,
    val fotoClave: String? = null,
    val ciudad: CiudadDto? = null,
    val preferencias: PreferenciasDto = PreferenciasDto(),
    val categorias: List<String> = emptyList(),
    val participaciones: Int = 0,
    val nivel: NivelDto? = null,
    val insignias: List<InsigniaDto> = emptyList(),
    /** true cuando hay un aviso de privacidad nuevo que la persona aún no acepta */
    val avisoPendiente: Boolean = false
)

/** PATCH /v1/yo. Los campos null no se envían (explicitNulls = false). */
@Serializable
data class EditarPerfilDto(
    val nombre: String? = null,
    val fotoClave: String? = null,
    val ciudadId: String? = null
)

// ---------------- Privacidad (RF-15) ----------------

@Serializable
data class AvisoVigenteDto(val id: String, val version: String, val contenido: String, val publicadoEn: String)

@Serializable
data class SolicitudEliminacionDto(val estado: String, val solicitadaEn: String)

@Serializable
data class PrivacidadDto(
    val aviso: AvisoVigenteDto? = null,
    val aceptado: Boolean = false,
    val aceptadoEn: String? = null,
    val solicitudEliminacion: SolicitudEliminacionDto? = null
)

@Serializable
data class AceptarAvisoDto(val avisoId: String)

@Serializable
data class ConfirmarPasswordDto(val password: String)

@Serializable
data class MensajeDto(val mensaje: String? = null)

@Serializable
data class GuardarPreferenciasDto(
    val quiereDonar: Boolean,
    val quiereParticipar: Boolean,
    val quiereProponer: Boolean,
    val recibirRecordatorios: Boolean,
    val recibirSugerencias: Boolean,
    val categorias: List<String>
)

@Serializable
data class ActividadResumenDto(
    val id: String,
    val titulo: String,
    val inicio: String,
    val fin: String,
    val direccion: String,
    val estado: String,
    val zonaHoraria: String,
    val asociacion: String,
    val categoriaClave: String,
    val tipo: String = "voluntariado"
)

@Serializable
data class MiInscripcionDto(
    val id: String,
    val estado: String,
    val cantidadPersonas: Int,
    val titularAsiste: Boolean,
    val miembros: List<String> = emptyList(),
    val asistio: Boolean = false,
    val estadoTestimonio: String? = null,
    val valoracion: Int? = null,
    val actividad: ActividadResumenDto
)

// ---------------- Comunidad ----------------

@Serializable
data class MiembroDto(
    val id: String,
    val nombre: String,
    val relacion: String,
    val fechaNacimiento: String,
    val correo: String? = null,
    val telefono: String? = null
)

@Serializable
data class NuevoMiembroDto(
    val nombre: String,
    val relacion: String,
    val fechaNacimiento: String,
    val correo: String? = null,
    val telefono: String? = null
)

@Serializable
data class ResultadoEliminarDto(val resultado: String? = null)

// ---------------- Asociaciones y actividades ----------------

@Serializable
data class ActividadDto(
    val id: String,
    val titulo: String,
    val descripcionCorta: String,
    val descripcion: String,
    val recomendaciones: String? = null,
    val inicio: String,
    val fin: String,
    val zonaHoraria: String,
    val direccion: String,
    val latitud: Double? = null,
    val longitud: Double? = null,
    val aptaNinos: Boolean,
    val edadMinima: Int? = null,
    val cupoTotal: Int,
    val cuposOcupados: Int,
    val cuposDisponibles: Int,
    val estado: String,
    /** voluntariado (Participar) o visita (Visiteo) */
    val tipo: String = "voluntariado",
    val fotoClave: String? = null,
    val categoria: CategoriaDto,
    val asociacion: RefDto,
    val ciudad: RefDto,
    val esFavorita: Boolean = false,
    val miInscripcion: InscripcionActualDto? = null
)

@Serializable
data class InscripcionActualDto(
    val id: String,
    val estado: String,
    val cantidadPersonas: Int,
    val titularAsiste: Boolean,
    val miembros: List<String> = emptyList()
)

@Serializable
data class AsociacionDto(
    val id: String,
    val nombre: String,
    val descripcion: String,
    val mision: String? = null,
    val necesidades: String? = null,
    val familiasBeneficiadas: Int = 0,
    val direccion: String,
    val latitud: Double? = null,
    val longitud: Double? = null,
    val telefono: String? = null,
    val whatsapp: String? = null,
    val correo: String? = null,
    val sitioWeb: String? = null,
    val fotoClave: String? = null,
    val verificada: Boolean = false,
    val categoria: CategoriaDto,
    val ciudad: RefDto,
    val esFavorita: Boolean = false,
    val notificacionesActivas: Boolean = false,
    val meGusta: Int = 0,
    val noMeGusta: Int = 0,
    val proximasActividades: List<ActividadDto> = emptyList()
)

@Serializable
data class FavoritoDto(val notificacionesActivas: Boolean)

@Serializable
data class InscribirDto(val titularAsiste: Boolean, val miembros: List<String>)

@Serializable
data class ResultadoInscripcionDto(
    val inscripcionId: String? = null,
    val cupoTotal: Int = 0,
    val cuposOcupados: Int = 0,
    val cuposDisponibles: Int = 0
)

// ---------------- Campañas ----------------

@Serializable
data class ArticuloDto(
    val id: String,
    val nombre: String,
    val descripcion: String? = null,
    val unidad: String,
    val prioridad: String,
    val cantidadMeta: Double,
    val cantidadRecibida: Double,
    val progreso: Double
)

@Serializable
data class PuntoEntregaDto(
    val id: String,
    val nombre: String,
    val direccion: String,
    val horarios: String,
    val indicaciones: String? = null,
    val latitud: Double? = null,
    val longitud: Double? = null
)

@Serializable
data class CampanaDto(
    val id: String,
    val titulo: String,
    val descripcion: String,
    val inicio: String,
    val fin: String,
    val urgente: Boolean = false,
    val estado: String,
    val fotoClave: String? = null,
    val organizacion: String? = null,
    val proyectoId: String? = null,
    val ciudad: RefDto,
    val progreso: Double = 0.0,
    val totalArticulos: Int = 0,
    val articulos: List<ArticuloDto> = emptyList(),
    val puntosEntrega: List<PuntoEntregaDto> = emptyList()
)

// ---------------- Proyectos (RF-09) ----------------

@Serializable
data class ProyectoDto(
    val id: String,
    val titulo: String,
    val descripcion: String,
    val fechaInicio: String,
    val fechaFin: String? = null,
    val fotoClave: String? = null,
    val estado: String,
    val asociacion: RefDto? = null,
    val ciudad: RefDto,
    val campanasActivas: Int = 0,
    val progreso: Double = 0.0,
    val campanas: List<CampanaDto> = emptyList()
)

// ---------------- Tiempo real ----------------

/** Dato de cada evento "cupos" del flujo /v1/cupos/stream. */
@Serializable
data class CuposEventoDto(
    val actividadId: String,
    val cupoTotal: Int,
    val cuposOcupados: Int,
    val cuposDisponibles: Int
)

@Serializable
data class NuevaEntregaDto(
    val articuloId: String,
    val puntoEntregaId: String,
    val cantidad: Double,
    val comentario: String? = null
)

@Serializable
data class EntregaCreadaDto(val id: String, val estado: String)

// ---------------- Propuestas, reportes, notificaciones ----------------

@Serializable
data class HistorialDto(val estado: String, val comentario: String? = null, val cambiadoEn: String)

@Serializable
data class PropuestaDto(
    val id: String,
    val tipo: String,
    val titulo: String,
    val descripcion: String,
    val ubicacion: String? = null,
    val estado: String,
    val comentarioAdmin: String? = null,
    val creadoEn: String,
    val historial: List<HistorialDto> = emptyList()
)

@Serializable
data class NuevaPropuestaDto(
    val tipo: String,
    val titulo: String,
    val descripcion: String,
    val ciudadId: String? = null,
    val ubicacion: String? = null,
    val contactoCorreo: String? = null,
    val contactoTelefono: String? = null
)

@Serializable
data class NuevoReporteDto(
    val tipo: String,
    val nivelSatisfaccion: Int? = null,
    val descripcion: String,
    val actividadId: String? = null,
    val asociacionId: String? = null,
    val campanaId: String? = null,
    val fotoClave: String? = null
)

@Serializable
data class CreadoDto(val id: String)

@Serializable
data class NotificacionDto(
    val id: String,
    val tipo: String,
    val titulo: String,
    val mensaje: String,
    val leida: Boolean,
    val creadoEn: String,
    val actividadId: String? = null,
    val asociacionId: String? = null,
    val propuestaId: String? = null,
    val campanaId: String? = null
)

@Serializable
data class ArchivoDto(val clave: String, val tipo: String, val tamano: Int)

@Serializable
data class NuevoTestimonioDto(val texto: String, val fotoClave: String)

// ---------------- Fase 2 ----------------

@Serializable
data class FavoritosDto(
    val asociaciones: List<AsociacionDto> = emptyList(),
    val actividades: List<ActividadDto> = emptyList()
)

@Serializable
data class OpcionEncuestaDto(val id: String, val texto: String)

@Serializable
data class PreguntaEncuestaDto(
    val id: String,
    val texto: String,
    val tipo: String,
    val obligatoria: Boolean = true,
    val orden: Int = 0,
    val opciones: List<OpcionEncuestaDto> = emptyList(),
    val respondida: Boolean = false
)

@Serializable
data class EncuestaDto(
    val id: String,
    val titulo: String,
    val momento: String,
    val preguntas: List<PreguntaEncuestaDto> = emptyList()
)

@Serializable
data class RespuestaEncuestaDto(val preguntaId: String, val opcionId: String? = null, val texto: String? = null)

@Serializable
data class RespuestasEncuestaDto(val respuestas: List<RespuestaEncuestaDto>)

@Serializable
data class ValoracionDto(val valor: Int)

@Serializable
data class ActividadDeTestimonioDto(val id: String, val titulo: String, val tipo: String = "voluntariado")

@Serializable
data class TestimonioDto(
    val id: String,
    val texto: String,
    val fotoClave: String? = null,
    val creadoEn: String,
    val username: String,
    val actividad: ActividadDeTestimonioDto? = null,
    val asociacion: String? = null
)

@Serializable
data class DispositivoDto(val tokenFcm: String)
