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

import androidx.annotation.NonNull;

import com.privateinternetaccess.android.BuildConfig;
import com.privateinternetaccess.android.pia.handlers.PiaPrefHandler;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;

/**
 * Utils methods for the API classes
 *
 * When extending this class, this requires you to call this constructor to use the OKHTTPCLIENT
 *
 * Created by arne on 21.03.15.
 */
public class PiaApi {

    public static final String GEN4_MACE_ENABLED_DNS = "10.0.0.241";

    public static final String ANDROID_HTTP_CLIENT = "privateinternetaccess.com Android Client/" + BuildConfig.VERSION_NAME + "(" + BuildConfig.VERSION_CODE + ")";

    private OkHttpClient OKHTTPCLIENT;

    public static final List<String> PROXY_PATHS = Arrays.asList(
            "https://www.privateinternetaccess.com/",
            "https://www.piaproxy.net/");

    public PiaApi() {
        X509TrustManager trustManager = null;
        SSLSocketFactory sslSocketFactory = null;
        OkHttpClient.Builder builder = new OkHttpClient.Builder();

        PIAAuthenticator AUTHENTICATOR = new PIAAuthenticator();
        builder.authenticator(AUTHENTICATOR);
        builder.connectTimeout(8, TimeUnit.SECONDS);

        try {
            trustManager = getX509TrustManager();
            sslSocketFactory = getSslSocketFactory(trustManager);
        } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
            e.printStackTrace();
        }

        if (trustManager != null && sslSocketFactory != null) {
            builder.sslSocketFactory(sslSocketFactory, trustManager);
        }

        OKHTTPCLIENT = builder.build();
    }

    @NonNull
    private X509TrustManager getX509TrustManager() throws NoSuchAlgorithmException, KeyStoreException {
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init((KeyStore) null);
        TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
        if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
            throw new IllegalStateException("Unexpected default trust managers:" + Arrays.toString(trustManagers));
        }
        return (X509TrustManager) trustManagers[0];
    }
    private SSLSocketFactory getSslSocketFactory(X509TrustManager trustManager) throws NoSuchAlgorithmException, KeyManagementException {
        SSLContext sslContext = SSLContext.getInstance("SSL");
        sslContext.init(null, new TrustManager[] { trustManager }, null);
        return sslContext.getSocketFactory();
    }

    /**
     * This is the host url in the buildconfig.HOST. Adjusted depending on build varient.
     *
     * @return
     */
    public static String getBaseURL(@NonNull Context context) {
        boolean isStaging = false;
        try {
            isStaging = PiaPrefHandler.useStaging(context);
        } catch (Exception e) {
        }
        if (isStaging) {
            String host = BuildConfig.STAGEINGHOST;
            if (host.contains("staging-1")) {
                int stagingServerNumber = PiaPrefHandler.getStagingServerNumber(context);
                host = host.replace("staging-1", "staging-" +stagingServerNumber);
            }
            return host;
        } else {
            String url = "https://www.privateinternetaccess.com/";
            try {
                url = PROXY_PATHS.get(PiaPrefHandler.getSelectedProxyPath(context));
            } catch (Exception e) {
            }
            return url;
        }
    }

    /**
     * This appends api/client/ to the base url and the api call you send it.
     *
     *
     * @param context
     * @param apiCall
     * @return
     * @throws MalformedURLException
     */
    protected static URL getClientURL(Context context, String apiCall) throws MalformedURLException {
        return new URL(getBaseURL(context) + "api/client/" + apiCall);
    }

    public OkHttpClient getOkHttpClient(){
        return OKHTTPCLIENT;
    }
}