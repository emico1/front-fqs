package mx.familiasquesuman.app.ui.nav

import android.net.Uri

/** Todas las rutas de navegación de la app en un solo lugar. */
object Routes {
    const val BIENVENIDA = "bienvenida"
    const val LOGIN = "login"
    const val REGISTRO = "registro"
    const val ONBOARDING_PREFERENCIAS = "onboarding_preferencias"
    const val PREFERENCIAS = "preferencias"

    const val INICIO = "inicio"
    const val EXPLORAR = "explorar"
    const val PARTICIPAR = "participar"
    const val PERFIL = "perfil"

    const val ASOCIACION_DETALLE = "asociacion/{asociacionId}"
    fun asociacionDetalle(id: String) = "asociacion/${Uri.encode(id)}"

    const val ACTIVIDAD_DETALLE = "actividad/{actividadId}"
    fun actividadDetalle(id: String) = "actividad/${Uri.encode(id)}"

    const val MI_COMUNIDAD = "mi_comunidad"
    const val PROPONER_INICIATIVA = "proponer_iniciativa?tipo={tipo}"
    /** tipo es el valor de la API de TipoPropuesta, por ejemplo asociacion desde Explorar. */
    fun proponerIniciativa(tipo: String? = null) =
        if (tipo == null) "proponer_iniciativa" else "proponer_iniciativa?tipo=${Uri.encode(tipo)}"
    const val PROPUESTA_ENVIADA = "propuesta_enviada"
    const val MIS_PROPUESTAS = "mis_propuestas"

    const val ESTADO_PROPUESTA = "estado_propuesta/{propuestaId}"
    fun estadoPropuesta(id: String) = "estado_propuesta/${Uri.encode(id)}"

    const val CAMPANA_DONACION = "campana/{campanaId}"
    fun campanaDonacion(id: String) = "campana/${Uri.encode(id)}"

    const val REPORTAR_EXPERIENCIA = "reportar_experiencia?asociacionId={asociacionId}&actividadId={actividadId}"
    fun reportarExperiencia(asociacionId: String? = null, actividadId: String? = null): String {
        val partes = listOfNotNull(
            asociacionId?.let { "asociacionId=${Uri.encode(it)}" },
            actividadId?.let { "actividadId=${Uri.encode(it)}" }
        )
        return if (partes.isEmpty()) "reportar_experiencia" else "reportar_experiencia?" + partes.joinToString("&")
    }

    const val NOTIFICACIONES = "notificaciones"

    const val PROYECTO_DETALLE = "proyecto/{proyectoId}"
    fun proyectoDetalle(id: String) = "proyecto/${Uri.encode(id)}"

    const val HISTORIAL = "historial"
    const val PRIVACIDAD = "privacidad"
    /** Verificación en dos pasos de las cuentas de administración */
    const val SEGURIDAD_ADMIN = "seguridad_admin"
    /** Último paso del registro, el código que llegó por correo o SMS */
    const val VERIFICAR_CONTACTO = "verificar_contacto"
    /** Dispositivos con sesión abierta */
    const val SESIONES = "sesiones"

    const val ENCUESTA = "encuesta/{actividadId}/{inscripcionId}"
    fun encuesta(actividadId: String, inscripcionId: String) = "encuesta/${Uri.encode(actividadId)}/${Uri.encode(inscripcionId)}"

    const val FAVORITOS = "favoritos"
    const val HISTORIAS = "historias"

    const val TODOS_VOLUNTARIADOS = "todos_voluntariados"
    const val TODAS_CAMPANAS = "todas_campanas"

    /** Rutas a las que se puede entrar sin sesión. */
    val publicas = setOf(BIENVENIDA, LOGIN, REGISTRO, ONBOARDING_PREFERENCIAS)
}

/** Pestañas de la barra de navegación inferior. */
enum class TabDestino(val ruta: String, val etiqueta: String) {
    INICIO(Routes.INICIO, "Inicio"),
    EXPLORAR(Routes.EXPLORAR, "Explorar"),
    PARTICIPAR(Routes.PARTICIPAR, "Participar"),
    PERFIL(Routes.PERFIL, "Perfil")
}
