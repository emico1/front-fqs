package mx.familiasquesuman.app.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import mx.familiasquesuman.app.ui.nav.TabDestino

/** Barra inferior de 4 pestañas: Inicio, Explorar, Participar, Perfil.
 * Íconos más grandes y una "píldora" de selección bien marcada, para que se note
 * claramente en qué sección estás sin tener que leer la etiqueta. */
@Composable
fun AppBottomNavBar(
    pestanaActual: TabDestino,
    onSeleccionar: (TabDestino) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        tonalElevation = 3.dp
    ) {
        TabDestino.entries.forEach { tab ->
            val icono = when (tab) {
                TabDestino.INICIO -> Icons.Filled.Home
                TabDestino.EXPLORAR -> Icons.Filled.Search
                TabDestino.PARTICIPAR -> Icons.Filled.VolunteerActivism
                TabDestino.PERFIL -> Icons.Filled.Person
            }
            val seleccionado = pestanaActual == tab
            NavigationBarItem(
                selected = seleccionado,
                onClick = { onSeleccionar(tab) },
                icon = {
                    Icon(
                        icono,
                        contentDescription = tab.etiqueta,
                        modifier = Modifier.size(26.dp)
                    )
                },
                label = {
                    Text(
                        tab.etiqueta,
                        fontWeight = if (seleccionado) FontWeight.Bold else FontWeight.Medium
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}