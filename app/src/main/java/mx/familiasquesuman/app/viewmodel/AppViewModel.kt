package mx.familiasquesuman.app.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mx.familiasquesuman.app.FqsApplication
import mx.familiasquesuman.app.data.ActividadItem
import mx.familiasquesuman.app.data.Asociacion
import mx.familiasquesuman.app.data.CambioCupo
import mx.familiasquesuman.app.data.Campana
import mx.familiasquesuman.app.data.Catalogo
import mx.familiasquesuman.app.data.Categoria
import mx.familiasquesuman.app.data.DatosRegistro
import mx.familiasquesuman.app.data.Encuesta
import mx.familiasquesuman.app.data.ResultadoRegistro
import mx.familiasquesuman.app.data.SesionDispositivo
import mx.familiasquesuman.app.data.EstadoSegundoFactor
import mx.familiasquesuman.app.data.InicioSegundoFactor
import mx.familiasquesuman.app.data.PasoLogin
import mx.familiasquesuman.app.data.Experiencia
import mx.familiasquesuman.app.data.Favoritos
import mx.familiasquesuman.app.data.RespuestaEncuesta
import mx.familiasquesuman.app.data.Testimonio
import mx.familiasquesuman.app.data.FiltroInicio
import mx.familiasquesuman.app.data.Imagenes
import mx.familiasquesuman.app.data.MiembroComunidad
import mx.familiasquesuman.app.data.Notificacion
import mx.familiasquesuman.app.data.NuevoMiembro
import mx.familiasquesuman.app.data.Perfil
import mx.familiasquesuman.app.data.Privacidad
import mx.familiasquesuman.app.data.Propuesta
import mx.familiasquesuman.app.data.Proyecto
import mx.familiasquesuman.app.data.TipoPropuesta
import mx.familiasquesuman.app.data.remote.ErrorApi
import mx.familiasquesuman.app.data.remote.mensajeDeError
import com.google.firebase.messaging.FirebaseMessaging

/** Estado de una carga desde el backend. */
sealed interface Carga<out T> {
    data object Cargando : Carga<Nothing>
    data class Error(val mensaje: String) : Carga<Nothing>
    data class Listo<T>(val datos: T) : Carga<T>
}

sealed interface EstadoSesion {
    data object Verificando : EstadoSesion
    data object Anonima : EstadoSesion
    data class SinConexion(val mensaje: String) : EstadoSesion
    data class Activa(val perfil: Perfil) : EstadoSesion
}

/** Resultado de una acción para que la pantalla muestre un mensaje o avance. */
typealias AlTerminar<T> = (Result<T>) -> Unit

private fun <T> Result<T>.aCarga(): Carga<T> = fold({ Carga.Listo(it) }, { Carga.Error(mensajeDeError(it)) })

/**
 * Estado compartido entre pantallas. Los datos vienen del backend a través de Repositorio,
 * o de la última copia guardada cuando no hay conexión. El estado de sesión decide qué grafo
 * de navegación se muestra (bienvenida o la app).
 */
class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = (application as FqsApplication).repositorio
    private val prefs = application.getSharedPreferences("familias_que_suman_prefs", Context.MODE_PRIVATE)

    // ---------------- Apariencia (local) ----------------

    var fondoAnimadoActivo by mutableStateOf(prefs.getBoolean(CLAVE_FONDO_ANIMADO, true))
        private set

    fun cambiarFondoAnimado(activo: Boolean) {
        fondoAnimadoActivo = activo
        prefs.edit().putBoolean(CLAVE_FONDO_ANIMADO, activo).apply()
    }

    // ---------------- Sesión ----------------

    var estadoSesion by mutableStateOf<EstadoSesion>(EstadoSesion.Verificando)
        private set

    val perfil: Perfil? get() = (estadoSesion as? EstadoSesion.Activa)?.perfil

    var catalogo by mutableStateOf<Carga<Catalogo>>(Carga.Cargando)
        private set

    /** Datos de Registro que esperan al onboarding para crear la cuenta. */
    var registroPendiente by mutableStateOf<DatosRegistro?>(null)
        private set

    // ---------------- Sin conexión ----------------

    /** null con conexión. Sin conexión, la hora de la copia guardada que se está mostrando. */
    var sinConexionDesde by mutableStateOf<Long?>(null)
        private set

    /** Escrituras hechas sin conexión que esperan red para mandarse. */
    var enviosPendientes by mutableStateOf(0)
        private set

    /** Mensaje del primer envío que el servidor rechazó al sincronizar, para avisar. */
    var envioRechazado by mutableStateOf<String?>(null)
        private set

    fun descartarRechazados() {
        viewModelScope.launch { repo.descartarRechazados() }
    }

    /** Vuelve a pedir lo principal, por ejemplo al tocar Reintentar en el aviso sin conexión. */
    fun reintentarConexion() {
        cargarCatalogo()
        if (estadoSesion is EstadoSesion.Activa) cargarInicio() else verificarSesion()
    }

    init {
        viewModelScope.launch {
            repo.sesionExpirada.collect {
                repo.cuposEnVivo.detener()
                repo.borrarDatosLocales()
                limpiarDatosDeUsuario()
                estadoSesion = EstadoSesion.Anonima
            }
        }
        viewModelScope.launch { repo.sinConexionDesde.collect { sinConexionDesde = it } }
        viewModelScope.launch { repo.enviosPendientes.collect { enviosPendientes = it } }
        viewModelScope.launch {
            repo.enviosRechazados.collect { lista ->
                envioRechazado = lista.firstOrNull()?.let { "${it.tipo} no se pudo enviar. ${it.rechazo ?: ""}".trim() }
            }
        }
        viewModelScope.launch { repo.cuposEnVivo.cambios.collect { aplicarCambioCupo(it) } }
        verificarSesion()
        cargarCatalogo()
    }

    fun verificarSesion() {
        if (!repo.haySesionGuardada()) {
            estadoSesion = EstadoSesion.Anonima
            return
        }
        estadoSesion = EstadoSesion.Verificando
        viewModelScope.launch {
            repo.perfil().fold(
                onSuccess = {
                    estadoSesion = EstadoSesion.Activa(it)
                    cargarInicio()
                    registrarParaPush()
                },
                onFailure = { e ->
                    estadoSesion = if (e is ErrorApi && e.status == 401) EstadoSesion.Anonima
                    else EstadoSesion.SinConexion(mensajeDeError(e))
                }
            )
        }
    }

    fun cargarCatalogo() {
        catalogo = Carga.Cargando
        viewModelScope.launch { catalogo = repo.catalogo().aCarga() }
    }

    fun iniciarSesion(identificador: String, password: String, alTerminar: AlTerminar<PasoLogin>) {
        viewModelScope.launch {
            val r = repo.iniciarSesion(identificador, password)
            r.onSuccess { (perfil, _) -> if (perfil != null) sesionAbierta(perfil) }
            alTerminar(r.map { it.second })
        }
    }

    /** Segundo paso del inicio de sesión de administración. */
    fun verificarSegundoFactor(desafio: String, codigo: String, alTerminar: AlTerminar<Unit>) {
        viewModelScope.launch {
            val r = repo.verificarSegundoFactor(desafio, codigo)
            r.onSuccess { sesionAbierta(it) }
            alTerminar(r.map { })
        }
    }

    private fun sesionAbierta(perfil: Perfil) {
        estadoSesion = EstadoSesion.Activa(perfil)
        cargarInicio()
        registrarParaPush()
    }

    // ---------------- Segundo factor de administración ----------------

    var segundoFactor by mutableStateOf<Carga<EstadoSegundoFactor>>(Carga.Cargando)
        private set

    fun cargarSegundoFactor() {
        viewModelScope.launch { segundoFactor = repo.estadoSegundoFactor().aCarga() }
    }

    fun iniciarSegundoFactor(password: String, alTerminar: AlTerminar<InicioSegundoFactor>) {
        viewModelScope.launch { alTerminar(repo.iniciarSegundoFactor(password)) }
    }

    fun confirmarSegundoFactor(codigo: String, alTerminar: AlTerminar<List<String>>) {
        viewModelScope.launch {
            val r = repo.confirmarSegundoFactor(codigo)
            r.onSuccess { cargarSegundoFactor() }
            alTerminar(r)
        }
    }

    fun guardarRegistroPendiente(datos: DatosRegistro) {
        registroPendiente = datos
    }

    fun completarRegistro(
        username: String,
        donar: Boolean,
        participar: Boolean,
        proponer: Boolean,
        categorias: Set<String>,
        alTerminar: AlTerminar<Unit>
    ) {
        val datos = registroPendiente
        val aviso = (catalogo as? Carga.Listo)?.datos?.aviso
        if (datos == null || aviso == null) {
            alTerminar(Result.failure(ErrorApi(0, "incompleto", "Vuelve a llenar el registro, faltan datos.")))
            return
        }
        viewModelScope.launch {
            val r = repo.registrar(datos, username, aviso.id, donar, participar, proponer, categorias)
            r.onSuccess { resultado ->
                when (resultado) {
                    is ResultadoRegistro.Listo -> {
                        registroPendiente = null
                        sesionAbierta(resultado.perfil)
                    }
                    // La cuenta se crea hasta escribir el código que llegó por correo o SMS
                    is ResultadoRegistro.VerificarContacto -> verificacionContacto = resultado
                }
            }
            alTerminar(r.map { })
        }
    }

    /** Registro esperando el código, la pantalla de verificación lo usa. */
    var verificacionContacto by mutableStateOf<ResultadoRegistro.VerificarContacto?>(null)
        private set

    fun verificarRegistro(codigo: String, alTerminar: AlTerminar<Unit>) {
        val pendiente = verificacionContacto
        if (pendiente == null) {
            alTerminar(Result.failure(ErrorApi(0, "incompleto", "Vuelve a llenar el registro.")))
            return
        }
        viewModelScope.launch {
            val r = repo.verificarRegistro(pendiente.registroId, codigo)
            r.onSuccess {
                registroPendiente = null
                verificacionContacto = null
                sesionAbierta(it)
            }
            alTerminar(r.map { })
        }
    }

    fun reenviarCodigoRegistro(alTerminar: AlTerminar<Unit>) {
        val pendiente = verificacionContacto ?: return
        viewModelScope.launch { alTerminar(repo.reenviarCodigoRegistro(pendiente.registroId)) }
    }

    // ---------------- Sesiones por dispositivo ----------------

    var sesionesDispositivos by mutableStateOf<Carga<List<SesionDispositivo>>>(Carga.Cargando)
        private set

    fun cargarSesiones() {
        viewModelScope.launch { sesionesDispositivos = repo.sesiones().aCarga() }
    }

    fun cerrarSesionDispositivo(id: String, alTerminar: AlTerminar<Unit>) {
        viewModelScope.launch {
            val r = repo.cerrarSesionDispositivo(id)
            r.onSuccess { cargarSesiones() }
            alTerminar(r)
        }
    }

    fun cerrarOtrasSesiones(alTerminar: AlTerminar<Unit>) {
        viewModelScope.launch {
            val r = repo.cerrarOtrasSesiones()
            r.onSuccess { cargarSesiones() }
            alTerminar(r)
        }
    }

    // ---------------- Notificaciones push (RF16) ----------------

    /**
     * Pide a Firebase el token de este teléfono y lo liga a la cuenta. Si la app se
     * compiló sin google-services.json, Firebase no existe y simplemente no hay push.
     */
    private fun registrarParaPush() {
        runCatching {
            FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                viewModelScope.launch { repo.registrarDispositivo(token) }
            }
        }
    }

    // ---------------- Cupos en tiempo real (RF-05) ----------------

    /** La pantalla principal avisa cuando la app entra o sale de primer plano. */
    fun cuposEnPrimerPlano(visible: Boolean) {
        if (visible && estadoSesion is EstadoSesion.Activa) repo.cuposEnVivo.iniciar() else repo.cuposEnVivo.detener()
    }

    private fun aplicarCambioCupo(c: CambioCupo) {
        val lista = actividades
        if (lista is Carga.Listo && lista.datos.any { it.id == c.actividadId }) {
            actividades = Carga.Listo(lista.datos.map {
                if (it.id == c.actividadId) it.copy(cuposDisponibles = c.cuposDisponibles, cuposTotales = c.cupoTotal) else it
            })
        }
        val detalle = actividadDetalle
        if (detalle is Carga.Listo && detalle.datos.id == c.actividadId) {
            actividadDetalle = Carga.Listo(detalle.datos.copy(cuposDisponibles = c.cuposDisponibles, cuposTotales = c.cupoTotal))
        }
    }

    fun cerrarSesion(alTerminar: () -> Unit) {
        viewModelScope.launch {
            repo.cerrarSesion()
            limpiarDatosDeUsuario()
            estadoSesion = EstadoSesion.Anonima
            alTerminar()
        }
    }

    fun solicitarCodigo(identificador: String, alTerminar: AlTerminar<Unit>) {
        viewModelScope.launch { alTerminar(repo.solicitarCodigo(identificador)) }
    }

    fun restablecer(identificador: String, codigo: String, password: String, alTerminar: AlTerminar<Unit>) {
        viewModelScope.launch { alTerminar(repo.restablecer(identificador, codigo, password)) }
    }

    private fun limpiarDatosDeUsuario() {
        segundoFactor = Carga.Cargando
        sesionesDispositivos = Carga.Cargando
        asociaciones = Carga.Cargando
        actividades = Carga.Cargando
        campanas = Carga.Cargando
        comunidad.clear()
        notificaciones.clear()
        propuestas = Carga.Cargando
        proximas = Carga.Cargando
        pasadas = Carga.Cargando
        explorar = Carga.Cargando
        proyectos = Carga.Cargando
        historial = Carga.Cargando
        privacidad = Carga.Cargando
        filtroInicio = null
        favoritos = Carga.Cargando
        encuestas = Carga.Cargando
        testimonios = Carga.Cargando
    }

    // ---------------- Listas principales ----------------

    var asociaciones by mutableStateOf<Carga<List<Asociacion>>>(Carga.Cargando)
        private set
    var actividades by mutableStateOf<Carga<List<ActividadItem>>>(Carga.Cargando)
        private set
    var campanas by mutableStateOf<Carga<List<Campana>>>(Carga.Cargando)
        private set

    val comunidad = mutableStateListOf<MiembroComunidad>()
    val notificaciones = mutableStateListOf<Notificacion>()

    val hayNotificacionesSinLeer: Boolean get() = notificaciones.any { !it.leida }

    var proyectos by mutableStateOf<Carga<List<Proyecto>>>(Carga.Cargando)
        private set

    /** Filtro inicial de Inicio (Participar, Donar, Visiteo, Proyectos). null muestra el resumen. */
    var filtroInicio by mutableStateOf<FiltroInicio?>(null)
        private set

    fun elegirFiltroInicio(filtro: FiltroInicio?) {
        filtroInicio = filtro
    }

    /** El backend ya filtra todo por la ciudad del perfil, la app no manda la ciudad. */
    fun cargarInicio() {
        viewModelScope.launch {
            val a = async { repo.asociaciones(null, null) }
            val b = async { repo.actividades() }
            val c = async { repo.campanas() }
            val p = async { repo.proyectos() }
            val d = async { repo.comunidad() }
            val e = async { repo.notificaciones() }
            asociaciones = a.await().aCarga()
            actividades = b.await().aCarga()
            campanas = c.await().aCarga()
            proyectos = p.await().aCarga()
            d.await().onSuccess { comunidad.clear(); comunidad.addAll(it) }
            e.await().onSuccess { notificaciones.clear(); notificaciones.addAll(it) }
        }
    }

    fun recargarActividades() {
        viewModelScope.launch { actividades = repo.actividades().aCarga() }
    }

    fun recargarCampanas() {
        viewModelScope.launch { campanas = repo.campanas().aCarga() }
    }

    fun recargarProyectos() {
        viewModelScope.launch { proyectos = repo.proyectos().aCarga() }
    }

    /**
     * Cambia la ciudad del perfil (RF-02). Queda guardada en el servidor, así que se
     * recuerda en cualquier teléfono, y todo el contenido se vuelve a pedir.
     */
    fun cambiarCiudad(ciudadId: String, alTerminar: AlTerminar<Unit>) {
        viewModelScope.launch {
            val r = repo.cambiarCiudad(ciudadId)
            r.onSuccess {
                estadoSesion = EstadoSesion.Activa(it)
                asociaciones = Carga.Cargando
                actividades = Carga.Cargando
                campanas = Carga.Cargando
                proyectos = Carga.Cargando
                explorar = Carga.Cargando
                cargarInicio()
                buscarAsociaciones(ultimaCategoria, ultimaBusqueda)
            }
            alTerminar(r.map { })
        }
    }

    // ---------------- Explorar ----------------

    var explorar by mutableStateOf<Carga<List<Asociacion>>>(Carga.Cargando)
        private set
    private var busquedaJob: Job? = null
    private var ultimaCategoria: Categoria? = null
    private var ultimaBusqueda: String = ""

    /** Busca en el servidor con una pequeña espera para no consultar en cada tecla. */
    fun buscarAsociaciones(categoria: Categoria?, texto: String) {
        ultimaCategoria = categoria
        ultimaBusqueda = texto
        busquedaJob?.cancel()
        busquedaJob = viewModelScope.launch {
            if (texto.isNotBlank()) delay(350)
            if (explorar !is Carga.Listo) explorar = Carga.Cargando
            explorar = repo.asociaciones(categoria, texto).aCarga()
        }
    }

    // ---------------- Detalles ----------------

    var asociacionDetalle by mutableStateOf<Carga<Asociacion>>(Carga.Cargando)
        private set
    var actividadDetalle by mutableStateOf<Carga<ActividadItem>>(Carga.Cargando)
        private set
    var campanaDetalle by mutableStateOf<Carga<Campana>>(Carga.Cargando)
        private set
    var propuestaDetalle by mutableStateOf<Carga<Propuesta>>(Carga.Cargando)
        private set
    var proyectoDetalle by mutableStateOf<Carga<Proyecto>>(Carga.Cargando)
        private set

    fun cargarProyecto(id: String) {
        proyectoDetalle = Carga.Cargando
        viewModelScope.launch { proyectoDetalle = repo.proyecto(id).aCarga() }
    }

    fun cargarAsociacion(id: String) {
        asociacionDetalle = Carga.Cargando
        viewModelScope.launch { asociacionDetalle = repo.asociacion(id).aCarga() }
    }

    fun cargarActividad(id: String) {
        actividadDetalle = Carga.Cargando
        viewModelScope.launch { actividadDetalle = repo.actividad(id).aCarga() }
    }

    fun cargarCampana(id: String) {
        campanaDetalle = Carga.Cargando
        viewModelScope.launch { campanaDetalle = repo.campana(id).aCarga() }
    }

    fun cargarPropuesta(id: String) {
        propuestaDetalle = Carga.Cargando
        viewModelScope.launch { propuestaDetalle = repo.propuesta(id).aCarga() }
    }

    fun cambiarNotificacionesAsociacion(id: String, activas: Boolean, alTerminar: AlTerminar<Unit>) {
        viewModelScope.launch {
            val r = repo.notificacionesAsociacion(id, activas)
            r.onSuccess {
                val actual = asociacionDetalle
                if (actual is Carga.Listo && actual.datos.id == id) {
                    asociacionDetalle = Carga.Listo(actual.datos.copy(notificacionesActivas = activas))
                }
            }
            alTerminar(r)
        }
    }

    fun inscribir(actividadId: String, titularAsiste: Boolean, miembros: Set<String>, alTerminar: AlTerminar<Unit>) {
        viewModelScope.launch {
            val r = repo.inscribir(actividadId, titularAsiste, miembros)
            // Se recarga siempre para mostrar los cupos reales, haya funcionado o no
            actividadDetalle = repo.actividad(actividadId).aCarga()
            r.onSuccess { recargarActividades(); proximas = repo.experiencias("proximas").aCarga() }
            alTerminar(r)
        }
    }

    fun cancelarInscripcion(actividadId: String, alTerminar: AlTerminar<Unit>) {
        viewModelScope.launch {
            val r = repo.cancelarInscripcion(actividadId)
            actividadDetalle = repo.actividad(actividadId).aCarga()
            r.onSuccess { recargarActividades(); proximas = repo.experiencias("proximas").aCarga() }
            alTerminar(r)
        }
    }

    fun registrarEntrega(
        campanaId: String,
        articuloId: String,
        puntoId: String,
        cantidad: Double,
        comentario: String?,
        alTerminar: AlTerminar<Unit>
    ) {
        viewModelScope.launch { alTerminar(repo.registrarEntrega(campanaId, articuloId, puntoId, cantidad, comentario)) }
    }

    // ---------------- Comunidad ----------------

    fun agregarMiembro(m: NuevoMiembro, alTerminar: AlTerminar<Unit>) {
        viewModelScope.launch {
            val r = repo.agregarMiembro(m)
            r.onSuccess { comunidad.add(it) }
            alTerminar(r.map { })
        }
    }

    fun eliminarMiembro(miembro: MiembroComunidad, alTerminar: AlTerminar<Unit>) {
        viewModelScope.launch {
            val r = repo.eliminarMiembro(miembro.id)
            r.onSuccess { comunidad.removeAll { it.id == miembro.id } }
            alTerminar(r)
        }
    }

    // ---------------- Propuestas ----------------

    var propuestas by mutableStateOf<Carga<List<Propuesta>>>(Carga.Cargando)
        private set

    fun cargarPropuestas() {
        viewModelScope.launch { propuestas = repo.propuestas().aCarga() }
    }

    fun enviarPropuesta(
        tipo: TipoPropuesta,
        titulo: String,
        descripcion: String,
        ubicacion: String,
        contacto: String,
        alTerminar: AlTerminar<Unit>
    ) {
        viewModelScope.launch {
            val r = repo.crearPropuesta(tipo, titulo, descripcion, ubicacion, contacto, perfil?.ciudadId)
            r.onSuccess { cargarPropuestas() }
            alTerminar(r.map { })
        }
    }

    // ---------------- Reportes ----------------

    fun enviarReporte(
        tipo: String,
        nivel: Int?,
        descripcion: String,
        actividadId: String?,
        asociacionId: String?,
        foto: Uri?,
        alTerminar: AlTerminar<Unit>
    ) {
        viewModelScope.launch {
            val bytes = foto?.let { Imagenes.prepararParaSubir(getApplication<Application>(), it) }
            if (foto != null && bytes == null) {
                alTerminar(Result.failure(ErrorApi(0, "imagen", "No se pudo leer la imagen, intenta con otra.")))
                return@launch
            }
            alTerminar(repo.reportar(tipo, nivel, descripcion, actividadId, asociacionId, bytes))
        }
    }

    // ---------------- Perfil ----------------

    var proximas by mutableStateOf<Carga<List<Experiencia>>>(Carga.Cargando)
        private set
    var pasadas by mutableStateOf<Carga<List<Experiencia>>>(Carga.Cargando)
        private set

    fun cargarPerfil() {
        viewModelScope.launch {
            repo.perfil().onSuccess { estadoSesion = EstadoSesion.Activa(it) }
            proximas = repo.experiencias("proximas").aCarga()
            pasadas = repo.experiencias("pasadas").aCarga()
            repo.comunidad().onSuccess { comunidad.clear(); comunidad.addAll(it) }
        }
    }

    // ---------------- Favoritos (RF20) ----------------

    var favoritos by mutableStateOf<Carga<Favoritos>>(Carga.Cargando)
        private set

    fun cargarFavoritos() {
        viewModelScope.launch { favoritos = repo.favoritos().aCarga() }
    }

    fun cambiarActividadFavorita(id: String, favorita: Boolean, alTerminar: AlTerminar<Unit>) {
        viewModelScope.launch {
            val r = repo.actividadFavorita(id, favorita)
            r.onSuccess {
                val d = actividadDetalle
                if (d is Carga.Listo && d.datos.id == id) actividadDetalle = Carga.Listo(d.datos.copy(esFavorita = favorita))
                if (favoritos is Carga.Listo) cargarFavoritos()
            }
            alTerminar(r)
        }
    }

    // ---------------- Encuestas (RF-13) ----------------

    var encuestas by mutableStateOf<Carga<List<Encuesta>>>(Carga.Cargando)
        private set

    fun cargarEncuestas(actividadId: String) {
        encuestas = Carga.Cargando
        viewModelScope.launch { encuestas = repo.encuestas(actividadId).aCarga() }
    }

    fun responderEncuesta(actividadId: String, inscripcionId: String, respuestas: List<RespuestaEncuesta>, alTerminar: AlTerminar<Unit>) {
        viewModelScope.launch {
            val r = repo.responderEncuesta(inscripcionId, respuestas)
            r.onSuccess { encuestas = repo.encuestas(actividadId).aCarga() }
            alTerminar(r)
        }
    }

    // ---------------- Calificaciones (RF21) ----------------

    /** valor 1 me gusta, -1 no me gusta, null quita la calificación. Solo si asistió. */
    fun valorar(inscripcionId: String, valor: Int?, alTerminar: AlTerminar<Unit>) {
        viewModelScope.launch {
            val r = repo.valorar(inscripcionId, valor)
            r.onSuccess {
                val h = historial
                if (h is Carga.Listo) {
                    historial = Carga.Listo(h.datos.map { if (it.inscripcionId == inscripcionId) it.copy(valoracion = valor) else it })
                }
            }
            alTerminar(r)
        }
    }

    // ---------------- Historias de la comunidad (RF-12) ----------------

    var testimonios by mutableStateOf<Carga<List<Testimonio>>>(Carga.Cargando)
        private set

    fun cargarTestimonios() {
        viewModelScope.launch { testimonios = repo.testimonios().aCarga() }
    }

    // ---------------- Historial completo (RF-11) ----------------

    var historial by mutableStateOf<Carga<List<Experiencia>>>(Carga.Cargando)
        private set

    fun cargarHistorial() {
        viewModelScope.launch { historial = repo.experiencias("todas").aCarga() }
    }

    // ---------------- Privacidad (RF-15) ----------------

    var privacidad by mutableStateOf<Carga<Privacidad>>(Carga.Cargando)
        private set

    fun cargarPrivacidad() {
        viewModelScope.launch { privacidad = repo.privacidad().aCarga() }
    }

    fun aceptarAviso(avisoId: String, alTerminar: AlTerminar<Unit>) {
        viewModelScope.launch {
            val r = repo.aceptarAviso(avisoId)
            r.onSuccess {
                estadoSesion = EstadoSesion.Activa(it)
                privacidad = repo.privacidad().aCarga()
            }
            alTerminar(r.map { })
        }
    }

    /** Pide borrar la cuenta. La cuenta queda desactivada y se cierra la sesión. */
    fun solicitarEliminacion(password: String, alTerminar: AlTerminar<Unit>) {
        viewModelScope.launch {
            val r = repo.solicitarEliminacion(password)
            r.onSuccess {
                limpiarDatosDeUsuario()
                estadoSesion = EstadoSesion.Anonima
            }
            alTerminar(r)
        }
    }

    fun cambiarNombre(nombre: String, alTerminar: AlTerminar<Unit>) {
        viewModelScope.launch {
            val r = repo.cambiarNombre(nombre)
            r.onSuccess { estadoSesion = EstadoSesion.Activa(it) }
            alTerminar(r.map { })
        }
    }

    fun cambiarFoto(uri: Uri, alTerminar: AlTerminar<Unit>) {
        viewModelScope.launch {
            val bytes = Imagenes.prepararParaSubir(getApplication<Application>(), uri)
            if (bytes == null) {
                alTerminar(Result.failure(ErrorApi(0, "imagen", "No se pudo leer la imagen, intenta con otra.")))
                return@launch
            }
            val r = repo.cambiarFoto(bytes)
            r.onSuccess { estadoSesion = EstadoSesion.Activa(it) }
            alTerminar(r.map { })
        }
    }

    fun guardarPreferencias(donar: Boolean, participar: Boolean, proponer: Boolean, categorias: Set<String>, alTerminar: AlTerminar<Unit>) {
        val p = perfil ?: return
        viewModelScope.launch {
            val r = repo.guardarPreferencias(p, donar, participar, proponer, categorias)
            r.onSuccess { estadoSesion = EstadoSesion.Activa(it) }
            alTerminar(r.map { })
        }
    }

    fun publicarTestimonio(inscripcionId: String, texto: String, foto: Uri, alTerminar: AlTerminar<Unit>) {
        viewModelScope.launch {
            val bytes = Imagenes.prepararParaSubir(getApplication<Application>(), foto)
            if (bytes == null) {
                alTerminar(Result.failure(ErrorApi(0, "imagen", "No se pudo leer la imagen, intenta con otra.")))
                return@launch
            }
            val r = repo.publicarTestimonio(inscripcionId, texto, bytes)
            r.onSuccess { pasadas = repo.experiencias("pasadas").aCarga() }
            alTerminar(r)
        }
    }

    // ---------------- Notificaciones ----------------

    fun cargarNotificaciones() {
        viewModelScope.launch {
            repo.notificaciones().onSuccess { notificaciones.clear(); notificaciones.addAll(it) }
        }
    }

    fun marcarLeida(id: String) {
        val idx = notificaciones.indexOfFirst { it.id == id }
        if (idx >= 0) notificaciones[idx] = notificaciones[idx].copy(leida = true)
        viewModelScope.launch { repo.marcarLeida(id) }
    }

    fun eliminarNotificacion(id: String) {
        notificaciones.removeAll { it.id == id }
        viewModelScope.launch { repo.eliminarNotificacion(id) }
    }

    override fun onCleared() {
        repo.cuposEnVivo.detener()
        super.onCleared()
    }

    companion object {
        private const val CLAVE_FONDO_ANIMADO = "fondo_animado_activo"
    }
}
