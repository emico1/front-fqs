package mx.familiasquesuman.app.ui.screens

import android.provider.Settings
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Diversity3
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Elderly
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import mx.familiasquesuman.app.ui.components.BarraEstadoClara
import mx.familiasquesuman.app.ui.components.PrimaryButton
import mx.familiasquesuman.app.ui.components.SecondaryButton
import mx.familiasquesuman.app.ui.theme.CategoriaAzul
import mx.familiasquesuman.app.ui.theme.CategoriaCafe
import mx.familiasquesuman.app.ui.theme.CategoriaMorado
import mx.familiasquesuman.app.ui.theme.CategoriaNaranja
import mx.familiasquesuman.app.ui.theme.CategoriaRosa
import mx.familiasquesuman.app.ui.theme.CategoriaVerde

/** Azul profundo para el final del degradado, sale del mismo tono que OnPrimaryContainer. */
private val AzulProfundo = Color(0xFF0A3AA8)

/** Las seis causas de la app alrededor del corazón, cada una con el color de su categoría. */
private val causas: List<Pair<ImageVector, Color>> = listOf(
    Icons.Filled.Eco to CategoriaVerde,
    Icons.Filled.Elderly to CategoriaMorado,
    Icons.Filled.ChildCare to CategoriaNaranja,
    Icons.Filled.Pets to CategoriaCafe,
    Icons.Filled.Palette to CategoriaRosa,
    Icons.Filled.Diversity3 to CategoriaAzul
)

/**
 * Pantalla de bienvenida. Arriba un bloque azul con las seis causas girando despacio
 * alrededor de un corazón, abajo el nombre, la propuesta de valor, las tres formas de
 * sumar y los dos caminos (crear cuenta o entrar).
 *
 * Todo el movimiento se apaga si la persona desactivó el fondo animado en su perfil o si
 * Android tiene activada la opción de accesibilidad "Quitar animaciones".
 */
@Composable
fun BienvenidaScreen(
    onIniciarSesion: () -> Unit,
    onCrearCuenta: () -> Unit,
    animacionesActivas: Boolean = true
) {
    val context = LocalContext.current
    val movimiento = remember(animacionesActivas) {
        animacionesActivas && runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
        }.getOrDefault(true)
    }

    // Íconos blancos en la barra de estado mientras se ve el bloque azul
    BarraEstadoClara()

    // Entrada suave del contenido la primera vez que se abre la pantalla
    var visible by remember { mutableStateOf(!movimiento) }
    LaunchedEffect(Unit) { visible = true }
    val entrada by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "entrada"
    )

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Hero(movimiento = movimiento, entrada = entrada, modifier = Modifier.weight(1f))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 24.dp)
                    .padding(top = 24.dp, bottom = 32.dp)
                    .alpha(entrada)
                    .offset(y = (24 * (1f - entrada)).dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    buildAnnotatedString {
                        append("Familias que ")
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) { append("Suman") }
                    },
                    style = MaterialTheme.typography.headlineLarge.copy(fontSize = 32.sp, lineHeight = 38.sp),
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Text(
                    "Conectamos familias con asociaciones verificadas para sumar tiempo, recursos e ideas a su comunidad.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .widthIn(max = 420.dp)
                        .padding(top = 10.dp)
                )

                Row(
                    modifier = Modifier.padding(top = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FormaDeSumar("Participa", Icons.Filled.Handshake, CategoriaAzul)
                    FormaDeSumar("Visita", Icons.Filled.Storefront, CategoriaMorado)
                    FormaDeSumar("Dona", Icons.Filled.VolunteerActivism, CategoriaVerde)
                }

                Spacer(modifier = Modifier.height(28.dp))
                Column(modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth()) {
                    PrimaryButton(text = "Crear cuenta", onClick = onCrearCuenta)
                    SecondaryButton(
                        text = "Ya tengo cuenta",
                        onClick = onIniciarSesion,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }
        }
    }
}

/**
 * Bloque azul de arriba. El degradado llega hasta detrás de la barra de estado y termina
 * con las esquinas inferiores redondeadas. Las causas giran una vuelta completa cada
 * 40 segundos y el corazón late muy despacio.
 */
@Composable
private fun Hero(movimiento: Boolean, entrada: Float, modifier: Modifier = Modifier) {
    val transicion = rememberInfiniteTransition(label = "hero")
    val giro by transicion.animateFloat(
        initialValue = 0f,
        targetValue = if (movimiento) 360f else 0f,
        animationSpec = infiniteRepeatable(tween(40_000, easing = LinearEasing), RepeatMode.Restart),
        label = "giro"
    )
    val latido by transicion.animateFloat(
        initialValue = 1f,
        targetValue = if (movimiento) 1.07f else 1f,
        animationSpec = infiniteRepeatable(tween(1_400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "latido"
    )
    val flotar by transicion.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3_200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "flotar"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primary, AzulProfundo)),
                RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp)
            )
            .windowInsetsPadding(WindowInsets.statusBars),
        contentAlignment = Alignment.Center
    ) {
        // En teléfonos chicos la órbita se reduce para caber completa en el bloque azul
        val escala = minOf(1f, maxHeight / 320.dp, maxWidth / 320.dp)
        // Círculos de luz decorativos, muy tenues
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 60.dp, y = (-30).dp)
                .size(200.dp)
                .background(Color.White.copy(alpha = 0.07f), CircleShape)
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-50).dp, y = 30.dp)
                .size(160.dp)
                .background(Color.White.copy(alpha = 0.06f), CircleShape)
        )

        // Órbita de causas alrededor del corazón
        Box(
            modifier = Modifier
                .requiredSize(280.dp)
                .scale(escala * (0.85f + 0.15f * entrada))
                .alpha(entrada),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(236.dp)
                    .border(1.5.dp, Color.White.copy(alpha = 0.22f), CircleShape)
            )
            val radio = 118f
            causas.forEachIndexed { i, (icono, color) ->
                val angulo = (giro + i * 60f - 90f) * (PI / 180.0)
                // Cada causa flota un poco a destiempo de las demás
                val vaiven = flotar * if (i % 2 == 0) 4f else -4f
                Burbuja(
                    icono = icono,
                    color = color,
                    tamano = 52.dp,
                    modifier = Modifier.offset(
                        x = (radio * cos(angulo)).toFloat().dp,
                        y = ((radio * sin(angulo)).toFloat() + vaiven).dp
                    )
                )
            }
            Box(
                modifier = Modifier
                    .scale(latido)
                    .size(112.dp)
                    .background(Color.White.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .background(Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.VolunteerActivism,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun Burbuja(icono: ImageVector, color: Color, tamano: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(tamano)
            .background(color, CircleShape)
            .border(3.dp, Color.White, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(icono, contentDescription = null, tint = Color.White, modifier = Modifier.size(tamano / 2))
    }
}

/** Píldora con una de las tres formas de sumar. */
@Composable
private fun FormaDeSumar(texto: String, icono: ImageVector, color: Color) {
    Row(
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(percent = 50))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        Text(
            texto,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = color,
            modifier = Modifier.padding(start = 6.dp)
        )
    }
}
