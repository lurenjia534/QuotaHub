package com.lurenjia534.quotahub.data.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * API Key加解密接口。
 *
 * 用于将敏感凭证以密文形式持久化到本地存储。
 */
interface ApiKeyCipher {
    fun encrypt(plainText: String): String
    fun decrypt(storedValue: String): String
}

/**
 * 基于Android Keystore的API Key加解密实现。
 *
 * 使用设备绑定的AES密钥进行AES/GCM加密，降低数据库被直接导出后
 * 凭证以明文暴露的风险。
 */
class AndroidKeystoreApiKeyCipher : ApiKeyCipher {
    override fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())

        val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val encodedIv = Base64.getEncoder().encodeToString(cipher.iv)
        val encodedPayload = Base64.getEncoder().encodeToString(encryptedBytes)
        return "$ENCRYPTED_PREFIX$encodedIv:$encodedPayload"
    }

    override fun decrypt(storedValue: String): String {
        require(storedValue.startsWith(ENCRYPTED_PREFIX)) {
            "API key is not stored in encrypted format"
        }

        val payload = storedValue.removePrefix(ENCRYPTED_PREFIX)
        val separatorIndex = payload.indexOf(':')
        require(separatorIndex in 1 until payload.lastIndex) {
            "Malformed encrypted API key payload"
        }

        val iv = Base64.getDecoder().decode(payload.substring(0, separatorIndex))
        val encryptedBytes = Base64.getDecoder().decode(payload.substring(separatorIndex + 1))

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateSecretKey(),
            GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        )
        return cipher.doFinal(encryptedBytes).toString(Charsets.UTF_8)
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
            load(null)
        }
        val existingKey = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existingKey != null) {
            return existingKey
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )
        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .setUserAuthenticationRequired(false)
                .build()
        )
        return keyGenerator.generateKey()
    }

    private companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "quota_hub_api_key_cipher"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val ENCRYPTED_PREFIX = "enc::v1::"
        private const val GCM_TAG_LENGTH_BITS = 128
    }
}
