package mx.familiasquesuman.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import mx.familiasquesuman.app.data.EstadoPropuesta
import mx.familiasquesuman.app.data.Propuesta
import mx.familiasquesuman.app.ui.components.BackTopBar
import mx.familiasquesuman.app.ui.components.ContenidoCarga
import mx.familiasquesuman.app.ui.components.VacioBloque
import mx.familiasquesuman.app.viewmodel.Carga
import mx.familiasquesuman.app.ui.components.FilterChip
import mx.familiasquesuman.app.ui.components.GhostBorderCard
import mx.familiasquesuman.app.ui.components.StatusTag

/** Basada en historial_de_propuestas. */
@Composable
fun MisPropuestasScreen(
    estado: Carga<List<Propuesta>>,
    onReintentar: () -> Unit,
    onBack: () -> Unit,
    onVerPropuesta: (String) -> Unit
) {
    var filtro by remember { mutableStateOf("Todas") }
    val filtros = listOf("Todas", "En revisión", "Completadas")

    fun filtrar(propuestas: List<Propuesta>) = when (filtro) {
        "En revisión" -> propuestas.filter { it.estado == EstadoPropuesta.EN_REVISION || it.estado == EstadoPropuesta.NECESITA_CAMBIOS }
        "Completadas" -> propuestas.filter { it.estado == EstadoPropuesta.APROBADA || it.estado == EstadoPropuesta.COMPLETADA }
        else -> propuestas
    }

    Column(modifier = Modifier.fillMaxSize()) {
        BackTopBar(titulo = "Mis Propuestas", onBack = onBack)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filtros.forEach { f ->
                FilterChip(texto = f, seleccionado = filtro == f, onClick = { filtro = f })
            }
        }
        ContenidoCarga(estado, onReintentar) { propuestas ->
            val propuestasFiltradas = filtrar(propuestas)
            if (propuestasFiltradas.isEmpty()) {
                VacioBloque(if (propuestas.isEmpty()) "Aún no has enviado propuestas." else "No hay propuestas con este filtro.")
            } else {
                LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(propuestasFiltradas, key = { it.id }) { propuesta ->
                        PropuestaCard(propuesta = propuesta, onClick = { onVerPropuesta(propuesta.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun PropuestaCard(propuesta: Propuesta, onClick: () -> Unit) {
    GhostBorderCard(onClick = onClick) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            val (contenedor, contenido) = colorEstado(propuesta.estado)
            StatusTag(texto = propuesta.estado.etiqueta, containerColor = contenedor, contentColor = contenido)
            Text(propuesta.fecha, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(propuesta.titulo, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
        Text(
            propuesta.descripcion,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Ver detalles", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun colorEstado(estado: EstadoPropuesta): Pair<Color, Color> = when (estado) {
    EstadoPropuesta.EN_REVISION -> MaterialTheme.colorScheme.tertiary to MaterialTheme.colorScheme.tertiary
    EstadoPropuesta.APROBADA -> MaterialTheme.colorScheme.secondary to MaterialTheme.colorScheme.secondary
    EstadoPropuesta.NECESITA_CAMBIOS -> MaterialTheme.colorScheme.error to MaterialTheme.colorScheme.error
    EstadoPropuesta.RECHAZADA -> MaterialTheme.colorScheme.error to MaterialTheme.colorScheme.error
    EstadoPropuesta.COMPLETADA -> MaterialTheme.colorScheme.secondary to MaterialTheme.colorScheme.secondary
}
