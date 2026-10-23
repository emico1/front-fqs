package mx.familiasquesuman.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import mx.familiasquesuman.app.viewmodel.Carga

/** Indicador de carga centrado. */
@Composable
fun CargandoBloque(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 32.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(modifier = Modifier.size(32.dp))
    }
}

/** Mensaje de error con botón para reintentar. */
@Composable
fun ErrorBloque(mensaje: String, onReintentar: (() -> Unit)?, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Filled.CloudOff, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            mensaje,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
        )
        if (onReintentar != null) TertiaryTextButton(text = "Reintentar", onClick = onReintentar)
    }
}

/** Mensaje para listas vacías. */
@Composable
fun VacioBloque(mensaje: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Filled.Inbox, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            mensaje,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

/** Muestra carga, error o el contenido según el estado. */
@Composable
fun <T> ContenidoCarga(
    estado: Carga<T>,
    onReintentar: (() -> Unit)? = null,
    contenido: @Composable (T) -> Unit
) {
    when (estado) {
        is Carga.Cargando -> CargandoBloque()
        is Carga.Error -> ErrorBloque(estado.mensaje, onReintentar)
        is Carga.Listo -> contenido(estado.datos)
    }
}

/**
 * Aviso al pie de la app cuando no hay conexión, cuando hay envíos esperando red o cuando el
 * servidor rechazó un envío hecho sin conexión. Sin nada de eso no ocupa espacio.
 */
@Composable
fun AvisoConexion(
    sinConexionDesde: Long?,
    pendientes: Int,
    rechazado: String?,
    onReintentar: () -> Unit,
    onEntendido: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (sinConexionDesde == null && pendientes == 0 && rechazado == null) return
    val error = rechazado != null
    val icono: ImageVector = when {
        error -> Icons.Filled.ErrorOutline
        sinConexionDesde != null -> Icons.Filled.CloudOff
        else -> Icons.Filled.CloudUpload
    }
    val lineas = buildList {
        if (rechazado != null) add(rechazado)
        if (sinConexionDesde != null) add("Sin conexión. Ves lo guardado el ${formatoHora(sinConexionDesde)}.")
        if (pendientes > 0) {
            add(if (pendientes == 1) "1 envío se mandará al volver la conexión." else "$pendientes envíos se mandarán al volver la conexión.")
        }
    }
    Surface(
        color = if (error) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
        contentColor = if (error) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .semantics { liveRegion = LiveRegionMode.Polite }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icono, contentDescription = null)
            Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
                lineas.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
            when {
                error -> TertiaryTextButton(text = "Entendido", onClick = onEntendido)
                sinConexionDesde != null -> TertiaryTextButton(text = "Reintentar", onClick = onReintentar)
            }
        }
    }
}

private fun formatoHora(milis: Long): String =
    SimpleDateFormat("d 'de' MMM, HH:mm", Locale.forLanguageTag("es-MX")).format(Date(milis))

