package mx.familiasquesuman.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Barra superior estándar con botón "menú" a la izquierda (pantallas principales). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrandTopBar(
    titulo: String = "Familias que Suman",
    onMenu: (() -> Unit)? = null,
    onNotificaciones: (() -> Unit)? = null,
    hayNotificacionesSinLeer: Boolean = false
) {
    TopAppBar(
        title = { Text(titulo, style = MaterialTheme.typography.titleLarge) },
        navigationIcon = {
            IconButton(onClick = { onMenu?.invoke() }) {
                Icon(Icons.Filled.Menu, contentDescription = "Menú")
            }
        },
        actions = {
            if (onNotificaciones != null) {
                Box {
                    IconButton(onClick = onNotificaciones) {
                        Icon(Icons.Filled.Notifications, contentDescription = "Notificaciones")
                    }
                    if (hayNotificacionesSinLeer) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = (-10).dp, y = 10.dp)
                                .size(8.dp)
                                .background(MaterialTheme.colorScheme.error, CircleShape)
                        )
                    }
                }
            }
        },
        // Transparente a propósito: así las formas de fondo pasan "por detrás" de la barra
        // sin que se note un corte/línea recta donde antes chocaban contra un rectángulo
        // opaco. El título y los íconos siguen siendo oscuros, así que se siguen leyendo bien.
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            scrolledContainerColor = androidx.compose.ui.graphics.Color.Transparent
        )
    )
}

/** Barra superior con flecha de regreso, para pantallas de detalle. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackTopBar(
    titulo: String,
    onBack: () -> Unit
) {
    CenterAlignedTopAppBar(
        title = { Text(titulo, style = MaterialTheme.typography.titleLarge) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            scrolledContainerColor = androidx.compose.ui.graphics.Color.Transparent
        )
    )
}