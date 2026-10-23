package mx.familiasquesuman.app.data.local

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import mx.familiasquesuman.app.data.remote.NuevaPropuestaDto
import mx.familiasquesuman.app.data.remote.NuevoReporteDto

/**
 * Escrituras que se pueden hacer sin conexión y mandar después. Son las que no compiten por
 * un recurso limitado. Las inscripciones y las entregas a campañas no están aquí porque
 * dependen de los lugares y de las campañas activas en ese momento, y solo se hacen en línea.
 */
@Serializable
sealed class Envio {

    @Serializable
    @SerialName("asociacion_favorita")
    data class AsociacionFavorita(val asociacionId: String, val activa: Boolean) : Envio()

    @Serializable
    @SerialName("actividad_favorita")
    data class ActividadFavorita(val actividadId: String, val favorita: Boolean) : Envio()

    /** valor 1 me gusta, -1 no me gusta, null quita la calificación. */
    @Serializable
    @SerialName("valoracion")
    data class Valoracion(val inscripcionId: String, val valor: Int? = null) : Envio()

    @Serializable
    @SerialName("propuesta")
    data class Propuesta(val datos: NuevaPropuestaDto) : Envio()

    /** foto es el nombre del archivo cifrado en el teléfono, fotoClave la imagen ya subida. */
    @Serializable
    @SerialName("reporte")
    data class Reporte(val datos: NuevoReporteDto, val foto: String? = null) : Envio()

    @Serializable
    @SerialName("testimonio")
    data class Testimonio(val inscripcionId: String, val texto: String, val foto: String, val fotoClave: String? = null) : Envio()
}

/** Texto corto para avisar en pantalla qué está pendiente o qué se rechazó. */
fun Envio.tipo(): String = when (this) {
    is Envio.AsociacionFavorita -> "Aviso de asociación"
    is Envio.ActividadFavorita -> "Favorito"
    is Envio.Valoracion -> "Calificación"
    is Envio.Propuesta -> "Propuesta"
    is Envio.Reporte -> "Reporte"
    is Envio.Testimonio -> "Experiencia"
}

/** Archivo de foto que el envío tiene guardado en el teléfono. */
fun Envio.foto(): String? = when (this) {
    is Envio.Reporte -> foto
    is Envio.Testimonio -> foto
    else -> null
}
