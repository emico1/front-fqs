package mx.familiasquesuman.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import mx.familiasquesuman.app.data.MiembroComunidad
import mx.familiasquesuman.app.data.NuevoMiembro
import mx.familiasquesuman.app.data.fechaIsoDesdeTexto
import mx.familiasquesuman.app.data.remote.mensajeDeError
import mx.familiasquesuman.app.ui.components.BackTopBar
import mx.familiasquesuman.app.ui.components.GhostBorderCard
import mx.familiasquesuman.app.ui.components.PrimaryButton
import mx.familiasquesuman.app.ui.components.SecondaryButton
import mx.familiasquesuman.app.ui.components.VacioBloque
import mx.familiasquesuman.app.viewmodel.AlTerminar

/** Basada en a_adir_miembros_de_la_comunidad. La lista vive en el backend y en AppViewModel,
 * así que agregar o quitar un miembro aquí se refleja en los acompañantes de una actividad. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiComunidadScreen(
    miembros: List<MiembroComunidad>,
    onAgregar: (NuevoMiembro, AlTerminar<Unit>) -> Unit,
    onEliminar: (MiembroComunidad, AlTerminar<Unit>) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var mostrarFormulario by remember { mutableStateOf(false) }
    var porEliminar by remember { mutableStateOf<MiembroComunidad?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        BackTopBar(titulo = "Mi Comunidad", onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                "Gestiona y administra los miembros de tu círculo.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (miembros.isEmpty()) VacioBloque("Todavía no agregas a nadie. Los miembros podrán acompañarte en las actividades.")

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                miembros.forEach { miembro ->
                    GhostBorderCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(miembro.iniciales, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            }
                            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                                Text(miembro.nombre, style = MaterialTheme.typography.titleMedium)
                                Text(miembro.relacion, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { porEliminar = miembro }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            PrimaryButton(
                text = "Añadir nuevo miembro",
                onClick = { mostrarFormulario = true },
                modifier = Modifier.padding(top = 20.dp)
            )
        }
    }

    porEliminar?.let { miembro ->
        AlertDialog(
            onDismissRequest = { porEliminar = null },
            title = { Text("Eliminar a ${miembro.nombre}") },
            text = { Text("Dejará de aparecer en tu comunidad. Su historial de actividades se conserva.") },
            confirmButton = {
                TextButton(onClick = {
                    porEliminar = null
                    onEliminar(miembro) { r ->
                        r.onFailure { Toast.makeText(context, mensajeDeError(it), Toast.LENGTH_LONG).show() }
                    }
                }) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { porEliminar = null }) { Text("Cancelar") } }
        )
    }

    if (mostrarFormulario) {
        Dialog(onDismissRequest = { mostrarFormulario = false }) {
            NuevoMiembroFormulario(
                onCancelar = { mostrarFormulario = false },
                onGuardar = { nuevo, alTerminar ->
                    onAgregar(nuevo) { r ->
                        alTerminar(r)
                        if (r.isSuccess) mostrarFormulario = false
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NuevoMiembroFormulario(
    onCancelar: () -> Unit,
    onGuardar: (NuevoMiembro, AlTerminar<Unit>) -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var relacionExpandida by remember { mutableStateOf(false) }
    var relacion by remember { mutableStateOf("") }
    val relaciones = listOf("Hijo/a", "Padre/Madre", "Abuelo/a", "Pareja", "Hermano/a", "Otro")
    var contacto by remember { mutableStateOf("") }
    var fechaNacimiento by remember { mutableStateOf("") }
    var enviando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val fechaIso = fechaIsoDesdeTexto(fechaNacimiento)

    GhostBorderCard {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Añadir nuevo miembro", style = MaterialTheme.typography.titleLarge)
            IconButton(onClick = onCancelar) {
                Icon(Icons.Filled.Close, contentDescription = "Cerrar")
            }
        }

        OutlinedTextField(
            value = nombre,
            onValueChange = { nombre = it.take(120) },
            label = { Text("Nombre completo") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
        )

        ExposedDropdownMenuBox(
            expanded = relacionExpandida,
            onExpandedChange = { relacionExpandida = it },
            modifier = Modifier.padding(top = 12.dp)
        ) {
            OutlinedTextField(
                value = relacion,
                onValueChange = {},
                readOnly = true,
                label = { Text("Relación") },
                placeholder = { Text("Selecciona una opción") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = relacionExpandida) },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(expanded = relacionExpandida, onDismissRequest = { relacionExpandida = false }) {
                relaciones.forEach { r ->
                    DropdownMenuItem(text = { Text(r) }, onClick = { relacion = r; relacionExpandida = false })
                }
            }
        }

        OutlinedTextField(
            value = contacto,
            onValueChange = { contacto = it.take(254) },
            label = { Text("Correo o Teléfono (opcional)") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
        )
        OutlinedTextField(
            value = fechaNacimiento,
            onValueChange = { v -> fechaNacimiento = v.filter { it.isDigit() || it == '/' }.take(10) },
            label = { Text("Fecha de Nacimiento") },
            placeholder = { Text("DD/MM/AAAA") },
            singleLine = true,
            isError = fechaNacimiento.length == 10 && fechaIso == null,
            supportingText = { Text("Se usa para verificar la edad mínima de cada actividad") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
        )

        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

        Row(modifier = Modifier.fillMaxWidth().padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SecondaryButton(text = "Cancelar", onClick = onCancelar, modifier = Modifier.weight(1f))
            PrimaryButton(
                text = if (enviando) "..." else "Guardar",
                enabled = !enviando && nombre.isNotBlank() && relacion.isNotBlank() && fechaIso != null,
                onClick = {
                    val iso = fechaIso ?: return@PrimaryButton
                    enviando = true
                    error = null
                    onGuardar(NuevoMiembro(nombre.trim(), relacion, iso, contacto)) { r ->
                        enviando = false
                        r.onFailure { error = mensajeDeError(it) }
                    }
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}
