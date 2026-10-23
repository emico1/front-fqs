package mx.familiasquesuman.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mx.familiasquesuman.app.data.ActividadItem
import mx.familiasquesuman.app.ui.components.ActividadResumenCard
import mx.familiasquesuman.app.ui.components.BackTopBar
import mx.familiasquesuman.app.ui.components.ContenidoCarga
import mx.familiasquesuman.app.ui.components.VacioBloque
import mx.familiasquesuman.app.viewmodel.Carga

/** Lista completa de voluntariados/actividades activas, a la que llega el "Ver todas" de
 * la sección "Voluntariados Activos" en Participar. */
@Composable
fun TodosVoluntariadosScreen(
    actividades: Carga<List<ActividadItem>>,
    onReintentar: () -> Unit,
    onBack: () -> Unit,
    onVerActividad: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        BackTopBar(titulo = "Voluntariados Activos", onBack = onBack)
        ContenidoCarga(actividades, onReintentar) { lista ->
            if (lista.isEmpty()) {
                VacioBloque("No hay voluntariados abiertos por ahora.")
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(lista, key = { it.id }) { actividad ->
                        ActividadResumenCard(actividad = actividad, onClick = { onVerActividad(actividad.id) })
                    }
                }
            }
        }
    }
}
