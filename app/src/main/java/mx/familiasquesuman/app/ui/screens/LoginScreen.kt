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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WavingHand
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import mx.familiasquesuman.app.data.PasoLogin
import mx.familiasquesuman.app.data.remote.ErrorApi
import mx.familiasquesuman.app.data.remote.mensajeDeError
import mx.familiasquesuman.app.ui.components.CampoContrasena
import mx.familiasquesuman.app.ui.components.CampoFormulario
import mx.familiasquesuman.app.ui.components.EncabezadoAzul
import mx.familiasquesuman.app.ui.components.NotaConIcono
import mx.familiasquesuman.app.ui.components.PrimaryButton
import mx.familiasquesuman.app.ui.components.TarjetaFormulario
import mx.familiasquesuman.app.ui.components.TertiaryTextButton
import mx.familiasquesuman.app.viewmodel.AlTerminar

/**
 * Inicio de sesión con el mismo estilo de Bienvenida. Encabezado azul, tarjeta con los
 * dos campos y la recuperación de contraseña. Se quitó el botón de Google porque el
 * backend no tiene ese inicio de sesión, y un botón que no funciona confunde.
 *
 * Las cuentas de administración con segundo factor pasan a un segundo paso donde se
 * escribe el código de 6 dígitos de su app de autenticación (o un código de respaldo).
 * La pantalla no permite capturas porque ahí se escriben contraseñas y códigos.
 */
@Composable
fun LoginScreen(
    onIniciarSesion: (identificador: String, password: String, alTerminar: AlTerminar<PasoLogin>) -> Unit,
    onVerificarCodigo: (desafio: String, codigo: String, alTerminar: AlTerminar<Unit>) -> Unit,
    onSolicitarCodigo: (identificador: String, alTerminar: AlTerminar<Unit>) -> Unit,
    onRestablecer: (identificador: String, codigo: String, password: String, alTerminar: AlTerminar<Unit>) -> Unit,
    onSesionIniciada: () -> Unit,
    onCrearCuenta: () -> Unit,
    onBack: () -> Unit,
    animacionesActivas: Boolean = true
) {
    var correo by remember { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") }
    var enviando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var mostrarRecuperar by remember { mutableStateOf(false) }
    // Desafío del segundo factor, cuando no es null se muestra el paso del código
    var desafio by remember { mutableStateOf<String?>(null) }
    var codigo by remember { mutableStateOf("") }
    val context = LocalContext.current

    val entrar = {
        if (!enviando && correo.isNotBlank() && contrasena.isNotBlank()) {
            enviando = true
            onIniciarSesion(correo, contrasena) { r ->
                enviando = false
                r.onSuccess { paso ->
                    contrasena = ""
                    when (paso) {
                        is PasoLogin.Listo -> onSesionIniciada()
                        is PasoLogin.Codigo -> {
                            desafio = paso.desafio
                            codigo = ""
                            error = null
                        }
                    }
                }.onFailure { error = mensajeDeError(it) }
            }
        }
    }

    val verificar = {
        val d = desafio
        if (!enviando && d != null && codigo.length >= 6) {
            enviando = true
            onVerificarCodigo(d, codigo) { r ->
                enviando = false
                r.onSuccess {
                    codigo = ""
                    desafio = null
                    onSesionIniciada()
                }.onFailure { e ->
                    error = mensajeDeError(e)
                    codigo = ""
                    // El desafío venció o se agotaron los intentos, hay que volver a la contraseña
                    if (e is ErrorApi && e.codigo == "desafio_invalido") desafio = null
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
                titulo = "¡Qué gusto verte!",
                subtitulo = "Entra para ver las actividades de tu ciudad y seguir sumando.",
                icono = Icons.Filled.WavingHand,
                onBack = onBack
            )

            TarjetaFormulario(
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .padding(horizontal = 20.dp)
                    .offset(y = (-32).dp)
            ) {
                if (desafio != null) {
                    PasoCodigo(
                        codigo = codigo,
                        alCambiar = { v -> codigo = v.filter { it.isLetterOrDigit() || it == '-' }.take(12); error = null },
                        error = error,
                        enviando = enviando,
                        onVerificar = verificar,
                        onCancelar = { desafio = null; codigo = ""; error = null }
                    )
                } else {
                    CampoFormulario(
                        valor = correo,
                        alCambiar = { correo = it; error = null },
                        etiqueta = "Correo o teléfono",
                        icono = Icons.Filled.AlternateEmail,
                        teclado = KeyboardType.Email,
                        error = error != null
                    )
                    CampoContrasena(
                        valor = contrasena,
                        alCambiar = { contrasena = it; error = null },
                        etiqueta = "Contraseña",
                        error = error != null,
                        modifier = Modifier.padding(top = 12.dp)
                    )

                    error?.let {
                        Row(modifier = Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Text(
                                it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(start = 6.dp)
                            )
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TertiaryTextButton(text = "¿Olvidaste tu contraseña?", onClick = { mostrarRecuperar = true })
                    }

                    PrimaryButton(
                        text = if (enviando) "Entrando..." else "Iniciar sesión",
                        enabled = !enviando && correo.isNotBlank() && contrasena.isNotBlank(),
                        onClick = entrar,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    NotaConIcono(
                        icono = Icons.Filled.Shield,
                        texto = "Tu sesión se guarda cifrada en este teléfono.",
                        modifier = Modifier.padding(top = 16.dp).align(Alignment.CenterHorizontally)
                    )
                }
            }

            Row(
                modifier = Modifier.padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "¿Aún no tienes cuenta?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TertiaryTextButton(text = "Crear cuenta", onClick = onCrearCuenta)
            }
        }
    }

    if (mostrarRecuperar) {
        RecuperarContrasenaDialogo(
            identificadorInicial = correo,
            onSolicitarCodigo = onSolicitarCodigo,
            onRestablecer = onRestablecer,
            onCerrar = { mostrarRecuperar = false },
            onListo = {
                mostrarRecuperar = false
                Toast.makeText(context, "Contraseña actualizada, ya puedes iniciar sesión.", Toast.LENGTH_LONG).show()
            }
        )
    }
}

/**
 * Segundo paso para administración. Acepta el código de 6 dígitos de la app de
 * autenticación o uno de los códigos de respaldo (como k7m2-q9xa).
 */
@Composable
private fun PasoCodigo(
    codigo: String,
    alCambiar: (String) -> Unit,
    error: String?,
    enviando: Boolean,
    onVerificar: () -> Unit,
    onCancelar: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(
            "Verificación en dos pasos",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
    Text(
        "Abre tu app de autenticación y escribe el código de 6 dígitos de Familias que Suman. Si perdiste el teléfono usa uno de tus códigos de respaldo.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
    )
    CampoFormulario(
        valor = codigo,
        alCambiar = alCambiar,
        etiqueta = "Código",
        icono = Icons.Filled.Password,
        teclado = KeyboardType.Ascii,
        error = error != null,
        placeholder = "123456"
    )
    error?.let {
        Row(modifier = Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(start = 6.dp))
        }
    }
    PrimaryButton(
        text = if (enviando) "Verificando..." else "Verificar",
        enabled = !enviando && codigo.length >= 6,
        onClick = onVerificar,
        modifier = Modifier.padding(top = 16.dp)
    )
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        TertiaryTextButton(text = "Usar otra cuenta", onClick = onCancelar)
    }
}

/**
 * Dos pasos. Primero se pide un código de 8 dígitos que llega por correo, luego se
 * escribe el código con la contraseña nueva.
 */
@Composable
private fun RecuperarContrasenaDialogo(
    identificadorInicial: String,
    onSolicitarCodigo: (String, AlTerminar<Unit>) -> Unit,
    onRestablecer: (String, String, String, AlTerminar<Unit>) -> Unit,
    onCerrar: () -> Unit,
    onListo: () -> Unit
) {
    var identificador by remember { mutableStateOf(identificadorInicial) }
    var codigoEnviado by remember { mutableStateOf(false) }
    var codigo by remember { mutableStateOf("") }
    var nueva by remember { mutableStateOf("") }
    var confirmar by remember { mutableStateOf("") }
    var enviando by remember { mutableStateOf(false) }
    var mensaje by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!enviando) onCerrar() },
        title = { Text("Recuperar contraseña") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!codigoEnviado) {
                    Text("Te enviaremos un código de 8 dígitos al correo de tu cuenta.", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = identificador,
                        onValueChange = { identificador = it },
                        label = { Text("Correo o teléfono") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text("Si la cuenta existe, el código ya va en camino. Vence en 30 minutos.", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = codigo,
                        onValueChange = { v -> codigo = v.filter { it.isDigit() }.take(8) },
                        label = { Text("Código") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = nueva,
                        onValueChange = { nueva = it },
                        label = { Text("Contraseña nueva (mínimo 10)") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = confirmar,
                        onValueChange = { confirmar = it },
                        label = { Text("Confirmar contraseña") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        isError = confirmar.isNotEmpty() && confirmar != nueva,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                mensaje?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !enviando && if (!codigoEnviado) identificador.isNotBlank()
                else codigo.length == 8 && nueva.length >= 10 && nueva == confirmar,
                onClick = {
                    enviando = true
                    mensaje = null
                    if (!codigoEnviado) {
                        onSolicitarCodigo(identificador) { r ->
                            enviando = false
                            r.onSuccess { codigoEnviado = true }.onFailure { mensaje = mensajeDeError(it) }
                        }
                    } else {
                        onRestablecer(identificador, codigo, nueva) { r ->
                            enviando = false
                            r.onSuccess { onListo() }.onFailure { mensaje = mensajeDeError(it) }
                        }
                    }
                }
            ) { Text(if (!codigoEnviado) "Enviar código" else "Cambiar contraseña") }
        },
        dismissButton = {
            TextButton(onClick = onCerrar, enabled = !enviando) { Text("Cancelar") }
        }
    )
}
