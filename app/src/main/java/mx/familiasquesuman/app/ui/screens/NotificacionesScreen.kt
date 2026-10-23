package mx.familiasquesuman.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mx.familiasquesuman.app.ui.components.BackTopBar
import mx.familiasquesuman.app.ui.components.GhostBorderCard
import mx.familiasquesuman.app.data.Notificacion

/** Lista de notificaciones respaldada por AppViewModel: tocar una la marca como leída
 * (se refleja de inmediato en el puntito rojo de BrandTopBar en toda la app), y deslizarla
 * hacia cualquier lado la elimina de la lista. */
@Composable
fun NotificacionesScreen(
    notificaciones: List<Notificacion>,
    onBack: () -> Unit,
    onMarcarLeida: (String) -> Unit,
    onEliminar: (String) -> Unit = {},
    onAbrir: (Notificacion) -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxSize()) {
        BackTopBar(titulo = "Notificaciones", onBack = onBack)

        if (notificaciones.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Filled.NotificationsNone, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "No tienes notificaciones por ahora.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(notificaciones, key = { it.id }) { notificacion ->
                    NotificacionDeslizable(
                        notificacion = notificacion,
                        onClick = {
                            if (!notificacion.leida) onMarcarLeida(notificacion.id)
                            // Lleva a la actividad, campaña o propuesta de la notificación
                            onAbrir(notificacion)
                        },
                        onEliminar = { onEliminar(notificacion.id) }
                    )
                }
            }
        }
    }
}

/** Envuelve la tarjeta de una notificación en un SwipeToDismissBox: deslizar en cualquier
 * dirección la borra (se ve un fondo rojo con un ícono de bote de basura mientras se desliza). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificacionDeslizable(
    notificacion: Notificacion,
    onClick: () -> Unit,
    onEliminar: () -> Unit
) {
    val estadoDeslizar = rememberSwipeToDismissBoxState(
        confirmValueChange = { valor ->
            if (valor != SwipeToDismissBoxValue.Settled) {
                onEliminar()
            }
            true
        }
    )

    SwipeToDismissBox(
        state = estadoDeslizar,
        modifier = Modifier.padding(vertical = 1.dp),
        backgroundContent = {
            val alineacion = when (estadoDeslizar.dismissDirection) {
                SwipeToDismissBoxValue.StartToEnd -> Arrangement.Start
                else -> Arrangement.End
            }
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.error, MaterialTheme.shapes.large)
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = alineacion
            ) {
                Icon(Icons.Filled.Delete, contentDescription = "Eliminar notificación", tint = MaterialTheme.colorScheme.onError)
            }
        }
    ) {
        GhostBorderCard(onClick = onClick) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            if (notificacion.leida) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.NotificationsNone,
                        contentDescription = null,
                        tint = if (notificacion.leida) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.padding(start = 12.dp))
                if (!notificacion.leida) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(MaterialTheme.colorScheme.error, CircleShape)
                    )
                    Spacer(modifier = Modifier.padding(start = 4.dp))
                }
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        notificacion.titulo,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (notificacion.leida) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        notificacion.cuerpo,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}