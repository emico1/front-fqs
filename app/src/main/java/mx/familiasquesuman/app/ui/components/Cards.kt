package mx.familiasquesuman.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import mx.familiasquesuman.app.data.ActividadItem
import mx.familiasquesuman.app.data.Asociacion
import mx.familiasquesuman.app.data.Campana
import mx.familiasquesuman.app.data.Categoria
import mx.familiasquesuman.app.ui.theme.colorAcento
import mx.familiasquesuman.app.ui.theme.colorContenedor

/** Tarjeta con sombra real (en vez del "ghost border" plano de la v1): le da profundidad
 * a la interfaz para que no se vea como un documento de texto. */
@Composable
fun GhostBorderCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val clickableModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Surface(
        modifier = modifier.fillMaxWidth().then(clickableModifier),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shadowElevation = 4.dp,
        tonalElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(20.dp), content = content)
    }
}

/** Círculo de color con el ícono de la categoría, para que cada asociación/actividad se
 * reconozca de un vistazo por color además de por texto (más accesible para todas las edades). */
@Composable
fun CategoriaBadge(
    categoria: Categoria,
    modifier: Modifier = Modifier,
    tamano: androidx.compose.ui.unit.Dp = 40.dp
) {
    Box(
        modifier = modifier
            .size(tamano)
            .background(categoria.colorContenedor(), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            categoria.icono,
            contentDescription = categoria.etiqueta,
            tint = categoria.colorAcento(),
            modifier = Modifier.size(tamano / 2)
        )
    }
}

/** Fila compacta: badge de color + nombre de la categoría, en el color de la categoría. */
@Composable
fun CategoriaEtiquetaConColor(categoria: Categoria, modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(categoria.colorAcento(), CircleShape)
        )
        Text(
            categoria.etiqueta,
            style = MaterialTheme.typography.labelSmall,
            color = categoria.colorAcento(),
            modifier = Modifier.padding(start = 6.dp)
        )
    }
}

/** Tarjeta de asociación recomendada (usada en Inicio). Ahora con sombra + badge de
 * categoría en color, para que el listado no se vea plano ni monocromático. */
@Composable
fun AsociacionDestacadaCard(asociacion: Asociacion, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .width(220.dp)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shadowElevation = 3.dp
    ) {
        Column {
            Box {
                // Sin foto se ve el color de fondo de la tarjeta (ya no se piden fotos a
                // servicios externos, que además recibían la IP de cada persona)
                AsyncImage(
                    model = asociacion.fotoUrl,
                    contentDescription = asociacion.nombre,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 3f)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    CategoriaBadge(categoria = asociacion.categoria, tamano = 32.dp)
                }
            }
            Column(modifier = Modifier.padding(12.dp)) {
                CategoriaEtiquetaConColor(categoria = asociacion.categoria)
                Text(
                    asociacion.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    asociacion.descripcion,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
        }
    }
}

/** Tarjeta resumen de una actividad/voluntariado, usada en Participar y en la lista completa
 * de voluntariados. Badge de fecha en color (como en Inicio) + descripción, organiza y cupos,
 * para que ocupe un poco más de espacio y diga más de un vistazo. */
@Composable
fun ActividadResumenCard(actividad: ActividadItem, onClick: () -> Unit) {
    GhostBorderCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.Top) {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.tertiary, MaterialTheme.shapes.medium)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(actividad.dia, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onTertiary)
                Text(actividad.mes, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onTertiary)
            }
            Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                Text(actividad.titulo, style = MaterialTheme.typography.titleMedium)
                Text(
                    actividad.asociacion,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    actividad.descripcionCorta,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    modifier = Modifier.padding(top = 6.dp)
                )
                UbicacionRow(texto = actividad.ubicacion, modifier = Modifier.padding(top = 8.dp))
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusTag(
                        texto = actividad.etiquetaAudiencia,
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        "  •  ${actividad.cuposDisponibles}/${actividad.cuposTotales} cupos",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/** Tarjeta resumen de una campaña de donación, usada en Participar y en la lista completa
 * de campañas. Ícono en círculo de color en vez de solo texto plano. */
@Composable
fun CampanaResumenCard(campana: Campana, onClick: () -> Unit) {
    GhostBorderCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        if (campana.urgente) MaterialTheme.colorScheme.error.copy(alpha = 0.14f) else MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = if (campana.urgente) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                )
            }
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                if (campana.urgente) {
                    StatusTag(texto = "Urgente", containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.error)
                }
                Text(campana.titulo, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 4.dp))
                Text(campana.organizacion, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    campana.descripcion,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    modifier = Modifier.padding(top = 6.dp)
                )
                LinearProgressIndicator(
                    progress = { campana.progreso },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .height(8.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.shapes.extraLarge),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = androidx.compose.ui.graphics.Color.Transparent,
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                )
                Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        "${(campana.progreso * 100).toInt()}% completado",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                    )
                    campana.diasRestantes?.let {
                        Text("Faltan $it días", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Text(
                    campana.metaTexto,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

/** Fila de ubicación con ícono, reutilizada en varias pantallas. */
@Composable
fun UbicacionRow(
    texto: String,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.layout.Row(
        modifier = modifier,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.LocationOn,
            contentDescription = null,
            tint = color,
            modifier = Modifier.padding(end = 4.dp)
        )
        Text(texto, style = MaterialTheme.typography.bodySmall, color = color)
    }
}