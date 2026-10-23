package mx.familiasquesuman.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import mx.familiasquesuman.app.data.ArticuloDonacion
import mx.familiasquesuman.app.data.Campana
import mx.familiasquesuman.app.data.PuntoEntrega
import mx.familiasquesuman.app.data.remote.mensajeDeError
import mx.familiasquesuman.app.ui.components.TituloSeccion
import mx.familiasquesuman.app.ui.components.BackTopBar
import mx.familiasquesuman.app.ui.components.ContenidoCarga
import mx.familiasquesuman.app.ui.components.GhostBorderCard
import mx.familiasquesuman.app.ui.components.PrimaryButton
import mx.familiasquesuman.app.ui.components.StatusTag
import mx.familiasquesuman.app.ui.components.UbicacionRow
import mx.familiasquesuman.app.viewmodel.AlTerminar
import mx.familiasquesuman.app.viewmodel.Carga

/** Basada en campana_de_donacion_azul (donación de artículos físicos, no dinero). */
@Composable
fun CampanaDonacionScreen(
    estado: Carga<Campana>,
    onReintentar: () -> Unit,
    onRegistrarEntrega: (
        articuloId: String,
        puntoId: String,
        cantidad: Double,
        comentario: String?,
        alTerminar: AlTerminar<Unit>
    ) -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        BackTopBar(titulo = "Donaciones", onBack = onBack)
        ContenidoCarga(estado, onReintentar) { campana -> DetalleCampana(campana, onRegistrarEntrega) }
    }
}

@Composable
private fun DetalleCampana(
    campana: Campana,
    onRegistrarEntrega: (String, String, Double, String?, AlTerminar<Unit>) -> Unit
) {
    val context = LocalContext.current
    var mostrarRegistro by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        if (campana.urgente) {
            StatusTag(texto = "Urgente", containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.error)
        }
        Text(campana.titulo, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        if (campana.organizacion.isNotBlank()) {
            Text(campana.organizacion, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
        Text(
            campana.descripcion,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.15f), MaterialTheme.shapes.medium)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
            Text(
                "No procesamos pagos monetarios. Solo recibimos donaciones físicas.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        TituloSeccion("Artículos Necesarios", modifier = Modifier.padding(top = 28.dp, bottom = 12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            campana.articulos.forEach { articulo ->
                ArticuloCard(icono = Icons.Filled.Inventory2, articulo = articulo)
            }
        }

        TituloSeccion(
            if (campana.puntosEntrega.size > 1) "Puntos de Entrega" else "Punto de Entrega",
            modifier = Modifier.padding(top = 28.dp, bottom = 12.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            campana.puntosEntrega.forEach { punto ->
                GhostBorderCard {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        UbicacionRow(texto = punto.nombre)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(punto.direccion)))
                                runCatching { context.startActivity(intent) }
                                    .onFailure { Toast.makeText(context, "No se encontró una app de mapas.", Toast.LENGTH_SHORT).show() }
                            }
                        ) {
                            Icon(Icons.Filled.Directions, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("Cómo llegar", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Text(punto.direccion, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                    punto.indicaciones?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(modifier = Modifier.padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Horario de Recepción", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 6.dp))
                    }
                    Text(punto.horarios, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }

        PrimaryButton(
            text = "Registrar mi entrega",
            enabled = campana.articulos.isNotEmpty() && campana.puntosEntrega.isNotEmpty(),
            onClick = { mostrarRegistro = true },
            modifier = Modifier.padding(top = 24.dp, bottom = 16.dp)
        )
    }

    if (mostrarRegistro) {
        RegistroEntregaDialogo(
            articulos = campana.articulos,
            puntos = campana.puntosEntrega,
            onCerrar = { mostrarRegistro = false },
            onEnviar = { articuloId, puntoId, cantidad, comentario, alTerminar ->
                onRegistrarEntrega(articuloId, puntoId, cantidad, comentario) { r ->
                    alTerminar(r)
                    r.onSuccess {
                        mostrarRegistro = false
                        Toast.makeText(
                            context,
                            "¡Gracias! Registramos tu entrega. El equipo la confirmará cuando la reciba.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        )
    }
}

@Composable
private fun RegistroEntregaDialogo(
    articulos: List<ArticuloDonacion>,
    puntos: List<PuntoEntrega>,
    onCerrar: () -> Unit,
    onEnviar: (String, String, Double, String?, AlTerminar<Unit>) -> Unit
) {
    var articulo by remember { mutableStateOf(articulos.first()) }
    var punto by remember { mutableStateOf(puntos.first()) }
    var cantidadTexto by remember { mutableStateOf("") }
    var comentario by remember { mutableStateOf("") }
    var enviando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val cantidad = cantidadTexto.replace(',', '.').toDoubleOrNull()?.takeIf { it > 0 && it <= 1_000_000 }

    AlertDialog(
        onDismissRequest = { if (!enviando) onCerrar() },
        title = { Text("Registrar mi entrega") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("¿Qué vas a entregar?", style = MaterialTheme.typography.titleSmall)
                articulos.forEach { a ->
                    Row(
                        modifier = Modifier.fillMaxWidth().selectable(selected = articulo.id == a.id, onClick = { articulo = a }),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = articulo.id == a.id, onClick = { articulo = a })
                        Text("${a.nombre} (${a.unidad})", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                OutlinedTextField(
                    value = cantidadTexto,
                    onValueChange = { v -> cantidadTexto = v.filter { it.isDigit() || it == '.' || it == ',' }.take(10) },
                    label = { Text("Cantidad en ${articulo.unidad}") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                )
                if (puntos.size > 1) {
                    Text("¿Dónde lo entregarás?", style = MaterialTheme.typography.titleSmall)
                    puntos.forEach { p ->
                        Row(
                            modifier = Modifier.fillMaxWidth().selectable(selected = punto.id == p.id, onClick = { punto = p }),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = punto.id == p.id, onClick = { punto = p })
                            Text(p.nombre, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                OutlinedTextField(
                    value = comentario,
                    onValueChange = { comentario = it.take(500) },
                    label = { Text("Comentario (opcional)") },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !enviando && cantidad != null,
                onClick = {
                    val c = cantidad ?: return@TextButton
                    enviando = true
                    error = null
                    onEnviar(articulo.id, punto.id, c, comentario) { r ->
                        enviando = false
                        r.onFailure { error = mensajeDeError(it) }
                    }
                }
            ) { Text("Registrar") }
        },
        dismissButton = { TextButton(onClick = onCerrar, enabled = !enviando) { Text("Cancelar") } }
    )
}

@Composable
private fun ArticuloCard(icono: ImageVector, articulo: ArticuloDonacion) {
    GhostBorderCard {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(icono, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(articulo.nombre, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 8.dp))
            }
            StatusTag(
                texto = articulo.prioridad,
                containerColor = if (articulo.urgente) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
                contentColor = if (articulo.urgente) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
            )
        }
        if (articulo.descripcion.isNotBlank()) {
            Text(articulo.descripcion, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
        }
        LinearProgressIndicator(
            progress = { articulo.progreso },
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp).height(6.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
        Text(
            articulo.progresoTexto,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}
