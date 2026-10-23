package mx.familiasquesuman.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import mx.familiasquesuman.app.data.Asociacion
import mx.familiasquesuman.app.data.remote.mensajeDeError
import mx.familiasquesuman.app.ui.components.TituloSeccion
import mx.familiasquesuman.app.ui.components.BackTopBar
import mx.familiasquesuman.app.ui.components.ContenidoCarga
import mx.familiasquesuman.app.ui.components.GhostBorderCard
import mx.familiasquesuman.app.ui.components.SecondaryButton
import mx.familiasquesuman.app.ui.components.StatusTag
import mx.familiasquesuman.app.ui.components.TertiaryTextButton
import mx.familiasquesuman.app.ui.components.UbicacionRow
import mx.familiasquesuman.app.ui.theme.colorAcento
import mx.familiasquesuman.app.viewmodel.AlTerminar
import mx.familiasquesuman.app.viewmodel.Carga

/** Basada en perfil_de_asociacion_azul, con datos reales de la asociación. */
@Composable
fun AsociacionDetalleScreen(
    estado: Carga<Asociacion>,
    onReintentar: () -> Unit,
    onBack: () -> Unit,
    onVerActividad: (String) -> Unit,
    onCambiarNotificaciones: (activas: Boolean, alTerminar: AlTerminar<Unit>) -> Unit,
    onReportar: (asociacionId: String) -> Unit
) {
    val titulo = (estado as? Carga.Listo)?.datos?.nombre ?: "Asociación"
    Column(modifier = Modifier.fillMaxSize()) {
        BackTopBar(titulo = titulo, onBack = onBack)
        ContenidoCarga(estado, onReintentar) { asociacion ->
            DetalleAsociacion(asociacion, onVerActividad, onCambiarNotificaciones, onReportar)
        }
    }
}

@Composable
private fun DetalleAsociacion(
    asociacion: Asociacion,
    onVerActividad: (String) -> Unit,
    onCambiarNotificaciones: (Boolean, AlTerminar<Unit>) -> Unit,
    onReportar: (String) -> Unit
) {
    val context = LocalContext.current
    var enviando by remember { mutableStateOf(false) }
    // Paso intermedio antes de desactivar los avisos, así un toque accidental no los apaga
    var confirmandoDesactivar by remember { mutableStateOf(false) }
    LaunchedEffect(confirmandoDesactivar) {
        if (confirmandoDesactivar) {
            delay(4_000)
            confirmandoDesactivar = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        AsyncImage(
            model = asociacion.fotoUrl,
            contentDescription = asociacion.nombre,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        )

        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusTag(
                    texto = asociacion.categoria.etiqueta,
                    containerColor = asociacion.categoria.colorAcento(),
                    contentColor = asociacion.categoria.colorAcento()
                )
                if (asociacion.verificada) {
                    StatusTag(texto = "✓ Verificado", containerColor = MaterialTheme.colorScheme.secondary, contentColor = MaterialTheme.colorScheme.secondary)
                }
            }
            Text(asociacion.nombre, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
            UbicacionRow(texto = asociacion.ubicacion)
            // Calificaciones de familias que asistieron a sus actividades (RF21)
            val votos = asociacion.meGusta + asociacion.noMeGusta
            Text(
                if (votos == 0) "Aún sin calificaciones de familias"
                else "A ${asociacion.meGusta} de $votos familias que participaron les gustó",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
            if (asociacion.familiasBeneficiadas > 0) {
                Text(
                    "Más de ${asociacion.familiasBeneficiadas} familias beneficiadas",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                asociacion.telefono?.let { tel ->
                    AccionContacto(
                        icono = Icons.Filled.Call,
                        texto = "Llamar",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(tel)))
                            runCatching { context.startActivity(intent) }
                                .onFailure { Toast.makeText(context, "No se encontró una app para llamar.", Toast.LENGTH_SHORT).show() }
                        }
                    )
                }
                asociacion.whatsapp?.let { wa ->
                    AccionContacto(
                        icono = Icons.AutoMirrored.Filled.Chat,
                        texto = "WhatsApp",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val numero = wa.filter { it.isDigit() }
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$numero"))
                            runCatching { context.startActivity(intent) }
                                .onFailure { Toast.makeText(context, "No se pudo abrir WhatsApp.", Toast.LENGTH_SHORT).show() }
                        }
                    )
                }
                asociacion.correo?.let { correo ->
                    AccionContacto(
                        icono = Icons.Filled.Mail,
                        texto = "Correo",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:" + Uri.encode(correo))
                                putExtra(Intent.EXTRA_SUBJECT, "Contacto desde Familias que Suman")
                            }
                            runCatching { context.startActivity(intent) }
                                .onFailure { Toast.makeText(context, "No se encontró una app de correo.", Toast.LENGTH_SHORT).show() }
                        }
                    )
                }
            }

            TituloSeccion("Nuestra Misión", modifier = Modifier.padding(top = 28.dp, bottom = 8.dp))
            Text(asociacion.mision, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            if (asociacion.necesidades.isNotBlank()) {
                TituloSeccion("Necesidades Actuales", modifier = Modifier.padding(top = 28.dp, bottom = 8.dp))
                NecesidadCard(Icons.Filled.VolunteerActivism, "Lo que más necesitan", asociacion.necesidades, Modifier.fillMaxWidth())
            }

            TituloSeccion("Próximas Actividades", modifier = Modifier.padding(top = 28.dp, bottom = 8.dp))
            if (asociacion.proximasActividades.isEmpty()) {
                Text("Por ahora no tiene actividades programadas.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                asociacion.proximasActividades.forEach { act ->
                    GhostBorderCard(onClick = { onVerActividad(act.id) }) {
                        Text("${act.dia} ${act.mes} · ${act.titulo}", style = MaterialTheme.typography.bodyMedium)
                        Text(act.fechaHoraCompleta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TertiaryTextButton(text = "Inscribirse", onClick = { onVerActividad(act.id) }, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }

            Column(modifier = Modifier.padding(top = 28.dp)) {
                SecondaryButton(
                    text = when {
                        !asociacion.notificacionesActivas -> "Activar notificaciones"
                        confirmandoDesactivar -> "¿Desactivar notificaciones?"
                        else -> "Notificaciones activas ✓"
                    },
                    onClick = {
                        if (enviando) return@SecondaryButton
                        // Activas y sin confirmar, el primer toque solo pregunta
                        if (asociacion.notificacionesActivas && !confirmandoDesactivar) {
                            confirmandoDesactivar = true
                            return@SecondaryButton
                        }
                        confirmandoDesactivar = false
                        enviando = true
                        val activar = !asociacion.notificacionesActivas
                        onCambiarNotificaciones(activar) { r ->
                            enviando = false
                            val mensaje = r.fold(
                                onSuccess = {
                                    if (activar) "Te avisaremos de nuevas actividades de ${asociacion.nombre}."
                                    else "Notificaciones desactivadas para ${asociacion.nombre}."
                                },
                                onFailure = { mensajeDeError(it) }
                            )
                            Toast.makeText(context, mensaje, Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                if (confirmandoDesactivar) {
                    Text(
                        "Toca otra vez para dejar de recibir avisos de ${asociacion.nombre}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
                Row(modifier = Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SecondaryButton(
                        text = "Compartir perfil",
                        onClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "Conoce a ${asociacion.nombre} en Familias que Suman. ${asociacion.descripcion}")
                            }
                            runCatching { context.startActivity(Intent.createChooser(intent, "Compartir perfil")) }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                TertiaryTextButton(
                    text = "Reportar asociación",
                    onClick = { onReportar(asociacion.id) },
                    modifier = Modifier.padding(top = 16.dp, bottom = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun AccionContacto(
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    texto: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape)
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icono, contentDescription = texto, tint = MaterialTheme.colorScheme.primary)
        }
        Text(texto, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
private fun NecesidadCard(icono: androidx.compose.ui.graphics.vector.ImageVector, titulo: String, descripcion: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f), MaterialTheme.shapes.medium)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.16f), CircleShape)
                .padding(10.dp)
        ) {
            Icon(icono, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
        }
        Text(titulo, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 10.dp))
        Text(descripcion, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
