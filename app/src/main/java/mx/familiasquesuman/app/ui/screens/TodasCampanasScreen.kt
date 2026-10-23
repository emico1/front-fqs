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
import mx.familiasquesuman.app.data.Campana
import mx.familiasquesuman.app.ui.components.BackTopBar
import mx.familiasquesuman.app.ui.components.CampanaResumenCard
import mx.familiasquesuman.app.ui.components.ContenidoCarga
import mx.familiasquesuman.app.ui.components.VacioBloque
import mx.familiasquesuman.app.viewmodel.Carga

/** Lista completa de campañas de donación, a la que llega el "Ver todas" de la sección
 * "Campañas de Donación" en Participar. */
@Composable
fun TodasCampanasScreen(
    campanas: Carga<List<Campana>>,
    onReintentar: () -> Unit,
    onBack: () -> Unit,
    onVerCampana: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        BackTopBar(titulo = "Campañas de Donación", onBack = onBack)
        ContenidoCarga(campanas, onReintentar) { lista ->
            if (lista.isEmpty()) {
                VacioBloque("No hay campañas activas en este momento.")
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(lista, key = { it.id }) { campana ->
                        CampanaResumenCard(campana = campana, onClick = { onVerCampana(campana.id) })
                    }
                }
            }
        }
    }
}
