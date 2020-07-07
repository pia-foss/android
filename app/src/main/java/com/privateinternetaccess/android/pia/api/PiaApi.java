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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import androidx.annotation.NonNull;

import com.privateinternetaccess.android.BuildConfig;
import com.privateinternetaccess.android.pia.handlers.PiaPrefHandler;
import com.privateinternetaccess.android.pia.utils.DLog;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.zip.Deflater;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;

/**
 * Utils methods for the API classes
 *
 * When extending this class, this requires you to call this constructor to use the OKHTTPCLIENT
 *
 * Created by arne on 21.03.15.
 */
public class PiaApi {

    public static final int VPN_DELAY_TIME = 1234;

    public static final String ANDROID_HTTP_CLIENT = "privateinternetaccess.com Android Client/" + BuildConfig.VERSION_NAME + "(" + BuildConfig.VERSION_CODE + ")";
    public static final String ANDROID_VERSION = "v" + BuildConfig.VERSION_NAME + "(" + BuildConfig.VERSION_CODE + ")";

    public static final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");

    private OkHttpClient OKHTTPCLIENT;
    protected static Interceptor INTERCEPTOR;
    private PIAAuthenticator AUTHENTICATOR;

    public static final List<String> PROXY_PATHS = Arrays.asList(
            "https://www.privateinternetaccess.com/",
            "https://piaproxy.net/");

    public PiaApi() {
        X509TrustManager trustManager = null;
        SSLSocketFactory sslSocketFactory = null;
        OkHttpClient.Builder builder = new OkHttpClient.Builder();

        AUTHENTICATOR = new PIAAuthenticator();
        builder.authenticator(AUTHENTICATOR);
        builder.connectTimeout(8, TimeUnit.SECONDS);

        if (INTERCEPTOR != null) {
            builder.addInterceptor(INTERCEPTOR);
        }

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
     * sets the username and password on the {@link PIAAuthenticator} class that is used on {@link #OKHTTPCLIENT} in this class.
     *
     * Call {@link #cleanAuthenticator()} when done with the call to make sure we clean up the username and password from the authenticator.
     *
     * @param username
     * @param password
     */
    protected void setAuthenticatorUP(String username, String password){
        AUTHENTICATOR.setUsername(username);
        AUTHENTICATOR.setPassword(password);
    }

    protected void setAuthenticatorUP(String token) {
        AUTHENTICATOR.setToken(token);
    }

    /**
     * sets the username and password to null.
     *
     */
    protected void cleanAuthenticator(){
        AUTHENTICATOR.setUsername(null);
        AUTHENTICATOR.setPassword(null);
        AUTHENTICATOR.setToken(null);
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
        if (isStaging)
            return BuildConfig.STAGEINGHOST;
        else {
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

    private static HttpURLConnection apiRequestNative(Context context, final String username, final String password, String apiCall) throws IOException {
        Authenticator.setDefault(new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password.toCharArray());
            }
        });

        HttpURLConnection urlConnection;

        URL url = new URL(getBaseURL(context) + "api/client/" + apiCall);
        urlConnection = (HttpURLConnection) url.openConnection();
        return urlConnection;
    }

    /**
     * Cycles through potential base URLs to find the first reachable.
     *
     * Returns FALSE if no network connection is present or no path is reachable
     * Returns TRUE if a reachable path is found
     * @param context
     * @return
     */
    public static boolean selectBaseURL(Context context) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();

        DLog.d("PiaApi", "Selected Proxy Starting");

        if (activeNetworkInfo == null || !activeNetworkInfo.isConnected()) {
            DLog.d("PiaApi", "Selected Proxy : No Network");
            return false;
        }

        for (int i = 0; i < PROXY_PATHS.size(); i++) {
            String path = PROXY_PATHS.get(i);

            try {
                URL url = new URL(path);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(1000);
                conn.connect();

                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    PiaPrefHandler.setSelectedProxyPath(context, i);
                    DLog.d("PiaApi", "Selected Proxy: " + PROXY_PATHS.get(i));
                    return true;
                }
            } catch (Exception e) {
                DLog.d("PiaApi", e.toString());
            }
        }

        return false;
    }

    /**
     * Its in the name.
     *
     * @param outputStream
     * @param data
     * @throws IOException
     */
    public static void compress(ByteArrayOutputStream outputStream, byte[] data) throws IOException {
        Deflater deflater = new Deflater();
        deflater.setInput(data);
        deflater.finish();
        byte[] buffer = new byte[1024];
        while (!deflater.finished()) {
            int count = deflater.deflate(buffer); // returns the generated code... index
            outputStream.write(buffer, 0, count);
        }
        outputStream.close();
    }

    public static void setInterceptor(Interceptor interceptor) {
        INTERCEPTOR = interceptor;
    }

    public OkHttpClient getOkHttpClient(){
        return OKHTTPCLIENT;
    }
}