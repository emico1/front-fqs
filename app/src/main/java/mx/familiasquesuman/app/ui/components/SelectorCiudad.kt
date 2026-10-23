package mx.familiasquesuman.app.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import mx.familiasquesuman.app.data.Ciudad
import mx.familiasquesuman.app.data.remote.mensajeDeError
import mx.familiasquesuman.app.viewmodel.AlTerminar

/**
 * Ciudad actual con acceso para cambiarla (RF-02). La ciudad se guarda en el perfil del
 * servidor, así se recuerda al volver a abrir la app y en cualquier teléfono, y el
 * contenido de otras ciudades nunca se muestra.
 */
@Composable
fun CiudadSelector(
    ciudadActualId: String?,
    ciudadActualNombre: String?,
    ciudades: List<Ciudad>,
    onCambiar: (ciudadId: String, alTerminar: AlTerminar<Unit>) -> Unit,
    modifier: Modifier = Modifier,
    /** true cuando va encima de un banner azul, entonces se dibuja en blanco */
    sobreAzul: Boolean = false
) {
    val tinta = if (sobreAzul) androidx.compose.ui.graphics.Color.White else MaterialTheme.colorScheme.primary
    val fondo = if (sobreAzul) androidx.compose.ui.graphics.Color.White.copy(alpha = 0.18f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
    val context = LocalContext.current
    var abierto by remember { mutableStateOf(false) }
    var enviando by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .background(fondo, RoundedCornerShape(percent = 50))
            .clickable(onClickLabel = "Cambiar de ciudad") { if (ciudades.isNotEmpty()) abierto = true }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.LocationOn, contentDescription = null, tint = tinta)
        Text(
            ciudadActualNombre ?: "Elige tu ciudad",
            style = MaterialTheme.typography.labelLarge,
            color = tinta,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Icon(Icons.Filled.ExpandMore, contentDescription = null, tint = tinta)
    }

    if (abierto) {
        var elegida by remember { mutableStateOf(ciudadActualId) }
        AlertDialog(
            onDismissRequest = { if (!enviando) abierto = false },
            title = { Text("¿En qué ciudad quieres participar?") },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        "Te mostraremos solo asociaciones, actividades y campañas de esa ciudad. Puedes cambiarla cuando quieras.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    ciudades.forEach { c ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(selected = elegida == c.id, onClick = { elegida = c.id }),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = elegida == c.id, onClick = { elegida = c.id })
                            Text(c.nombre, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !enviando && elegida != null,
                    onClick = {
                        val id = elegida ?: return@TextButton
                        if (id == ciudadActualId) {
                            abierto = false
                            return@TextButton
                        }
                        enviando = true
                        onCambiar(id) { r ->
                            enviando = false
                            r.onSuccess { abierto = false }
                                .onFailure { Toast.makeText(context, mensajeDeError(it), Toast.LENGTH_LONG).show() }
                        }
                    }
                ) { Text(if (enviando) "Guardando..." else "Guardar") }
            },
            dismissButton = {
                TextButton(enabled = !enviando, onClick = { abierto = false }) { Text("Cancelar") }
            }
        )
    }
}
