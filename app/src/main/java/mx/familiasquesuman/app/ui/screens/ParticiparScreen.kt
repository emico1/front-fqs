package mx.familiasquesuman.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddReaction
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import mx.familiasquesuman.app.data.ActividadItem
import mx.familiasquesuman.app.data.Campana
import mx.familiasquesuman.app.ui.components.BannerAzul
import mx.familiasquesuman.app.ui.components.BotonBlanco
import mx.familiasquesuman.app.ui.components.TituloSeccion
import mx.familiasquesuman.app.ui.components.ActividadResumenCard
import mx.familiasquesuman.app.ui.components.BrandTopBar
import mx.familiasquesuman.app.ui.components.CampanaResumenCard
import mx.familiasquesuman.app.ui.components.ContenidoCarga
import mx.familiasquesuman.app.ui.components.GhostBorderCard
import mx.familiasquesuman.app.ui.components.PrimaryButton
import mx.familiasquesuman.app.ui.components.TertiaryTextButton
import mx.familiasquesuman.app.ui.components.VacioBloque
import mx.familiasquesuman.app.viewmodel.Carga

/** Basada en participar_familias_que_suman: hub para proponer, ver voluntariados y campañas.
 * Muestra solo un adelanto de cada lista (3 voluntariados, 3 campañas); "Ver todas" manda a
 * TodosVoluntariadosScreen / TodasCampanasScreen con el listado completo. */
@Composable
fun ParticiparScreen(
    actividades: Carga<List<ActividadItem>>,
    campanas: Carga<List<Campana>>,
    onReintentar: () -> Unit,
    onProponerIniciativa: () -> Unit,
    onVerActividad: (String) -> Unit,
    onVerCampana: (String) -> Unit,
    onVerTodosVoluntariados: () -> Unit = {},
    onVerTodasCampanas: () -> Unit = {},
    onMenu: () -> Unit = {},
    onNotificaciones: () -> Unit = {},
    hayNotificacionesSinLeer: Boolean = false
) {
    Column(modifier = Modifier.fillMaxSize()) {
        BrandTopBar(titulo = "Participar", onMenu = onMenu, onNotificaciones = onNotificaciones, hayNotificacionesSinLeer = hayNotificacionesSinLeer)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            BannerAzul {
                Icon(Icons.Filled.AddReaction, contentDescription = null, tint = Color.White)
                Text(
                    "Proponer una asociación o proyecto",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    "¿Conoces una causa que necesite apoyo? Ayúdanos a sumar más familias y crecer nuestra comunidad.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.88f),
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )
                BotonBlanco(texto = "Comenzar propuesta", onClick = onProponerIniciativa)
            }

            SeccionConAccion(
                titulo = "Voluntariados Activos",
                onAccion = onVerTodosVoluntariados,
                modifier = Modifier.padding(top = 28.dp, bottom = 12.dp)
            )
            ContenidoCarga(actividades, onReintentar) { lista ->
                if (lista.isEmpty()) VacioBloque("No hay voluntariados abiertos por ahora.")
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    lista.take(3).forEach { actividad ->
                        ActividadResumenCard(actividad = actividad, onClick = { onVerActividad(actividad.id) })
                    }
                }
            }

            SeccionConAccion(
                titulo = "Campañas de Donación",
                onAccion = onVerTodasCampanas,
                modifier = Modifier.padding(top = 28.dp, bottom = 12.dp)
            )
            ContenidoCarga(campanas, onReintentar) { lista ->
                if (lista.isEmpty()) VacioBloque("No hay campañas activas en este momento.")
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    lista.take(3).forEach { campana ->
                        CampanaResumenCard(campana = campana, onClick = { onVerCampana(campana.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun SeccionConAccion(titulo: String, onAccion: () -> Unit, modifier: Modifier = Modifier) {
    TituloSeccion(titulo = titulo, accion = "Ver todas", onAccion = onAccion, modifier = modifier)
}