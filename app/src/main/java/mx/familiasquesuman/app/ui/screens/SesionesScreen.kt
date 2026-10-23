package mx.familiasquesuman.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import mx.familiasquesuman.app.data.SesionDispositivo
import mx.familiasquesuman.app.data.remote.mensajeDeError
import mx.familiasquesuman.app.ui.components.BackTopBar
import mx.familiasquesuman.app.ui.components.ContenidoCarga
import mx.familiasquesuman.app.ui.components.GhostBorderCard
import mx.familiasquesuman.app.ui.components.SecondaryButton
import mx.familiasquesuman.app.viewmodel.AlTerminar
import mx.familiasquesuman.app.viewmodel.Carga

/**
 * Dispositivos con sesión abierta. Si la persona no reconoce uno (un teléfono perdido o
 * prestado) puede cerrarlo desde aquí y deja de funcionar al instante.
 */
@Composable
fun SesionesScreen(
    estado: Carga<List<SesionDispositivo>>,
    onReintentar: () -> Unit,
    onBack: () -> Unit,
    onCerrar: (id: String, alTerminar: AlTerminar<Unit>) -> Unit,
    onCerrarOtras: (alTerminar: AlTerminar<Unit>) -> Unit
) {
    val context = LocalContext.current
    var aCerrar by remember { mutableStateOf<SesionDispositivo?>(null) }
    var confirmarTodas by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        BackTopBar(titulo = "Mis dispositivos", onBack = onBack)
        ContenidoCarga(estado, onReintentar) { lista ->
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(bottom = 6.dp)) {
                        Icon(Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "Aquí están los teléfonos donde tu cuenta tiene la sesión abierta. Si no reconoces alguno, ciérralo y cambia tu contraseña.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
                items(lista, key = { it.id }) { s ->
                    GhostBorderCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.PhoneAndroid, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                                Text(nombreDispositivo(s.dispositivo), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text(
                                    if (s.actual) "Este teléfono" else "Último uso ${fechaCorta(s.ultimaActividad)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (s.actual) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "Desde ${fechaCorta(s.iniciada)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (!s.actual) {
                                TextButton(onClick = { aCerrar = s }) {
                                    Text("Cerrar", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
                if (lista.count { !it.actual } > 1) {
                    item {
                        SecondaryButton(
                            text = "Cerrar todos los demás",
                            onClick = { confirmarTodas = true },
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }
    }

    aCerrar?.let { s ->
        AlertDialog(
            onDismissRequest = { aCerrar = null },
            title = { Text("¿Cerrar esta sesión?") },
            text = { Text("${nombreDispositivo(s.dispositivo)} tendrá que volver a iniciar sesión.") },
            confirmButton = {
                TextButton(onClick = {
                    onCerrar(s.id) { r ->
                        r.onSuccess { aCerrar = null }
                            .onFailure { Toast.makeText(context, mensajeDeError(it), Toast.LENGTH_LONG).show() }
                    }
                }) { Text("Cerrar sesión", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { aCerrar = null }) { Text("Cancelar") } }
        )
    }

    if (confirmarTodas) {
        AlertDialog(
            onDismissRequest = { confirmarTodas = false },
            title = { Text("¿Cerrar todos los demás?") },
            text = { Text("Solo este teléfono seguirá con la sesión abierta.") },
            confirmButton = {
                TextButton(onClick = {
                    onCerrarOtras { r ->
                        r.onSuccess { confirmarTodas = false }
                            .onFailure { Toast.makeText(context, mensajeDeError(it), Toast.LENGTH_LONG).show() }
                    }
                }) { Text("Cerrar todos", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmarTodas = false }) { Text("Cancelar") } }
        )
    }
}

/** El agente que manda la app es "FamiliasQueSuman-Android/0.3.0 (Modelo; Android 14)". */
private fun nombreDispositivo(agente: String): String {
    val dentro = Regex("\\(([^)]*)\\)").find(agente)?.groupValues?.get(1)
    return when {
        dentro != null -> dentro.replace(";", ",")
        agente.startsWith("FamiliasQueSuman-Android") -> "Teléfono Android"
        else -> "Otro dispositivo"
    }
}

/** De 2026-10-18T19:58:00.000Z a 18/10/2026. */
private fun fechaCorta(iso: String): String {
    val partes = iso.take(10).split("-")
    return if (partes.size == 3) "${partes[2]}/${partes[1]}/${partes[0]}" else iso
}
