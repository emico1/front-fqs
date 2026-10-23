package mx.familiasquesuman.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import mx.familiasquesuman.app.ui.nav.Routes
import mx.familiasquesuman.app.ui.nav.TabDestino
import mx.familiasquesuman.app.ui.theme.CategoriaAzul
import mx.familiasquesuman.app.ui.theme.CategoriaCafe
import mx.familiasquesuman.app.ui.theme.CategoriaMorado
import mx.familiasquesuman.app.ui.theme.CategoriaNaranja
import mx.familiasquesuman.app.ui.theme.CategoriaRosa
import mx.familiasquesuman.app.ui.theme.CategoriaVerde

private data class OpcionMenu(val ruta: String, val etiqueta: String, val icono: ImageVector, val color: Color)

/**
 * Menú lateral con el estilo de Bienvenida. Arriba un encabezado azul con la persona y su
 * ciudad, luego las cuatro pestañas principales, después accesos a lo que antes solo
 * estaba dentro del Perfil, y al final Cerrar sesión en rojo para que no se toque por error.
 */
@Composable
fun AppDrawerContent(
    rutaActual: String?,
    onNavegar: (String) -> Unit,
    onCerrarSesion: () -> Unit,
    nombre: String? = null,
    ciudad: String? = null,
    /** Para pantallas secundarias, se abren encima sin cambiar de pestaña */
    onAbrir: (String) -> Unit = onNavegar
) {
    val principales = listOf(
        OpcionMenu(TabDestino.INICIO.ruta, "Inicio", Icons.Filled.Home, CategoriaAzul),
        OpcionMenu(TabDestino.EXPLORAR.ruta, "Explorar", Icons.Filled.Search, CategoriaMorado),
        OpcionMenu(TabDestino.PARTICIPAR.ruta, "Participar", Icons.Filled.VolunteerActivism, CategoriaVerde),
        OpcionMenu(TabDestino.PERFIL.ruta, "Perfil", Icons.Filled.Person, CategoriaNaranja)
    )
    val secundarias = listOf(
        OpcionMenu(Routes.FAVORITOS, "Mis favoritos", Icons.Filled.Favorite, CategoriaRosa),
        OpcionMenu(Routes.HISTORIAL, "Mi historial", Icons.Filled.History, CategoriaCafe),
        OpcionMenu(Routes.MIS_PROPUESTAS, "Mis propuestas", Icons.Filled.TipsAndUpdates, CategoriaNaranja),
        OpcionMenu(Routes.HISTORIAS, "Historias de la comunidad", Icons.Filled.AutoStories, CategoriaMorado),
        OpcionMenu(Routes.NOTIFICACIONES, "Notificaciones", Icons.Filled.Notifications, CategoriaAzul),
        OpcionMenu(Routes.PRIVACIDAD, "Privacidad y datos", Icons.Filled.PrivacyTip, CategoriaVerde)
    )

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        Encabezado(nombre = nombre, ciudad = ciudad)

        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)) {
            principales.forEach { o -> Fila(o, seleccionada = rutaActual == o.ruta, onClick = { onNavegar(o.ruta) }) }

            Text(
                "Tu actividad",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 6.dp)
            )
            secundarias.forEach { o -> Fila(o, seleccionada = rutaActual == o.ruta, onClick = { onAbrir(o.ruta) }) }

            Box(
                modifier = Modifier
                    .padding(top = 20.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                    .clickable(onClick = onCerrarSesion)
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Text(
                        "Cerrar sesión",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
            }
        }
    }
}

/** Bloque azul con iniciales, nombre y ciudad. Llega hasta detrás de la barra de estado. */
@Composable
private fun Encabezado(nombre: String?, ciudad: String?) {
    val iniciales = nombre?.trim()?.split(Regex("\\s+"))?.take(2)?.mapNotNull { it.firstOrNull()?.uppercaseChar() }?.joinToString("")
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(degradadoMarca(), RoundedCornerShape(bottomEnd = 32.dp))
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 40.dp, y = (-30).dp)
                .size(140.dp)
                .background(Color.White.copy(alpha = 0.08f), CircleShape)
        )
        Column(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(Color.White.copy(alpha = 0.2f), CircleShape)
                    .border(2.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (iniciales.isNullOrEmpty()) "FS" else iniciales,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Text(
                nombre ?: "Familias que Suman",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                modifier = Modifier.padding(top = 12.dp)
            )
            if (ciudad != null) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Icon(Icons.Filled.LocationOn, contentDescription = null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(16.dp))
                    Text(
                        ciudad,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }
    }
}

/** Opción del menú con su ícono dentro de un círculo de color. La elegida se resalta. */
@Composable
private fun Fila(o: OpcionMenu, seleccionada: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (seleccionada) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(if (seleccionada) o.color else o.color.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(o.icono, contentDescription = null, tint = if (seleccionada) Color.White else o.color, modifier = Modifier.size(20.dp))
        }
        Text(
            o.etiqueta,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = if (seleccionada) FontWeight.Bold else FontWeight.Medium,
            color = if (seleccionada) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 14.dp)
        )
    }
}
