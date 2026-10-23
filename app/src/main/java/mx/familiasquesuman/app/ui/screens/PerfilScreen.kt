package mx.familiasquesuman.app.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import mx.familiasquesuman.app.data.Experiencia
import mx.familiasquesuman.app.data.Imagenes
import mx.familiasquesuman.app.data.MiembroComunidad
import mx.familiasquesuman.app.data.Perfil
import mx.familiasquesuman.app.data.remote.mensajeDeError
import mx.familiasquesuman.app.ui.components.BannerAzul
import mx.familiasquesuman.app.ui.components.BrandTopBar
import mx.familiasquesuman.app.ui.components.TituloSeccion
import mx.familiasquesuman.app.ui.components.ContenidoCarga
import mx.familiasquesuman.app.ui.components.GhostBorderCard
import mx.familiasquesuman.app.ui.components.PrimaryButton
import mx.familiasquesuman.app.ui.components.SecondaryButton
import mx.familiasquesuman.app.ui.components.TertiaryTextButton
import mx.familiasquesuman.app.ui.components.UbicacionRow
import mx.familiasquesuman.app.viewmodel.AlTerminar
import mx.familiasquesuman.app.viewmodel.Carga

@Composable
fun PerfilScreen(
    perfil: Perfil,
    comunidad: List<MiembroComunidad>,
    proximas: Carga<List<Experiencia>>,
    pasadas: Carga<List<Experiencia>>,
    onReintentar: () -> Unit,
    onCambiarNombre: (String, AlTerminar<Unit>) -> Unit,
    onCambiarFoto: (Uri, AlTerminar<Unit>) -> Unit,
    onPublicarTestimonio: (inscripcionId: String, texto: String, foto: Uri, alTerminar: AlTerminar<Unit>) -> Unit,
    fondoAnimadoActivo: Boolean = true,
    onCambiarFondoAnimado: (Boolean) -> Unit = {},
    onProponerIniciativa: () -> Unit,
    onMiComunidad: () -> Unit,
    onMisPropuestas: () -> Unit,
    onPreferencias: () -> Unit,
    onHistorial: () -> Unit,
    onPrivacidad: () -> Unit,
    onFavoritos: () -> Unit,
    /** Solo se muestra a cuentas de administración */
    onSeguridadAdmin: () -> Unit = {},
    onSesiones: () -> Unit = {},
    onVerActividad: (String) -> Unit,
    onMenu: () -> Unit = {},
    onNotificaciones: () -> Unit = {},
    hayNotificacionesSinLeer: Boolean = false
) {
    val context = LocalContext.current
    var mostrarEditarPerfil by remember { mutableStateOf(false) }
    var nombreEditado by remember { mutableStateOf(perfil.nombre) }
    var subiendoFoto by remember { mutableStateOf(false) }
    var mostrarPublicar by remember { mutableStateOf(false) }

    val selectorFoto = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            subiendoFoto = true
            onCambiarFoto(uri) { r ->
                subiendoFoto = false
                r.onFailure { Toast.makeText(context, mensajeDeError(it), Toast.LENGTH_LONG).show() }
            }
        }
    }

    if (mostrarEditarPerfil) {
        AlertDialog(
            onDismissRequest = { mostrarEditarPerfil = false },
            title = { Text("Editar perfil") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = nombreEditado,
                        onValueChange = { nombreEditado = it.take(120) },
                        label = { Text("Nombre del perfil") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    SecondaryButton(
                        text = "Cambiar foto",
                        onClick = {
                            selectorFoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                    )
                    Text(
                        text = "La foto se sube al seleccionarla. Para guardar el nombre, pulsa Guardar.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = nombreEditado.trim().length >= 2,
                    onClick = {
                        mostrarEditarPerfil = false
                        onCambiarNombre(nombreEditado) { r ->
                            r.onFailure { Toast.makeText(context, mensajeDeError(it), Toast.LENGTH_LONG).show() }
                        }
                    }
                ) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarEditarPerfil = false }) { Text("Cancelar") }
            }
        )
    }

    if (mostrarPublicar) {
        val elegibles = (pasadas as? Carga.Listo)?.datos?.filter { it.puedePublicarTestimonio }.orEmpty()
        PublicarExperienciaDialogo(
            elegibles = elegibles,
            onCerrar = { mostrarPublicar = false },
            onPublicar = { inscripcionId, texto, foto, alTerminar ->
                onPublicarTestimonio(inscripcionId, texto, foto) { r ->
                    alTerminar(r)
                    r.onSuccess {
                        mostrarPublicar = false
                        Toast.makeText(context, "¡Gracias! Tu experiencia se publicará cuando el equipo la revise.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        BrandTopBar(
            titulo = "Perfil",
            onMenu = onMenu,
            onNotificaciones = onNotificaciones,
            hayNotificacionesSinLeer = hayNotificacionesSinLeer
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            BannerAzul {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.2f))
                            .border(3.dp, androidx.compose.ui.graphics.Color.White, CircleShape)
                            .clickable(onClickLabel = "Cambiar foto de perfil") {
                                selectorFoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            subiendoFoto -> CircularProgressIndicator(modifier = Modifier.size(32.dp), color = androidx.compose.ui.graphics.Color.White)
                            perfil.fotoUrl != null -> AsyncImage(
                                model = perfil.fotoUrl,
                                contentDescription = "Foto de perfil",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            else -> Text(
                                text = perfil.nombre.trim()
                                    .split(Regex("\\s+"))
                                    .take(2)
                                    .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                                    .joinToString(""),
                                style = MaterialTheme.typography.headlineMedium,
                                color = androidx.compose.ui.graphics.Color.White
                            )
                        }
                    }

                    Text(
                        text = perfil.nombre,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = androidx.compose.ui.graphics.Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    Text(
                        text = "@${perfil.username}",
                        style = MaterialTheme.typography.bodySmall,
                        color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.85f)
                    )
                    Text(
                        text = perfil.nivel?.let { "Nivel $it · ${perfil.participaciones} participaciones" }
                            ?: "${perfil.participaciones} participaciones",
                        style = MaterialTheme.typography.bodySmall,
                        color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center
                    )

                    TextButton(
                        colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = androidx.compose.ui.graphics.Color.White),
                        onClick = {
                            nombreEditado = perfil.nombre
                            mostrarEditarPerfil = true
                        }
                    ) {
                        Icon(imageVector = Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(text = "Editar perfil", modifier = Modifier.padding(start = 6.dp))
                    }
                }
            }

            if (perfil.insignias.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    perfil.insignias.take(3).forEach { insignia ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                    MaterialTheme.shapes.small
                                )
                                .padding(horizontal = 6.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = insignia.nombre,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Surface(
                onClick = onMiComunidad,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Mi Comunidad", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        Text(
                            text = "Gestiona los miembros de tu círculo",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(imageVector = Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }

            PrimaryButton(
                text = "Proponer una iniciativa",
                onClick = onProponerIniciativa,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            )

            FilaEnlace(titulo = "Mis Propuestas", subtitulo = "Revisa el estado de tus iniciativas", onClick = onMisPropuestas)
            FilaEnlace(titulo = "Preferencias", subtitulo = "Cómo te gustaría involucrarte", onClick = onPreferencias)
            FilaEnlace(titulo = "Mis favoritos", subtitulo = "Asociaciones y actividades que guardaste", onClick = onFavoritos)
            FilaEnlace(titulo = "Mi historial", subtitulo = "Todas tus participaciones, mes por mes", onClick = onHistorial)
            FilaEnlace(
                titulo = "Privacidad y datos",
                subtitulo = if (perfil.avisoPendiente) "Hay un aviso de privacidad nuevo por revisar" else "Aviso de privacidad y eliminación de tu cuenta",
                onClick = onPrivacidad
            )
            FilaEnlace(
                titulo = "Mis dispositivos",
                subtitulo = "Dónde tienes la sesión abierta, ciérrala si no reconoces alguno",
                onClick = onSesiones
            )
            if (perfil.esAdmin) {
                FilaEnlace(
                    titulo = "Seguridad de administración",
                    subtitulo = "Verificación en dos pasos con tu app de autenticación",
                    onClick = onSeguridadAdmin
                )
            }

            EncabezadoSeccion(titulo = "Apariencia", modifier = Modifier.padding(top = 24.dp, bottom = 8.dp))

            GhostBorderCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Fondo animado", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Formas de color moviéndose de fondo en las pantallas principales",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = fondoAnimadoActivo,
                        onCheckedChange = onCambiarFondoAnimado,
                        colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary)
                    )
                }
            }

            EncabezadoSeccion(titulo = "Próximas Actividades", modifier = Modifier.padding(top = 24.dp, bottom = 8.dp))
            ContenidoCarga(proximas, onReintentar) { lista ->
                if (lista.isEmpty()) {
                    Text(
                        "No tienes actividades próximas. Explora los voluntariados en Participar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    lista.take(3).forEach { exp ->
                        GhostBorderCard(onClick = { onVerActividad(exp.actividadId) }) {
                            Text(text = exp.titulo, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = exp.fecha,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            UbicacionRow(texto = exp.ubicacion)
                        }
                    }
                }
            }

            EncabezadoSeccion(titulo = "Experiencias Pasadas", onVerTodo = onHistorial, modifier = Modifier.padding(top = 24.dp, bottom = 8.dp))
            ContenidoCarga(pasadas, onReintentar) { lista ->
                val asistidas = lista.filter { it.asistio }
                if (asistidas.isEmpty()) {
                    Text(
                        "Aquí aparecerán las actividades en las que participes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    asistidas.take(5).forEach { exp ->
                        GhostBorderCard {
                            Text(text = exp.titulo, style = MaterialTheme.typography.bodyMedium)
                            Text(text = exp.fecha, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            when (exp.estadoTestimonio) {
                                "pendiente" -> Text("Testimonio en revisión", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                                "aprobado" -> Text("Testimonio publicado", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                else -> Unit
                            }
                        }
                    }
                }
                if (lista.any { it.puedePublicarTestimonio }) {
                    SecondaryButton(
                        text = "Publicar nueva experiencia",
                        onClick = { mostrarPublicar = true },
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }

            EncabezadoSeccion(titulo = "Comunidad", onVerTodo = onMiComunidad, modifier = Modifier.padding(top = 24.dp, bottom = 8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 16.dp)) {
                comunidad.take(5).forEach { miembro -> MiembroBurbuja(miembro) }
            }
        }
    }
}

/** Elegir una actividad a la que se asistió, escribir la experiencia y adjuntar una foto. */
@Composable
private fun PublicarExperienciaDialogo(
    elegibles: List<Experiencia>,
    onCerrar: () -> Unit,
    onPublicar: (String, String, Uri, AlTerminar<Unit>) -> Unit
) {
    var elegida by remember { mutableStateOf(elegibles.firstOrNull()) }
    var texto by remember { mutableStateOf("") }
    var foto by remember { mutableStateOf<Uri?>(null) }
    var enviando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val selector = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) foto = uri }
    // Foto con la cámara del teléfono (RF-12). La app de cámara la guarda en un archivo
    // temporal propio, sin que esta app pida permiso de cámara.
    val context = LocalContext.current
    var fotoCamara by remember { mutableStateOf<Uri?>(null) }
    val camara = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) foto = fotoCamara
    }

    AlertDialog(
        onDismissRequest = { if (!enviando) onCerrar() },
        title = { Text("Publicar experiencia") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                elegibles.forEach { e ->
                    Row(
                        modifier = Modifier.fillMaxWidth().selectable(selected = elegida == e, onClick = { elegida = e }),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = elegida == e, onClick = { elegida = e })
                        Text(e.titulo, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                OutlinedTextField(
                    value = texto,
                    onValueChange = { texto = it.take(1500) },
                    label = { Text("Cuéntanos cómo te fue") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
                SecondaryButton(
                    text = "Tomar foto",
                    onClick = {
                        runCatching {
                            val destino = Imagenes.nuevaFotoTemporal(context)
                            fotoCamara = destino
                            camara.launch(destino)
                        }.onFailure { error = "No se encontró una app de cámara, elige una foto de tu galería." }
                    },
                    modifier = Modifier.padding(top = 8.dp)
                )
                SecondaryButton(
                    text = if (foto == null) "Elegir de la galería" else "Cambiar foto",
                    onClick = { selector.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier.padding(top = 8.dp)
                )
                foto?.let {
                    AsyncImage(
                        model = it,
                        contentDescription = "Foto elegida",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.padding(top = 8.dp).fillMaxWidth().aspectRatio(16f / 9f)
                    )
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !enviando && elegida != null && texto.trim().length >= 10 && foto != null,
                onClick = {
                    val e = elegida ?: return@TextButton
                    val f = foto ?: return@TextButton
                    enviando = true
                    error = null
                    onPublicar(e.inscripcionId, texto, f) { r ->
                        enviando = false
                        r.onFailure { error = mensajeDeError(it) }
                    }
                }
            ) { Text(if (enviando) "Publicando..." else "Publicar") }
        },
        dismissButton = { TextButton(onClick = onCerrar, enabled = !enviando) { Text("Cancelar") } }
    )
}

@Composable
private fun EncabezadoSeccion(
    titulo: String,
    onVerTodo: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    TituloSeccion(
        titulo = titulo,
        accion = if (onVerTodo != null) "Ver todo" else null,
        onAccion = { onVerTodo?.invoke() },
        modifier = modifier
    )
}

@Composable
private fun FilaEnlace(
    titulo: String,
    subtitulo: String,
    onClick: () -> Unit
) {
    GhostBorderCard(
        onClick = onClick,
        modifier = Modifier.padding(top = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = titulo, style = MaterialTheme.typography.titleMedium)
                Text(text = subtitulo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(imageVector = Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MiembroBurbuja(miembro: MiembroComunidad) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = miembro.iniciales, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        }
        Text(text = miembro.nombre, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
    }
}
