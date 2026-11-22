package com.letimax.app.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object EncryptionManager {
    private const val KEY_ALIAS = "LetimaXMasterKey"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    init {
        criarChaveSeNecessario()
    }

    private fun criarChaveSeNecessario() {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            val spec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
            keyGenerator.init(spec)
            keyGenerator.generateKey()
        }
    }

    private fun obterChave(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        return keyStore.getKey(KEY_ALIAS, null) as SecretKey
    }

    fun encrypt(texto: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, obterChave())
        val iv = cipher.iv
        val bytesCifrados = cipher.doFinal(texto.toByteArray(Charsets.UTF_8))
        val combinado = iv + bytesCifrados
        return Base64.encodeToString(combinado, Base64.DEFAULT)
    }

    fun decrypt(textoCifrado: String): String {
        val combinado = Base64.decode(textoCifrado, Base64.DEFAULT)
        val iv = combinado.copyOfRange(0, 12)
        val bytesCifrados = combinado.copyOfRange(12, combinado.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, obterChave(), spec)
        val bytesOriginais = cipher.doFinal(bytesCifrados)
        return String(bytesOriginais, Charsets.UTF_8)
    }
}
