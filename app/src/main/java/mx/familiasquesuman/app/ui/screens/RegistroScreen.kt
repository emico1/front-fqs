package mx.familiasquesuman.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import mx.familiasquesuman.app.data.Catalogo
import mx.familiasquesuman.app.data.Ciudad
import mx.familiasquesuman.app.data.DatosRegistro
import mx.familiasquesuman.app.data.edadDesdeIso
import mx.familiasquesuman.app.ui.components.CampoContrasena
import mx.familiasquesuman.app.ui.components.CampoFormulario
import mx.familiasquesuman.app.ui.components.ContenidoCarga
import mx.familiasquesuman.app.ui.components.EncabezadoAzul
import mx.familiasquesuman.app.ui.components.NotaConIcono
import mx.familiasquesuman.app.ui.components.PrimaryButton
import mx.familiasquesuman.app.ui.components.TarjetaFormulario
import mx.familiasquesuman.app.ui.components.TertiaryTextButton
import mx.familiasquesuman.app.ui.theme.CategoriaNaranja
import mx.familiasquesuman.app.ui.theme.CategoriaVerde
import mx.familiasquesuman.app.viewmodel.Carga

private val formatoCorreo = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
private val formatoTelefono = Regex("^\\+?[0-9]{10,15}$")
private val formatoFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy")

/**
 * Qué tan fuerte se ve la contraseña, solo como guía visual. La regla real (mínimo 10,
 * sin contraseñas comunes ni tu nombre o correo) la aplica el backend.
 * Devuelve 0 vacía, 1 débil, 2 aceptable y 3 fuerte.
 */
private fun fuerza(p: String): Int {
    if (p.isEmpty()) return 0
    if (p.length < 10) return 1
    val variedad = listOf(
        p.any { it.isLowerCase() },
        p.any { it.isUpperCase() },
        p.any { it.isDigit() },
        p.any { !it.isLetterOrDigit() }
    ).count { it }
    return if (p.length >= 14 && variedad >= 3) 3 else if (variedad >= 2) 2 else 1
}

/**
 * Paso 1 de 2 del registro, con el mismo estilo de Bienvenida. Junta los datos de la
 * cuenta y en el paso 2 (preferencias) se elige el username y se acepta el aviso.
 * Todas las cuentas son individuales por decisión del socio formador. La familia se
 * arma después agregando acompañantes en Mi Comunidad.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistroScreen(
    catalogo: Carga<Catalogo>,
    onReintentarCatalogo: () -> Unit,
    onContinuar: (DatosRegistro) -> Unit,
    onBack: () -> Unit,
    onIniciarSesion: () -> Unit = {},
    animacionesActivas: Boolean = true,
    /** Lo que ya se había capturado, al regresar del paso 2 los campos no se pierden. */
    inicial: DatosRegistro? = null
) {
    var nombre by remember { mutableStateOf(inicial?.nombre.orEmpty()) }
    var contacto by remember { mutableStateOf(inicial?.contacto.orEmpty()) }
    var contrasena by remember { mutableStateOf(inicial?.password.orEmpty()) }
    var confirmar by remember { mutableStateOf(inicial?.password.orEmpty()) }
    var nacimiento by remember {
        mutableStateOf(inicial?.fechaNacimientoIso?.let { runCatching { LocalDate.parse(it) }.getOrNull() })
    }
    var mostrarCalendario by remember { mutableStateOf(false) }
    var ciudadExpandida by remember { mutableStateOf(false) }
    var ciudad by remember(catalogo) {
        mutableStateOf((catalogo as? Carga.Listo)?.datos?.ciudades?.firstOrNull { it.id == inicial?.ciudadId })
    }

    val contactoLimpio = contacto.trim().let { if (it.contains("@")) it else it.filter { c -> c.isDigit() || c == '+' } }
    val contactoValido = formatoCorreo.matches(contactoLimpio) || formatoTelefono.matches(contactoLimpio)
    val nacimientoIso = nacimiento?.toString()
    val edad = nacimientoIso?.let { edadDesdeIso(it) }
    val errorNacimiento = if (edad != null && edad < 18) "La persona titular debe ser mayor de edad" else null
    val nivel = fuerza(contrasena)

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            EncabezadoAzul(
                titulo = "Crea tu cuenta",
                subtitulo = "Te toma menos de un minuto. Después eliges qué te interesa.",
                icono = Icons.Filled.PersonAdd,
                onBack = onBack
            ) {
                // Indicador de pasos, este es el 1 de 2
                Row(
                    modifier = Modifier.padding(top = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(Modifier.width(28.dp).height(6.dp).background(Color.White, RoundedCornerShape(3.dp)))
                    Box(Modifier.width(28.dp).height(6.dp).background(Color.White.copy(alpha = 0.35f), RoundedCornerShape(3.dp)))
                    Text(
                        "Paso 1 de 2",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
            }

            TarjetaFormulario(
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .padding(horizontal = 20.dp)
                    .offset(y = (-32).dp)
            ) {
                CampoFormulario(
                    valor = nombre,
                    alCambiar = { nombre = it.take(120) },
                    etiqueta = "Nombre completo",
                    icono = Icons.Filled.Person
                )
                CampoFormulario(
                    valor = contacto,
                    alCambiar = { contacto = it.take(254) },
                    etiqueta = "Correo o teléfono",
                    icono = Icons.Filled.AlternateEmail,
                    teclado = KeyboardType.Email,
                    error = contacto.isNotBlank() && !contactoValido,
                    ayuda = when {
                        contacto.isNotBlank() && !contactoValido -> "Escribe un correo o un teléfono de 10 dígitos"
                        // Le avisa a dónde llegará el código para crear la cuenta
                        contactoValido && contactoLimpio.contains("@") -> "Te enviaremos un código a este correo"
                        contactoValido -> "Te enviaremos un código por SMS a este número"
                        else -> null
                    },
                    modifier = Modifier.padding(top = 12.dp)
                )
                CampoContrasena(
                    valor = contrasena,
                    alCambiar = { contrasena = it.take(128) },
                    etiqueta = "Contraseña",
                    modifier = Modifier.padding(top = 12.dp)
                )
                MedidorContrasena(nivel)
                CampoContrasena(
                    valor = confirmar,
                    alCambiar = { confirmar = it.take(128) },
                    etiqueta = "Confirmar contraseña",
                    error = confirmar.isNotEmpty() && confirmar != contrasena,
                    ayuda = if (confirmar.isNotEmpty() && confirmar != contrasena) "Las contraseñas no coinciden" else null,
                    modifier = Modifier.padding(top = 12.dp)
                )

                // Fecha con calendario, así no hay que adivinar el formato
                Box(modifier = Modifier.padding(top = 12.dp)) {
                    CampoFormulario(
                        valor = nacimiento?.format(formatoFecha) ?: "",
                        alCambiar = {},
                        etiqueta = "Fecha de nacimiento",
                        icono = Icons.Filled.Cake,
                        soloLectura = true,
                        placeholder = "Toca para elegir",
                        error = errorNacimiento != null,
                        ayuda = errorNacimiento
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable(onClickLabel = "Elegir fecha de nacimiento") { mostrarCalendario = true }
                    )
                }

                ContenidoCarga(estado = catalogo, onReintentar = onReintentarCatalogo) { cat ->
                    ExposedDropdownMenuBox(
                        expanded = ciudadExpandida,
                        onExpandedChange = { ciudadExpandida = it },
                        modifier = Modifier.padding(top = 12.dp)
                    ) {
                        CampoFormulario(
                            valor = ciudad?.nombre ?: "",
                            alCambiar = {},
                            etiqueta = "Ciudad",
                            icono = Icons.Filled.LocationCity,
                            soloLectura = true,
                            placeholder = "Selecciona tu ciudad",
                            final = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = ciudadExpandida) },
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(expanded = ciudadExpandida, onDismissRequest = { ciudadExpandida = false }) {
                            cat.ciudades.forEach { c ->
                                DropdownMenuItem(text = { Text(c.nombre) }, onClick = { ciudad = c; ciudadExpandida = false })
                            }
                        }
                    }
                }

                val completo = nombre.trim().length >= 2 && contactoValido && contrasena.length >= 10 &&
                    contrasena == confirmar && nacimientoIso != null && errorNacimiento == null && ciudad != null
                PrimaryButton(
                    text = "Continuar",
                    enabled = completo,
                    onClick = {
                        val c = ciudad ?: return@PrimaryButton
                        val iso = nacimientoIso ?: return@PrimaryButton
                        onContinuar(
                            DatosRegistro(
                                nombre = nombre.trim(),
                                contacto = contactoLimpio,
                                password = contrasena,
                                fechaNacimientoIso = iso,
                                ciudadId = c.id
                            )
                        )
                    },
                    modifier = Modifier.padding(top = 24.dp)
                )
                NotaConIcono(
                    icono = Icons.Filled.Shield,
                    texto = "Solo usamos tus datos para tus inscripciones y avisos.",
                    modifier = Modifier.padding(top = 16.dp).align(Alignment.CenterHorizontally)
                )
            }

            Row(
                modifier = Modifier.padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "¿Ya tienes cuenta?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TertiaryTextButton(text = "Inicia sesión", onClick = onIniciarSesion)
            }
        }
    }

    if (mostrarCalendario) {
        CalendarioNacimiento(
            inicial = nacimiento,
            onElegir = { nacimiento = it; mostrarCalendario = false },
            onCerrar = { mostrarCalendario = false }
        )
    }
}

/** Barra de tres segmentos que se llenan según la fuerza de la contraseña. */
@Composable
private fun MedidorContrasena(nivel: Int) {
    if (nivel == 0) {
        Text(
            "Usa al menos 10 caracteres y evita tu nombre o correo",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp, start = 4.dp)
        )
        return
    }
    val (color, texto) = when (nivel) {
        1 -> MaterialTheme.colorScheme.error to "Débil, usa al menos 10 caracteres"
        2 -> CategoriaNaranja to "Aceptable, una frase larga la hace más fuerte"
        else -> CategoriaVerde to "Fuerte"
    }
    Column(modifier = Modifier.padding(top = 8.dp, start = 4.dp, end = 4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(3) { i ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(5.dp)
                        .background(
                            if (i < nivel) color else MaterialTheme.colorScheme.surfaceContainerHigh,
                            RoundedCornerShape(3.dp)
                        )
                )
            }
        }
        Text(texto, style = MaterialTheme.typography.bodySmall, color = color, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 4.dp))
    }
}

/**
 * Calendario para la fecha de nacimiento. Solo deja elegir fechas pasadas y abre en un
 * año razonable para un adulto. El calendario trabaja en UTC, por eso la conversión.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarioNacimiento(inicial: LocalDate?, onElegir: (LocalDate) -> Unit, onCerrar: () -> Unit) {
    val hoy = LocalDate.now()
    val sugerida = inicial ?: hoy.minusYears(30)
    val estado = rememberDatePickerState(
        initialSelectedDateMillis = inicial?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
        initialDisplayedMonthMillis = sugerida.withDayOfMonth(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        yearRange = (hoy.year - 110)..hoy.year,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                !Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate().isAfter(hoy)
        }
    )
    DatePickerDialog(
        onDismissRequest = onCerrar,
        confirmButton = {
            TextButton(
                enabled = estado.selectedDateMillis != null,
                onClick = {
                    estado.selectedDateMillis?.let { ms ->
                        onElegir(Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                }
            ) { Text("Listo") }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar") } }
    ) {
        DatePicker(state = estado, title = { Text("Fecha de nacimiento", modifier = Modifier.padding(start = 24.dp, top = 16.dp)) })
    }
}
