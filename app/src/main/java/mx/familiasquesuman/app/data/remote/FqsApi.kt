package mx.familiasquesuman.app.data.remote

import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Endpoints del backend (ver fqs-backend/README.md). El token de sesión lo agrega
 * ClienteHttp en cada petición, por eso no aparece aquí.
 *
 * Las escrituras que se pueden mandar después de estar sin conexión llevan la clave de
 * idempotencia. Si la respuesta se pierde y se reintenta con la misma clave, el backend
 * devuelve la respuesta original sin crear un duplicado. Con null, Retrofit no manda el encabezado.
 */
interface FqsApi {

    // Públicas
    @GET("v1/catalogos")
    suspend fun catalogos(): CatalogosDto

    @POST("v1/auth/registro")
    suspend fun registro(@Body cuerpo: RegistroDto): RespuestaRegistroDto

    @POST("v1/auth/registro/verificar")
    suspend fun verificarRegistro(@Body cuerpo: VerificarRegistroDto): SesionDto

    @POST("v1/auth/registro/reenviar")
    suspend fun reenviarRegistro(@Body cuerpo: ReenviarRegistroDto)

    @POST("v1/auth/login")
    suspend fun login(@Body cuerpo: LoginDto): RespuestaLoginDto

    /** Segundo paso del inicio de sesión de administración */
    @POST("v1/auth/2fa")
    suspend fun segundoPaso(@Body cuerpo: SegundoPasoDto): SesionDto

    @POST("v1/auth/logout")
    suspend fun logout(@Body cuerpo: RefreshDto)

    @POST("v1/auth/recuperar")
    suspend fun recuperar(@Body cuerpo: RecuperarDto)

    @POST("v1/auth/restablecer")
    suspend fun restablecer(@Body cuerpo: RestablecerDto)

    // Perfil
    @GET("v1/yo")
    suspend fun perfil(): PerfilDto

    @PATCH("v1/yo")
    suspend fun editarPerfil(@Body cuerpo: EditarPerfilDto): PerfilDto

    @PUT("v1/yo/preferencias")
    suspend fun guardarPreferencias(@Body cuerpo: GuardarPreferenciasDto): PerfilDto

    @GET("v1/yo/privacidad")
    suspend fun privacidad(): PrivacidadDto

    @POST("v1/yo/privacidad/aceptar")
    suspend fun aceptarAviso(@Body cuerpo: AceptarAvisoDto): PerfilDto

    @POST("v1/yo/eliminacion")
    suspend fun solicitarEliminacion(@Body cuerpo: ConfirmarPasswordDto): MensajeDto

    @GET("v1/yo/inscripciones")
    suspend fun misInscripciones(@Query("momento") momento: String): List<MiInscripcionDto>

    // Comunidad
    @GET("v1/yo/comunidad")
    suspend fun comunidad(): List<MiembroDto>

    @POST("v1/yo/comunidad")
    suspend fun agregarMiembro(@Body cuerpo: NuevoMiembroDto): MiembroDto

    @DELETE("v1/yo/comunidad/{id}")
    suspend fun eliminarMiembro(@Path("id") id: String): ResultadoEliminarDto

    // Asociaciones. La ciudad la decide el backend con la del perfil (RF-02).
    @GET("v1/asociaciones")
    suspend fun asociaciones(
        @Query("categoria") categoria: String?,
        @Query("q") busqueda: String?,
        @Query("limite") limite: Int = 50
    ): List<AsociacionDto>

    @GET("v1/asociaciones/{id}")
    suspend fun asociacion(@Path("id") id: String): AsociacionDto

    @PUT("v1/asociaciones/{id}/favorito")
    suspend fun marcarFavorita(@Path("id") id: String, @Body cuerpo: FavoritoDto)

    @DELETE("v1/asociaciones/{id}/favorito")
    suspend fun quitarFavorita(@Path("id") id: String)

    // Actividades
    @GET("v1/actividades")
    suspend fun actividades(
        @Query("tipo") tipo: String? = null,
        @Query("categoria") categoria: String? = null,
        @Query("limite") limite: Int = 50
    ): List<ActividadDto>

    @GET("v1/actividades/{id}")
    suspend fun actividad(@Path("id") id: String): ActividadDto

    @POST("v1/actividades/{id}/inscripcion")
    suspend fun inscribir(@Path("id") id: String, @Body cuerpo: InscribirDto): ResultadoInscripcionDto

    @DELETE("v1/actividades/{id}/inscripcion")
    suspend fun cancelarInscripcion(@Path("id") id: String)

    @PUT("v1/actividades/{id}/favorito")
    suspend fun marcarActividadFavorita(@Path("id") id: String)

    @DELETE("v1/actividades/{id}/favorito")
    suspend fun quitarActividadFavorita(@Path("id") id: String)

    @GET("v1/yo/favoritos")
    suspend fun favoritos(): FavoritosDto

    @GET("v1/actividades/{id}/encuestas")
    suspend fun encuestas(@Path("id") actividadId: String): List<EncuestaDto>

    @POST("v1/yo/inscripciones/{id}/respuestas")
    suspend fun responderEncuesta(@Path("id") inscripcionId: String, @Body cuerpo: RespuestasEncuestaDto)

    @PUT("v1/yo/inscripciones/{id}/valoracion")
    suspend fun valorar(@Path("id") inscripcionId: String, @Body cuerpo: ValoracionDto)

    @DELETE("v1/yo/inscripciones/{id}/valoracion")
    suspend fun quitarValoracion(@Path("id") inscripcionId: String)

    @GET("v1/testimonios")
    suspend fun testimonios(@Query("limite") limite: Int = 30): List<TestimonioDto>

    // Notificaciones push, token del teléfono
    @PUT("v1/yo/dispositivos")
    suspend fun registrarDispositivo(@Body cuerpo: DispositivoDto)

    @HTTP(method = "DELETE", path = "v1/yo/dispositivos", hasBody = true)
    suspend fun quitarDispositivo(@Body cuerpo: DispositivoDto)

    @POST("v1/yo/inscripciones/{id}/testimonio")
    suspend fun publicarTestimonio(
        @Path("id") inscripcionId: String,
        @Body cuerpo: NuevoTestimonioDto,
        @Header("Idempotency-Key") clave: String? = null
    ): CreadoDto

    // Campañas
    @GET("v1/campanas")
    suspend fun campanas(@Query("limite") limite: Int = 50): List<CampanaDto>

    @GET("v1/campanas/{id}")
    suspend fun campana(@Path("id") id: String): CampanaDto

    @POST("v1/campanas/{id}/entregas")
    suspend fun registrarEntrega(@Path("id") campanaId: String, @Body cuerpo: NuevaEntregaDto): EntregaCreadaDto

    // Proyectos
    @GET("v1/proyectos")
    suspend fun proyectos(@Query("limite") limite: Int = 50): List<ProyectoDto>

    @GET("v1/proyectos/{id}")
    suspend fun proyecto(@Path("id") id: String): ProyectoDto

    // Propuestas y reportes
    @GET("v1/propuestas")
    suspend fun propuestas(): List<PropuestaDto>

    @GET("v1/propuestas/{id}")
    suspend fun propuesta(@Path("id") id: String): PropuestaDto

    @POST("v1/propuestas")
    suspend fun crearPropuesta(@Body cuerpo: NuevaPropuestaDto, @Header("Idempotency-Key") clave: String? = null): PropuestaDto

    @POST("v1/reportes")
    suspend fun crearReporte(@Body cuerpo: NuevoReporteDto, @Header("Idempotency-Key") clave: String? = null): CreadoDto

    // Notificaciones
    @GET("v1/notificaciones")
    suspend fun notificaciones(): List<NotificacionDto>

    @POST("v1/notificaciones/{id}/leida")
    suspend fun marcarLeida(@Path("id") id: String)

    @DELETE("v1/notificaciones/{id}")
    suspend fun eliminarNotificacion(@Path("id") id: String)

    // Imágenes, el cuerpo es el JPEG ya comprimido por la app
    @POST("v1/archivos")
    suspend fun subirImagen(@Body imagen: RequestBody): ArchivoDto

    // Segundo factor de la cuenta de administración
    @GET("v1/admin/2fa")
    suspend fun estado2fa(): Estado2faDto

    @POST("v1/admin/2fa/iniciar")
    suspend fun iniciar2fa(@Body cuerpo: Iniciar2faDto): Inicio2faDto

    @POST("v1/admin/2fa/confirmar")
    suspend fun confirmar2fa(@Body cuerpo: Codigo2faDto): CodigosRespaldoDto

    // Sesiones por dispositivo
    @GET("v1/yo/sesiones")
    suspend fun sesiones(): List<SesionDispositivoDto>

    @DELETE("v1/yo/sesiones/{id}")
    suspend fun cerrarSesionDispositivo(@Path("id") id: String)

    @POST("v1/yo/sesiones/cerrar-otras")
    suspend fun cerrarOtrasSesiones()
}
