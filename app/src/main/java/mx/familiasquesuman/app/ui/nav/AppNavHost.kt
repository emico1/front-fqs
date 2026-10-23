package mx.familiasquesuman.app.ui.nav

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import mx.familiasquesuman.app.FqsApplication
import mx.familiasquesuman.app.data.TipoPropuesta
import mx.familiasquesuman.app.ui.components.AppBottomNavBar
import mx.familiasquesuman.app.ui.components.AppDrawerContent
import mx.familiasquesuman.app.ui.components.AvisoConexion
import mx.familiasquesuman.app.ui.components.ErrorBloque
import mx.familiasquesuman.app.ui.components.FormasFlotantesFondo
import mx.familiasquesuman.app.ui.screens.ActividadDetalleScreen
import mx.familiasquesuman.app.ui.screens.AsociacionDetalleScreen
import mx.familiasquesuman.app.ui.screens.BienvenidaScreen
import mx.familiasquesuman.app.ui.screens.CampanaDonacionScreen
import mx.familiasquesuman.app.ui.screens.EstadoPropuestaScreen
import mx.familiasquesuman.app.ui.screens.EncuestaScreen
import mx.familiasquesuman.app.ui.screens.ExplorarScreen
import mx.familiasquesuman.app.ui.screens.FavoritosScreen
import mx.familiasquesuman.app.ui.screens.HistoriasScreen
import mx.familiasquesuman.app.ui.screens.HistorialScreen
import mx.familiasquesuman.app.ui.screens.InicioScreen
import mx.familiasquesuman.app.ui.screens.LoginScreen
import mx.familiasquesuman.app.ui.screens.SeguridadAdminScreen
import mx.familiasquesuman.app.ui.screens.SesionesScreen
import mx.familiasquesuman.app.ui.screens.VerificarContactoScreen
import mx.familiasquesuman.app.ui.screens.MiComunidadScreen
import mx.familiasquesuman.app.ui.screens.MisPropuestasScreen
import mx.familiasquesuman.app.ui.screens.NotificacionesScreen
import mx.familiasquesuman.app.ui.screens.OnboardingPreferenciasScreen
import mx.familiasquesuman.app.ui.screens.ParticiparScreen
import mx.familiasquesuman.app.ui.screens.PerfilScreen
import mx.familiasquesuman.app.ui.screens.PrivacidadScreen
import mx.familiasquesuman.app.ui.screens.ProponerIniciativaScreen
import mx.familiasquesuman.app.ui.screens.PropuestaEnviadaScreen
import mx.familiasquesuman.app.ui.screens.ProyectoDetalleScreen
import mx.familiasquesuman.app.ui.screens.RegistroScreen
import mx.familiasquesuman.app.ui.screens.ReportarExperienciaScreen
import mx.familiasquesuman.app.ui.screens.TodasCampanasScreen
import mx.familiasquesuman.app.ui.screens.TodosVoluntariadosScreen
import mx.familiasquesuman.app.viewmodel.AppViewModel
import mx.familiasquesuman.app.viewmodel.Carga
import mx.familiasquesuman.app.viewmodel.EstadoSesion

/** Rutas que muestran la barra de navegación inferior (las 4 pestañas principales). */
private val rutasConBottomNav = TabDestino.entries.map { it.ruta }.toSet()

/** Rutas donde tiene sentido abrir el menú lateral (las mismas 4 pantallas principales). */
private val rutasConDrawer = rutasConBottomNav

/**
 * Decide qué mostrar según la sesión. Mientras se verifica la sesión guardada se ve un
 * indicador, si no hay red se ofrece reintentar, y en cuanto hay respuesta se arma la
 * navegación empezando en Bienvenida o en Inicio.
 */
@Composable
fun AppNavHost(esPantallaAncha: Boolean) {
    val appViewModel: AppViewModel = viewModel()

    // El flujo de cupos en tiempo real solo está abierto con sesión y con la app en
    // primer plano, así no gasta batería ni datos en segundo plano.
    val lifecycleOwner = LocalLifecycleOwner.current
    val sesionActiva = appViewModel.estadoSesion is EstadoSesion.Activa
    DisposableEffect(lifecycleOwner, sesionActiva) {
        val observador = LifecycleEventObserver { _, evento ->
            when (evento) {
                Lifecycle.Event.ON_START -> appViewModel.cuposEnPrimerPlano(true)
                Lifecycle.Event.ON_STOP -> appViewModel.cuposEnPrimerPlano(false)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observador)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observador) }
    }

    when (val estado = appViewModel.estadoSesion) {
        is EstadoSesion.Verificando -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        is EstadoSesion.SinConexion -> Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            ErrorBloque(estado.mensaje, onReintentar = { appViewModel.verificarSesion() })
        }
        else -> NavegacionApp(
            esPantallaAncha = esPantallaAncha,
            appViewModel = appViewModel,
            inicio = if (estado is EstadoSesion.Activa) Routes.INICIO else Routes.BIENVENIDA
        )
    }
}

private fun NavHostController.irALaRaiz(ruta: String) {
    navigate(ruta) { popUpTo(0) { inclusive = true } }
}

@Composable
private fun NavegacionApp(esPantallaAncha: Boolean, appViewModel: AppViewModel, inicio: String) {
    val navController = rememberNavController()
    // El destino inicial se fija una sola vez. Si cambiara al iniciar o cerrar sesión, el
    // NavHost reconstruiría el grafo y la navegación explícita se duplicaría.
    val destinoInicial = remember { inicio }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val rutaActual = backStackEntry?.destination?.route
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val fondo = appViewModel.fondoAnimadoActivo

    // Si la sesión expira o se revoca mientras se usa la app, se regresa a Bienvenida
    val sesion = appViewModel.estadoSesion
    LaunchedEffect(sesion) {
        if (sesion is EstadoSesion.Anonima && rutaActual != null && rutaActual !in Routes.publicas) {
            navController.irALaRaiz(Routes.BIENVENIDA)
        }
    }

    // Notificación push tocada (ver MainActivity). Se abre su pantalla cuando hay sesión.
    val context = LocalContext.current
    val app = context.applicationContext as FqsApplication
    val enlace by app.enlacePendiente.collectAsState()
    LaunchedEffect(enlace, sesion is EstadoSesion.Activa) {
        val ruta = enlace ?: return@LaunchedEffect
        if (sesion is EstadoSesion.Activa) {
            app.enlacePendiente.value = null
            appViewModel.cargarNotificaciones()
            navController.navigate(ruta) { launchSingleTop = true }
        }
    }

    // Android 13 o más pide permiso para mostrar notificaciones. Se pregunta una vez
    // al tener sesión, si la persona lo niega la app sigue funcionando igual.
    val pedirPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(sesion is EstadoSesion.Activa) {
        if (sesion is EstadoSesion.Activa && Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            pedirPermiso.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = rutaActual in rutasConDrawer,
        drawerContent = {
            // Sin márgenes del sistema, el encabezado azul del menú llega hasta arriba
            ModalDrawerSheet(windowInsets = WindowInsets(0, 0, 0, 0)) {
                AppDrawerContent(
                    rutaActual = rutaActual,
                    nombre = appViewModel.perfil?.nombre,
                    ciudad = appViewModel.perfil?.ciudadNombre,
                    onAbrir = { ruta ->
                        scope.launch { drawerState.close() }
                        navController.navigate(ruta) { launchSingleTop = true }
                    },
                    onNavegar = { ruta ->
                        scope.launch { drawerState.close() }
                        navController.navigate(ruta) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onCerrarSesion = {
                        scope.launch { drawerState.close() }
                        appViewModel.cerrarSesion { navController.irALaRaiz(Routes.BIENVENIDA) }
                    }
                )
            }
        }
    ) {
        Scaffold(
            // Cada pantalla ya maneja su propio inset de status bar a través de su TopAppBar
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (rutaActual in rutasConBottomNav) {
                    val tabActual = TabDestino.entries.firstOrNull { it.ruta == rutaActual } ?: TabDestino.INICIO
                    AppBottomNavBar(
                        pestanaActual = tabActual,
                        onSeleccionar = { tab ->
                            navController.navigate(tab.ruta) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        ) { paddingValues ->
            Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    NavHost(navController = navController, startDestination = destinoInicial) {

                        // ---------------- Sin sesión ----------------

                        composable(Routes.BIENVENIDA) {
                            BienvenidaScreen(
                                onIniciarSesion = { navController.navigate(Routes.LOGIN) },
                                onCrearCuenta = { navController.navigate(Routes.REGISTRO) },
                                animacionesActivas = fondo
                            )
                        }
                        composable(Routes.LOGIN) {
                            LoginScreen(
                                onIniciarSesion = appViewModel::iniciarSesion,
                                onVerificarCodigo = appViewModel::verificarSegundoFactor,
                                onSolicitarCodigo = appViewModel::solicitarCodigo,
                                onRestablecer = appViewModel::restablecer,
                                onSesionIniciada = { navController.irALaRaiz(Routes.INICIO) },
                                onCrearCuenta = { navController.navigate(Routes.REGISTRO) },
                                onBack = { navController.popBackStack() },
                                animacionesActivas = fondo
                            )
                        }
                        composable(Routes.REGISTRO) {
                            RegistroScreen(
                                catalogo = appViewModel.catalogo,
                                onReintentarCatalogo = appViewModel::cargarCatalogo,
                                onContinuar = { datos ->
                                    appViewModel.guardarRegistroPendiente(datos)
                                    navController.navigate(Routes.ONBOARDING_PREFERENCIAS)
                                },
                                onBack = { navController.popBackStack() },
                                onIniciarSesion = {
                                    navController.navigate(Routes.LOGIN) { popUpTo(Routes.REGISTRO) { inclusive = true } }
                                },
                                animacionesActivas = fondo,
                                inicial = appViewModel.registroPendiente
                            )
                        }
                        composable(Routes.ONBOARDING_PREFERENCIAS) {
                            val catalogo = (appViewModel.catalogo as? Carga.Listo)?.datos
                            OnboardingPreferenciasScreen(
                                categorias = catalogo?.categorias.orEmpty(),
                                aviso = catalogo?.aviso,
                                perfil = null,
                                onGuardar = { username, donar, participar, proponer, categorias, alTerminar ->
                                    appViewModel.completarRegistro(username, donar, participar, proponer, categorias, alTerminar)
                                },
                                onListo = {
                                    // Falta escribir el código que llegó por correo o SMS
                                    if (appViewModel.verificacionContacto != null) navController.navigate(Routes.VERIFICAR_CONTACTO)
                                    else navController.irALaRaiz(Routes.INICIO)
                                },
                                onBack = { navController.popBackStack() },
                                animacionesActivas = fondo
                            )
                        }
                        composable(Routes.VERIFICAR_CONTACTO) {
                            VerificarContactoScreen(
                                destino = appViewModel.verificacionContacto?.destino.orEmpty(),
                                porSms = appViewModel.verificacionContacto?.porSms == true,
                                onVerificar = appViewModel::verificarRegistro,
                                onReenviar = appViewModel::reenviarCodigoRegistro,
                                onListo = { navController.irALaRaiz(Routes.INICIO) },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        // ---------------- Pestañas principales ----------------

                        composable(Routes.INICIO) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                FormasFlotantesFondo(activo = fondo)
                                val perfil = appViewModel.perfil
                                InicioScreen(
                                    esPantallaAncha = esPantallaAncha,
                                    asociaciones = appViewModel.asociaciones,
                                    actividades = appViewModel.actividades,
                                    campanas = appViewModel.campanas,
                                    proyectos = appViewModel.proyectos,
                                    filtro = appViewModel.filtroInicio,
                                    onFiltro = appViewModel::elegirFiltroInicio,
                                    nombre = perfil?.nombre,
                                    ciudadId = perfil?.ciudadId,
                                    ciudadNombre = perfil?.ciudadNombre,
                                    ciudades = (appViewModel.catalogo as? Carga.Listo)?.datos?.ciudades.orEmpty(),
                                    onCambiarCiudad = appViewModel::cambiarCiudad,
                                    avisoPendiente = perfil?.avisoPendiente == true,
                                    onVerPrivacidad = { navController.navigate(Routes.PRIVACIDAD) },
                                    onVerProyecto = { id -> navController.navigate(Routes.proyectoDetalle(id)) },
                                    onVerHistorias = { navController.navigate(Routes.HISTORIAS) },
                                    onReintentar = appViewModel::cargarInicio,
                                    onVerAsociacion = { id -> navController.navigate(Routes.asociacionDetalle(id)) },
                                    onVerActividad = { id -> navController.navigate(Routes.actividadDetalle(id)) },
                                    onVerTodasAsociaciones = { navController.navigate(Routes.EXPLORAR) { launchSingleTop = true } },
                                    onVerCampana = { id -> navController.navigate(Routes.campanaDonacion(id)) },
                                    onMenu = { scope.launch { drawerState.open() } },
                                    onNotificaciones = { navController.navigate(Routes.NOTIFICACIONES) },
                                    hayNotificacionesSinLeer = appViewModel.hayNotificacionesSinLeer
                                )
                            }
                        }
                        composable(Routes.EXPLORAR) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                FormasFlotantesFondo(activo = fondo)
                                ExplorarScreen(
                                    resultados = appViewModel.explorar,
                                    ciudadId = appViewModel.perfil?.ciudadId,
                                    ciudadNombre = appViewModel.perfil?.ciudadNombre,
                                    ciudades = (appViewModel.catalogo as? Carga.Listo)?.datos?.ciudades.orEmpty(),
                                    onCambiarCiudad = appViewModel::cambiarCiudad,
                                    onProponerAsociacion = { navController.navigate(Routes.proponerIniciativa(TipoPropuesta.ASOCIACION.api)) },
                                    onBuscar = appViewModel::buscarAsociaciones,
                                    onVerAsociacion = { id -> navController.navigate(Routes.asociacionDetalle(id)) },
                                    onMenu = { scope.launch { drawerState.open() } },
                                    onNotificaciones = { navController.navigate(Routes.NOTIFICACIONES) },
                                    hayNotificacionesSinLeer = appViewModel.hayNotificacionesSinLeer
                                )
                            }
                        }
                        composable(Routes.PARTICIPAR) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                FormasFlotantesFondo(activo = fondo)
                                ParticiparScreen(
                                    actividades = appViewModel.actividades,
                                    campanas = appViewModel.campanas,
                                    onReintentar = appViewModel::cargarInicio,
                                    onProponerIniciativa = { navController.navigate(Routes.proponerIniciativa()) },
                                    onVerActividad = { id -> navController.navigate(Routes.actividadDetalle(id)) },
                                    onVerCampana = { id -> navController.navigate(Routes.campanaDonacion(id)) },
                                    onVerTodosVoluntariados = { navController.navigate(Routes.TODOS_VOLUNTARIADOS) },
                                    onVerTodasCampanas = { navController.navigate(Routes.TODAS_CAMPANAS) },
                                    onMenu = { scope.launch { drawerState.open() } },
                                    onNotificaciones = { navController.navigate(Routes.NOTIFICACIONES) },
                                    hayNotificacionesSinLeer = appViewModel.hayNotificacionesSinLeer
                                )
                            }
                        }
                        composable(Routes.PERFIL) {
                            LaunchedEffect(Unit) { appViewModel.cargarPerfil() }
                            val perfil = appViewModel.perfil
                            Box(modifier = Modifier.fillMaxSize()) {
                                FormasFlotantesFondo(activo = fondo)
                                if (perfil != null) {
                                    PerfilScreen(
                                        perfil = perfil,
                                        comunidad = appViewModel.comunidad,
                                        proximas = appViewModel.proximas,
                                        pasadas = appViewModel.pasadas,
                                        onReintentar = appViewModel::cargarPerfil,
                                        onCambiarNombre = appViewModel::cambiarNombre,
                                        onCambiarFoto = appViewModel::cambiarFoto,
                                        onPublicarTestimonio = appViewModel::publicarTestimonio,
                                        fondoAnimadoActivo = fondo,
                                        onCambiarFondoAnimado = appViewModel::cambiarFondoAnimado,
                                        onProponerIniciativa = { navController.navigate(Routes.proponerIniciativa()) },
                                        onMiComunidad = { navController.navigate(Routes.MI_COMUNIDAD) },
                                        onMisPropuestas = { navController.navigate(Routes.MIS_PROPUESTAS) },
                                        onPreferencias = { navController.navigate(Routes.PREFERENCIAS) },
                                        onHistorial = { navController.navigate(Routes.HISTORIAL) },
                                        onPrivacidad = { navController.navigate(Routes.PRIVACIDAD) },
                                        onSeguridadAdmin = { navController.navigate(Routes.SEGURIDAD_ADMIN) },
                                        onSesiones = { navController.navigate(Routes.SESIONES) },
                                        onFavoritos = { navController.navigate(Routes.FAVORITOS) },
                                        onVerActividad = { id -> navController.navigate(Routes.actividadDetalle(id)) },
                                        onMenu = { scope.launch { drawerState.open() } },
                                        onNotificaciones = { navController.navigate(Routes.NOTIFICACIONES) },
                                        hayNotificacionesSinLeer = appViewModel.hayNotificacionesSinLeer
                                    )
                                }
                            }
                        }

                        // ---------------- Detalles y formularios ----------------

                        composable(Routes.PREFERENCIAS) {
                            val catalogo = (appViewModel.catalogo as? Carga.Listo)?.datos
                            OnboardingPreferenciasScreen(
                                categorias = catalogo?.categorias.orEmpty(),
                                aviso = catalogo?.aviso,
                                perfil = appViewModel.perfil,
                                esEdicion = true,
                                onGuardar = { _, donar, participar, proponer, categorias, alTerminar ->
                                    appViewModel.guardarPreferencias(donar, participar, proponer, categorias, alTerminar)
                                },
                                onListo = { navController.popBackStack() },
                                onBack = { navController.popBackStack() },
                                animacionesActivas = fondo
                            )
                        }
                        composable(Routes.ASOCIACION_DETALLE) { entry ->
                            val id = entry.arguments?.getString("asociacionId").orEmpty()
                            LaunchedEffect(id) { appViewModel.cargarAsociacion(id) }
                            Box(modifier = Modifier.fillMaxSize()) {
                                FormasFlotantesFondo(activo = fondo)
                                AsociacionDetalleScreen(
                                    estado = appViewModel.asociacionDetalle,
                                    onReintentar = { appViewModel.cargarAsociacion(id) },
                                    onBack = { navController.popBackStack() },
                                    onVerActividad = { actId -> navController.navigate(Routes.actividadDetalle(actId)) },
                                    onCambiarNotificaciones = { activas, alTerminar ->
                                        appViewModel.cambiarNotificacionesAsociacion(id, activas, alTerminar)
                                    },
                                    onReportar = { asocId -> navController.navigate(Routes.reportarExperiencia(asociacionId = asocId)) }
                                )
                            }
                        }
                        composable(Routes.ACTIVIDAD_DETALLE) { entry ->
                            val id = entry.arguments?.getString("actividadId").orEmpty()
                            LaunchedEffect(id) { appViewModel.cargarActividad(id) }
                            Box(modifier = Modifier.fillMaxSize()) {
                                FormasFlotantesFondo(activo = fondo)
                                ActividadDetalleScreen(
                                    estado = appViewModel.actividadDetalle,
                                    miembrosComunidad = appViewModel.comunidad,
                                    onReintentar = { appViewModel.cargarActividad(id) },
                                    onBack = { navController.popBackStack() },
                                    onIrAMiComunidad = { navController.navigate(Routes.MI_COMUNIDAD) },
                                    onInscribir = { titular, miembros, alTerminar ->
                                        appViewModel.inscribir(id, titular, miembros, alTerminar)
                                    },
                                    onCancelarInscripcion = { alTerminar -> appViewModel.cancelarInscripcion(id, alTerminar) },
                                    onInscripcionExitosa = { navController.popBackStack() },
                                    onFavorita = { favorita, alTerminar -> appViewModel.cambiarActividadFavorita(id, favorita, alTerminar) },
                                    onVerEncuestas = { inscripcionId -> navController.navigate(Routes.encuesta(id, inscripcionId)) },
                                    esAdmin = appViewModel.perfil?.esAdmin == true
                                )
                            }
                        }
                        composable(Routes.MI_COMUNIDAD) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                FormasFlotantesFondo(activo = fondo)
                                MiComunidadScreen(
                                    miembros = appViewModel.comunidad,
                                    onAgregar = appViewModel::agregarMiembro,
                                    onEliminar = appViewModel::eliminarMiembro,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                        }
                        composable(
                            route = Routes.PROPONER_INICIATIVA,
                            arguments = listOf(navArgument("tipo") { type = NavType.StringType; nullable = true; defaultValue = null })
                        ) { entry ->
                            val tipoInicial = entry.arguments?.getString("tipo")?.let { TipoPropuesta.desdeApi(it) }
                            Box(modifier = Modifier.fillMaxSize()) {
                                FormasFlotantesFondo(activo = fondo)
                                ProponerIniciativaScreen(
                                    tipoInicial = tipoInicial,
                                    onVerAsociacion = { asocId -> navController.navigate(Routes.asociacionDetalle(asocId)) },
                                    onEnviar = appViewModel::enviarPropuesta,
                                    onEnviada = {
                                        navController.navigate(Routes.PROPUESTA_ENVIADA) {
                                            popUpTo(Routes.PROPONER_INICIATIVA) { inclusive = true }
                                        }
                                    },
                                    onBack = { navController.popBackStack() }
                                )
                            }
                        }
                        composable(Routes.PROPUESTA_ENVIADA) {
                            PropuestaEnviadaScreen(
                                onVolverInicio = { navController.navigate(Routes.INICIO) { popUpTo(Routes.INICIO) { inclusive = true } } },
                                onVerMisPropuestas = { navController.navigate(Routes.MIS_PROPUESTAS) },
                                animacionesActivas = fondo
                            )
                        }
                        composable(Routes.MIS_PROPUESTAS) {
                            LaunchedEffect(Unit) { appViewModel.cargarPropuestas() }
                            Box(modifier = Modifier.fillMaxSize()) {
                                FormasFlotantesFondo(activo = fondo)
                                MisPropuestasScreen(
                                    estado = appViewModel.propuestas,
                                    onReintentar = appViewModel::cargarPropuestas,
                                    onBack = { navController.popBackStack() },
                                    onVerPropuesta = { id -> navController.navigate(Routes.estadoPropuesta(id)) }
                                )
                            }
                        }
                        composable(Routes.ESTADO_PROPUESTA) { entry ->
                            val id = entry.arguments?.getString("propuestaId").orEmpty()
                            LaunchedEffect(id) { appViewModel.cargarPropuesta(id) }
                            Box(modifier = Modifier.fillMaxSize()) {
                                FormasFlotantesFondo(activo = fondo)
                                EstadoPropuestaScreen(
                                    estado = appViewModel.propuestaDetalle,
                                    onReintentar = { appViewModel.cargarPropuesta(id) },
                                    onBack = { navController.popBackStack() }
                                )
                            }
                        }
                        composable(Routes.CAMPANA_DONACION) { entry ->
                            val id = entry.arguments?.getString("campanaId").orEmpty()
                            LaunchedEffect(id) { appViewModel.cargarCampana(id) }
                            Box(modifier = Modifier.fillMaxSize()) {
                                FormasFlotantesFondo(activo = fondo)
                                CampanaDonacionScreen(
                                    estado = appViewModel.campanaDetalle,
                                    onReintentar = { appViewModel.cargarCampana(id) },
                                    onRegistrarEntrega = { articuloId, puntoId, cantidad, comentario, alTerminar ->
                                        appViewModel.registrarEntrega(id, articuloId, puntoId, cantidad, comentario, alTerminar)
                                    },
                                    onBack = { navController.popBackStack() }
                                )
                            }
                        }
                        composable(
                            route = Routes.REPORTAR_EXPERIENCIA,
                            arguments = listOf(
                                navArgument("asociacionId") { type = NavType.StringType; nullable = true; defaultValue = null },
                                navArgument("actividadId") { type = NavType.StringType; nullable = true; defaultValue = null }
                            )
                        ) { entry ->
                            val asociacionId = entry.arguments?.getString("asociacionId")
                            val actividadId = entry.arguments?.getString("actividadId")
                            Box(modifier = Modifier.fillMaxSize()) {
                                FormasFlotantesFondo(activo = fondo)
                                ReportarExperienciaScreen(
                                    asociacionId = asociacionId,
                                    actividadId = actividadId,
                                    onBack = { navController.popBackStack() },
                                    onEnviar = { tipo, nivel, descripcion, foto, alTerminar ->
                                        // El backend exige que cada tipo lleve solo su propio objetivo
                                        appViewModel.enviarReporte(
                                            tipo = tipo,
                                            nivel = nivel,
                                            descripcion = descripcion,
                                            actividadId = actividadId.takeIf { tipo == "problema_actividad" },
                                            asociacionId = asociacionId.takeIf { tipo == "problema_asociacion" },
                                            foto = foto,
                                            alTerminar = alTerminar
                                        )
                                    },
                                    onEnviado = { navController.popBackStack() }
                                )
                            }
                        }
                        composable(Routes.NOTIFICACIONES) {
                            LaunchedEffect(Unit) { appViewModel.cargarNotificaciones() }
                            Box(modifier = Modifier.fillMaxSize()) {
                                FormasFlotantesFondo(activo = fondo)
                                NotificacionesScreen(
                                    notificaciones = appViewModel.notificaciones,
                                    onBack = { navController.popBackStack() },
                                    onMarcarLeida = appViewModel::marcarLeida,
                                    onEliminar = appViewModel::eliminarNotificacion,
                                    onAbrir = { n ->
                                        val ruta = when {
                                            n.actividadId != null -> Routes.actividadDetalle(n.actividadId)
                                            n.campanaId != null -> Routes.campanaDonacion(n.campanaId)
                                            n.propuestaId != null -> Routes.estadoPropuesta(n.propuestaId)
                                            n.asociacionId != null -> Routes.asociacionDetalle(n.asociacionId)
                                            else -> null
                                        }
                                        if (ruta != null) navController.navigate(ruta)
                                    }
                                )
                            }
                        }
                        composable(Routes.PROYECTO_DETALLE) { entry ->
                            val id = entry.arguments?.getString("proyectoId").orEmpty()
                            LaunchedEffect(id) { appViewModel.cargarProyecto(id) }
                            Box(modifier = Modifier.fillMaxSize()) {
                                FormasFlotantesFondo(activo = fondo)
                                ProyectoDetalleScreen(
                                    estado = appViewModel.proyectoDetalle,
                                    onReintentar = { appViewModel.cargarProyecto(id) },
                                    onBack = { navController.popBackStack() },
                                    onVerCampana = { campanaId -> navController.navigate(Routes.campanaDonacion(campanaId)) }
                                )
                            }
                        }
                        composable(Routes.HISTORIAL) {
                            LaunchedEffect(Unit) { appViewModel.cargarHistorial() }
                            Box(modifier = Modifier.fillMaxSize()) {
                                FormasFlotantesFondo(activo = fondo)
                                HistorialScreen(
                                    estado = appViewModel.historial,
                                    onReintentar = appViewModel::cargarHistorial,
                                    onBack = { navController.popBackStack() },
                                    onVerActividad = { id -> navController.navigate(Routes.actividadDetalle(id)) },
                                    onValorar = appViewModel::valorar
                                )
                            }
                        }
                        composable(Routes.PRIVACIDAD) {
                            LaunchedEffect(Unit) { appViewModel.cargarPrivacidad() }
                            Box(modifier = Modifier.fillMaxSize()) {
                                FormasFlotantesFondo(activo = fondo)
                                PrivacidadScreen(
                                    estado = appViewModel.privacidad,
                                    onReintentar = appViewModel::cargarPrivacidad,
                                    onBack = { navController.popBackStack() },
                                    onAceptar = appViewModel::aceptarAviso,
                                    onSolicitarEliminacion = appViewModel::solicitarEliminacion,
                                    onCuentaEliminada = { navController.irALaRaiz(Routes.BIENVENIDA) }
                                )
                            }
                        }
                        composable(Routes.SESIONES) {
                            LaunchedEffect(Unit) { appViewModel.cargarSesiones() }
                            Box(modifier = Modifier.fillMaxSize()) {
                                FormasFlotantesFondo(activo = fondo)
                                SesionesScreen(
                                    estado = appViewModel.sesionesDispositivos,
                                    onReintentar = appViewModel::cargarSesiones,
                                    onBack = { navController.popBackStack() },
                                    onCerrar = appViewModel::cerrarSesionDispositivo,
                                    onCerrarOtras = appViewModel::cerrarOtrasSesiones
                                )
                            }
                        }
                        composable(Routes.SEGURIDAD_ADMIN) {
                            LaunchedEffect(Unit) { appViewModel.cargarSegundoFactor() }
                            Box(modifier = Modifier.fillMaxSize()) {
                                FormasFlotantesFondo(activo = fondo)
                                SeguridadAdminScreen(
                                    estado = appViewModel.segundoFactor,
                                    onReintentar = appViewModel::cargarSegundoFactor,
                                    onBack = { navController.popBackStack() },
                                    onIniciar = appViewModel::iniciarSegundoFactor,
                                    onConfirmar = appViewModel::confirmarSegundoFactor
                                )
                            }
                        }
                        composable(Routes.ENCUESTA) { entry ->
                            val actividadId = entry.arguments?.getString("actividadId").orEmpty()
                            val inscripcionId = entry.arguments?.getString("inscripcionId").orEmpty()
                            LaunchedEffect(actividadId) { appViewModel.cargarEncuestas(actividadId) }
                            Box(modifier = Modifier.fillMaxSize()) {
                                FormasFlotantesFondo(activo = fondo)
                                EncuestaScreen(
                                    estado = appViewModel.encuestas,
                                    onReintentar = { appViewModel.cargarEncuestas(actividadId) },
                                    onBack = { navController.popBackStack() },
                                    onEnviar = { respuestas, alTerminar ->
                                        appViewModel.responderEncuesta(actividadId, inscripcionId, respuestas, alTerminar)
                                    }
                                )
                            }
                        }
                        composable(Routes.FAVORITOS) {
                            LaunchedEffect(Unit) { appViewModel.cargarFavoritos() }
                            Box(modifier = Modifier.fillMaxSize()) {
                                FormasFlotantesFondo(activo = fondo)
                                FavoritosScreen(
                                    estado = appViewModel.favoritos,
                                    onReintentar = appViewModel::cargarFavoritos,
                                    onBack = { navController.popBackStack() },
                                    onVerAsociacion = { id -> navController.navigate(Routes.asociacionDetalle(id)) },
                                    onVerActividad = { id -> navController.navigate(Routes.actividadDetalle(id)) }
                                )
                            }
                        }
                        composable(Routes.HISTORIAS) {
                            LaunchedEffect(Unit) { appViewModel.cargarTestimonios() }
                            Box(modifier = Modifier.fillMaxSize()) {
                                FormasFlotantesFondo(activo = fondo)
                                HistoriasScreen(
                                    estado = appViewModel.testimonios,
                                    onReintentar = appViewModel::cargarTestimonios,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                        }
                        composable(Routes.TODOS_VOLUNTARIADOS) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                FormasFlotantesFondo(activo = fondo)
                                TodosVoluntariadosScreen(
                                    actividades = appViewModel.actividades,
                                    onReintentar = appViewModel::recargarActividades,
                                    onBack = { navController.popBackStack() },
                                    onVerActividad = { id -> navController.navigate(Routes.actividadDetalle(id)) }
                                )
                            }
                        }
                        composable(Routes.TODAS_CAMPANAS) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                FormasFlotantesFondo(activo = fondo)
                                TodasCampanasScreen(
                                    campanas = appViewModel.campanas,
                                    onReintentar = appViewModel::recargarCampanas,
                                    onBack = { navController.popBackStack() },
                                    onVerCampana = { id -> navController.navigate(Routes.campanaDonacion(id)) }
                                )
                            }
                        }
                    }
                }
                // Aviso sin conexión y envíos pendientes, solo con sesión
                if (sesion is EstadoSesion.Activa) {
                    AvisoConexion(
                        sinConexionDesde = appViewModel.sinConexionDesde,
                        pendientes = appViewModel.enviosPendientes,
                        rechazado = appViewModel.envioRechazado,
                        onReintentar = appViewModel::reintentarConexion,
                        onEntendido = appViewModel::descartarRechazados,
                        modifier = if (rutaActual in rutasConBottomNav) Modifier else Modifier.navigationBarsPadding()
                    )
                }
            }
        }
    }
}
