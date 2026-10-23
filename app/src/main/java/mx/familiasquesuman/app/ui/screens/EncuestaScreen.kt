package mx.familiasquesuman.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import mx.familiasquesuman.app.data.Encuesta
import mx.familiasquesuman.app.data.RespuestaEncuesta
import mx.familiasquesuman.app.data.remote.mensajeDeError
import mx.familiasquesuman.app.ui.components.BackTopBar
import mx.familiasquesuman.app.ui.components.ContenidoCarga
import mx.familiasquesuman.app.ui.components.GhostBorderCard
import mx.familiasquesuman.app.ui.components.PrimaryButton
import mx.familiasquesuman.app.ui.components.VacioBloque
import mx.familiasquesuman.app.viewmodel.AlTerminar
import mx.familiasquesuman.app.viewmodel.Carga

/**
 * Encuestas de una actividad (RF-13). La previa se responde antes de que termine la
 * actividad y la posterior cuando el equipo ya registró la asistencia. Si alguien
 * intenta responder la posterior sin haber asistido, el backend lo rechaza y aquí se
 * muestra su mensaje (error diseñado de CU-02).
 */
@Composable
fun EncuestaScreen(
    estado: Carga<List<Encuesta>>,
    onReintentar: () -> Unit,
    onBack: () -> Unit,
    onEnviar: (respuestas: List<RespuestaEncuesta>, alTerminar: AlTerminar<Unit>) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        BackTopBar(titulo = "Encuestas", onBack = onBack)
        ContenidoCarga(estado, onReintentar) { lista ->
            if (lista.isEmpty()) {
                VacioBloque("Esta actividad no tiene encuestas.")
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    lista.sortedByDescending { it.esPrevia }.forEach { e -> FormularioEncuesta(e, onEnviar) }
                }
            }
        }
    }
}

@Composable
private fun FormularioEncuesta(encuesta: Encuesta, onEnviar: (List<RespuestaEncuesta>, AlTerminar<Unit>) -> Unit) {
    val context = LocalContext.current
    // Respuestas en memoria, por id de pregunta
    var opciones by remember(encuesta.id) { mutableStateOf(mapOf<String, String>()) }
    var textos by remember(encuesta.id) { mutableStateOf(mapOf<String, String>()) }
    var enviando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val pendientes = encuesta.preguntas.filter { !it.respondida }
    val completas = pendientes.filter { it.obligatoria }.all { p ->
        if (p.esOpcionUnica) opciones[p.id] != null else !textos[p.id].isNullOrBlank()
    }

    GhostBorderCard {
        Text(
            if (encuesta.esPrevia) "Antes de la actividad" else "Después de la actividad",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text(encuesta.titulo, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)

        if (pendientes.isEmpty()) {
            Row(modifier = Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Ya respondiste esta encuesta, ¡gracias!", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 8.dp))
            }
            return@GhostBorderCard
        }

        pendientes.forEachIndexed { i, p ->
            Text(
                "${i + 1}. ${p.texto}" + if (p.obligatoria) "" else " (opcional)",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
            )
            if (p.esOpcionUnica) {
                p.opciones.forEach { o ->
                    val elegida = opciones[p.id] == o.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(selected = elegida, onClick = { opciones = opciones + (p.id to o.id) }),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = elegida, onClick = { opciones = opciones + (p.id to o.id) })
                        Text(o.texto, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else {
                OutlinedTextField(
                    value = textos[p.id].orEmpty(),
                    onValueChange = { v -> textos = textos + (p.id to v.take(2000)) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp)) }

        PrimaryButton(
            text = if (enviando) "Enviando..." else "Enviar respuestas",
            enabled = !enviando && completas,
            onClick = {
                val respuestas = pendientes.mapNotNull { p ->
                    if (p.esOpcionUnica) opciones[p.id]?.let { RespuestaEncuesta(p.id, opcionId = it) }
                    else textos[p.id]?.trim()?.takeIf { it.isNotEmpty() }?.let { RespuestaEncuesta(p.id, texto = it) }
                }
                enviando = true
                error = null
                onEnviar(respuestas) { r ->
                    enviando = false
                    r.onSuccess { Toast.makeText(context, "¡Gracias por responder!", Toast.LENGTH_SHORT).show() }
                        .onFailure { error = mensajeDeError(it) }
                }
            },
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}
