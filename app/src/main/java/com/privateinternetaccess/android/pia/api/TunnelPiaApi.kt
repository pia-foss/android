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
import okhttp3.OkHttpClient
import java.io.IOException
import java.security.*
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.*
import java.util.concurrent.TimeUnit
import javax.net.ssl.*

/**
 * Base class for tunnel related requests requiring the use of our 4096 cert.
 * @See PiaApi for other requests.
 */
open class TunnelPiaApi {

    protected val okHttpClient: OkHttpClient

    init {
        var publicKey: PublicKey? = null
        var trustManager: X509TrustManager? = null
        var sslSocketFactory: SSLSocketFactory? = null
        val builder = OkHttpClient.Builder()
        try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)
            val inputStream = PIAApplication.getRSA4096Certificate()
            val certificateFactory = CertificateFactory.getInstance("X.509")
            val certificate = certificateFactory.generateCertificate(inputStream)
            publicKey = certificate.publicKey
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
        if (PiaApi.INTERCEPTOR != null) {
            builder.addInterceptor(PiaApi.INTERCEPTOR)
        }

        if (trustManager != null && sslSocketFactory != null) {
            builder.sslSocketFactory(sslSocketFactory, trustManager)
        }
        val finalPublicKey = publicKey
        builder.hostnameVerifier { _, session ->
            var verified = false
            try {
                for (sessionCertificate in session.peerCertificates) {
                    val x509Certificate = sessionCertificate as X509Certificate
                    x509Certificate.verify(finalPublicKey)
                    verified = true
                }
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
            verified
        }
        okHttpClient = builder.build()
    }
}