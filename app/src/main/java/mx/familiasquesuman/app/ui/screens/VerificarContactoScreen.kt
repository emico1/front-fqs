package mx.familiasquesuman.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import mx.familiasquesuman.app.data.remote.mensajeDeError
import mx.familiasquesuman.app.ui.components.CampoFormulario
import mx.familiasquesuman.app.ui.components.EncabezadoAzul
import mx.familiasquesuman.app.ui.components.NotaConIcono
import mx.familiasquesuman.app.ui.components.PrimaryButton
import mx.familiasquesuman.app.ui.components.TarjetaFormulario
import mx.familiasquesuman.app.ui.components.TertiaryTextButton
import mx.familiasquesuman.app.viewmodel.AlTerminar

/**
 * Último paso del registro. La cuenta se crea hasta que la persona escribe el código de
 * 8 dígitos que le llegó por correo o por SMS, así nadie puede registrarse con el correo o
 * el teléfono de otra persona. El código se puede reenviar cada minuto (máximo tres veces).
 */
@Composable
fun VerificarContactoScreen(
    destino: String,
    porSms: Boolean,
    onVerificar: (codigo: String, alTerminar: AlTerminar<Unit>) -> Unit,
    onReenviar: (alTerminar: AlTerminar<Unit>) -> Unit,
    onListo: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var codigo by remember { mutableStateOf("") }
    var enviando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var espera by remember { mutableIntStateOf(60) }
    var reenvios by remember { mutableIntStateOf(0) }

    LaunchedEffect(espera) {
        if (espera > 0) {
            delay(1_000)
            espera--
        }
    }

    val verificar = {
        if (!enviando && codigo.length == 8) {
            enviando = true
            onVerificar(codigo) { r ->
                enviando = false
                r.onSuccess { onListo() }.onFailure {
                    error = mensajeDeError(it)
                    codigo = ""
                }
            }
        }
    }

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            EncabezadoAzul(
                titulo = if (porSms) "Revisa tus mensajes" else "Revisa tu correo",
                subtitulo = if (porSms) "Te enviamos un código de 8 dígitos por SMS al $destino" else "Te enviamos un código de 8 dígitos a $destino",
                icono = if (porSms) Icons.Filled.Sms else Icons.Filled.MarkEmailRead,
                onBack = onBack
            )
            TarjetaFormulario(
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .padding(horizontal = 20.dp)
                    .offset(y = (-32).dp)
            ) {
                Text(
                    if (porSms) "Escríbelo aquí para terminar tu registro. Puede tardar un par de minutos en llegar."
                    else "Escríbelo aquí para terminar tu registro. Si no lo ves en unos minutos, revisa la carpeta de spam.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                CampoFormulario(
                    valor = codigo,
                    alCambiar = { v -> codigo = v.filter { it.isDigit() }.take(8); error = null },
                    etiqueta = "Código",
                    icono = Icons.Filled.Password,
                    teclado = KeyboardType.NumberPassword,
                    error = error != null,
                    placeholder = "12345678"
                )
                error?.let {
                    Row(modifier = Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(start = 6.dp))
                    }
                }
                PrimaryButton(
                    text = if (enviando) "Verificando..." else "Crear mi cuenta",
                    enabled = !enviando && codigo.length == 8,
                    onClick = verificar,
                    modifier = Modifier.padding(top = 16.dp)
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    TertiaryTextButton(
                        text = when {
                            reenvios >= 3 -> "Ya no se puede reenviar"
                            espera > 0 -> "Reenviar código en $espera s"
                            else -> "Reenviar código"
                        },
                        onClick = {
                            if (espera == 0 && reenvios < 3) {
                                onReenviar { r ->
                                    r.onSuccess {
                                        reenvios++
                                        espera = 60
                                        Toast.makeText(context, "Te enviamos un código nuevo", Toast.LENGTH_SHORT).show()
                                    }.onFailure { error = mensajeDeError(it) }
                                }
                            }
                        }
                    )
                }
                NotaConIcono(
                    icono = Icons.Filled.Schedule,
                    texto = "El código vence en 30 minutos.",
                    modifier = Modifier.padding(top = 8.dp).align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}
