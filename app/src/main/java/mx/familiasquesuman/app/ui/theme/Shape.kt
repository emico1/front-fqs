package mx.familiasquesuman.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Esquinas más redondeadas que la v1: look más suave/amigable y menos "corporativo",
// pensado para que se sienta cómodo tanto para niños como para adultos mayores.
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

val PillShape = RoundedCornerShape(percent = 50)