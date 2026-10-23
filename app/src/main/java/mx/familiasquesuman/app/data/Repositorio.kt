package mx.familiasquesuman.app.data

import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import mx.familiasquesuman.app.data.local.DatosLocales
import mx.familiasquesuman.app.data.local.Envio
import mx.familiasquesuman.app.data.local.foto
import mx.familiasquesuman.app.data.remote.AceptarAvisoDto
import mx.familiasquesuman.app.data.remote.ActividadDto
import mx.familiasquesuman.app.data.remote.AsociacionDto
import mx.familiasquesuman.app.data.remote.CampanaDto
import mx.familiasquesuman.app.data.remote.CatalogosDto
import mx.familiasquesuman.app.data.remote.ClienteHttp
import mx.familiasquesuman.app.data.remote.Codigo2faDto
import mx.familiasquesuman.app.data.remote.ConfirmarPasswordDto
import mx.familiasquesuman.app.data.remote.CuposEnVivo
import mx.familiasquesuman.app.data.remote.DispositivoDto
import mx.familiasquesuman.app.data.remote.EditarPerfilDto
import mx.familiasquesuman.app.data.remote.EncuestaDto
import mx.familiasquesuman.app.data.remote.ErrorApi
import mx.familiasquesuman.app.data.remote.FavoritoDto
import mx.familiasquesuman.app.data.remote.FavoritosDto
import mx.familiasquesuman.app.data.remote.GuardarPreferenciasDto
import mx.familiasquesuman.app.data.remote.Iniciar2faDto
import mx.familiasquesuman.app.data.remote.InscribirDto
import mx.familiasquesuman.app.data.remote.LoginDto
import mx.familiasquesuman.app.data.remote.MiInscripcionDto
import mx.familiasquesuman.app.data.remote.MiembroDto
import mx.familiasquesuman.app.data.remote.NotificacionDto
import mx.familiasquesuman.app.data.remote.NuevaEntregaDto
import mx.familiasquesuman.app.data.remote.NuevaPropuestaDto
import mx.familiasquesuman.app.data.remote.NuevoMiembroDto
import mx.familiasquesuman.app.data.remote.NuevoReporteDto
import mx.familiasquesuman.app.data.remote.NuevoTestimonioDto
import mx.familiasquesuman.app.data.remote.PerfilDto
import mx.familiasquesuman.app.data.remote.PreferenciasRegistroDto
import mx.familiasquesuman.app.data.remote.PrivacidadDto
import mx.familiasquesuman.app.data.remote.PropuestaDto
import mx.familiasquesuman.app.data.remote.ProyectoDto
import mx.familiasquesuman.app.data.remote.RecuperarDto
import mx.familiasquesuman.app.data.remote.ReenviarRegistroDto
import mx.familiasquesuman.app.data.remote.RefreshDto
import mx.familiasquesuman.app.data.remote.RegistroDto
import mx.familiasquesuman.app.data.remote.RespuestaEncuestaDto
import mx.familiasquesuman.app.data.remote.RespuestasEncuestaDto
import mx.familiasquesuman.app.data.remote.RestablecerDto
import mx.familiasquesuman.app.data.remote.SegundoPasoDto
import mx.familiasquesuman.app.data.remote.TestimonioDto
import mx.familiasquesuman.app.data.remote.ValoracionDto
import mx.familiasquesuman.app.data.remote.VerificarRegistroDto
import mx.familiasquesuman.app.data.remote.llamar
import mx.familiasquesuman.app.data.remote.mensajeDeError
import mx.familiasquesuman.app.data.sesion.AlmacenSesion
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

/** Resultado de un intento de mandar los envíos pendientes. */
enum class ResultadoEnvios { Listo, Reintentar, SinSesion }

/**
 * Único punto de acceso a los datos. Cada método devuelve Result, así las pantallas
 * siempre pueden mostrar un mensaje en lugar de cerrarse por una excepción.
 *
 * Sin conexión. Las consultas guardan su última respuesta correcta en el teléfono y, si no
 * hay red, devuelven esa copia y avisan en sinConexionDesde la hora en que se guardó. Las
 * escrituras que no compiten por lugares (favoritos, calificaciones, propuestas, reportes y
 * testimonios) se guardan si no hay red y se mandan después con la misma clave de
 * idempotencia, así nunca quedan duplicadas. Inscribirse, cancelar y registrar entregas
 * siempre necesitan conexión porque dependen de los lugares y campañas en ese momento.
 */
class Repositorio(
    private val http: ClienteHttp,
    private val sesion: AlmacenSesion,
    private val local: DatosLocales,
    private val programarEnvios: () -> Unit,
    private val cancelarEnvios: () -> Unit
) {

    private val api = http.api
    private val img: (String?) -> String? = http::urlImagen

    fun haySesionGuardada() = sesion.tokens() != null
    val sesionExpirada = sesion.sesionExpirada

    /** Cambios de cupo en tiempo real (RF-05). */
    val cuposEnVivo = CuposEnVivo(http.okHttp, http.urlCuposEnVivo)

    // ---------------- Sin conexión ----------------

    private val _sinConexionDesde = MutableStateFlow<Long?>(null)

    /** null con conexión. Sin conexión, la hora (milisegundos) de la copia más vieja que se está mostrando. */
    val sinConexionDesde: StateFlow<Long?> = _sinConexionDesde.asStateFlow()

    val enviosPendientes = local.pendientes
    val enviosRechazados = local.rechazados

    /**
     * Pide al backend y guarda la respuesta. Si no hay red devuelve la última copia guardada.
     * Un error del servidor (401, 403, 404, 500) no usa la copia, se muestra el error.
     */
    private suspend fun <D> guardado(clave: String, serializador: KSerializer<D>, pedir: suspend () -> D): D {
        try {
            val datos = pedir()
            _sinConexionDesde.value = null
            try {
                local.guardar(clave, serializador, datos)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // No poder guardar la copia no impide mostrar los datos
            }
            return datos
        } catch (e: IOException) {
            val (datos, guardadoEn) = local.leer(clave, serializador) ?: throw e
            val actual = _sinConexionDesde.value
            _sinConexionDesde.value = if (actual == null) guardadoEn else minOf(actual, guardadoEn)
            return datos
        }
    }

    /**
     * Intenta mandar la escritura en ese momento. Si falla por falta de red se guarda en el
     * teléfono, se programa el envío y la acción se da por hecha. Cualquier otro error
     * (por ejemplo una propuesta repetida) se muestra igual que siempre.
     */
    private suspend fun enviarOGuardar(envio: Envio): Result<Unit> {
        val clave = UUID.randomUUID().toString()
        var actual = envio
        val r = llamar { ejecutar(envio, clave) { actual = it } }
        val error = r.exceptionOrNull()
        if (error is IOException) {
            local.encolar(actual, clave)
            programarEnvios()
            return Result.success(Unit)
        }
        if (r.isSuccess) envio.foto()?.let { local.borrarFoto(it) }
        return r
    }

    /** Hace la escritura en el backend. alSubirFoto recibe el envío con la foto ya subida. */
    private suspend fun ejecutar(envio: Envio, clave: String, alSubirFoto: suspend (Envio) -> Unit) {
        when (envio) {
            is Envio.AsociacionFavorita ->
                if (envio.activa) api.marcarFavorita(envio.asociacionId, FavoritoDto(true)) else api.quitarFavorita(envio.asociacionId)
            is Envio.ActividadFavorita ->
                if (envio.favorita) api.marcarActividadFavorita(envio.actividadId) else api.quitarActividadFavorita(envio.actividadId)
            is Envio.Valoracion -> {
                val valor = envio.valor
                if (valor == null) api.quitarValoracion(envio.inscripcionId) else api.valorar(envio.inscripcionId, ValoracionDto(valor))
            }
            is Envio.Propuesta -> api.crearPropuesta(envio.datos, clave)
            is Envio.Reporte -> {
                var datos = envio.datos
                val foto = envio.foto
                if (foto != null && datos.fotoClave == null) {
                    val subida = api.subirImagen(leerFoto(foto).toRequestBody(TIPO_JPEG))
                    datos = datos.copy(fotoClave = subida.clave)
                    alSubirFoto(envio.copy(datos = datos))
                }
                api.crearReporte(datos, clave)
            }
            is Envio.Testimonio -> {
                val fotoClave: String = envio.fotoClave
                    ?: api.subirImagen(leerFoto(envio.foto).toRequestBody(TIPO_JPEG)).clave.also { alSubirFoto(envio.copy(fotoClave = it)) }
                api.publicarTestimonio(envio.inscripcionId, NuevoTestimonioDto(envio.texto, fotoClave), clave)
            }
        }
    }

    private suspend fun leerFoto(nombre: String): ByteArray =
        local.leerFoto(nombre) ?: throw ErrorApi(0, "foto_perdida", "La foto guardada ya no está disponible en el teléfono.")

    /**
     * Manda los envíos pendientes en el orden en que se hicieron. Lo llama SincronizarEnvios.
     * Un envío que el servidor rechaza (por ejemplo una calificación de una actividad a la
     * que no se asistió) queda como rechazado con el mensaje del servidor para mostrarlo.
     */
    suspend fun enviarPendientes(): ResultadoEnvios {
        if (!haySesionGuardada()) return ResultadoEnvios.SinSesion
        while (true) {
            val (fila, envio) = local.siguiente() ?: return ResultadoEnvios.Listo
            var actual = envio
            val r = llamar {
                ejecutar(envio, fila.idempotencia) {
                    actual = it
                    local.actualizar(fila, it, fila.intentos)
                }
            }
            val error = r.exceptionOrNull()
            val reintentable = error is IOException ||
                (error is ErrorApi && (error.status == 429 || error.status >= 500 || error.codigo == "idempotencia_en_proceso"))
            when {
                error == null -> {
                    local.quitar(fila)
                    envio.foto()?.let { local.borrarFoto(it) }
                }
                error is ErrorApi && error.status == 401 -> return ResultadoEnvios.SinSesion
                reintentable && fila.intentos + 1 < MAX_INTENTOS -> {
                    local.actualizar(fila, actual, fila.intentos + 1)
                    return ResultadoEnvios.Reintentar
                }
                else -> {
                    local.rechazar(fila, error?.let { mensajeDeError(it) } ?: "No se pudo enviar.")
                    envio.foto()?.let { local.borrarFoto(it) }
                }
            }
        }
    }

    suspend fun descartarRechazados() = local.descartarRechazados()

    /** Al abrir la app, poda las copias viejas y retoma los envíos que quedaron pendientes. */
    suspend fun alIniciar() {
        runCatching { local.podar() }
        if (haySesionGuardada()) programarEnvios()
    }

    /** Borra todo lo guardado en el teléfono de la cuenta que se va. */
    suspend fun borrarDatosLocales() {
        cancelarEnvios()
        _sinConexionDesde.value = null
        runCatching { local.borrarTodo() }
    }

    // ---------------- Sesión ----------------

    suspend fun catalogo() = llamar { guardado("catalogos", CatalogosDto.serializer()) { api.catalogos() }.aUi() }

    /**
     * Devuelve el perfil y PasoLogin.Listo, o PasoLogin.Codigo cuando la cuenta de
     * administración tiene segundo factor. En ese caso todavía no hay sesión guardada.
     */
    suspend fun iniciarSesion(identificador: String, password: String) = llamar {
        val r = api.login(LoginDto(identificador.trim(), password))
        val tokens = r.tokens
        val usuario = r.usuario
        if (r.requiere2fa && r.desafio != null) {
            null to PasoLogin.Codigo(r.desafio)
        } else if (tokens != null && usuario != null) {
            sesion.guardar(tokens)
            usuario.aUi(img) to PasoLogin.Listo
        } else {
            throw IllegalStateException("Respuesta de inicio de sesión incompleta")
        }
    }

    /** Canjea el desafío con el código de 6 dígitos o un código de respaldo. */
    suspend fun verificarSegundoFactor(desafio: String, codigo: String) = llamar {
        val r = api.segundoPaso(SegundoPasoDto(desafio, codigo.trim()))
        sesion.guardar(r.tokens)
        r.usuario.aUi(img)
    }

    suspend fun estadoSegundoFactor() = llamar {
        val e = api.estado2fa()
        EstadoSegundoFactor(e.activo, e.codigosRespaldoRestantes, e.obligatorio)
    }

    suspend fun iniciarSegundoFactor(password: String) = llamar {
        val r = api.iniciar2fa(Iniciar2faDto(password))
        InicioSegundoFactor(r.secreto, r.uri)
    }

    suspend fun confirmarSegundoFactor(codigo: String) = llamar { api.confirmar2fa(Codigo2faDto(codigo.trim())).codigosRespaldo }

    suspend fun registrar(
        datos: DatosRegistro,
        username: String,
        avisoId: String,
        donar: Boolean,
        participar: Boolean,
        proponer: Boolean,
        categorias: Set<String>
    ) = llamar {
        val contacto = datos.contacto.trim()
        val esCorreo = contacto.contains("@")
        val r = api.registro(
            RegistroDto(
                nombre = datos.nombre.trim(),
                username = username.trim().removePrefix("@"),
                correo = if (esCorreo) contacto.lowercase() else null,
                telefono = if (esCorreo) null else contacto.filter { it.isDigit() || it == '+' },
                password = datos.password,
                fechaNacimiento = datos.fechaNacimientoIso,
                ciudadId = datos.ciudadId,
                avisoPrivacidadId = avisoId,
                preferencias = PreferenciasRegistroDto(donar, participar, proponer),
                categorias = categorias.toList()
            )
        )
        val tokens = r.tokens
        val usuario = r.usuario
        when {
            tokens != null && usuario != null -> {
                sesion.guardar(tokens)
                ResultadoRegistro.Listo(usuario.aUi(img))
            }
            r.registroId != null -> ResultadoRegistro.VerificarContacto(r.registroId, r.destino ?: contacto, r.canal == "sms")
            else -> throw IllegalStateException("Respuesta de registro incompleta")
        }
    }

    /** Crea la cuenta con el código que llegó al correo y abre la sesión. */
    suspend fun verificarRegistro(registroId: String, codigo: String) = llamar {
        val r = api.verificarRegistro(VerificarRegistroDto(registroId, codigo.trim()))
        sesion.guardar(r.tokens)
        r.usuario.aUi(img)
    }

    suspend fun reenviarCodigoRegistro(registroId: String) = llamar { api.reenviarRegistro(ReenviarRegistroDto(registroId)) }

    // ---------------- Sesiones por dispositivo ----------------

    suspend fun sesiones() = llamar {
        api.sesiones().map { SesionDispositivo(it.id, it.dispositivo, it.iniciada, it.ultimaActividad, it.actual) }
    }

    suspend fun cerrarSesionDispositivo(id: String) = llamar { api.cerrarSesionDispositivo(id) }

    suspend fun cerrarOtrasSesiones() = llamar { api.cerrarOtrasSesiones() }

    // ---------------- Notificaciones push (RF16) ----------------

    /** Último token de Firebase de este teléfono, para darlo de baja al cerrar sesión. */
    @Volatile
    private var tokenPush: String? = null

    /** Liga el teléfono a la cuenta para recibir push. Sin sesión no hace nada. */
    suspend fun registrarDispositivo(token: String) {
        tokenPush = token
        if (!haySesionGuardada()) return
        llamar { api.registrarDispositivo(DispositivoDto(token)) }
    }

    /**
     * Cierra la sesión en el servidor y borra los tokens y los datos guardados del teléfono
     * aunque no haya red. Antes intenta mandar lo pendiente, lo que no salga se pierde.
     */
    suspend fun cerrarSesion() {
        cuposEnVivo.detener()
        runCatching { enviarPendientes() }
        // Primero se desliga el teléfono, así la siguiente persona no recibe avisos ajenos
        tokenPush?.let { t -> llamar { api.quitarDispositivo(DispositivoDto(t)) } }
        val refresh = sesion.tokens()?.refreshToken
        if (refresh != null) llamar { api.logout(RefreshDto(refresh)) }
        sesion.borrar()
        borrarDatosLocales()
    }

    suspend fun solicitarCodigo(identificador: String) = llamar { api.recuperar(RecuperarDto(identificador.trim())) }

    suspend fun restablecer(identificador: String, codigo: String, password: String) =
        llamar { api.restablecer(RestablecerDto(identificador.trim(), codigo.trim(), password)) }

    // ---------------- Perfil ----------------

    suspend fun perfil() = llamar { guardado("yo", PerfilDto.serializer()) { api.perfil() }.aUi(img) }

    /** La ciudad elegida se guarda en el perfil y el backend filtra todo con ella (RF-02). */
    suspend fun cambiarCiudad(ciudadId: String) = llamar { api.editarPerfil(EditarPerfilDto(ciudadId = ciudadId)).aUi(img) }

    suspend fun cambiarNombre(nombre: String) = llamar { api.editarPerfil(EditarPerfilDto(nombre = nombre.trim())).aUi(img) }

    suspend fun cambiarFoto(jpeg: ByteArray) = llamar {
        val subida = api.subirImagen(jpeg.toRequestBody(TIPO_JPEG))
        api.editarPerfil(EditarPerfilDto(fotoClave = subida.clave)).aUi(img)
    }

    suspend fun guardarPreferencias(p: Perfil, donar: Boolean, participar: Boolean, proponer: Boolean, categorias: Set<String>) =
        llamar {
            api.guardarPreferencias(
                GuardarPreferenciasDto(donar, participar, proponer, p.recibirRecordatorios, p.recibirSugerencias, categorias.toList())
            ).aUi(img)
        }

    // ---------------- Privacidad (RF-15) ----------------

    suspend fun privacidad() = llamar { guardado("privacidad", PrivacidadDto.serializer()) { api.privacidad() }.aUi() }

    suspend fun aceptarAviso(avisoId: String) = llamar { api.aceptarAviso(AceptarAvisoDto(avisoId)).aUi(img) }

    /** La cuenta queda desactivada en cuanto se pide, así que se borra todo lo del teléfono. */
    suspend fun solicitarEliminacion(password: String) = llamar {
        api.solicitarEliminacion(ConfirmarPasswordDto(password))
        cuposEnVivo.detener()
        sesion.borrar()
        borrarDatosLocales()
    }

    suspend fun experiencias(momento: String) = llamar {
        guardado("inscripciones/$momento", ListSerializer(MiInscripcionDto.serializer())) { api.misInscripciones(momento) }
            .map { it.aExperiencia() }
    }

    suspend fun publicarTestimonio(inscripcionId: String, texto: String, jpeg: ByteArray): Result<Unit> {
        val foto = local.guardarFoto(jpeg)
        return enviarOGuardar(Envio.Testimonio(inscripcionId, texto.trim(), foto))
    }

    // ---------------- Comunidad ----------------

    suspend fun comunidad() = llamar {
        guardado("comunidad", ListSerializer(MiembroDto.serializer())) { api.comunidad() }.map { it.aUi() }
    }

    suspend fun agregarMiembro(m: NuevoMiembro) = llamar {
        val contacto = m.contacto.trim()
        api.agregarMiembro(
            NuevoMiembroDto(
                nombre = m.nombre.trim(),
                relacion = m.relacion,
                fechaNacimiento = m.fechaNacimientoIso,
                correo = contacto.takeIf { it.contains("@") }?.lowercase(),
                telefono = contacto.takeIf { it.isNotEmpty() && !it.contains("@") }?.filter { it.isDigit() || it == '+' }
            )
        ).aUi()
    }

    suspend fun eliminarMiembro(id: String) = llamar { api.eliminarMiembro(id); Unit }

    // ---------------- Asociaciones y actividades ----------------

    /** La lista completa se guarda. Una búsqueda o un filtro sin red busca en esa copia. */
    suspend fun asociaciones(categoria: Categoria?, busqueda: String?) = llamar {
        val texto = busqueda?.trim()?.takeIf { it.isNotEmpty() }
        if (categoria == null && texto == null) {
            guardado("asociaciones", ListSerializer(AsociacionDto.serializer())) { api.asociaciones(null, null) }.map { it.aUi(img) }
        } else {
            buscarAsociaciones(categoria, texto)
        }
    }

    private suspend fun buscarAsociaciones(categoria: Categoria?, texto: String?): List<Asociacion> {
        try {
            return api.asociaciones(categoria?.clave, texto).map { it.aUi(img) }
        } catch (e: IOException) {
            val (todas, desde) = local.leer("asociaciones", ListSerializer(AsociacionDto.serializer())) ?: throw e
            _sinConexionDesde.value = desde
            return todas.map { it.aUi(img) }
                .filter { a -> categoria == null || a.categoria == categoria }
                .filter { a -> texto == null || a.nombre.contains(texto, ignoreCase = true) }
        }
    }

    suspend fun asociacion(id: String) = llamar { guardado("asociacion/$id", AsociacionDto.serializer()) { api.asociacion(id) }.aUi(img) }

    suspend fun notificacionesAsociacion(id: String, activas: Boolean) = enviarOGuardar(Envio.AsociacionFavorita(id, activas))

    /** Todas las actividades de la ciudad. La app separa Participar y Visiteo por tipo. */
    suspend fun actividades() = llamar {
        guardado("actividades", ListSerializer(ActividadDto.serializer())) { api.actividades() }.map { it.aUi(img) }
    }

    suspend fun actividad(id: String) = llamar { guardado("actividad/$id", ActividadDto.serializer()) { api.actividad(id) }.aUi(img) }

    /** Siempre en línea, el lugar se aparta en el servidor en ese momento. */
    suspend fun inscribir(actividadId: String, titularAsiste: Boolean, miembros: Set<String>) =
        llamar { api.inscribir(actividadId, InscribirDto(titularAsiste, miembros.toList())); Unit }

    suspend fun cancelarInscripcion(actividadId: String) = llamar { api.cancelarInscripcion(actividadId) }

    // ---------------- Campañas ----------------

    suspend fun campanas() = llamar { guardado("campanas", ListSerializer(CampanaDto.serializer())) { api.campanas() }.map { it.aUi() } }

    suspend fun campana(id: String) = llamar { guardado("campana/$id", CampanaDto.serializer()) { api.campana(id) }.aUi() }

    /** Siempre en línea, la campaña y el punto de entrega tienen que seguir activos. */
    suspend fun registrarEntrega(campanaId: String, articuloId: String, puntoId: String, cantidad: Double, comentario: String?) =
        llamar {
            api.registrarEntrega(campanaId, NuevaEntregaDto(articuloId, puntoId, cantidad, comentario?.trim()?.takeIf { it.isNotEmpty() }))
            Unit
        }

    // ---------------- Favoritos (RF20) ----------------

    suspend fun favoritos() = llamar { guardado("favoritos", FavoritosDto.serializer()) { api.favoritos() }.aUi(img) }

    suspend fun actividadFavorita(id: String, favorita: Boolean) = enviarOGuardar(Envio.ActividadFavorita(id, favorita))

    // ---------------- Encuestas (RF-13) y calificaciones (RF21) ----------------

    suspend fun encuestas(actividadId: String) = llamar {
        guardado("encuestas/$actividadId", ListSerializer(EncuestaDto.serializer())) { api.encuestas(actividadId) }.map { it.aUi() }
    }

    suspend fun responderEncuesta(inscripcionId: String, respuestas: List<RespuestaEncuesta>) = llamar {
        api.responderEncuesta(
            inscripcionId,
            RespuestasEncuestaDto(respuestas.map { RespuestaEncuestaDto(it.preguntaId, it.opcionId, it.texto?.trim()) })
        )
    }

    /** valor 1 me gusta, -1 no me gusta, null quita la calificación. */
    suspend fun valorar(inscripcionId: String, valor: Int?) = enviarOGuardar(Envio.Valoracion(inscripcionId, valor))

    // ---------------- Historias de la comunidad (RF-12) ----------------

    suspend fun testimonios() = llamar {
        guardado("testimonios", ListSerializer(TestimonioDto.serializer())) { api.testimonios() }.map { it.aUi(img) }
    }

    // ---------------- Proyectos ----------------

    suspend fun proyectos() = llamar {
        guardado("proyectos", ListSerializer(ProyectoDto.serializer())) { api.proyectos() }.map { it.aUi(img) }
    }

    suspend fun proyecto(id: String) = llamar { guardado("proyecto/$id", ProyectoDto.serializer()) { api.proyecto(id) }.aUi(img) }

    // ---------------- Propuestas y reportes ----------------

    suspend fun propuestas() = llamar {
        guardado("propuestas", ListSerializer(PropuestaDto.serializer())) { api.propuestas() }.map { it.aUi() }
    }

    suspend fun propuesta(id: String) = llamar { guardado("propuesta/$id", PropuestaDto.serializer()) { api.propuesta(id) }.aUi() }

    suspend fun crearPropuesta(tipo: TipoPropuesta, titulo: String, descripcion: String, ubicacion: String, contacto: String, ciudadId: String?): Result<Unit> {
        val c = contacto.trim()
        return enviarOGuardar(
            Envio.Propuesta(
                NuevaPropuestaDto(
                    tipo = tipo.api,
                    titulo = titulo.trim(),
                    descripcion = descripcion.trim(),
                    ciudadId = ciudadId,
                    ubicacion = ubicacion.trim().takeIf { it.isNotEmpty() },
                    contactoCorreo = c.takeIf { it.contains("@") }?.lowercase(),
                    contactoTelefono = c.takeIf { it.isNotEmpty() && !it.contains("@") }?.filter { it.isDigit() || it == '+' }
                )
            )
        )
    }

    suspend fun reportar(
        tipo: String,
        nivel: Int?,
        descripcion: String,
        actividadId: String?,
        asociacionId: String?,
        foto: ByteArray?
    ): Result<Unit> {
        val archivo = foto?.let { local.guardarFoto(it) }
        return enviarOGuardar(
            Envio.Reporte(
                NuevoReporteDto(
                    tipo = tipo,
                    nivelSatisfaccion = nivel,
                    descripcion = descripcion.trim(),
                    actividadId = actividadId,
                    asociacionId = asociacionId
                ),
                foto = archivo
            )
        )
    }

    // ---------------- Notificaciones ----------------

    suspend fun notificaciones() = llamar {
        guardado("notificaciones", ListSerializer(NotificacionDto.serializer())) { api.notificaciones() }.map { it.aUi() }
    }

    suspend fun marcarLeida(id: String) = llamar { api.marcarLeida(id) }

    suspend fun eliminarNotificacion(id: String) = llamar { api.eliminarNotificacion(id) }

    private companion object {
        val TIPO_JPEG = "image/jpeg".toMediaType()

        /** Después de tantos intentos con el servidor fallando, el envío se marca como rechazado. */
        const val MAX_INTENTOS = 10
    }
}
