package mx.familiasquesuman.app.ui.screens

import androidx.compose.foundation.background
import mx.familiasquesuman.app.ui.components.BannerAzul
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import mx.familiasquesuman.app.data.Asociacion
import mx.familiasquesuman.app.data.Categoria
import mx.familiasquesuman.app.data.Ciudad
import mx.familiasquesuman.app.ui.components.CiudadSelector
import mx.familiasquesuman.app.viewmodel.AlTerminar
import mx.familiasquesuman.app.ui.components.BrandTopBar
import mx.familiasquesuman.app.ui.components.CargandoBloque
import mx.familiasquesuman.app.ui.components.CategoriaBadge
import mx.familiasquesuman.app.ui.components.CategoriaEtiquetaConColor
import mx.familiasquesuman.app.ui.components.ErrorBloque
import mx.familiasquesuman.app.ui.components.FilterChip
import mx.familiasquesuman.app.ui.components.GhostBorderCard
import mx.familiasquesuman.app.ui.components.UbicacionRow
import mx.familiasquesuman.app.ui.components.VacioBloque
import mx.familiasquesuman.app.viewmodel.Carga

/**
 * Basada en explorar_familias_que_suman_azul. Solo muestra asociaciones verificadas de
 * la ciudad del perfil (RF-01 y RF-02), la ciudad se cambia desde aquí mismo.
 */
@Composable
fun ExplorarScreen(
    resultados: Carga<List<Asociacion>>,
    ciudadId: String?,
    ciudadNombre: String?,
    ciudades: List<Ciudad>,
    onCambiarCiudad: (String, AlTerminar<Unit>) -> Unit,
    onBuscar: (categoria: Categoria?, texto: String) -> Unit,
    onVerAsociacion: (String) -> Unit,
    onProponerAsociacion: () -> Unit = {},
    onMenu: () -> Unit = {},
    onNotificaciones: () -> Unit = {},
    hayNotificacionesSinLeer: Boolean = false
) {
    var busqueda by remember { mutableStateOf("") }
    var categoriaSeleccionada by remember { mutableStateOf<Categoria?>(null) }

    // La búsqueda se hace en el servidor; el ViewModel espera un momento entre teclas
    LaunchedEffect(categoriaSeleccionada, busqueda) {
        onBuscar(categoriaSeleccionada, busqueda)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        BrandTopBar(onMenu = onMenu, onNotificaciones = onNotificaciones, hayNotificacionesSinLeer = hayNotificacionesSinLeer)

        // Todo va en una sola lista, así el banner y los filtros se desplazan con los
        // resultados y en pantallas chicas se ven más asociaciones
        LazyColumn(
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            item(key = "banner") {
                BannerAzul(modifier = Modifier.padding(horizontal = 20.dp).padding(top = 4.dp)) {
                    Text(
                        "Descubre oportunidades",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        "Asociaciones verificadas de tu ciudad",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.88f),
                        modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
                    )
                    OutlinedTextField(
                        value = busqueda,
                        onValueChange = { busqueda = it.take(80) },
                        placeholder = { Text("Buscar asociaciones") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions.Default,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 12.dp)
                    ) {
                        CiudadSelector(
                            ciudadActualId = ciudadId,
                            ciudadActualNombre = ciudadNombre,
                            ciudades = ciudades,
                            onCambiar = onCambiarCiudad,
                            sobreAzul = true
                        )
                        Row(
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(percent = 50))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Verified, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Text(
                                "Verificadas",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                }
            }

            // Chips de categoría con scroll horizontal, con 6 categorías más "Todos" no caben
            item(key = "categorias") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(texto = "Todos", seleccionado = categoriaSeleccionada == null, onClick = { categoriaSeleccionada = null })
                    Categoria.entries.forEach { cat ->
                        FilterChip(
                            texto = cat.etiqueta,
                            icono = cat.icono,
                            seleccionado = categoriaSeleccionada == cat,
                            onClick = { categoriaSeleccionada = if (categoriaSeleccionada == cat) null else cat }
                        )
                    }
                }
            }

            when (resultados) {
                is Carga.Cargando -> item(key = "cargando") { CargandoBloque() }
                is Carga.Error -> item(key = "error") {
                    ErrorBloque(resultados.mensaje, onReintentar = { onBuscar(categoriaSeleccionada, busqueda) })
                }
                is Carga.Listo -> if (resultados.datos.isEmpty()) {
                    item(key = "vacio") { VacioBloque("No encontramos asociaciones con esos filtros.") }
                } else {
                    items(resultados.datos, key = { it.id }) { asociacion ->
                        Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                            AsociacionListaCard(asociacion = asociacion, onClick = { onVerAsociacion(asociacion.id) })
                        }
                    }
                }
            }

            // Entrada al flujo CP-01, al final de la lista donde la persona ya buscó y no encontró
            item(key = "proponer") {
                GhostBorderCard(onClick = onProponerAsociacion, modifier = Modifier.padding(horizontal = 20.dp).padding(top = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.AddBusiness, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                            Text("¿No encuentras una asociación?", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Propón que se sume a Familias que Suman",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
private fun AsociacionListaCard(asociacion: Asociacion, onClick: () -> Unit) {
    GhostBorderCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.Top) {
            CategoriaBadge(categoria = asociacion.categoria, tamano = 44.dp)
            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                CategoriaEtiquetaConColor(categoria = asociacion.categoria)
                Text(
                    asociacion.nombre,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    asociacion.descripcion,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                )
                UbicacionRow(texto = asociacion.ubicacion)
            }
        }
    }
}