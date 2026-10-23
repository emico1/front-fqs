package mx.familiasquesuman.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import mx.familiasquesuman.app.data.Experiencia
import mx.familiasquesuman.app.data.ResultadoParticipacion
import mx.familiasquesuman.app.data.TipoActividad
import mx.familiasquesuman.app.data.remote.mensajeDeError
import mx.familiasquesuman.app.ui.components.BackTopBar
import mx.familiasquesuman.app.ui.components.ContenidoCarga
import mx.familiasquesuman.app.ui.components.GhostBorderCard
import mx.familiasquesuman.app.ui.components.StatusTag
import mx.familiasquesuman.app.ui.components.UbicacionRow
import mx.familiasquesuman.app.ui.components.VacioBloque
import mx.familiasquesuman.app.viewmodel.AlTerminar
import mx.familiasquesuman.app.viewmodel.Carga

/**
 * Historial completo de participación (RF-11). Muestra todas las inscripciones de la
 * cuenta, de la más reciente a la más antigua, agrupadas por mes, con un resumen
 * del último año arriba. En las que asistió puede calificar con me gusta o no me
 * gusta (RF21), eso ordena las asociaciones por calidad para las demás familias.
 */
@Composable
fun HistorialScreen(
    estado: Carga<List<Experiencia>>,
    onReintentar: () -> Unit,
    onBack: () -> Unit,
    onVerActividad: (String) -> Unit,
    onValorar: (inscripcionId: String, valor: Int?, alTerminar: AlTerminar<Unit>) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        BackTopBar(titulo = "Mi historial", onBack = onBack)
        ContenidoCarga(estado, onReintentar) { lista ->
            if (lista.isEmpty()) {
                VacioBloque("Todavía no tienes participaciones. Cuando te inscribas a una actividad aparecerá aquí.")
            } else {
                val haceUnAnio = System.currentTimeMillis() - 365L * 24 * 60 * 60 * 1000
                val ultimoAnio = lista.filter { it.inicioMillis >= haceUnAnio }
                val asistidas = ultimoAnio.count { it.resultado == ResultadoParticipacion.ASISTIO }
                val visitas = ultimoAnio.count { it.resultado == ResultadoParticipacion.ASISTIO && it.tipo == TipoActividad.VISITA }
                val porMes = lista.groupBy { it.mesAnio }

                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        GhostBorderCard {
                            Text("Último año", style = MaterialTheme.typography.titleMedium)
                            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Resumen(numero = asistidas, etiqueta = "participaciones")
                                Resumen(numero = visitas, etiqueta = "visitas")
                                Resumen(numero = ultimoAnio.count { it.resultado == ResultadoParticipacion.PROXIMA }, etiqueta = "próximas")
                            }
                        }
                    }
                    porMes.forEach { (mes, experiencias) ->
                        item(key = "mes-$mes") {
                            Text(
                                mes,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                            )
                        }
                        items(experiencias, key = { it.inscripcionId }) { exp ->
                            FilaHistorial(exp, onClick = { onVerActividad(exp.actividadId) }, onValorar = onValorar)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Resumen(numero: Int, etiqueta: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$numero", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(etiqueta, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun FilaHistorial(
    exp: Experiencia,
    onClick: () -> Unit,
    onValorar: (String, Int?, AlTerminar<Unit>) -> Unit
) {
    val context = LocalContext.current
    val (fondo, texto) = when (exp.resultado) {
        ResultadoParticipacion.ASISTIO -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.primary
        ResultadoParticipacion.PROXIMA -> MaterialTheme.colorScheme.secondary to MaterialTheme.colorScheme.secondary
        ResultadoParticipacion.CANCELADA -> MaterialTheme.colorScheme.error to MaterialTheme.colorScheme.error
        ResultadoParticipacion.SIN_ASISTENCIA -> MaterialTheme.colorScheme.outline to MaterialTheme.colorScheme.onSurfaceVariant
    }
    GhostBorderCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusTag(texto = exp.resultado.etiqueta, containerColor = fondo, contentColor = texto)
            if (exp.tipo == TipoActividad.VISITA) {
                StatusTag(texto = "Visita")
            }
        }
        Text(exp.titulo, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 6.dp))
        Text(exp.fecha, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        UbicacionRow(texto = exp.ubicacion)
        when (exp.estadoTestimonio) {
            "pendiente" -> Text("Testimonio en revisión", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
            "aprobado" -> Text("Testimonio publicado", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            "rechazado" -> Text("Testimonio no publicado", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            else -> Unit
        }
        if (exp.resultado == ResultadoParticipacion.ASISTIO) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                Text("¿Te gustó?", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                listOf(1 to Icons.Filled.ThumbUp, -1 to Icons.Filled.ThumbDown).forEach { (valor, icono) ->
                    val activo = exp.valoracion == valor
                    IconButton(onClick = {
                        // Tocar la opción ya elegida la quita
                        onValorar(exp.inscripcionId, if (activo) null else valor) { r ->
                            r.onFailure { android.widget.Toast.makeText(context, mensajeDeError(it), android.widget.Toast.LENGTH_LONG).show() }
                        }
                    }) {
                        Icon(
                            icono,
                            contentDescription = if (valor == 1) "Me gustó" else "No me gustó",
                            tint = when {
                                !activo -> MaterialTheme.colorScheme.outline
                                valor == 1 -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.error
                            }
                        )
                    }
                }
            }
        }
    }
}
