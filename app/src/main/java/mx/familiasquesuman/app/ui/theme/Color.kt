package mx.familiasquesuman.app.ui.theme

import androidx.compose.ui.graphics.Color

// Paleta "Modern Kindred" extraída del mockup de Stitch (design tokens del equipo).

// Superficies (cálidas, no grises puros)
val Surface = Color(0xFFFDF9F6)
val SurfaceDim = Color(0xFFDDD9D6)
val SurfaceBright = Color(0xFFFDF9F6)
val SurfaceContainerLowest = Color(0xFFFFFFFF)
val SurfaceContainerLow = Color(0xFFF7F3F0)
val SurfaceContainer = Color(0xFFF1EDEA)
val SurfaceContainerHigh = Color(0xFFEBE7E5)
val SurfaceContainerHighest = Color(0xFFE5E2DF)
val SurfaceVariant = Color(0xFFE5E2DF)

val OnSurface = Color(0xFF1C1B1A)
val OnSurfaceVariant = Color(0xFF56423E)
val InverseSurface = Color(0xFF31302F)
val InverseOnSurface = Color(0xFFF4F0ED)
val Outline = Color(0xFF737686)
val OutlineVariant = Color(0xFFC3C6D7)

// Primario: azul vívido y cálido (más saturado que el original para que "jale" la vista)
val Primary = Color(0xFF1857E0)
val OnPrimary = Color(0xFFFFFFFF)
val PrimaryContainer = Color(0xFFDCE6FF)
val OnPrimaryContainer = Color(0xFF0A3AA8)
val InversePrimary = Color(0xFFB4C5FF)
val PrimaryFixed = Color(0xFFDBE1FF)
val PrimaryFixedDim = Color(0xFFB4C5FF)

// Secundario: verde franco (asociaciones/ONG, éxito, naturaleza), mucho más vivo que el sage anterior
val Secondary = Color(0xFF1E8E3E)
val OnSecondary = Color(0xFFFFFFFF)
val SecondaryContainer = Color(0xFFCFF3D6)
val OnSecondaryContainer = Color(0xFF0F5C24)

// Terciario: naranja/ámbar cálido y alegre (CTAs secundarios, destacados, "casero" y accesible)
val Tertiary = Color(0xFFE8600C)
val OnTertiary = Color(0xFFFFFFFF)
val TertiaryContainer = Color(0xFFFFE1CC)
val OnTertiaryContainer = Color(0xFF8A3600)

// Error
val ErrorColor = Color(0xFFD3212C)
val OnErrorColor = Color(0xFFFFFFFF)
val ErrorContainer = Color(0xFFFFDAD6)
val OnErrorContainer = Color(0xFF93000A)

val Background = Color(0xFFFDF9F6)
val OnBackground = Color(0xFF1C1B1A)

// Utilidad: azul vívido puro para acentos puntuales (íconos activos, progreso)
val VividBlue = Color(0xFF1857E0)

// Semáforo de urgencia (chips de "Alta prioridad" / "Urgente")
val Urgent = Color(0xFFD3212C)
val HighPriority = Color(0xFFE8600C)

// ---- Colores por categoría ----
// Cada categoría de asociación tiene su propio color de acento, para que las tarjetas
// y badges no se vean todas del mismo azul y sea más fácil distinguirlas de un vistazo
// (útil también para personas mayores o con menor familiaridad con apps: color = categoría).
val CategoriaVerde = Color(0xFF1E8E3E)   // Medio Ambiente
val CategoriaMorado = Color(0xFF8B3FE0)  // Adultos Mayores
val CategoriaNaranja = Color(0xFFE8600C) // Niños
val CategoriaCafe = Color(0xFFA1662F)    // Mascotas
val CategoriaRosa = Color(0xFFD6217E)    // Arte y Cultura
val CategoriaAzul = Color(0xFF1857E0)    // Comunidad