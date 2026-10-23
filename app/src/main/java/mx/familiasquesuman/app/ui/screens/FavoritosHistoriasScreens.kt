package mx.familiasquesuman.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import mx.familiasquesuman.app.data.Favoritos
import mx.familiasquesuman.app.data.Testimonio
import mx.familiasquesuman.app.ui.components.ActividadResumenCard
import mx.familiasquesuman.app.ui.components.BackTopBar
import mx.familiasquesuman.app.ui.components.CategoriaBadge
import mx.familiasquesuman.app.ui.components.ContenidoCarga
import mx.familiasquesuman.app.ui.components.TituloSeccion
import mx.familiasquesuman.app.ui.components.GhostBorderCard
import mx.familiasquesuman.app.ui.components.VacioBloque
import mx.familiasquesuman.app.viewmodel.Carga

/**
 * Favoritos (RF20). Asociaciones marcadas, con aviso cuando publican algo nuevo, y
 * actividades guardadas para después.
 */
@Composable
fun FavoritosScreen(
    estado: Carga<Favoritos>,
    onReintentar: () -> Unit,
    onBack: () -> Unit,
    onVerAsociacion: (String) -> Unit,
    onVerActividad: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        BackTopBar(titulo = "Mis favoritos", onBack = onBack)
        ContenidoCarga(estado, onReintentar) { f ->
            if (f.asociaciones.isEmpty() && f.actividades.isEmpty()) {
                VacioBloque("Aún no tienes favoritos. Toca el corazón en una actividad o activa los avisos de una asociación.")
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (f.asociaciones.isNotEmpty()) {
                        item { Subtitulo("Asociaciones") }
                        items(f.asociaciones, key = { "a-" + it.id }) { a ->
                            GhostBorderCard(onClick = { onVerAsociacion(a.id) }) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CategoriaBadge(categoria = a.categoria, tamano = 40.dp)
                                    Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                                        Text(a.nombre, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            if (a.notificacionesActivas) "Te avisamos cuando publique algo nuevo" else "Sin avisos",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (a.notificacionesActivas) {
                                        Icon(Icons.Filled.NotificationsActive, contentDescription = "Avisos activos", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                    if (f.actividades.isNotEmpty()) {
                        item { Subtitulo("Actividades guardadas") }
                        items(f.actividades, key = { "x-" + it.id }) { a ->
                            ActividadResumenCard(actividad = a, onClick = { onVerActividad(a.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Subtitulo(texto: String) {
    TituloSeccion(titulo = texto, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
}

/**
 * Historias de la comunidad (RF-12). Solo se ven los testimonios que el equipo aprobó,
 * de la ciudad de la familia. Lo que no se aprobó nunca llega a esta lista.
 */
@Composable
fun HistoriasScreen(
    estado: Carga<List<Testimonio>>,
    onReintentar: () -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        BackTopBar(titulo = "Historias de la comunidad", onBack = onBack)
        ContenidoCarga(estado, onReintentar) { lista ->
            if (lista.isEmpty()) {
                VacioBloque("Todavía no hay historias publicadas en tu ciudad. Después de participar puedes compartir la tuya desde tu perfil.")
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(lista, key = { it.id }) { t -> HistoriaCard(t) }
                }
            }
        }
    }
}

@Composable
private fun HistoriaCard(t: Testimonio) {
    GhostBorderCard {
        if (t.fotoUrl != null) {
            AsyncImage(
                model = t.fotoUrl,
                contentDescription = "Foto de ${t.actividad ?: "la actividad"}",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.shapes.medium)
            )
        }
        Text(t.texto, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 10.dp))
        Text(
            listOfNotNull(t.autor, t.actividad, t.asociacion).joinToString(" · "),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(t.fecha, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
