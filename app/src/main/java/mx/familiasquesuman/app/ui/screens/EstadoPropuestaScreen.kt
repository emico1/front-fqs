package mx.familiasquesuman.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import mx.familiasquesuman.app.data.EstadoPropuesta
import mx.familiasquesuman.app.data.Propuesta
import mx.familiasquesuman.app.ui.components.BackTopBar
import mx.familiasquesuman.app.ui.components.ContenidoCarga
import mx.familiasquesuman.app.ui.components.StatusTag
import mx.familiasquesuman.app.viewmodel.Carga

private data class PasoProgreso(val icono: ImageVector, val titulo: String, val descripcion: String, val completado: Boolean)

private fun descripcionEstado(e: EstadoPropuesta) = when (e) {
    EstadoPropuesta.EN_REVISION -> "Nuestro equipo está validando los detalles de tu iniciativa. Te avisaremos cuando termine la revisión."
    EstadoPropuesta.NECESITA_CAMBIOS -> "El equipo revisó tu propuesta y necesita algunos ajustes antes de aprobarla."
    EstadoPropuesta.APROBADA -> "Tu propuesta fue aprobada y está lista para buscar voluntarios o apoyo."
    EstadoPropuesta.RECHAZADA -> "Por ahora tu propuesta no fue aprobada. Gracias por proponer, puedes enviar otra cuando quieras."
    EstadoPropuesta.COMPLETADA -> "La iniciativa se llevó a cabo. ¡Gracias por sumar!"
}

private fun iconoEstado(e: EstadoPropuesta) = when (e) {
    EstadoPropuesta.EN_REVISION -> Icons.Filled.Sync
    EstadoPropuesta.NECESITA_CAMBIOS -> Icons.Filled.Edit
    EstadoPropuesta.APROBADA -> Icons.Filled.ThumbUp
    EstadoPropuesta.RECHAZADA -> Icons.Filled.Close
    EstadoPropuesta.COMPLETADA -> Icons.Filled.Flag
}

/** Basada en estado_de_revision_de_propuesta. La línea de tiempo sale del historial real. */
@Composable
fun EstadoPropuestaScreen(
    estado: Carga<Propuesta>,
    onReintentar: () -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        BackTopBar(titulo = "Estado de tu Propuesta", onBack = onBack)
        ContenidoCarga(estado, onReintentar) { propuesta -> DetallePropuesta(propuesta) }
    }
}

@Composable
private fun DetallePropuesta(propuesta: Propuesta) {
    // Primer paso siempre es el envío, luego cada cambio registrado por el equipo
    val pasos = buildList {
        add(PasoProgreso(Icons.Filled.Check, "Enviada", "Recibida el ${propuesta.fecha}.", true))
        propuesta.historial.drop(1).forEach { h ->
            val detalle = listOfNotNull(h.fecha, h.comentario).joinToString(". ")
            add(PasoProgreso(iconoEstado(h.estado), h.estado.etiqueta, detalle, true))
        }
        if (propuesta.estado == EstadoPropuesta.EN_REVISION && propuesta.historial.size <= 1) {
            add(PasoProgreso(Icons.Filled.Sync, "En Revisión", "Estamos analizando la viabilidad de la iniciativa.", true))
        }
        if (propuesta.estado == EstadoPropuesta.EN_REVISION || propuesta.estado == EstadoPropuesta.NECESITA_CAMBIOS) {
            add(PasoProgreso(Icons.Filled.ThumbUp, "Aprobada", "Lista para buscar voluntarios o apoyo.", false))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        StatusTag(
            texto = propuesta.tipo,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.primary
        )
        Text(propuesta.titulo, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
        Text(propuesta.estado.etiqueta, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.tertiary)
        Text(
            descripcionEstado(propuesta.estado),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
        propuesta.comentarioAdmin?.takeIf { it.isNotBlank() }?.let {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f), MaterialTheme.shapes.medium)
                    .padding(12.dp)
            ) {
                Text("Comentario del equipo", style = MaterialTheme.typography.labelLarge)
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
        }

        Text(
            "PROGRESO DE LA PROPUESTA",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 28.dp, bottom = 16.dp)
        )

        pasos.forEachIndexed { index, paso ->
            Row {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                if (paso.completado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            paso.icono,
                            contentDescription = null,
                            tint = if (paso.completado) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    if (index != pasos.lastIndex) {
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(36.dp)
                                .background(if (paso.completado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                        )
                    }
                }
                Column(modifier = Modifier.padding(start = 16.dp, bottom = 24.dp)) {
                    Text(paso.titulo, style = MaterialTheme.typography.titleMedium)
                    Text(paso.descripcion, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
