package mx.familiasquesuman.app.ui.components

import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import mx.familiasquesuman.app.ui.theme.CategoriaAzul
import mx.familiasquesuman.app.ui.theme.CategoriaMorado
import mx.familiasquesuman.app.ui.theme.CategoriaNaranja
import mx.familiasquesuman.app.ui.theme.CategoriaVerde

/**
 * Fondo decorativo con manchas de color flotando muy lentamente (sube y baja, nunca cruza
 * texto en primer plano porque va casi transparente). Pensado solo para pantallas "ligeras"
 * con harto espacio en blanco (Bienvenida, Login), NO para listas o formularios largos,
 * donde le restaría legibilidad en vez de sumar.
 *
 * Respeta la opción de accesibilidad "Quitar animaciones" de Android: si el usuario la activó
 * (Settings > Accesibilidad, o developer options con animator_duration_scale = 0), las formas
 * se quedan quietas en su posición base en vez de moverse.
 */
@Composable
fun FormasFlotantesFondo(modifier: Modifier = Modifier, activo: Boolean = true) {
    if (!activo) return

    val context = LocalContext.current
    val animacionesActivas = remember {
        runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
        }.getOrDefault(true)
    }

    val transicion = rememberInfiniteTransition(label = "formas-flotantes")

    @Composable
    fun flotar(duracionMs: Int, retrasoMs: Int): Float {
        if (!animacionesActivas) return 0.5f
        val progreso by transicion.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = duracionMs, delayMillis = retrasoMs, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "flotar"
        )
        return progreso
    }

    val p1 = flotar(6500, 0)
    val p2 = flotar(5200, 300)
    val p3 = flotar(7200, 600)
    val p4 = flotar(6000, 150)

    // Las barras superiores (BrandTopBar/BackTopBar) ahora son transparentes, así que estas
    // formas ya pueden asomarse arriba sin que se vea un corte recto contra un rectángulo
    // opaco: donde antes había un borde duro, ahora la forma sigue de fondo "a través" de la
    // barra, con el título/íconos encima.
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val ancho = maxWidth
        val alto = maxHeight
        ManchaFlotante(CategoriaAzul, 150.dp, ancho * 0.02f, alto * 0.02f, p1, 18.dp)
        ManchaFlotante(CategoriaNaranja, 110.dp, ancho * 0.72f, alto * 0.10f, p2, 16.dp)
        ManchaFlotante(CategoriaVerde, 100.dp, ancho * 0.10f, alto * 0.70f, p3, 20.dp)
        ManchaFlotante(CategoriaMorado, 130.dp, ancho * 0.66f, alto * 0.80f, p4, 16.dp)
    }
}

/** Un solo círculo con degradado (efecto "mancha suave", sin necesitar blur real) que sube
 * y baja según `progreso` (0f..1f, donde 0.5f es la posición base). */
@Composable
private fun ManchaFlotante(
    color: Color,
    tamano: Dp,
    x: Dp,
    yBase: Dp,
    progreso: Float,
    amplitud: Dp
) {
    val desplazamientoY = amplitud * (progreso - 0.5f) * 2f
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .offset(x = x, y = yBase + desplazamientoY)
            .size(tamano)
            .background(
                Brush.radialGradient(listOf(color.copy(alpha = 0.20f), color.copy(alpha = 0f))),
                CircleShape
            )
    )
}