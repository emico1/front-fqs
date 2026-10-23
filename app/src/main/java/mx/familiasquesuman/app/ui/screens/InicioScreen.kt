package mx.familiasquesuman.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import mx.familiasquesuman.app.data.ActividadItem
import mx.familiasquesuman.app.data.Asociacion
import mx.familiasquesuman.app.data.Campana
import mx.familiasquesuman.app.data.Ciudad
import mx.familiasquesuman.app.data.FiltroInicio
import mx.familiasquesuman.app.data.Proyecto
import mx.familiasquesuman.app.data.TipoActividad
import mx.familiasquesuman.app.ui.components.BannerAzul
import mx.familiasquesuman.app.ui.components.CampanaResumenCard
import mx.familiasquesuman.app.ui.components.TituloSeccion
import mx.familiasquesuman.app.ui.components.CiudadSelector
import mx.familiasquesuman.app.ui.components.FilterChip
import mx.familiasquesuman.app.viewmodel.AlTerminar
import mx.familiasquesuman.app.ui.components.AsociacionDestacadaCard
import mx.familiasquesuman.app.ui.components.BrandTopBar
import mx.familiasquesuman.app.ui.components.ContenidoCarga
import mx.familiasquesuman.app.ui.components.VacioBloque
import mx.familiasquesuman.app.viewmodel.Carga
import mx.familiasquesuman.app.ui.components.GhostBorderCard
import mx.familiasquesuman.app.ui.components.PrimaryButton
import mx.familiasquesuman.app.ui.components.TertiaryTextButton
import mx.familiasquesuman.app.ui.components.UbicacionRow

/**
 * Basada en inicio_familias_que_suman_azul. Los datos vienen del backend, siempre de la
 * ciudad del perfil (RF-02). Arriba está el filtro inicial que pidió el socio formador,
 * igual al de su sitio web, con Participar, Donar, Visiteo y Proyectos.
 */
@Composable
fun InicioScreen(
    esPantallaAncha: Boolean,
    asociaciones: Carga<List<Asociacion>>,
    actividades: Carga<List<ActividadItem>>,
    campanas: Carga<List<Campana>>,
    proyectos: Carga<List<Proyecto>>,
    filtro: FiltroInicio?,
    onFiltro: (FiltroInicio?) -> Unit,
    nombre: String? = null,
    ciudadId: String?,
    ciudadNombre: String?,
    ciudades: List<Ciudad>,
    onCambiarCiudad: (String, AlTerminar<Unit>) -> Unit,
    avisoPendiente: Boolean,
    onVerPrivacidad: () -> Unit,
    onReintentar: () -> Unit,
    onVerAsociacion: (String) -> Unit,
    onVerActividad: (String) -> Unit,
    onVerTodasAsociaciones: () -> Unit,
    onVerCampana: (String) -> Unit,
    onVerProyecto: (String) -> Unit,
    onVerHistorias: () -> Unit = {},
    onMenu: () -> Unit = {},
    onNotificaciones: () -> Unit = {},
    hayNotificacionesSinLeer: Boolean = false
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Column(modifier = Modifier.fillMaxSize()) {
        BrandTopBar(onMenu = onMenu, onNotificaciones = onNotificaciones, hayNotificacionesSinLeer = hayNotificacionesSinLeer)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            // Saludo con el estilo de Bienvenida
            BannerAzul(modifier = Modifier.padding(top = 8.dp)) {
                Text(
                    nombre?.trim()?.split(Regex("\\s+"))?.firstOrNull()?.let { "Hola, $it" } ?: "Hola",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    "¿Cómo quieres sumar hoy?",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.88f),
                    modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
                )
                CiudadSelector(
                    ciudadActualId = ciudadId,
                    ciudadActualNombre = ciudadNombre,
                    ciudades = ciudades,
                    onCambiar = onCambiarCiudad,
                    sobreAzul = true
                )
            }

            if (avisoPendiente) {
                GhostBorderCard(onClick = onVerPrivacidad, modifier = Modifier.padding(top = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                        Text(
                            "Actualizamos nuestro aviso de privacidad. Tócalo para revisarlo.",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(top = 16.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FiltroInicio.entries.forEach { f ->
                    FilterChip(
                        texto = f.etiqueta,
                        icono = when (f) {
                            FiltroInicio.PARTICIPAR -> Icons.Filled.Handshake
                            FiltroInicio.DONAR -> Icons.Filled.VolunteerActivism
                            FiltroInicio.VISITEO -> Icons.Filled.Storefront
                            FiltroInicio.PROYECTOS -> Icons.Filled.AccountTree
                        },
                        seleccionado = filtro == f,
                        onClick = { onFiltro(if (filtro == f) null else f) }
                    )
                }
            }

            if (filtro != null) {
                ContenidoFiltrado(filtro, actividades, campanas, proyectos, onReintentar, onVerActividad, onVerCampana, onVerProyecto)
            } else if (esPantallaAncha) {

                // Vista horizontal
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {

                    // Columna izquierda
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        SeccionHeader(
                            titulo = "Asociaciones Recomendadas",
                            accion = "Ver todas",
                            onAccion = onVerTodasAsociaciones
                        )

                        SeccionAsociaciones(asociaciones, onReintentar, onVerAsociacion)
                    }

                    // Columna derecha
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        SeccionHeader(
                            titulo = "Actividades Próximas",
                            accion = "Ver calendario",
                            onAccion = {
                                android.widget.Toast.makeText(
                                    context,
                                    "El calendario completo todavía no está disponible.",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        )

                        SeccionActividades(actividades, onReintentar, onVerActividad)

                        SeccionHeader(
                            titulo = "Campañas Activas",
                            accion = null,
                            onAccion = {},
                            modifier = Modifier.padding(top = 32.dp)
                        )

                        SeccionCampana(campanas, onReintentar, onVerCampana)
                    }
                }

            } else {

                // Vista vertical
                SeccionHeader(
                    titulo = "Asociaciones Recomendadas",
                    accion = "Ver todas",
                    onAccion = onVerTodasAsociaciones
                )

                SeccionAsociaciones(asociaciones, onReintentar, onVerAsociacion)

                SeccionHeader(
                    titulo = "Actividades Próximas",
                    accion = "Ver calendario",
                    onAccion = {
                        android.widget.Toast.makeText(
                            context,
                            "El calendario completo todavía no está disponible.",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.padding(top = 32.dp)
                )

                SeccionActividades(actividades, onReintentar, onVerActividad)

                SeccionHeader(
                    titulo = "Campañas Activas",
                    accion = null,
                    onAccion = {},
                    modifier = Modifier.padding(top = 32.dp)
                )

                SeccionCampana(campanas, onReintentar, onVerCampana)
            }

            if (filtro == null) {
                SeccionHeader(
                    titulo = "Historias de la comunidad",
                    accion = "Ver todas",
                    onAccion = onVerHistorias,
                    modifier = Modifier.padding(top = 32.dp)
                )
                Text(
                    "Lo que vivieron otras familias en sus actividades.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/** Lista completa de lo que corresponde al filtro elegido. */
@Composable
private fun ContenidoFiltrado(
    filtro: FiltroInicio,
    actividades: Carga<List<ActividadItem>>,
    campanas: Carga<List<Campana>>,
    proyectos: Carga<List<Proyecto>>,
    onReintentar: () -> Unit,
    onVerActividad: (String) -> Unit,
    onVerCampana: (String) -> Unit,
    onVerProyecto: (String) -> Unit
) {
    when (filtro) {
        FiltroInicio.PARTICIPAR, FiltroInicio.VISITEO -> {
            val tipo = if (filtro == FiltroInicio.VISITEO) TipoActividad.VISITA else TipoActividad.VOLUNTARIADO
            TituloSeccion(
                if (tipo == TipoActividad.VISITA) "Visitas para convivir en familia" else "Voluntariados abiertos",
                modifier = Modifier.padding(bottom = 12.dp)
            )
            ContenidoCarga(actividades, onReintentar) { todas ->
                val lista = todas.filter { it.tipo == tipo }
                if (lista.isEmpty()) {
                    VacioBloque(
                        if (tipo == TipoActividad.VISITA) "No hay visitas programadas en tu ciudad por ahora."
                        else "No hay voluntariados abiertos en tu ciudad por ahora."
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        lista.forEach { a -> ActividadFilaCard(actividad = a, onClick = { onVerActividad(a.id) }) }
                    }
                }
            }
        }
        FiltroInicio.DONAR -> {
            TituloSeccion("Campañas de donación", modifier = Modifier.padding(bottom = 12.dp))
            ContenidoCarga(campanas, onReintentar) { lista ->
                if (lista.isEmpty()) {
                    VacioBloque("No hay campañas activas en este momento.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        lista.forEach { c -> CampanaResumenCard(campana = c, onClick = { onVerCampana(c.id) }) }
                    }
                }
            }
        }
        FiltroInicio.PROYECTOS -> {
            TituloSeccion("Proyectos", modifier = Modifier.padding(bottom = 12.dp))
            ListaProyectos(proyectos, onReintentar, onVerProyecto)
        }
    }
}

@Composable
private fun SeccionAsociaciones(estado: Carga<List<Asociacion>>, onReintentar: () -> Unit, onVer: (String) -> Unit) {
    ContenidoCarga(estado, onReintentar) { lista ->
        if (lista.isEmpty()) {
            VacioBloque("Aún no hay asociaciones registradas en tu ciudad.")
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 4.dp)
            ) {
                items(lista.take(5), key = { it.id }) { asociacion ->
                    AsociacionDestacadaCard(asociacion = asociacion, onClick = { onVer(asociacion.id) })
                }
            }
        }
    }
}

@Composable
private fun SeccionActividades(estado: Carga<List<ActividadItem>>, onReintentar: () -> Unit, onVer: (String) -> Unit) {
    ContenidoCarga(estado, onReintentar) { lista ->
        if (lista.isEmpty()) {
            VacioBloque("No hay actividades próximas por ahora.")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                lista.take(3).forEach { actividad ->
                    ActividadFilaCard(actividad = actividad, onClick = { onVer(actividad.id) })
                }
            }
        }
    }
}

@Composable
private fun SeccionCampana(estado: Carga<List<Campana>>, onReintentar: () -> Unit, onVer: (String) -> Unit) {
    ContenidoCarga(estado, onReintentar) { lista ->
        val campana = lista.firstOrNull()
        if (campana == null) {
            VacioBloque("No hay campañas activas en este momento.")
        } else {
            CampanaCard(campana = campana, onClick = { onVer(campana.id) })
        }
    }
}

@Composable
private fun SeccionHeader(
    titulo: String,
    accion: String?,
    onAccion: () -> Unit,
    modifier: Modifier = Modifier
) {
    TituloSeccion(titulo = titulo, accion = accion, onAccion = onAccion, modifier = modifier.padding(bottom = 12.dp))
}

@Composable
private fun ActividadFilaCard(actividad: ActividadItem, onClick: () -> Unit) {
    GhostBorderCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.tertiary, MaterialTheme.shapes.medium)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(actividad.dia, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onTertiary, fontWeight = FontWeight.Bold)
                Text(actividad.mes, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onTertiary)
            }
            Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                Text(actividad.titulo, style = MaterialTheme.typography.titleMedium)
                Text(actividad.etiquetaAudiencia, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                UbicacionRow(texto = actividad.ubicacion)
                // Se actualiza en tiempo real con el flujo de cupos
                Text(
                    when (actividad.cuposDisponibles) {
                        0 -> "Sin lugares disponibles"
                        1 -> "Queda 1 lugar"
                        else -> "${actividad.cuposDisponibles} lugares disponibles"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (actividad.cuposDisponibles == 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CampanaCard(campana: Campana, onClick: () -> Unit) {
    GhostBorderCard(onClick = onClick) {
        Text(campana.titulo, style = MaterialTheme.typography.titleMedium)
        Text(
            campana.descripcion,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
        )
        LinearProgressIndicator(
            progress = { campana.progreso },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.shapes.extraLarge),
            color = MaterialTheme.colorScheme.secondary,
            trackColor = androidx.compose.ui.graphics.Color.Transparent,
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(campana.recaudadoTexto, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(campana.metaTexto, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PrimaryButton(text = "Donar", onClick = onClick, modifier = Modifier.weight(1f))
            val context = androidx.compose.ui.platform.LocalContext.current
            IconButton(onClick = {
                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(android.content.Intent.EXTRA_TEXT, "Apoya la campaña \"${campana.titulo}\" en Familias que Suman: ${campana.descripcion}")
                }
                runCatching { context.startActivity(android.content.Intent.createChooser(intent, "Compartir campaña")) }
            }) {
                Icon(Icons.Filled.Share, contentDescription = "Compartir")
            }
        }
    }
}