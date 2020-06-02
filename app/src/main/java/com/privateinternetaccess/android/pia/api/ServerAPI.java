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

package com.privateinternetaccess.android.pia.api;

import android.content.Context;
import android.util.Base64;

import com.privateinternetaccess.android.pia.handlers.PIAServerHandler;
import com.privateinternetaccess.android.pia.handlers.PiaPrefHandler;
import com.privateinternetaccess.android.pia.model.PIAServer;
import com.privateinternetaccess.android.pia.model.response.ServerResponse;
import com.privateinternetaccess.android.pia.utils.DLog;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import okhttp3.Request;
import okhttp3.Response;

/**
 * Grabs the server list to use.
 *
 * Notice that this works in conjunction with {@link com.privateinternetaccess.android.pia.vpn.PiaServerDatabase} and doesn't act like other apis.
 *
 * This may be adjusted in the future to align the code up to the other classes.
 *
 * Created by hfrede on 6/13/17.
 */

public class ServerAPI extends PiaApi {

    private static String publicKey = "-----BEGIN PUBLIC KEY-----\n" +
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAzLYHwX5Ug/oUObZ5eH5P\n" +
            "rEwmfj4E/YEfSKLgFSsyRGGsVmmjiXBmSbX2s3xbj/ofuvYtkMkP/VPFHy9E/8ox\n" +
            "Y+cRjPzydxz46LPY7jpEw1NHZjOyTeUero5e1nkLhiQqO/cMVYmUnuVcuFfZyZvc\n" +
            "8Apx5fBrIp2oWpF/G9tpUZfUUJaaHiXDtuYP8o8VhYtyjuUu3h7rkQFoMxvuoOFH\n" +
            "6nkc0VQmBsHvCfq4T9v8gyiBtQRy543leapTBMT34mxVIQ4ReGLPVit/6sNLoGLb\n" +
            "gSnGe9Bk/a5V/5vlqeemWF0hgoRtUxMtU1hFbe7e8tSq1j+mu0SHMyKHiHd+OsmU\n" +
            "IQIDAQAB\n" +
            "-----END PUBLIC KEY-----";

    public static final int SERVER_FILE_NUMBER = 1001;

    private Context context;
    private static boolean testing;

    public ServerAPI(Context context) {
        super();
        this.context = context;
    }

    /**
     * Grabs the servers from /vpninfo/servers
     *
     * @return ServerResponse object with body and servers if the signature is verified.
     */
    public ServerResponse fetchServers(){
        ServerResponse res = new ServerResponse();

        try { // Wrapping for tests
            selectBaseURL(context);
        } catch (Exception e) {
        }

        String baseUrl = getBaseURL(context);
        //baseUrl = baseUrl.replaceFirst("https", "http");

        try {
            Request request = new Request.Builder()
                    .url(baseUrl + "vpninfo/servers?version=" + SERVER_FILE_NUMBER + "&os=android")
                    .header("User-Agent", ANDROID_HTTP_CLIENT)
                    .build();
            Response response = getOkHttpClient().newCall(request).execute();

            if (response.body().source().request(100000)) { // this is to prevent an attack vector on this call
                DLog.d("PIA", "Vpn config to long");
                return res;
            }

            if(response.isSuccessful()){
                String body = response.body().string();

                if(verifySignature(body)){
                    res = PIAServerHandler.parseServers(body);
                    res.setBody(body);

                    PiaPrefHandler.setLastServerVersion(context, SERVER_FILE_NUMBER);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return res;
    }

    public static boolean forceUpdate(Context context) {
        return SERVER_FILE_NUMBER != PiaPrefHandler.getLastServerVersion(context);
    }

    public static boolean verifySignature(String json) {
        String[] parts = json.split("\n\n", 2);
        String data = parts[0];
        try {
            String signature = parts[1];
            /* java wants der format ... */
            String pubkeyPEM = publicKey.replace("-----BEGIN PUBLIC KEY-----\n", "");
            pubkeyPEM = pubkeyPEM.replace("-----END PUBLIC KEY-----", "");
            byte[] pubKeyEncoded = decodeB64(pubkeyPEM);


            X509EncodedKeySpec pubkeySpec = new X509EncodedKeySpec(pubKeyEncoded);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            PublicKey pubkey = kf.generatePublic(pubkeySpec);
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initVerify(pubkey);
            sig.update(data.getBytes());

            return sig.verify(decodeB64(signature));
        } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException
                | InvalidKeySpecException | IllegalArgumentException | ArrayIndexOutOfBoundsException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            if (testing) {
                return parts.length == 2;
            } else {
                e.printStackTrace();
            }
        }
        return false;
    }

    public static void setTesting(boolean testing) {
        ServerAPI.testing = testing;
    }

    public static byte[] decodeB64(String base64) {
        return Base64.decode(base64, Base64.DEFAULT);
    }
}
