package mx.familiasquesuman.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Diversity3
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import mx.familiasquesuman.app.data.AvisoPrivacidad
import mx.familiasquesuman.app.data.Categoria
import mx.familiasquesuman.app.data.Perfil
import mx.familiasquesuman.app.data.remote.mensajeDeError
import mx.familiasquesuman.app.ui.components.BackTopBar
import mx.familiasquesuman.app.ui.components.FilterChip
import mx.familiasquesuman.app.ui.components.FormasFlotantesFondo
import mx.familiasquesuman.app.ui.components.PrimaryButton
import mx.familiasquesuman.app.ui.components.TertiaryTextButton
import mx.familiasquesuman.app.viewmodel.AlTerminar

private data class Interes(val etiqueta: String, val icono: ImageVector)

private val formatoUsername = Regex("^[A-Za-z0-9_.]{3,30}$")

/**
 * Dos usos de la misma pantalla.
 * Onboarding (esEdicion = false) termina el registro, pide username, intereses, temas y
 * la aceptación del aviso de privacidad, y crea la cuenta en el backend.
 * Preferencias (esEdicion = true) parte de lo guardado en el perfil y solo actualiza
 * intereses y temas.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingPreferenciasScreen(
    categorias: List<Categoria>,
    aviso: AvisoPrivacidad?,
    perfil: Perfil?,
    onGuardar: (
        username: String,
        donar: Boolean,
        participar: Boolean,
        proponer: Boolean,
        categorias: Set<String>,
        alTerminar: AlTerminar<Unit>
    ) -> Unit,
    onListo: () -> Unit,
    esEdicion: Boolean = false,
    onBack: () -> Unit = {},
    animacionesActivas: Boolean = true
) {
    val intereses = listOf(
        Interes("Donar", Icons.Filled.VolunteerActivism),
        Interes("Participar", Icons.Filled.Diversity3),
        Interes("Proponer", Icons.Filled.Lightbulb)
    )
    var seleccionado by remember(perfil?.id) {
        mutableStateOf(
            if (perfil != null) {
                buildSet {
                    if (perfil.quiereDonar) add("Donar")
                    if (perfil.quiereParticipar) add("Participar")
                    if (perfil.quiereProponer) add("Proponer")
                }
            } else setOf("Participar")
        )
    }
    var username by remember { mutableStateOf("") }
    var aceptaAviso by remember { mutableStateOf(esEdicion) }
    var temasSeleccionados by remember(perfil?.id) { mutableStateOf(perfil?.categorias ?: emptySet()) }
    var enviando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var mostrarAviso by remember { mutableStateOf(false) }

    val usernameValido = esEdicion || formatoUsername.matches(username.removePrefix("@"))
    val avisoDisponible = esEdicion || aviso != null

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        FormasFlotantesFondo(activo = animacionesActivas)
        Column(modifier = Modifier.fillMaxSize()) {
            if (esEdicion) {
                BackTopBar(titulo = "Preferencias", onBack = onBack)
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .let {
                        // Sin BackTopBar (modo onboarding) esta pantalla reserva ella misma el
                        // espacio de la barra de estado
                        if (esEdicion) it else it.windowInsetsPadding(androidx.compose.foundation.layout.WindowInsets.statusBars)
                    }
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                if (!esEdicion) {
                    // Regresar al paso 1 sin perder lo capturado, y el indicador de pasos (2 de 2)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack, enabled = !enviando) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar al paso 1")
                        }
                        Row(
                            modifier = Modifier.padding(start = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(Modifier.width(28.dp).height(6.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(3.dp)))
                            Box(Modifier.width(28.dp).height(6.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(3.dp)))
                            Text(
                                "Paso 2 de 2",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 6.dp)
                            )
                        }
                    }
                    Text(
                        "Personaliza tu experiencia",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Cuéntanos cómo te gustaría involucrarte para sugerirte las mejores oportunidades.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp, bottom = 28.dp)
                    )
                }

                Text("¿Qué te interesa más?", style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 28.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    intereses.forEach { interes ->
                        val activo = seleccionado.contains(interes.etiqueta)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .selectable(
                                    selected = activo,
                                    onClick = {
                                        seleccionado = if (activo) seleccionado - interes.etiqueta else seleccionado + interes.etiqueta
                                    }
                                )
                                .background(
                                    if (activo) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceContainerLowest,
                                    MaterialTheme.shapes.medium
                                )
                                .border(
                                    1.dp,
                                    if (activo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                    MaterialTheme.shapes.medium
                                )
                                .padding(vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                interes.icono,
                                contentDescription = null,
                                tint = if (activo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                interes.etiqueta,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (activo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }
                }

                if (!esEdicion) {
                    Text("Crea tu username", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it.removePrefix("@").take(30); error = null },
                        leadingIcon = { Text("@", style = MaterialTheme.typography.titleMedium) },
                        singleLine = true,
                        isError = username.isNotEmpty() && !usernameValido,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )
                    Text(
                        "Este nombre será público en la comunidad. Usa de 3 a 30 letras, números, punto o guion bajo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                    )
                }

                Text(
                    "Temas de interés",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "Opcional",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categorias.forEach { cat ->
                        val activo = temasSeleccionados.contains(cat.clave)
                        FilterChip(
                            texto = cat.etiqueta,
                            icono = cat.icono,
                            seleccionado = activo,
                            onClick = {
                                temasSeleccionados = if (activo) temasSeleccionados - cat.clave else temasSeleccionados + cat.clave
                            }
                        )
                    }
                }

                if (!esEdicion) {
                    if (aviso == null) {
                        Text(
                            "El registro todavía no está disponible porque falta publicar el aviso de privacidad.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 24.dp)
                                .selectable(selected = aceptaAviso, onClick = { aceptaAviso = !aceptaAviso }),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = aceptaAviso, onCheckedChange = { aceptaAviso = it })
                            Text(
                                "He leído y acepto el aviso de privacidad y los términos de uso.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TertiaryTextButton(
                            text = "Leer aviso de privacidad (versión ${aviso.version})",
                            onClick = { mostrarAviso = true },
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }
                }

                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 8.dp))
                }

                PrimaryButton(
                    text = when {
                        enviando -> "Guardando..."
                        esEdicion -> "Guardar cambios"
                        else -> "Empezar"
                    },
                    enabled = !enviando && aceptaAviso && usernameValido && avisoDisponible,
                    onClick = {
                        enviando = true
                        error = null
                        onGuardar(
                            username,
                            seleccionado.contains("Donar"),
                            seleccionado.contains("Participar"),
                            seleccionado.contains("Proponer"),
                            temasSeleccionados
                        ) { r ->
                            enviando = false
                            r.onSuccess { onListo() }.onFailure { error = mensajeDeError(it) }
                        }
                    },
                    modifier = Modifier.padding(top = if (esEdicion) 24.dp else 0.dp, bottom = 16.dp)
                )
            }
        }
    }

    if (mostrarAviso && aviso != null) {
        AlertDialog(
            onDismissRequest = { mostrarAviso = false },
            title = { Text("Aviso de privacidad") },
            text = {
                Column(modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                    Text(aviso.contenido, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = { aceptaAviso = true; mostrarAviso = false }) { Text("Acepto") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarAviso = false }) { Text("Cerrar") }
            }
        )
    }
}
