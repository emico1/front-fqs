package mx.familiasquesuman.app.data.sesion

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import mx.familiasquesuman.app.data.remote.TokensDto

/**
 * Guarda los tokens de sesión cifrados con AES-256-GCM. La llave vive en el Android
 * Keystore (en hardware cuando el teléfono lo tiene) y nunca sale de ahí, así que
 * aunque alguien copie el archivo de preferencias no puede leer los tokens.
 *
 * El access token se mantiene además en memoria para no descifrar en cada petición.
 * Si el teléfono pierde la llave (por ejemplo al restaurar), simplemente se pide
 * iniciar sesión de nuevo.
 */
class AlmacenSesion(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)
    private val candado = Any()

    @Volatile
    private var enMemoria: TokensDto? = null

    private val _sesionExpirada = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Emite cuando el backend rechaza el refresh y hay que volver a iniciar sesión. */
    val sesionExpirada: SharedFlow<Unit> = _sesionExpirada.asSharedFlow()

    fun tokens(): TokensDto? = synchronized(candado) {
        enMemoria ?: leer()?.also { enMemoria = it }
    }

    fun guardar(t: TokensDto) = synchronized(candado) {
        enMemoria = t
        prefs.edit()
            .putString(K_ACCESS, cifrar(t.accessToken))
            .putString(K_REFRESH, cifrar(t.refreshToken))
            .putString(K_ACCESS_EXP, t.accessExpiraEn)
            .putString(K_REFRESH_EXP, t.refreshExpiraEn)
            .apply()
    }

    fun borrar() = synchronized(candado) {
        enMemoria = null
        prefs.edit().clear().apply()
    }

    fun avisarExpiracion() {
        borrar()
        _sesionExpirada.tryEmit(Unit)
    }

    private fun leer(): TokensDto? = runCatching {
        val access = prefs.getString(K_ACCESS, null) ?: return null
        val refresh = prefs.getString(K_REFRESH, null) ?: return null
        TokensDto(
            accessToken = descifrar(access),
            refreshToken = descifrar(refresh),
            accessExpiraEn = prefs.getString(K_ACCESS_EXP, "") ?: "",
            refreshExpiraEn = prefs.getString(K_REFRESH_EXP, "") ?: ""
        )
    }.getOrElse {
        // Llave perdida o datos corruptos, se descarta la sesión
        prefs.edit().clear().apply()
        null
    }

    private fun llave(): SecretKey {
        val ks = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (ks.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        gen.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return gen.generateKey()
    }

    private fun cifrar(texto: String): String {
        val cipher = Cipher.getInstance(TRANSFORMACION)
        cipher.init(Cipher.ENCRYPT_MODE, llave())
        val datos = cipher.doFinal(texto.toByteArray(Charsets.UTF_8))
        // Se guarda el IV (12 bytes, generado por el Keystore) delante del texto cifrado
        return Base64.encodeToString(cipher.iv + datos, Base64.NO_WRAP)
    }

    private fun descifrar(guardado: String): String {
        val bytes = Base64.decode(guardado, Base64.NO_WRAP)
        val iv = bytes.copyOfRange(0, 12)
        val datos = bytes.copyOfRange(12, bytes.size)
        val cipher = Cipher.getInstance(TRANSFORMACION)
        cipher.init(Cipher.DECRYPT_MODE, llave(), GCMParameterSpec(128, iv))
        return String(cipher.doFinal(datos), Charsets.UTF_8)
    }

    private companion object {
        const val ARCHIVO = "fqs_sesion"
        const val KEYSTORE = "AndroidKeyStore"
        const val ALIAS = "fqs_sesion_llave"
        const val TRANSFORMACION = "AES/GCM/NoPadding"
        const val K_ACCESS = "a"
        const val K_REFRESH = "r"
        const val K_ACCESS_EXP = "ae"
        const val K_REFRESH_EXP = "re"
    }
}
