package mx.familiasquesuman.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/** Chip de filtro tipo píldora, usado en Explorar y en onboarding (temas de interés).
 * Cuando está seleccionado se rellena por completo (en vez de un tinte al 10%) para que
 * se note claramente cuál filtro está activo, incluso de un vistazo rápido. */
@Composable
fun FilterChip(
    texto: String,
    seleccionado: Boolean,
    modifier: Modifier = Modifier,
    icono: ImageVector? = null,
    onClick: () -> Unit
) {
    val bg = if (seleccionado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerLow
    val fg = if (seleccionado) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    val borderColor = if (seleccionado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant

    Row(
        modifier = modifier
            .selectable(selected = seleccionado, onClick = onClick)
            .background(bg, RoundedCornerShape(percent = 50))
            .border(1.5.dp, borderColor, RoundedCornerShape(percent = 50))
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        if (icono != null) {
            Icon(icono, contentDescription = null, tint = fg, modifier = Modifier.padding(end = 6.dp))
        }
        Text(texto, style = MaterialTheme.typography.labelLarge, color = fg)
    }
}

/** Etiqueta pequeña de estado (ej. "En revisión", "Alta Prioridad", "Urgente"). */
@Composable
fun StatusTag(
    texto: String,
    modifier: Modifier = Modifier,
    containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.tertiaryContainer,
    contentColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onTertiaryContainer
) {
    Row(
        modifier = modifier
            .background(containerColor.copy(alpha = 0.18f), RoundedCornerShape(percent = 50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(texto, style = MaterialTheme.typography.labelSmall, color = contentColor)
    }
}