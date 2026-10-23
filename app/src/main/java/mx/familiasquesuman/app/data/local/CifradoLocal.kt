package mx.familiasquesuman.app.data.local

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Cifra con AES-256-GCM lo que la app guarda para usar sin conexión (respuestas del backend,
 * envíos pendientes y sus fotos). La llave vive en el Android Keystore y es distinta de la de
 * la sesión. Cada valor se cifra con su clave como dato asociado, así un registro no se puede
 * hacer pasar por otro aunque alguien copie la base del teléfono.
 */
class CifradoLocal {

    fun cifrar(datos: ByteArray, asociado: String): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMACION)
        cipher.init(Cipher.ENCRYPT_MODE, llave())
        cipher.updateAAD(asociado.toByteArray(Charsets.UTF_8))
        // El IV (12 bytes, generado por el Keystore) va delante del texto cifrado
        return cipher.iv + cipher.doFinal(datos)
    }

    fun descifrar(guardado: ByteArray, asociado: String): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMACION)
        cipher.init(Cipher.DECRYPT_MODE, llave(), GCMParameterSpec(128, guardado.copyOfRange(0, 12)))
        cipher.updateAAD(asociado.toByteArray(Charsets.UTF_8))
        return cipher.doFinal(guardado.copyOfRange(12, guardado.size))
    }

    fun cifrarTexto(texto: String, asociado: String): String =
        Base64.encodeToString(cifrar(texto.toByteArray(Charsets.UTF_8), asociado), Base64.NO_WRAP)

    fun descifrarTexto(guardado: String, asociado: String): String =
        String(descifrar(Base64.decode(guardado, Base64.NO_WRAP), asociado), Charsets.UTF_8)

    @Synchronized
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

    private companion object {
        const val KEYSTORE = "AndroidKeyStore"
        const val ALIAS = "fqs_datos_locales"
        const val TRANSFORMACION = "AES/GCM/NoPadding"
    }
}
