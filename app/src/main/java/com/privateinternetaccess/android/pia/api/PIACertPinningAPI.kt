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

package com.privateinternetaccess.android.pia.api

import com.privateinternetaccess.android.PIAApplication
import com.privateinternetaccess.android.pia.utils.DLog
import okhttp3.OkHttpClient
import org.spongycastle.asn1.x500.X500Name
import org.spongycastle.asn1.x500.style.BCStyle
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.security.*
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.*
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLPeerUnverifiedException
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.SSLContext
import javax.security.auth.x500.X500Principal

/**
 * Base class for requests requiring the use of our 4096 cert.
 * @See PiaApi for other requests.
 */
open class PIACertPinningAPI {

    companion object {
        const val TAG = "PIACertPinningAPI"
    }

    private lateinit var knownEndpointCommonName: List<Pair<String, String>>

    val okHttpClient: OkHttpClient

    init {
        var trustManager: X509TrustManager? = null
        var sslSocketFactory: SSLSocketFactory? = null
        val builder = OkHttpClient.Builder()
        try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)
            val inputStream = PIAApplication.getRSA4096Certificate()
            val certificateFactory = CertificateFactory.getInstance("X.509")
            val certificate = certificateFactory.generateCertificate(inputStream)
            keyStore.setCertificateEntry("pia", certificate)
            inputStream.close()
            val trustManagerFactory =
                    TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
            trustManagerFactory.init(keyStore)
            val trustManagers = trustManagerFactory.trustManagers
            check(!(trustManagers.size != 1 || trustManagers[0] !is X509TrustManager)) {
                "Unexpected default trust managers:" + Arrays.toString(trustManagers)
            }
            trustManager = trustManagers[0] as X509TrustManager
            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustManagers, SecureRandom())
            sslSocketFactory = sslContext.socketFactory
        } catch (e: KeyStoreException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: CertificateException) {
            e.printStackTrace()
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        } catch (e: KeyManagementException) {
            e.printStackTrace()
        }
        builder.connectTimeout(8, TimeUnit.SECONDS)

        if (trustManager != null && sslSocketFactory != null) {
            builder.sslSocketFactory(sslSocketFactory, trustManager)
        }
        builder.hostnameVerifier { endpoint, session ->
            var verified = false
            try {
                val x509CertificateChain = session.peerCertificates as Array<out X509Certificate>
                trustManager?.checkServerTrusted(x509CertificateChain, "RSA")
                val sessionCertificate = session.peerCertificates.first()
                verified = verifyCommonName(endpoint, sessionCertificate as X509Certificate)
            } catch (e: SSLPeerUnverifiedException) {
                e.printStackTrace()
            } catch (e: CertificateException) {
                e.printStackTrace()
            } catch (e: InvalidKeyException) {
                e.printStackTrace()
            } catch (e: NoSuchAlgorithmException) {
                e.printStackTrace()
            } catch (e: NoSuchProviderException) {
                e.printStackTrace()
            } catch (e: SignatureException) {
                e.printStackTrace()
            }
            DLog.d(TAG, "Verifier succeeded? $verified")
            verified
        }
        okHttpClient = builder.build()
    }

    fun setKnownEndpointCommonName(knownEndpointCommonName: List<Pair<String, String>>) {
        this.knownEndpointCommonName = knownEndpointCommonName
    }

    // region private
    private fun verifyCommonName(
            requestEndpoint: String,
            certificate: X509Certificate
    ): Boolean {
        val principal = certificate.subjectDN as X500Principal
        certificateCommonName(X500Name.getInstance(principal.encoded))?.let { certCommonName ->
            for ((endpoint, commonName) in knownEndpointCommonName) {
                if (isEqual(endpoint.toByteArray(), requestEndpoint.toByteArray()) &&
                        isEqual(commonName.toByteArray(), certCommonName.toByteArray())) {
                    return true
                }
            }
        }
        return false
    }

    private fun certificateCommonName(name: X500Name): String? {
        val rdns = name.getRDNs(BCStyle.CN)
        return if (rdns.isEmpty()) {
            null
        } else rdns.first().first.value.toString()
    }

    private fun isEqual(a: ByteArray, b: ByteArray): Boolean {
        val messageDigest = MessageDigest.getInstance("SHA-256")
        val random = SecureRandom()
        val randomBytes = ByteArray(20)
        random.nextBytes(randomBytes)

        val concatA = ByteArrayOutputStream()
        concatA.write(randomBytes)
        concatA.write(a)
        val digestA = messageDigest.digest(concatA.toByteArray())

        val concatB = ByteArrayOutputStream()
        concatB.write(randomBytes)
        concatB.write(b)
        val digestB = messageDigest.digest(concatB.toByteArray())

        return MessageDigest.isEqual(digestA, digestB)
    }
    // endregion
}