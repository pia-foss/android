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

package com.privateinternetaccess.android.pia.handlers

import android.util.Base64
import com.privateinternetaccess.regions.MessageVerificator
import java.security.*
import java.security.spec.InvalidKeySpecException
import java.security.spec.X509EncodedKeySpec

class Gen4MessageVerificationHandler : MessageVerificator {

    companion object {
        private const val PUBLIC_KEY = "-----BEGIN PUBLIC KEY-----\n"+
                "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAzLYHwX5Ug/oUObZ5eH5P\n" +
                "rEwmfj4E/YEfSKLgFSsyRGGsVmmjiXBmSbX2s3xbj/ofuvYtkMkP/VPFHy9E/8ox\n" +
                "Y+cRjPzydxz46LPY7jpEw1NHZjOyTeUero5e1nkLhiQqO/cMVYmUnuVcuFfZyZvc\n" +
                "8Apx5fBrIp2oWpF/G9tpUZfUUJaaHiXDtuYP8o8VhYtyjuUu3h7rkQFoMxvuoOFH\n" +
                "6nkc0VQmBsHvCfq4T9v8gyiBtQRy543leapTBMT34mxVIQ4ReGLPVit/6sNLoGLb\n" +
                "gSnGe9Bk/a5V/5vlqeemWF0hgoRtUxMtU1hFbe7e8tSq1j+mu0SHMyKHiHd+OsmU\n" +
                "IQIDAQAB\n" +
                "-----END PUBLIC KEY-----"
    }

    override fun verifyMessage(message: String, key: String): Boolean {
        try {
            var pubKeyPEM = PUBLIC_KEY.replace("-----BEGIN PUBLIC KEY-----\n", "")
            pubKeyPEM = pubKeyPEM.replace("-----END PUBLIC KEY-----", "")
            val pubKeyEncoded = Base64.decode(pubKeyPEM, Base64.DEFAULT)
            val pubKeySpec = X509EncodedKeySpec(pubKeyEncoded)
            val kf = KeyFactory.getInstance("RSA")
            val pubKey = kf.generatePublic(pubKeySpec)
            val sig = Signature.getInstance("SHA256withRSA")
            sig.initVerify(pubKey)
            sig.update(message.toByteArray())
            return sig.verify(Base64.decode(key, Base64.DEFAULT))
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        } catch (e: SignatureException) {
            e.printStackTrace()
        } catch (e: InvalidKeyException) {
            e.printStackTrace()
        } catch (e: InvalidKeySpecException) {
            e.printStackTrace()
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        } catch (e: ArrayIndexOutOfBoundsException) {
            e.printStackTrace()
        } catch (e: NullPointerException) {
            e.printStackTrace()
        }
        return false
    }
}