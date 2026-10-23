package mx.familiasquesuman.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import mx.familiasquesuman.app.data.Proyecto
import mx.familiasquesuman.app.ui.components.TituloSeccion
import mx.familiasquesuman.app.ui.components.BackTopBar
import mx.familiasquesuman.app.ui.components.CampanaResumenCard
import mx.familiasquesuman.app.ui.components.ContenidoCarga
import mx.familiasquesuman.app.ui.components.GhostBorderCard
import mx.familiasquesuman.app.ui.components.VacioBloque
import mx.familiasquesuman.app.viewmodel.Carga

/**
 * Proyectos (RF-09). Un proyecto junta varias campañas con un mismo objetivo. La
 * tarjeta muestra el avance conjunto y cuántas campañas siguen abiertas.
 */
@Composable
fun ProyectoCard(proyecto: Proyecto, onClick: () -> Unit) {
    GhostBorderCard(onClick = onClick) {
        Text(proyecto.titulo, style = MaterialTheme.typography.titleMedium)
        proyecto.organizacion?.let {
            Text(it, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
        Text(
            proyecto.descripcion,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 3,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
        )
        BarraAvance(proyecto.progreso)
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Avance ${(proyecto.progreso * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                when (proyecto.campanasActivas) {
                    0 -> "Sin campañas abiertas"
                    1 -> "1 campaña abierta"
                    else -> "${proyecto.campanasActivas} campañas abiertas"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BarraAvance(progreso: Float) {
    LinearProgressIndicator(
        progress = { progreso },
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.shapes.extraLarge),
        color = MaterialTheme.colorScheme.secondary,
        trackColor = Color.Transparent,
        strokeCap = StrokeCap.Round
    )
}

/** Lista de proyectos, la usa el filtro Proyectos de Inicio. */
@Composable
fun ListaProyectos(estado: Carga<List<Proyecto>>, onReintentar: () -> Unit, onVer: (String) -> Unit) {
    ContenidoCarga(estado, onReintentar) { lista ->
        if (lista.isEmpty()) {
            VacioBloque("No hay proyectos activos en tu ciudad por ahora.")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                lista.forEach { p -> ProyectoCard(proyecto = p, onClick = { onVer(p.id) }) }
            }
        }
    }
}

@Composable
fun ProyectoDetalleScreen(
    estado: Carga<Proyecto>,
    onReintentar: () -> Unit,
    onBack: () -> Unit,
    onVerCampana: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        BackTopBar(titulo = "Proyecto", onBack = onBack)
        ContenidoCarga(estado, onReintentar) { proyecto ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text(proyecto.titulo, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                proyecto.organizacion?.let {
                    Text(it, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 4.dp))
                }
                Text(proyecto.periodo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                Text(
                    proyecto.descripcion,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp, bottom = 16.dp)
                )
                BarraAvance(proyecto.progreso)
                Text(
                    "Avance conjunto ${(proyecto.progreso * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )

                TituloSeccion(
                    "Campañas del proyecto",
                    modifier = Modifier.padding(top = 28.dp, bottom = 8.dp)
                )
                if (proyecto.campanas.isEmpty()) {
                    VacioBloque("Este proyecto todavía no tiene campañas publicadas.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(bottom = 24.dp)) {
                        proyecto.campanas.forEach { c ->
                            CampanaResumenCard(campana = c, onClick = { onVerCampana(c.id) })
                        }
                    }
                }
            }
        }
    }
}
