package mx.familiasquesuman.app.data

import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import mx.familiasquesuman.app.data.remote.ActividadDto
import mx.familiasquesuman.app.data.remote.ArticuloDto
import mx.familiasquesuman.app.data.remote.AsociacionDto
import mx.familiasquesuman.app.data.remote.CampanaDto
import mx.familiasquesuman.app.data.remote.CatalogosDto
import mx.familiasquesuman.app.data.remote.EncuestaDto
import mx.familiasquesuman.app.data.remote.FavoritosDto
import mx.familiasquesuman.app.data.remote.TestimonioDto
import mx.familiasquesuman.app.data.remote.MiInscripcionDto
import mx.familiasquesuman.app.data.remote.MiembroDto
import mx.familiasquesuman.app.data.remote.NotificacionDto
import mx.familiasquesuman.app.data.remote.PerfilDto
import mx.familiasquesuman.app.data.remote.PrivacidadDto
import mx.familiasquesuman.app.data.remote.ProyectoDto
import mx.familiasquesuman.app.data.remote.PropuestaDto

/**
 * Conversión de lo que llega de la API a lo que muestran las pantallas.
 * Las fechas se presentan en la zona horaria de la ciudad de la actividad, no la del
 * teléfono, para que una actividad en Monterrey siempre diga la hora de Monterrey.
 */
private val es = Locale.forLanguageTag("es-MX")
private val fmtDia = DateTimeFormatter.ofPattern("d", es)
private val fmtMes = DateTimeFormatter.ofPattern("MMM", es)
private val fmtDiaSemana = DateTimeFormatter.ofPattern("EEE, d MMM", es)
private val fmtHora = DateTimeFormatter.ofPattern("HH:mm", es)
private val fmtFechaLarga = DateTimeFormatter.ofPattern("d MMM yyyy", es)
private val fmtMesAnio = DateTimeFormatter.ofPattern("MMMM yyyy", es)

private fun zona(id: String?): ZoneId = runCatching { ZoneId.of(id ?: "America/Monterrey") }.getOrDefault(ZoneId.of("America/Monterrey"))

/**
 * Acepta fechas que terminan en Z (las de primer nivel) y fechas con desfase horario
 * (las que vienen dentro de objetos JSON armados por PostgreSQL). Instant.parse en
 * Android 8 solo acepta las primeras.
 */
fun aInstante(iso: String): Instant = OffsetDateTime.parse(iso).toInstant()

private fun capital(s: String) = s.replace(".", "").replaceFirstChar { it.titlecase(es) }

fun fechaCorta(iso: String, zonaId: String? = null): String =
    runCatching { aInstante(iso).atZone(zona(zonaId)).format(fmtFechaLarga) }.getOrDefault("")

private fun audiencia(aptaNinos: Boolean, edadMinima: Int?): String = when {
    edadMinima != null && edadMinima > 0 -> "+$edadMinima años"
    aptaNinos -> "Apta para niños"
    else -> "Todo público"
}

fun ActividadDto.aUi(urlImagen: (String?) -> String?): ActividadItem {
    val z = zona(zonaHoraria)
    val ini = aInstante(inicio).atZone(z)
    val fn = aInstante(fin).atZone(z)
    return ActividadItem(
        id = id,
        titulo = titulo,
        asociacion = asociacion.nombre,
        asociacionId = asociacion.id,
        categoria = Categoria.desdeClave(categoria.clave),
        dia = ini.format(fmtDia),
        mes = capital(ini.format(fmtMes)),
        fechaHoraCompleta = "${capital(ini.format(fmtDiaSemana))} • ${ini.format(fmtHora)} - ${fn.format(fmtHora)}",
        ubicacion = direccion,
        etiquetaAudiencia = audiencia(aptaNinos, edadMinima),
        descripcionCorta = descripcionCorta,
        descripcionLarga = descripcion,
        recomendaciones = recomendaciones.orEmpty(),
        cuposDisponibles = cuposDisponibles,
        cuposTotales = cupoTotal,
        edadMinima = edadMinima,
        inicioMillis = ini.toInstant().toEpochMilli(),
        finMillis = fn.toInstant().toEpochMilli(),
        fotoUrl = urlImagen(fotoClave),
        tipo = TipoActividad.desdeApi(tipo),
        latitud = latitud,
        longitud = longitud,
        esFavorita = esFavorita,
        inscripcion = miInscripcion?.let {
            InscripcionActual(
                id = it.id,
                confirmada = it.estado == "confirmada",
                titularAsiste = it.titularAsiste,
                miembros = it.miembros.toSet()
            )
        }
    )
}

fun AsociacionDto.aUi(urlImagen: (String?) -> String?) = Asociacion(
    id = id,
    nombre = nombre,
    categoria = Categoria.desdeClave(categoria.clave),
    descripcion = descripcion,
    ubicacion = direccion,
    verificada = verificada,
    familiasBeneficiadas = familiasBeneficiadas,
    mision = mision ?: descripcion,
    necesidades = necesidades.orEmpty(),
    fotoUrl = urlImagen(fotoClave),
    telefono = telefono,
    whatsapp = whatsapp ?: telefono,
    correo = correo,
    latitud = latitud,
    longitud = longitud,
    notificacionesActivas = notificacionesActivas,
    meGusta = meGusta,
    noMeGusta = noMeGusta,
    proximasActividades = proximasActividades.map { it.aUi(urlImagen) }
)

private fun numero(v: Double): String = if (v % 1.0 == 0.0) v.toLong().toString() else String.format(es, "%.1f", v)

private fun ArticuloDto.aUi() = ArticuloDonacion(
    id = id,
    nombre = nombre,
    descripcion = descripcion.orEmpty(),
    unidad = unidad,
    prioridad = when (prioridad) {
        "urgente" -> "Urgente"
        "alta" -> "Alta prioridad"
        "baja" -> "Baja prioridad"
        else -> "Prioridad media"
    },
    urgente = prioridad == "urgente",
    progresoTexto = "${numero(cantidadRecibida)}/${numero(cantidadMeta)} $unidad",
    progreso = progreso.toFloat().coerceIn(0f, 1f)
)

fun CampanaDto.aUi(): Campana {
    val pct = (progreso * 100).toInt().coerceIn(0, 100)
    val dias = runCatching {
        ChronoUnit.DAYS.between(Instant.now(), aInstante(fin)).toInt().takeIf { it >= 0 }
    }.getOrNull()
    val total = if (articulos.isNotEmpty()) articulos.size else totalArticulos
    return Campana(
        id = id,
        titulo = titulo,
        organizacion = organizacion.orEmpty(),
        descripcion = descripcion,
        recaudadoTexto = "Avance $pct%",
        metaTexto = if (total == 1) "1 artículo" else "$total artículos",
        progreso = progreso.toFloat().coerceIn(0f, 1f),
        urgente = urgente,
        diasRestantes = dias,
        articulos = articulos.map { it.aUi() },
        puntosEntrega = puntosEntrega.map { PuntoEntrega(it.id, it.nombre, it.direccion, it.horarios, it.indicaciones) }
    )
}

fun MiembroDto.aUi() = MiembroComunidad(
    id = id,
    nombre = nombre,
    relacion = relacion,
    iniciales = nombre.trim().split(Regex("\\s+")).take(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString(""),
    fechaNacimiento = fechaNacimiento
)

fun PropuestaDto.aUi() = Propuesta(
    id = id,
    tipo = TipoPropuesta.desdeApi(tipo).etiqueta,
    titulo = titulo,
    descripcion = descripcion,
    fecha = fechaCorta(creadoEn),
    estado = EstadoPropuesta.desdeApi(estado),
    comentarioAdmin = comentarioAdmin,
    historial = historial.map { PasoHistorial(EstadoPropuesta.desdeApi(it.estado), fechaCorta(it.cambiadoEn), it.comentario) }
)

fun MiInscripcionDto.aExperiencia(ahora: Instant = Instant.now()): Experiencia {
    val z = zona(actividad.zonaHoraria)
    val ini = runCatching { aInstante(actividad.inicio).atZone(z) }.getOrNull()
    val terminada = runCatching { aInstante(actividad.fin).isBefore(ahora) }.getOrDefault(false)
    val resultado = when {
        estado != "confirmada" -> ResultadoParticipacion.CANCELADA
        asistio -> ResultadoParticipacion.ASISTIO
        !terminada -> ResultadoParticipacion.PROXIMA
        else -> ResultadoParticipacion.SIN_ASISTENCIA
    }
    return Experiencia(
        inscripcionId = id,
        actividadId = actividad.id,
        titulo = "${actividad.titulo} con ${actividad.asociacion}",
        fecha = ini?.let { "${capital(it.format(fmtDiaSemana))} • ${it.format(fmtHora)}" }.orEmpty(),
        ubicacion = actividad.direccion,
        asistio = asistio,
        estadoTestimonio = estadoTestimonio,
        inicioMillis = ini?.toInstant()?.toEpochMilli() ?: 0L,
        mesAnio = ini?.let { capital(it.format(fmtMesAnio)) }.orEmpty(),
        resultado = resultado,
        tipo = TipoActividad.desdeApi(actividad.tipo),
        valoracion = valoracion
    )
}

fun ProyectoDto.aUi(urlImagen: (String?) -> String?): Proyecto {
    val inicio = runCatching { LocalDate.parse(fechaInicio).format(fmtFechaLarga) }.getOrDefault(fechaInicio)
    val fin = fechaFin?.let { f -> runCatching { LocalDate.parse(f).format(fmtFechaLarga) }.getOrDefault(f) }
    return Proyecto(
        id = id,
        titulo = titulo,
        descripcion = descripcion,
        organizacion = asociacion?.nombre,
        periodo = if (fin != null) "Del $inicio al $fin" else "Desde el $inicio",
        progreso = progreso.toFloat().coerceIn(0f, 1f),
        campanasActivas = campanasActivas,
        fotoUrl = urlImagen(fotoClave),
        campanas = campanas.map { it.aUi() }
    )
}

fun PrivacidadDto.aUi() = Privacidad(
    aviso = aviso?.let { AvisoPrivacidad(it.id, it.version, it.contenido) },
    publicadoEn = aviso?.let { fechaCorta(it.publicadoEn) },
    aceptado = aceptado,
    aceptadoEn = aceptadoEn?.let { fechaCorta(it) },
    solicitudEliminacion = solicitudEliminacion?.let { "Solicitud ${if (it.estado == "en_proceso") "en proceso" else "pendiente"} desde el ${fechaCorta(it.solicitadaEn)}" }
)

fun NotificacionDto.aUi() = Notificacion(
    id = id,
    titulo = titulo,
    cuerpo = mensaje,
    leida = leida,
    tipo = tipo,
    actividadId = actividadId,
    campanaId = campanaId,
    propuestaId = propuestaId,
    asociacionId = asociacionId
)

fun EncuestaDto.aUi() = Encuesta(
    id = id,
    titulo = titulo,
    esPrevia = momento == "antes",
    preguntas = preguntas.sortedBy { it.orden }.map { p ->
        PreguntaEncuesta(
            id = p.id,
            texto = p.texto,
            esOpcionUnica = p.tipo == "opcion_unica",
            obligatoria = p.obligatoria,
            opciones = p.opciones.map { OpcionEncuesta(it.id, it.texto) },
            respondida = p.respondida
        )
    }
)

fun FavoritosDto.aUi(urlImagen: (String?) -> String?) = Favoritos(
    asociaciones = asociaciones.map { it.aUi(urlImagen) },
    actividades = actividades.map { it.aUi(urlImagen) }
)

fun TestimonioDto.aUi(urlImagen: (String?) -> String?) = Testimonio(
    id = id,
    texto = texto,
    fotoUrl = urlImagen(fotoClave),
    autor = "@$username",
    actividad = actividad?.titulo,
    asociacion = asociacion,
    fecha = fechaCorta(creadoEn)
)

fun PerfilDto.aUi(urlImagen: (String?) -> String?) = Perfil(
    id = id,
    nombre = nombre,
    username = username,
    correo = correo,
    ciudadId = ciudad?.id,
    ciudadNombre = ciudad?.nombre,
    fotoUrl = urlImagen(fotoClave),
    nivel = nivel?.nombre,
    participaciones = participaciones,
    insignias = insignias.map { Insignia(it.nombre, it.descripcion) },
    quiereDonar = preferencias.quiereDonar,
    quiereParticipar = preferencias.quiereParticipar,
    quiereProponer = preferencias.quiereProponer,
    recibirRecordatorios = preferencias.recibirRecordatorios,
    recibirSugerencias = preferencias.recibirSugerencias,
    categorias = categorias.toSet(),
    avisoPendiente = avisoPendiente,
    esAdmin = rol == "admin"
)

fun CatalogosDto.aUi() = Catalogo(
    ciudades = ciudades.map { Ciudad(it.id, it.nombre) },
    categorias = categorias.mapNotNull { c -> Categoria.entries.firstOrNull { it.clave == c.clave } },
    aviso = avisoPrivacidad?.let { AvisoPrivacidad(it.id, it.version, it.contenido) }
)

/** Convierte "DD/MM/AAAA" en "AAAA-MM-DD", o null si la fecha no es válida o es futura. */
fun fechaIsoDesdeTexto(texto: String): String? {
    val partes = texto.trim().split("/")
    if (partes.size != 3) return null
    val (d, m, a) = partes
    return runCatching { LocalDate.of(a.toInt(), m.toInt(), d.toInt()) }
        .getOrNull()
        ?.takeIf { !it.isAfter(LocalDate.now()) && it.year > 1900 }
        ?.toString()
}

/** Edad cumplida a hoy a partir de "AAAA-MM-DD". */
fun edadDesdeIso(iso: String): Int? = runCatching {
    ChronoUnit.YEARS.between(LocalDate.parse(iso), LocalDate.now()).toInt()
}.getOrNull()
