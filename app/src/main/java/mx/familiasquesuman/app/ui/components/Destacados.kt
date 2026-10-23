package mx.familiasquesuman.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/*
 * Piezas del estilo de Bienvenida, Login y Registro para usar en el resto de la app sin
 * rehacer las pantallas. Un banner azul con degradado para lo más importante de cada
 * pantalla, un botón blanco para usarse encima de ese azul y un título de sección con
 * una pequeña barra de acento.
 */

/** Mismo azul profundo del final del degradado de Bienvenida. */
val AzulProfundo = Color(0xFF0A3AA8)

/** Degradado de la marca, del azul principal al azul profundo. */
@Composable
fun degradadoMarca(): Brush = Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, AzulProfundo))

/**
 * Tarjeta azul con degradado, esquinas grandes y dos círculos de luz tenues. Todo lo que
 * va adentro debe ser blanco o casi blanco para leerse bien.
 */
@Composable
fun BannerAzul(
    modifier: Modifier = Modifier,
    contenido: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = Color.Transparent,
        shadowElevation = 6.dp
    ) {
        Box(modifier = Modifier.background(degradadoMarca())) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 50.dp, y = (-40).dp)
                    .size(150.dp)
                    .background(Color.White.copy(alpha = 0.08f), CircleShape)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = (-36).dp, y = 36.dp)
                    .size(110.dp)
                    .background(Color.White.copy(alpha = 0.06f), CircleShape)
            )
            Column(modifier = Modifier.padding(20.dp), content = contenido)
        }
    }
}

/** Botón blanco con texto azul, pensado para ir encima de BannerAzul. */
@Composable
fun BotonBlanco(texto: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(52.dp),
        shape = MaterialTheme.shapes.extraLarge,
        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = MaterialTheme.colorScheme.primary)
    ) {
        Text(texto, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

/**
 * Título de sección con una barra corta de color a la izquierda, igual en todas las
 * pantallas. La acción opcional ("Ver todas") va a la derecha.
 */
@Composable
fun TituloSeccion(
    titulo: String,
    modifier: Modifier = Modifier,
    accion: String? = null,
    onAccion: () -> Unit = {}
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(5.dp)
                .height(22.dp)
                .background(degradadoMarca(), RoundedCornerShape(3.dp))
        )
        Text(
            titulo,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp)
        )
        if (accion != null) TertiaryTextButton(text = accion, onClick = onAccion)
    }
}
