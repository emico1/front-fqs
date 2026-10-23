package mx.familiasquesuman.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import mx.familiasquesuman.app.data.Privacidad
import mx.familiasquesuman.app.data.remote.mensajeDeError
import mx.familiasquesuman.app.ui.components.TituloSeccion
import mx.familiasquesuman.app.ui.components.BackTopBar
import mx.familiasquesuman.app.ui.components.ContenidoCarga
import mx.familiasquesuman.app.ui.components.GhostBorderCard
import mx.familiasquesuman.app.ui.components.PrimaryButton
import mx.familiasquesuman.app.ui.components.SecondaryButton
import mx.familiasquesuman.app.viewmodel.AlTerminar
import mx.familiasquesuman.app.viewmodel.Carga

/**
 * Privacidad y datos (RF-15). Muestra el aviso vigente en lenguaje claro, si ya se
 * aceptó, y permite pedir que se borre la cuenta. La eliminación pide la contraseña
 * para que nadie con el teléfono desbloqueado la pueda pedir por la persona.
 */
@Composable
fun PrivacidadScreen(
    estado: Carga<Privacidad>,
    onReintentar: () -> Unit,
    onBack: () -> Unit,
    onAceptar: (avisoId: String, alTerminar: AlTerminar<Unit>) -> Unit,
    onSolicitarEliminacion: (password: String, alTerminar: AlTerminar<Unit>) -> Unit,
    onCuentaEliminada: () -> Unit
) {
    val context = LocalContext.current
    var aceptando by remember { mutableStateOf(false) }
    var mostrarEliminar by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        BackTopBar(titulo = "Privacidad y datos", onBack = onBack)
        ContenidoCarga(estado, onReintentar) { p ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                TituloSeccion("En pocas palabras")
                listOf(
                    "Usamos tus datos solo para que puedas inscribirte a actividades, donar y proponer iniciativas.",
                    "Las asociaciones ven el nombre de quienes asisten a sus actividades, nada más.",
                    "Tus fotos solo se publican si tú las compartes y el equipo las aprueba.",
                    "No vendemos ni compartimos tus datos con terceros.",
                    "Puedes pedir que borremos tu cuenta en cualquier momento desde esta pantalla."
                ).forEach { punto ->
                    Row(modifier = Modifier.padding(top = 8.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(punto, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 8.dp))
                    }
                }

                TituloSeccion(
                    "Aviso de privacidad",
                    modifier = Modifier.padding(top = 28.dp, bottom = 8.dp)
                )
                val aviso = p.aviso
                if (aviso == null) {
                    Text("Todavía no hay un aviso publicado.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    Text(
                        "Versión ${aviso.version}" + (p.publicadoEn?.let { ", publicada el $it" } ?: ""),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    GhostBorderCard(modifier = Modifier.padding(top = 8.dp)) {
                        Text(aviso.contenido, style = MaterialTheme.typography.bodySmall)
                    }
                    if (p.aceptado) {
                        Row(modifier = Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                "Aceptaste esta versión" + (p.aceptadoEn?.let { " el $it" } ?: ""),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    } else {
                        Row(modifier = Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                            Text(
                                "Actualizamos el aviso. Léelo y acéptalo para seguir usando la app con normalidad.",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                        PrimaryButton(
                            text = if (aceptando) "Guardando..." else "Leí y acepto el aviso",
                            enabled = !aceptando,
                            onClick = {
                                aceptando = true
                                onAceptar(aviso.id) { r ->
                                    aceptando = false
                                    r.onFailure { Toast.makeText(context, mensajeDeError(it), Toast.LENGTH_LONG).show() }
                                }
                            },
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                }

                TituloSeccion(
                    "Eliminar mi cuenta",
                    modifier = Modifier.padding(top = 28.dp, bottom = 8.dp)
                )
                Text(
                    "Si la pides, tu cuenta se desactiva de inmediato y el equipo borra tus datos personales y tus fotos. " +
                        "Lo que ya se publicó de forma anónima, como el conteo de participaciones de una actividad, se conserva sin tu nombre.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (p.solicitudEliminacion != null) {
                    Text(p.solicitudEliminacion, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 12.dp))
                } else {
                    SecondaryButton(
                        text = "Solicitar la eliminación de mi cuenta",
                        onClick = { mostrarEliminar = true },
                        modifier = Modifier.padding(top = 12.dp, bottom = 24.dp)
                    )
                }
            }
        }
    }

    if (mostrarEliminar) {
        var password by remember { mutableStateOf("") }
        var enviando by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { if (!enviando) mostrarEliminar = false },
            title = { Text("¿Eliminar tu cuenta?") },
            text = {
                Column {
                    Text(
                        "Se cerrará tu sesión en todos tus dispositivos y no podrás volver a entrar con esta cuenta. Escribe tu contraseña para confirmar.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it.take(128); error = null },
                        label = { Text("Contraseña") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        isError = error != null,
                        supportingText = { error?.let { Text(it) } },
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !enviando && password.isNotEmpty(),
                    onClick = {
                        enviando = true
                        onSolicitarEliminacion(password) { r ->
                            enviando = false
                            r.onSuccess {
                                mostrarEliminar = false
                                Toast.makeText(context, "Recibimos tu solicitud. Tu cuenta quedó desactivada.", Toast.LENGTH_LONG).show()
                                onCuentaEliminada()
                            }.onFailure { error = mensajeDeError(it) }
                        }
                    }
                ) { Text(if (enviando) "Enviando..." else "Eliminar cuenta", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(enabled = !enviando, onClick = { mostrarEliminar = false }) { Text("Cancelar") }
            }
        )
    }
}
