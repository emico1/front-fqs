package mx.familiasquesuman.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Poll
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import mx.familiasquesuman.app.data.ActividadItem
import mx.familiasquesuman.app.data.MiembroComunidad
import mx.familiasquesuman.app.data.TipoActividad
import mx.familiasquesuman.app.data.remote.mensajeDeError
import mx.familiasquesuman.app.ui.components.TituloSeccion
import mx.familiasquesuman.app.ui.components.BackTopBar
import mx.familiasquesuman.app.ui.components.ContenidoCarga
import mx.familiasquesuman.app.ui.components.GhostBorderCard
import mx.familiasquesuman.app.ui.components.PrimaryButton
import mx.familiasquesuman.app.ui.components.SecondaryButton
import mx.familiasquesuman.app.ui.components.StatusTag
import mx.familiasquesuman.app.ui.components.UbicacionRow
import mx.familiasquesuman.app.viewmodel.AlTerminar
import mx.familiasquesuman.app.viewmodel.Carga

/**
 * Combina actividad_e_inscripcion_azul con la selección de "Miembros de mi comunidad".
 * Cupos, edad mínima e inscripción se validan en el backend, aquí solo se adelanta
 * la información para que la persona no intente algo que va a ser rechazado.
 */
@Composable
fun ActividadDetalleScreen(
    estado: Carga<ActividadItem>,
    miembrosComunidad: List<MiembroComunidad>,
    onReintentar: () -> Unit,
    onBack: () -> Unit,
    onIrAMiComunidad: () -> Unit,
    onInscribir: (titularAsiste: Boolean, miembros: Set<String>, alTerminar: AlTerminar<Unit>) -> Unit,
    onCancelarInscripcion: (alTerminar: AlTerminar<Unit>) -> Unit,
    onInscripcionExitosa: () -> Unit,
    onFavorita: (favorita: Boolean, alTerminar: AlTerminar<Unit>) -> Unit,
    onVerEncuestas: (inscripcionId: String) -> Unit,
    esAdmin: Boolean = false
) {
    Column(modifier = Modifier.fillMaxSize()) {
        BackTopBar(titulo = "Detalle de Actividad", onBack = onBack)
        ContenidoCarga(estado, onReintentar) { actividad ->
            DetalleActividad(
                actividad = actividad,
                miembrosComunidad = miembrosComunidad,
                onIrAMiComunidad = onIrAMiComunidad,
                onInscribir = onInscribir,
                onCancelarInscripcion = onCancelarInscripcion,
                onInscripcionExitosa = onInscripcionExitosa,
                onFavorita = onFavorita,
                onVerEncuestas = onVerEncuestas,
                esAdmin = esAdmin
            )
        }
    }
}

/** Edad que tendrá la persona el día de la actividad, o null si no se puede calcular. */
private fun edadAlEvento(fechaNacimiento: String, inicioMillis: Long): Int? = runCatching {
    val dia = Instant.ofEpochMilli(inicioMillis).atZone(ZoneId.of("America/Monterrey")).toLocalDate()
    ChronoUnit.YEARS.between(LocalDate.parse(fechaNacimiento), dia).toInt()
}.getOrNull()

@Composable
private fun DetalleActividad(
    actividad: ActividadItem,
    miembrosComunidad: List<MiembroComunidad>,
    onIrAMiComunidad: () -> Unit,
    onInscribir: (Boolean, Set<String>, AlTerminar<Unit>) -> Unit,
    onCancelarInscripcion: (AlTerminar<Unit>) -> Unit,
    onInscripcionExitosa: () -> Unit,
    onFavorita: (Boolean, AlTerminar<Unit>) -> Unit,
    onVerEncuestas: (String) -> Unit,
    esAdmin: Boolean
) {
    val context = LocalContext.current
    val inscripcion = actividad.inscripcion?.takeIf { it.confirmada }
    var titularAsiste by remember(actividad.id) { mutableStateOf(true) }
    var acompanantes by remember(actividad.id) { mutableStateOf(setOf<String>()) }
    var mostrarExito by remember { mutableStateOf(false) }
    var confirmarCancelacion by remember { mutableStateOf(false) }
    var enviando by remember { mutableStateOf(false) }
    val yaEmpezo = actividad.inicioMillis <= System.currentTimeMillis()
    val personas = acompanantes.size + if (titularAsiste) 1 else 0

    // Los cupos llegan en tiempo real (RF-05). Si bajan mientras la familia ve la
    // pantalla, se le avisa de forma clara antes de que intente inscribirse.
    var cuposAnteriores by remember(actividad.id) { mutableStateOf(actividad.cuposDisponibles) }
    var avisoCupos by remember(actividad.id) { mutableStateOf<String?>(null) }
    LaunchedEffect(actividad.cuposDisponibles) {
        val antes = cuposAnteriores
        val ahora = actividad.cuposDisponibles
        if (inscripcion == null && !enviando && ahora < antes) {
            avisoCupos = when (ahora) {
                0 -> "Otra familia acaba de tomar el último lugar."
                1 -> "Otra familia acaba de inscribirse, queda 1 lugar."
                else -> "Otra familia acaba de inscribirse, quedan $ahora lugares."
            }
        } else if (ahora > antes) {
            avisoCupos = null
        }
        cuposAnteriores = ahora
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusTag(texto = actividad.etiquetaAudiencia, containerColor = MaterialTheme.colorScheme.secondary, contentColor = MaterialTheme.colorScheme.secondary)
            if (actividad.tipo == TipoActividad.VISITA) {
                StatusTag(texto = "Visita", containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.primary)
            }
        }
        Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(top = 8.dp)) {
            Text(actividad.titulo, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            // Guardar en favoritos (RF20)
            IconButton(onClick = {
                onFavorita(!actividad.esFavorita) { r ->
                    r.onFailure { Toast.makeText(context, mensajeDeError(it), Toast.LENGTH_LONG).show() }
                }
            }) {
                Icon(
                    if (actividad.esFavorita) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = if (actividad.esFavorita) "Quitar de favoritos" else "Guardar en favoritos",
                    tint = if (actividad.esFavorita) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(actividad.asociacion, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        UbicacionRow(texto = actividad.ubicacion)
        Row(
            modifier = Modifier
                .padding(top = 8.dp)
                .clickable(onClickLabel = "Abrir indicaciones en el mapa") { abrirComoLlegar(context, actividad) },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Directions, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text("Cómo llegar", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 6.dp))
        }

        Row(modifier = Modifier.padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            InfoCard(icono = Icons.Filled.CalendarMonth, titulo = "Fecha y Hora", valor = actividad.fechaHoraCompleta, modifier = Modifier.weight(1f))
            InfoCard(icono = Icons.Filled.Groups, titulo = "Cupos Disponibles", valor = "${actividad.cuposDisponibles} de ${actividad.cuposTotales}", modifier = Modifier.weight(1f))
        }

        TituloSeccion("Acerca de la actividad", modifier = Modifier.padding(top = 28.dp, bottom = 8.dp))
        Text(actividad.descripcionLarga, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (actividad.recomendaciones.isNotBlank()) {
            Text(actividad.recomendaciones, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
        }

        TituloSeccion("Inscripción", modifier = Modifier.padding(top = 28.dp, bottom = 8.dp))

        avisoCupos?.let { aviso ->
            Text(
                aviso,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.10f), MaterialTheme.shapes.medium)
                    .padding(12.dp)
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(12.dp))
        }

        if (inscripcion != null) {
            GhostBorderCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Ya estás inscrito", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 8.dp))
                }
                val nombres = buildList {
                    if (inscripcion.titularAsiste) add("Tú")
                    miembrosComunidad.filter { it.id in inscripcion.miembros }.forEach { add(it.nombre) }
                }
                Text(
                    "Asisten ${nombres.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            SecondaryButton(
                text = "Agregar al Calendario",
                onClick = { agregarAlCalendario(context, actividad) },
                modifier = Modifier.padding(top = 16.dp)
            )
            // Encuesta previa y posterior (RF-13)
            Row(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .clickable(onClickLabel = "Abrir encuestas") { onVerEncuestas(inscripcion.id) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Poll, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Encuestas de la actividad", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 6.dp))
            }
            if (!yaEmpezo) {
                SecondaryButton(
                    text = "Cancelar inscripción",
                    onClick = { confirmarCancelacion = true },
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        } else if (esAdmin) {
            Text(
                "Estás usando una cuenta de administración, que sirve para gestionar actividades. Para inscribirte inicia sesión con una cuenta de familia.",
                style = MaterialTheme.typography.bodyMedium
            )
        } else if (yaEmpezo) {
            Text("Esta actividad ya comenzó, las inscripciones están cerradas.", style = MaterialTheme.typography.bodyMedium)
        } else if (actividad.cuposDisponibles <= 0) {
            Text(
                "Lo sentimos, esta actividad ya no tiene lugares disponibles. Si alguna familia cancela, el lugar aparecerá aquí al momento.",
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            val minima = actividad.edadMinima ?: 0
            GhostBorderCard(onClick = { titularAsiste = !titularAsiste }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = titularAsiste, onCheckedChange = { titularAsiste = it })
                    Box(modifier = Modifier.size(36.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape), contentAlignment = Alignment.Center) {
                        Text("Tú", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    Text("Titular (Tú)", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 12.dp))
                }
            }

            Text("Acompañantes", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
            Text("Miembros de mi comunidad", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                miembrosComunidad.forEach { miembro ->
                    val edad = edadAlEvento(miembro.fechaNacimiento, actividad.inicioMillis)
                    val permitido = edad == null || edad >= minima
                    val seleccionado = acompanantes.contains(miembro.id)
                    val alternar = {
                        if (permitido) acompanantes = if (seleccionado) acompanantes - miembro.id else acompanantes + miembro.id
                    }
                    GhostBorderCard(onClick = alternar) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = seleccionado, enabled = permitido, onCheckedChange = { alternar() })
                            Column(modifier = Modifier.padding(start = 4.dp)) {
                                Text(miembro.nombre, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    if (permitido) miembro.relacion else "${miembro.relacion} • requiere $minima años o más",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (permitido) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.PersonAdd, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    "Gestionar mi comunidad",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .clickable(onClick = onIrAMiComunidad)
                )
            }

            if (personas > actividad.cuposDisponibles) {
                Text(
                    "Seleccionaste $personas personas y solo quedan ${actividad.cuposDisponibles} cupos.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            PrimaryButton(
                text = if (enviando) "Registrando..." else "Confirmar Registro",
                enabled = !enviando && personas > 0 && personas <= actividad.cuposDisponibles,
                onClick = {
                    enviando = true
                    onInscribir(titularAsiste, acompanantes) { r ->
                        enviando = false
                        r.onSuccess { mostrarExito = true }
                            .onFailure { Toast.makeText(context, mensajeDeError(it), Toast.LENGTH_LONG).show() }
                    }
                },
                modifier = Modifier.padding(top = 24.dp)
            )
            Text(
                "Al confirmar, aceptas nuestras condiciones de participación.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
            )
        }
    }

    if (confirmarCancelacion) {
        AlertDialog(
            onDismissRequest = { confirmarCancelacion = false },
            title = { Text("Cancelar inscripción") },
            text = { Text("Se liberarán tus lugares para otras familias. ¿Quieres continuar?") },
            confirmButton = {
                TextButton(
                    enabled = !enviando,
                    onClick = {
                        enviando = true
                        onCancelarInscripcion { r ->
                            enviando = false
                            confirmarCancelacion = false
                            r.onSuccess { Toast.makeText(context, "Inscripción cancelada.", Toast.LENGTH_SHORT).show() }
                                .onFailure { Toast.makeText(context, mensajeDeError(it), Toast.LENGTH_LONG).show() }
                        }
                    }
                ) { Text("Sí, cancelar") }
            },
            dismissButton = { TextButton(onClick = { confirmarCancelacion = false }) { Text("No") } }
        )
    }

    if (mostrarExito) {
        Dialog(onDismissRequest = { }) {
            GhostBorderCard {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                Text("¡Inscripción Exitosa!", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 12.dp))
                Text(
                    "Te registraste en ${actividad.titulo}. Encontrarás los detalles en tu perfil.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
                )
                SecondaryButton(
                    text = "Agregar al Calendario",
                    onClick = { agregarAlCalendario(context, actividad) }
                )
                PrimaryButton(text = "Cerrar", onClick = { mostrarExito = false; onInscripcionExitosa() }, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

/**
 * Abre la app de mapas del teléfono con el destino. Usa las coordenadas si existen y
 * si no la dirección escrita. No pide permiso de ubicación, el mapa calcula la ruta.
 */
private fun abrirComoLlegar(context: android.content.Context, actividad: ActividadItem) {
    val destino = if (actividad.latitud != null && actividad.longitud != null) {
        "geo:${actividad.latitud},${actividad.longitud}?q=${actividad.latitud},${actividad.longitud}(${Uri.encode(actividad.titulo)})"
    } else {
        "geo:0,0?q=${Uri.encode(actividad.ubicacion)}"
    }
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(destino))) }
        .onFailure { Toast.makeText(context, "No se encontró una app de mapas.", Toast.LENGTH_SHORT).show() }
}

private fun agregarAlCalendario(context: android.content.Context, actividad: ActividadItem) {
    val intent = Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI).apply {
        putExtra(CalendarContract.Events.TITLE, actividad.titulo)
        putExtra(CalendarContract.Events.EVENT_LOCATION, actividad.ubicacion)
        putExtra(CalendarContract.Events.DESCRIPTION, actividad.descripcionCorta)
        putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, actividad.inicioMillis)
        putExtra(CalendarContract.EXTRA_EVENT_END_TIME, actividad.finMillis)
    }
    runCatching { context.startActivity(intent) }
        .onFailure { Toast.makeText(context, "No se encontró una app de calendario.", Toast.LENGTH_SHORT).show() }
}

@Composable
private fun InfoCard(icono: androidx.compose.ui.graphics.vector.ImageVector, titulo: String, valor: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.medium)
            .padding(14.dp)
    ) {
        Icon(icono, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(titulo, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
        Text(valor, style = MaterialTheme.typography.titleMedium)
    }
}
