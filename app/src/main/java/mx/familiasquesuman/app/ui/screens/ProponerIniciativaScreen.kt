package mx.familiasquesuman.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import mx.familiasquesuman.app.data.TipoPropuesta
import mx.familiasquesuman.app.data.remote.ErrorApi
import mx.familiasquesuman.app.data.remote.mensajeDeError
import mx.familiasquesuman.app.ui.components.BackTopBar
import mx.familiasquesuman.app.ui.components.GhostBorderCard
import mx.familiasquesuman.app.ui.components.PrimaryButton
import mx.familiasquesuman.app.ui.components.SecondaryButton
import mx.familiasquesuman.app.viewmodel.AlTerminar

private data class OpcionPropuesta(val tipo: TipoPropuesta, val icono: ImageVector)

/**
 * Basada en proponer_iniciativa_azul_2, sigue el flujo de CP-01. Se llena la
 * información, se revisa en un resumen y se envía. Si la asociación ya existe o la
 * misma familia ya la propuso, el backend responde con un error diseñado y aquí se
 * explica qué hacer en lugar de mostrar un error genérico.
 */
@Composable
fun ProponerIniciativaScreen(
    tipoInicial: TipoPropuesta? = null,
    onEnviar: (
        tipo: TipoPropuesta,
        titulo: String,
        descripcion: String,
        ubicacion: String,
        contacto: String,
        alTerminar: AlTerminar<Unit>
    ) -> Unit,
    onEnviada: () -> Unit,
    onBack: () -> Unit,
    onVerAsociacion: (String) -> Unit = {}
) {
    val opciones = listOf(
        OpcionPropuesta(TipoPropuesta.ACTIVIDAD, Icons.Filled.Event),
        OpcionPropuesta(TipoPropuesta.INICIATIVA_VECINAL, Icons.Filled.Groups),
        OpcionPropuesta(TipoPropuesta.ASOCIACION, Icons.Filled.Apartment)
    )
    var tipoSeleccionado by remember { mutableStateOf(tipoInicial ?: TipoPropuesta.ACTIVIDAD) }
    var nombreIniciativa by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var ubicacion by remember { mutableStateOf("") }
    var contacto by remember { mutableStateOf("") }
    var enviando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var revisando by remember { mutableStateOf(false) }
    var aviso by remember { mutableStateOf<ErrorApi?>(null) }

    aviso?.let { e ->
        val asociacionId = e.detalle("asociacionId")
        AlertDialog(
            onDismissRequest = { aviso = null },
            title = { Text(if (e.codigo == "asociacion_existente") "Esta asociación ya existe" else "Ya la habías propuesto") },
            text = { Text(e.message) },
            confirmButton = {
                if (asociacionId != null) {
                    TextButton(onClick = { aviso = null; onVerAsociacion(asociacionId) }) { Text("Ver asociación") }
                } else {
                    TextButton(onClick = { aviso = null }) { Text("Entendido") }
                }
            },
            dismissButton = {
                if (asociacionId != null) TextButton(onClick = { aviso = null; revisando = false }) { Text("Editar propuesta") }
            }
        )
    }

    if (revisando) {
        RevisarPropuesta(
            tipo = tipoSeleccionado,
            nombre = nombreIniciativa,
            descripcion = descripcion,
            ubicacion = ubicacion,
            contacto = contacto,
            enviando = enviando,
            error = error,
            onEditar = { revisando = false; error = null },
            onEnviar = {
                enviando = true
                error = null
                onEnviar(tipoSeleccionado, nombreIniciativa, descripcion, ubicacion, contacto) { r ->
                    enviando = false
                    r.onSuccess { onEnviada() }.onFailure {
                        if (it is ErrorApi && (it.codigo == "asociacion_existente" || it.codigo == "propuesta_duplicada")) aviso = it
                        else error = mensajeDeError(it)
                    }
                }
            },
            onBack = { revisando = false }
        )
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        BackTopBar(
            titulo = "Proponer Iniciativa",
            onBack = onBack
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Nueva Iniciativa", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 6.dp))
            }
            Text("Proponer Iniciativa", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
            Text(
                "Comparte tu idea para mejorar la comunidad. Nuestro equipo la revisará para ayudar a hacerla realidad.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)
            )

            Text("Información Básica", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 12.dp))
            Text("Tipo de propuesta", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                opciones.forEach { opcion ->
                    val activo = tipoSeleccionado == opcion.tipo
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .selectable(selected = activo, onClick = { tipoSeleccionado = opcion.tipo })
                            .background(
                                if (activo) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceContainerLowest,
                                MaterialTheme.shapes.medium
                            )
                            .border(
                                1.dp,
                                if (activo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                MaterialTheme.shapes.medium
                            )
                            .padding(vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(opcion.icono, contentDescription = null, tint = if (activo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            opcion.tipo.etiqueta,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (activo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            OutlinedTextField(
                value = nombreIniciativa,
                onValueChange = { nombreIniciativa = it.take(150) },
                label = { Text("Nombre de la iniciativa") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp)
            )
            OutlinedTextField(
                value = descripcion,
                onValueChange = { descripcion = it.take(3000) },
                label = { Text("Descripción breve") },
                minLines = 3,
                supportingText = { if (descripcion.isNotEmpty() && descripcion.trim().length < 10) Text("Escribe al menos 10 caracteres") },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
            )
            OutlinedTextField(
                value = ubicacion,
                onValueChange = { ubicacion = it.take(200) },
                label = { Text("Ciudad / Ubicación") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
            )

            Text("Datos de contacto", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 20.dp, bottom = 8.dp))
            OutlinedTextField(
                value = contacto,
                onValueChange = { contacto = it.take(254) },
                label = { Text("Correo o teléfono") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth()
            )

            PrimaryButton(
                text = "Revisar propuesta",
                enabled = nombreIniciativa.trim().length >= 3 && descripcion.trim().length >= 10 &&
                    ubicacion.isNotBlank() && contacto.isNotBlank(),
                onClick = { revisando = true },
                modifier = Modifier.padding(top = 24.dp)
            )

            val context = androidx.compose.ui.platform.LocalContext.current
            Text(
                "¿Necesitas ayuda? Contactar soporte",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(top = 16.dp, bottom = 16.dp)
                    .clickable {
                        val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                            data = android.net.Uri.parse("mailto:soporte@familiasquesuman.mx")
                            putExtra(android.content.Intent.EXTRA_SUBJECT, "Ayuda con una propuesta")
                        }
                        runCatching { context.startActivity(intent) }
                    }
            )
        }
    }
}

/** Paso "Revisar" de CP-01. Muestra todo lo capturado antes de enviarlo. */
@Composable
private fun RevisarPropuesta(
    tipo: TipoPropuesta,
    nombre: String,
    descripcion: String,
    ubicacion: String,
    contacto: String,
    enviando: Boolean,
    error: String?,
    onEditar: () -> Unit,
    onEnviar: () -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        BackTopBar(titulo = "Revisa tu propuesta", onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                "Confirma que todo esté bien. Después de enviarla el equipo la revisa y te avisa del resultado.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            GhostBorderCard(modifier = Modifier.padding(top = 16.dp)) {
                listOf(
                    "Tipo" to tipo.etiqueta,
                    "Nombre" to nombre.trim(),
                    "Descripción" to descripcion.trim(),
                    "Ubicación" to ubicacion.trim(),
                    "Contacto" to contacto.trim()
                ).forEach { (etiqueta, valor) ->
                    Text(etiqueta, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 10.dp))
                    Text(valor, style = MaterialTheme.typography.bodyMedium)
                }
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 12.dp)) }
            PrimaryButton(
                text = if (enviando) "Enviando..." else "Enviar propuesta",
                enabled = !enviando,
                onClick = onEnviar,
                modifier = Modifier.padding(top = 24.dp)
            )
            SecondaryButton(text = "Editar", onClick = onEditar, modifier = Modifier.padding(top = 12.dp, bottom = 24.dp))
        }
    }
}
