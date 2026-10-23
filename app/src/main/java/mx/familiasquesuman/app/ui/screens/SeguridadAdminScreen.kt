package mx.familiasquesuman.app.ui.screens

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PersistableBundle
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
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import mx.familiasquesuman.app.data.EstadoSegundoFactor
import mx.familiasquesuman.app.data.InicioSegundoFactor
import mx.familiasquesuman.app.data.remote.mensajeDeError
import mx.familiasquesuman.app.ui.components.BackTopBar
import mx.familiasquesuman.app.ui.components.CampoContrasena
import mx.familiasquesuman.app.ui.components.CampoFormulario
import mx.familiasquesuman.app.ui.components.ContenidoCarga
import mx.familiasquesuman.app.ui.components.GhostBorderCard
import mx.familiasquesuman.app.ui.components.PrimaryButton
import mx.familiasquesuman.app.ui.components.SecondaryButton
import mx.familiasquesuman.app.ui.components.TituloSeccion
import mx.familiasquesuman.app.viewmodel.AlTerminar
import mx.familiasquesuman.app.viewmodel.Carga

/**
 * Segundo factor de la cuenta de administración. Sin él, el servidor no deja usar
 * ninguna ruta de administración. Se activa en tres pasos.
 *   1. Confirmar la contraseña.
 *   2. Agregar la clave a una app de autenticación (Google Authenticator, Microsoft
 *      Authenticator, 1Password) y escribir el primer código.
 *   3. Guardar los códigos de respaldo, que se muestran una sola vez.
 * La pantalla no permite capturas.
 */
@Composable
fun SeguridadAdminScreen(
    estado: Carga<EstadoSegundoFactor>,
    onReintentar: () -> Unit,
    onBack: () -> Unit,
    onIniciar: (password: String, alTerminar: AlTerminar<InicioSegundoFactor>) -> Unit,
    onConfirmar: (codigo: String, alTerminar: AlTerminar<List<String>>) -> Unit
) {
    // Los códigos de respaldo se muestran aunque el estado ya diga activo
    var codigosRespaldo by remember { mutableStateOf<List<String>?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        BackTopBar(titulo = "Seguridad de administración", onBack = onBack)
        ContenidoCarga(estado, onReintentar) { e ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                val codigos = codigosRespaldo
                when {
                    codigos != null -> CodigosRespaldo(codigos, onListo = { codigosRespaldo = null })
                    e.activo -> Activo(e)
                    else -> Activar(onIniciar, onConfirmar, onActivado = { codigosRespaldo = it })
                }
            }
        }
    }
}

@Composable
private fun Activo(e: EstadoSegundoFactor) {
    GhostBorderCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text("Verificación en dos pasos activa", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 8.dp))
        }
        Text(
            "Cada vez que inicies sesión te pediremos el código de tu app de autenticación.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            "Te quedan ${e.codigosRespaldoRestantes} códigos de respaldo.",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (e.codigosRespaldoRestantes <= 2) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
    Nota(
        "Si pierdes el teléfono y los códigos de respaldo, la persona que administra el servidor puede reiniciar tu segundo factor con el comando npm run admin-2fa."
    )
}

@Composable
private fun Activar(
    onIniciar: (String, AlTerminar<InicioSegundoFactor>) -> Unit,
    onConfirmar: (String, AlTerminar<List<String>>) -> Unit,
    onActivado: (List<String>) -> Unit
) {
    val context = LocalContext.current
    var password by remember { mutableStateOf("") }
    var inicio by remember { mutableStateOf<InicioSegundoFactor?>(null) }
    var codigo by remember { mutableStateOf("") }
    var enviando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Nota(
        "Para administrar necesitas la verificación en dos pasos. Así, aunque alguien conozca tu contraseña, no puede entrar sin tu teléfono."
    )

    val datos = inicio
    if (datos == null) {
        TituloSeccion("Paso 1 de 2, confirma tu contraseña", modifier = Modifier.padding(top = 20.dp, bottom = 8.dp))
        CampoContrasena(
            valor = password,
            alCambiar = { password = it.take(128); error = null },
            etiqueta = "Contraseña",
            error = error != null,
            ayuda = error
        )
        PrimaryButton(
            text = if (enviando) "Generando..." else "Continuar",
            enabled = !enviando && password.isNotEmpty(),
            onClick = {
                enviando = true
                onIniciar(password) { r ->
                    enviando = false
                    r.onSuccess { inicio = it; password = "" }.onFailure { error = mensajeDeError(it) }
                }
            },
            modifier = Modifier.padding(top = 12.dp)
        )
    } else {
        TituloSeccion("Paso 2 de 2, agrega la clave a tu app", modifier = Modifier.padding(top = 20.dp, bottom = 8.dp))
        PrimaryButton(
            text = "Abrir en mi app de autenticación",
            onClick = {
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(datos.uri)))
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(context, "Instala Google Authenticator o Microsoft Authenticator, o escribe la clave a mano.", Toast.LENGTH_LONG).show()
                }
            }
        )
        Text(
            "O escribe esta clave en la app, tipo basada en tiempo",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 16.dp)
        )
        GhostBorderCard(modifier = Modifier.padding(top = 6.dp)) {
            Text(
                datos.secreto.chunked(4).joinToString(" "),
                style = MaterialTheme.typography.titleMedium,
                fontFamily = FontFamily.Monospace
            )
        }
        SecondaryButton(
            text = "Copiar clave",
            onClick = { copiarSensible(context, "Clave", datos.secreto) },
            modifier = Modifier.padding(top = 8.dp)
        )

        CampoFormulario(
            valor = codigo,
            alCambiar = { v -> codigo = v.filter { it.isDigit() }.take(6); error = null },
            etiqueta = "Código de 6 dígitos",
            icono = Icons.Filled.Password,
            teclado = KeyboardType.NumberPassword,
            error = error != null,
            ayuda = error,
            modifier = Modifier.padding(top = 20.dp)
        )
        PrimaryButton(
            text = if (enviando) "Activando..." else "Activar",
            enabled = !enviando && codigo.length == 6,
            onClick = {
                enviando = true
                onConfirmar(codigo) { r ->
                    enviando = false
                    r.onSuccess { onActivado(it) }.onFailure { error = mensajeDeError(it); codigo = "" }
                }
            },
            modifier = Modifier.padding(top = 12.dp, bottom = 24.dp)
        )
    }
}

@Composable
private fun CodigosRespaldo(codigos: List<String>, onListo: () -> Unit) {
    val context = LocalContext.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text("Verificación activada", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 8.dp))
    }
    Text(
        "Guarda estos códigos de respaldo en un lugar seguro, fuera de este teléfono. Cada uno sirve una sola vez si pierdes el teléfono. No se volverán a mostrar.",
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(top = 8.dp)
    )
    GhostBorderCard(modifier = Modifier.padding(top = 12.dp)) {
        codigos.chunked(2).forEach { par ->
            Row(modifier = Modifier.fillMaxWidth()) {
                par.forEach { c ->
                    Text(
                        c,
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(1f).padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
    SecondaryButton(
        text = "Copiar códigos",
        onClick = { copiarSensible(context, "Códigos de respaldo", codigos.joinToString("\n")) },
        modifier = Modifier.padding(top = 12.dp)
    )
    PrimaryButton(text = "Ya los guardé", onClick = onListo, modifier = Modifier.padding(top = 8.dp, bottom = 24.dp))
}

@Composable
private fun Nota(texto: String) {
    Row(modifier = Modifier.padding(top = 16.dp), verticalAlignment = Alignment.Top) {
        Icon(Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(texto, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 8.dp))
    }
}

/**
 * Copia marcando el contenido como sensible. Android 13 y posteriores no lo muestran en
 * la vista previa del portapapeles ni en el teclado.
 */
private fun copiarSensible(context: Context, etiqueta: String, texto: String) {
    val clip = ClipData.newPlainText(etiqueta, texto)
    clip.description.extras = PersistableBundle().apply { putBoolean("android.content.extra.IS_SENSITIVE", true) }
    (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(clip)
    Toast.makeText(context, "Copiado", Toast.LENGTH_SHORT).show()
}
