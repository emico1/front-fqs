package mx.familiasquesuman.app.ui.theme

import androidx.compose.ui.graphics.Color
import mx.familiasquesuman.app.data.Categoria

/** Color de acento fijo por categoría (independiente del tema), para que cada tipo de
 * asociación/actividad se distinga a simple vista sin tener que leer el texto. */
fun Categoria.colorAcento(): Color = when (this) {
    Categoria.MEDIO_AMBIENTE -> CategoriaVerde
    Categoria.ADULTOS_MAYORES -> CategoriaMorado
    Categoria.NINOS -> CategoriaNaranja
    Categoria.MASCOTAS -> CategoriaCafe
    Categoria.ARTE_CULTURA -> CategoriaRosa
    Categoria.COMUNIDAD -> CategoriaAzul
}

/** Versión suave del color de acento, para fondos de badges/chips (mantiene buen contraste
 * con el texto/ícono de color sólido encima). */
fun Categoria.colorContenedor(): Color = colorAcento().copy(alpha = 0.14f)