package mx.familiasquesuman.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material.icons.filled.SentimentNeutral
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.SentimentVeryDissatisfied
import androidx.compose.material.icons.filled.SentimentVerySatisfied
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import mx.familiasquesuman.app.data.remote.mensajeDeError
import mx.familiasquesuman.app.ui.components.BackTopBar
import mx.familiasquesuman.app.ui.components.PrimaryButton
import mx.familiasquesuman.app.ui.components.SecondaryButton
import mx.familiasquesuman.app.viewmodel.AlTerminar
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.Role

/** Tipo de reporte que se muestra y su valor en la API. */
private data class TipoReporte(val etiqueta: String, val api: String)

/**
 * Basada en reportar_mala_experiencia. Si se abre desde una asociación o una actividad,
 * el reporte queda ligado a ella y aparece la opción correspondiente.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportarExperienciaScreen(
    asociacionId: String?,
    actividadId: String?,
    onBack: () -> Unit,
    onEnviar: (tipo: String, nivel: Int, descripcion: String, foto: Uri?, alTerminar: AlTerminar<Unit>) -> Unit,
    onEnviado: () -> Unit
) {
    var tipoExpandido by remember { mutableStateOf(false) }
    val tipos = buildList {
        if (asociacionId != null) add(TipoReporte("Problema con la asociación", "problema_asociacion"))
        if (actividadId != null) add(TipoReporte("Problema con una actividad", "problema_actividad"))
        add(TipoReporte("Comportamiento inapropiado", "comportamiento_inapropiado"))
        add(TipoReporte("Problema de la aplicación", "problema_aplicacion"))
        add(TipoReporte("Otro", "otro"))
    }
    var tipoProblema by remember { mutableStateOf<TipoReporte?>(null) }
    var enviando by remember { mutableStateOf(false) }
    var nivelSeleccionado by remember { mutableStateOf(2) }
    val niveles = listOf(
        Icons.Filled.SentimentVeryDissatisfied,
        Icons.Filled.SentimentDissatisfied,
        Icons.Filled.SentimentNeutral,
        Icons.Filled.SentimentSatisfied,
        Icons.Filled.SentimentVerySatisfied


    )
    val etiquetas = listOf(
        "Muy mala",
        "Mala",
        "Neutral",
        "Buena",
        "Muy buena"
    )

    val colores = listOf(
        Color(0xFFD32F2F),
        Color(0xFFEF6C00),
        Color(0xFFFBC02D),
        Color(0xFF7CB342),
        Color(0xFF2E7D32)
    )
    var descripcion by remember { mutableStateOf("") }
    var fotoUri by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current

    val selectorFoto = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> fotoUri = uri }

    Column(modifier = Modifier.fillMaxSize()) {
        BackTopBar(titulo = "Reportar Experiencia", onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.15f), MaterialTheme.shapes.medium)
                    .padding(12.dp)
            ) {
                Icon(Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                Column(modifier = Modifier.padding(start = 8.dp)) {
                    Text("Tu opinión nos ayuda a mejorar", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Lamentamos que hayas tenido una mala experiencia. Detallar lo sucedido nos permite mantener la calidad y seguridad de la comunidad.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text("Tipo de problema", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 24.dp, bottom = 8.dp))
            ExposedDropdownMenuBox(expanded = tipoExpandido, onExpandedChange = { tipoExpandido = it }) {
                OutlinedTextField(
                    value = tipoProblema?.etiqueta ?: "",
                    onValueChange = {},
                    readOnly = true,
                    placeholder = { Text("Selecciona una opción...") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = tipoExpandido) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(expanded = tipoExpandido, onDismissRequest = { tipoExpandido = false }) {
                    tipos.forEach { t ->
                        DropdownMenuItem(text = { Text(t.etiqueta) }, onClick = { tipoProblema = t; tipoExpandido = false })
                    }
                }
            }

            Text("Nivel de insatisfacción", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 24.dp, bottom = 12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                niveles.forEachIndexed { index, icono ->
                    NivelSentimiento(
                        icono = icono,
                        etiqueta = etiquetas[index],
                        color = colores[index],
                        seleccionado = nivelSeleccionado == index,
                        onClick = { nivelSeleccionado = index },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Text("Describe lo sucedido *", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 24.dp, bottom = 8.dp))
            OutlinedTextField(
                value = descripcion,
                onValueChange = { descripcion = it.take(3000) },
                minLines = 4,
                supportingText = { if (descripcion.isNotEmpty() && descripcion.trim().length < 10) Text("Escribe al menos 10 caracteres") },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth()
            )

            Text("Adjuntar foto o evidencia (Opcional)", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 24.dp, bottom = 8.dp))
            SecondaryButton(
                text = if (fotoUri == null) "Subir archivo" else "Cambiar archivo",
                onClick = {
                    selectorFoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
            )
            fotoUri?.let { uri ->
                AsyncImage(
                    model = uri,
                    contentDescription = "Foto adjunta",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.shapes.medium)
                )
            }
            Text(
                "PNG, JPG, max 5MB",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )

            PrimaryButton(
                text = if (enviando) "Enviando..." else "Enviar Reporte",
                enabled = !enviando && tipoProblema != null && descripcion.trim().length >= 10,
                onClick = {
                    val tipo = tipoProblema ?: return@PrimaryButton
                    enviando = true
                    // Las caras van de "Muy mala" (índice 0) a "Muy buena" (índice 4), la API usa 1 a 5
                    onEnviar(tipo.api, nivelSeleccionado + 1, descripcion, fotoUri) { r ->
                        enviando = false
                        r.onSuccess {
                            Toast.makeText(context, "Gracias, tu reporte fue enviado a nuestro equipo.", Toast.LENGTH_LONG).show()
                            onEnviado()
                        }.onFailure { Toast.makeText(context, mensajeDeError(it), Toast.LENGTH_LONG).show() }
                    }
                },
                modifier = Modifier.padding(top = 28.dp, bottom = 16.dp)
            )
        }
    }
}

@Composable
private fun NivelSentimiento(
    icono: ImageVector,
    etiqueta: String,
    color: Color,
    seleccionado: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(
                color = if (seleccionado) {
                    color.copy(alpha = 0.12f)
                } else {
                    Color.Transparent
                },
                shape = MaterialTheme.shapes.medium
            )
            .border(
                width = 2.dp,
                color = if (seleccionado) color else Color.Transparent,
                shape = MaterialTheme.shapes.medium
            )
            .selectable(
                selected = seleccionado,
                role = Role.RadioButton,
                onClick = onClick
            )
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icono,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(32.dp)
        )

        Text(
            text = etiqueta,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
