package mx.familiasquesuman.app.ui.components

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

/*
 * Piezas compartidas por Login y Registro, con el mismo estilo de la pantalla de
 * Bienvenida. Encabezado azul con degradado, tarjeta blanca encima y campos con ícono.
 */


/**
 * Pone blancos los íconos de la barra de estado mientras la pantalla tiene encabezado
 * azul y los regresa como estaban al salir.
 */
@Composable
fun BarraEstadoClara() {
    val vista = LocalView.current
    DisposableEffect(vista) {
        val ventana = (vista.context as? Activity)?.window
        val control = ventana?.let { WindowCompat.getInsetsController(it, vista) }
        val antes = control?.isAppearanceLightStatusBars
        control?.isAppearanceLightStatusBars = false
        onDispose { if (antes != null) control?.isAppearanceLightStatusBars = antes }
    }
}

/**
 * Encabezado azul que llega hasta detrás de la barra de estado. Deja espacio abajo para
 * que la tarjeta del formulario se monte encima con un desplazamiento negativo.
 */
@Composable
fun EncabezadoAzul(
    titulo: String,
    subtitulo: String,
    icono: ImageVector,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    extra: @Composable ColumnScope.() -> Unit = {}
) {
    BarraEstadoClara()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primary, AzulProfundo)),
                RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp)
            )
    ) {
        // Círculos de luz decorativos
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 50.dp, y = (-20).dp)
                .size(170.dp)
                .background(Color.White.copy(alpha = 0.07f), CircleShape)
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-40).dp, y = 20.dp)
                .size(120.dp)
                .background(Color.White.copy(alpha = 0.06f), CircleShape)
        )
        Column(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(start = 8.dp, end = 24.dp, top = 4.dp, bottom = 56.dp)
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar", tint = Color.White)
                }
            }
            Column(modifier = Modifier.padding(start = 16.dp, top = if (onBack != null) 4.dp else 24.dp)) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icono, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
                }
                Text(
                    titulo,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    modifier = Modifier.padding(top = 16.dp)
                )
                Text(
                    subtitulo,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.padding(top = 6.dp)
                )
                extra()
            }
        }
    }
}

/** Tarjeta blanca con sombra donde va el formulario. */
@Composable
fun TarjetaFormulario(modifier: Modifier = Modifier, contenido: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shadowElevation = 6.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp), content = contenido)
    }
}

/** Campo de texto con ícono a la izquierda y esquinas redondeadas. */
@Composable
fun CampoFormulario(
    valor: String,
    alCambiar: (String) -> Unit,
    etiqueta: String,
    icono: ImageVector,
    modifier: Modifier = Modifier,
    error: Boolean = false,
    ayuda: String? = null,
    teclado: KeyboardType = KeyboardType.Text,
    soloLectura: Boolean = false,
    placeholder: String? = null,
    final: @Composable (() -> Unit)? = null,
    transformacion: VisualTransformation = VisualTransformation.None
) {
    OutlinedTextField(
        value = valor,
        onValueChange = alCambiar,
        label = { Text(etiqueta) },
        placeholder = placeholder?.let { { Text(it) } },
        leadingIcon = {
            Icon(icono, contentDescription = null, tint = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
        },
        trailingIcon = final,
        singleLine = true,
        readOnly = soloLectura,
        isError = error,
        supportingText = ayuda?.let { { Text(it) } },
        visualTransformation = transformacion,
        keyboardOptions = KeyboardOptions(keyboardType = teclado),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = modifier.fillMaxWidth()
    )
}

/** Campo de contraseña con botón para mostrarla u ocultarla. */
@Composable
fun CampoContrasena(
    valor: String,
    alCambiar: (String) -> Unit,
    etiqueta: String,
    modifier: Modifier = Modifier,
    error: Boolean = false,
    ayuda: String? = null
) {
    var visible by remember { mutableStateOf(false) }
    CampoFormulario(
        valor = valor,
        alCambiar = alCambiar,
        etiqueta = etiqueta,
        icono = Icons.Filled.Lock,
        modifier = modifier,
        error = error,
        ayuda = ayuda,
        teclado = KeyboardType.Password,
        transformacion = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        final = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = if (visible) "Ocultar contraseña" else "Mostrar contraseña"
                )
            }
        }
    )
}

/** Fila pequeña con ícono y texto, para notas al pie del formulario. */
@Composable
fun NotaConIcono(icono: ImageVector, texto: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(icono, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
        Text(
            texto,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 6.dp)
        )
    }
}
