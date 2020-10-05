/*
 *  Copyright (c) 2020 Private Internet Access, Inc.
 *
 *  This file is part of the Private Internet Access Android Client.
 *
 *  The Private Internet Access Android Client is free software: you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License as published by the Free
 *  Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  The Private Internet Access Android Client is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License along with the Private
 *  Internet Access Android Client.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.privateinternetaccess.android.utils

import android.content.Context
import android.os.Build
import android.security.KeyPairGeneratorSpec
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.privateinternetaccess.android.pia.utils.MultiPreferences
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.math.BigInteger
import java.security.*
import java.util.*
import javax.crypto.*
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.security.auth.x500.X500Principal
import kotlin.collections.ArrayList


class KeyStoreUtils(private val context: Context, private val multiPreferences: MultiPreferences) {

    companion object {
        private const val RSA_MODE = "RSA/ECB/PKCS1Padding"
        private const val AES_MODE = "AES/GCM/NoPadding"
        private const val KEY_ALIAS = "PIA_KEY"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val ENCRYPTED_KEY = "ENCRYPTED_KEY"
        private const val INITIALIZATION_VECTOR = "INITIALIZATION_VECTOR"
        private const val CIPHER_NAME_PROVIDER = "AndroidOpenSSL"
    }

    private var keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)

    init {
        generateEncryptKey()
        generateInitializationVector()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            generateAESKey()
        }
    }

    private fun generateEncryptKey() {
        keyStore.load(null)
        if (keyStore.containsAlias(KEY_ALIAS)) {
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val keyGenerator =
                    KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            keyGenerator.init(
                    KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                            .setRandomizedEncryptionRequired(false)
                            .build()
            )
            keyGenerator.generateKey()
        } else {
            val start: Calendar = Calendar.getInstance()
            val end: Calendar = Calendar.getInstance()
            end.add(Calendar.YEAR, 20)
            val keyPairSpec = KeyPairGeneratorSpec.Builder(context)
                    .setAlias(KEY_ALIAS)
                    .setSubject(X500Principal("CN=$KEY_ALIAS"))
                    .setSerialNumber(BigInteger.TEN)
                    .setStartDate(start.time)
                    .setEndDate(end.time)
                    .build()
            val keyPairGenerator =
                    KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, ANDROID_KEYSTORE)
            keyPairGenerator.initialize(keyPairSpec)
            keyPairGenerator.generateKeyPair()
        }
    }

    private fun rsaEncrypt(secret: ByteArray): ByteArray {
        val privateKeyEntry = keyStore.getEntry(KEY_ALIAS, null) as KeyStore.PrivateKeyEntry
        val cipher = Cipher.getInstance(RSA_MODE, CIPHER_NAME_PROVIDER)
        cipher.init(Cipher.ENCRYPT_MODE, privateKeyEntry.certificate.publicKey)
        val outputStream = ByteArrayOutputStream()
        val cipherOutputStream = CipherOutputStream(outputStream, cipher)
        cipherOutputStream.write(secret)
        cipherOutputStream.close()
        return outputStream.toByteArray()
    }

    private fun rsaDecrypt(encrypted: ByteArray): ByteArray {
        val privateKeyEntry = keyStore.getEntry(KEY_ALIAS, null) as KeyStore.PrivateKeyEntry
        val cipher = Cipher.getInstance(RSA_MODE, CIPHER_NAME_PROVIDER)
        cipher.init(Cipher.DECRYPT_MODE, privateKeyEntry.privateKey)
        val cipherInputStream = CipherInputStream(ByteArrayInputStream(encrypted), cipher)
        val values: ArrayList<Byte> = ArrayList()
        var nextByte: Int
        while (cipherInputStream.read().also { nextByte = it } != -1) {
            values.add(nextByte.toByte())
        }
        val bytes = ByteArray(values.size)
        for (i in bytes.indices) {
            bytes[i] = values[i]
        }
        return bytes
    }

    private fun generateAESKey() {
        val encryptedKeyBase64 = multiPreferences.getString(ENCRYPTED_KEY, null)
        if (encryptedKeyBase64 == null) {
            val key = ByteArray(16)
            val secureRandom = SecureRandom()
            secureRandom.nextBytes(key)
            val encryptedKey = rsaEncrypt(key)
            multiPreferences.setString(
                    ENCRYPTED_KEY, Base64.encodeToString(encryptedKey, Base64.DEFAULT)
            )
        }
    }

    private fun getSecretKey(): Key {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            keyStore.load(null)
            return keyStore.getKey(KEY_ALIAS, null) as SecretKey
        }

        val encryptedKeyBase64 = multiPreferences.getString(ENCRYPTED_KEY, null)
        val encryptedKey: ByteArray = Base64.decode(encryptedKeyBase64, Base64.DEFAULT)
        val key = rsaDecrypt(encryptedKey)
        return SecretKeySpec(key, "AES")
    }

    private fun generateInitializationVector() {
        val initializationVector = multiPreferences.getString(INITIALIZATION_VECTOR, null)
        if (initializationVector == null) {
            val random = SecureRandom()
            val generated = random.generateSeed(12)
            val generatedVector: String = Base64.encodeToString(generated, Base64.DEFAULT)
            multiPreferences.setString(INITIALIZATION_VECTOR, generatedVector)
        }
    }

    public fun encrypt(value: String): String {
        val cipher = Cipher.getInstance(AES_MODE)
        val initializationVector = multiPreferences.getString(INITIALIZATION_VECTOR, null)

        try {
            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    getSecretKey(),
                    GCMParameterSpec(128, Base64.decode(initializationVector, Base64.DEFAULT))
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val encodedBytes: ByteArray = cipher.doFinal(value.toByteArray(charset("UTF-8")))
        return Base64.encodeToString(encodedBytes, Base64.DEFAULT)
    }

    public fun decrypt(value: String): String? {
        val cipher = Cipher.getInstance(AES_MODE)
        val initializationVector = multiPreferences.getString(INITIALIZATION_VECTOR, null)
        var decryptedVal: ByteArray? = null

        try {
            cipher.init(
                    Cipher.DECRYPT_MODE,
                    getSecretKey(),
                    GCMParameterSpec(128, Base64.decode(initializationVector, Base64.DEFAULT))
            )
            val decodedValue = Base64.decode(value.toByteArray(charset("UTF-8")), Base64.DEFAULT)
            decryptedVal = cipher.doFinal(decodedValue)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return decryptedVal?.let { String(it) }
    }
}