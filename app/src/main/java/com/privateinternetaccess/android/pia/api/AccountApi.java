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
import android.text.TextUtils;

import com.privateinternetaccess.android.pia.model.TrialData;
import com.privateinternetaccess.android.pia.model.TrialTestingData;
import com.privateinternetaccess.android.pia.model.UpdateAccountInfo;
import com.privateinternetaccess.android.pia.model.enums.LoginResponseStatus;
import com.privateinternetaccess.android.pia.model.response.ExpireTokenResponse;
import com.privateinternetaccess.android.pia.model.response.LoginResponse;
import com.privateinternetaccess.android.pia.model.response.TokenResponse;
import com.privateinternetaccess.android.pia.model.response.TrialResponse;
import com.privateinternetaccess.android.pia.model.response.UpdateEmailResponse;
import com.privateinternetaccess.android.pia.utils.DLog;

import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;

import okhttp3.Credentials;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * All methods pertaining account information getting and setting.
 *
 * Created by hfrede on 6/13/17.
 */

public class AccountApi extends PiaApi {

    public static final String TAG = "AccountAPI";

    private Context context;

    public AccountApi(Context context) {
        super();
        this.context = context;
    }

    /**
     * Get login information from the user
     *
     * @param token
     * @return {@link LoginResponse} with a status on whether it was a success, failure or throttled.
     */
    public LoginResponse getAccountInformation(String token) {
        try {
            //This fixes a 21 redirect crash on login
            LoginResponse ai = new LoginResponse();
            setAuthenticatorUP(token);
            Request request = new Request.Builder().url(getClientURL(context,"v2/account")).
                    addHeader("Authorization", "Token " + token).
                    addHeader("client_version", ANDROID_HTTP_CLIENT).
                    build();
            Response response = getOkHttpClient().newCall(request).execute();
            int status = response.code();
            DLog.d("PIAAPI", "status = " + status);
            String res = response.body().string();
            DLog.d("PIAAPI", "body = " + res);
            if (status == 200) {
                ai.setLrStatus(LoginResponseStatus.CONNECTED);
                if (!TextUtils.isEmpty(res)) {
                    ai.parse(new JSONObject(res));
                }
            } else if (status == 401) {
                ai.setLrStatus(LoginResponseStatus.AUTH_FAILED);
            } else if (status == 429) {
                ai.setLrStatus(LoginResponseStatus.THROTTLED);
            }
            cleanAuthenticator();
            return ai;
        } catch (Exception e) {
            e.printStackTrace();
            LoginResponse res = new LoginResponse();
            res.setException(e);
            res.setLrStatus(LoginResponseStatus.AUTH_FAILED);
            return res;
        }
    }

    /**
     * Alter the email of a user.
     *
     * @param username
     * @param accountInfo - new email
     * @return boolean - 200 = true, else = false
     */
    public UpdateEmailResponse changeEmail(String username, UpdateAccountInfo accountInfo) {
        UpdateEmailResponse response = new UpdateEmailResponse(accountInfo.getEmail());
        try {
            String postdata = "email=" + URLEncoder.encode(String.format("%s", accountInfo.getEmail()), "UTF-8");
            String credential = Credentials.basic(username, accountInfo.getPassword());

            RequestBody body = MultipartBody.create(MediaType.parse("application/x-www-form-urlencoded"),
                    postdata);

            Request request = new Request.Builder()
                    .url(getClientURL(context,"account"))
                    .post(body)
                    .addHeader("Authorization", credential)
                    .addHeader("User-Agent", PiaApi.ANDROID_HTTP_CLIENT)
                    .addHeader("Content-Type", "application/x-www-form-urlencoded")
                    .build();

            Response res = getOkHttpClient().newCall(request).execute();

            int status = res.code();

            cleanAuthenticator();

            DLog.d("AccountAPI", "Email: " + res);

            if (status != 200) {
                response.setChanged(false);
                return response;
            }

            response.setChanged(true);
            return response;

        } catch (IOException e) {
            e.printStackTrace();
        }
        response.setChanged(false);
        return response;
    }

    public TokenResponse authenticate(String username, String password) {
        try {
            TokenResponse ai = new TokenResponse();
            RequestBody body = new FormBody.Builder().
                    add("username", username).
                    add("password", password).
                    build();
            Request request = new Request.Builder().url(getClientURL(context,"v2/token")).
                    addHeader("client_version", ANDROID_HTTP_CLIENT).
                    post(body).
                    build();
            Response response = getOkHttpClient().newCall(request).execute();
            int status = response.code();
            DLog.d("PIAAPI", "status = " + status);
            String res = response.body().string();
            DLog.d("PIAAPI", "body = " + res);
            if (status == 200) {
                ai.setLrStatus(LoginResponseStatus.CONNECTED);
                if (!TextUtils.isEmpty(res)) {
                    ai.parse(new JSONObject(res));
                    DLog.d("PIAAPI", "Token: " + ai.getToken());
                }
            } else if (status == 401) {
                ai.setLrStatus(LoginResponseStatus.AUTH_FAILED);
            } else if (status == 429) {
                ai.setLrStatus(LoginResponseStatus.THROTTLED);
            }
            cleanAuthenticator();
            return ai;
        } catch (Exception e) {
            e.printStackTrace();
            TokenResponse res = new TokenResponse();
            res.setLrStatus(LoginResponseStatus.AUTH_FAILED);
            return res;
        }
    }

    public ExpireTokenResponse expireToken(String token) {
        try {
            ExpireTokenResponse etRes = new ExpireTokenResponse();
            setAuthenticatorUP(token);
            RequestBody body = new FormBody.Builder().
                    build();
            Request request = new Request.Builder().url(getClientURL(context,"v2/expire_token")).
                    addHeader("client_version", ANDROID_HTTP_CLIENT).
                    addHeader("Authorization", "Token " + token).
                    post(body).
                    build();
            Response response = getOkHttpClient().newCall(request).execute();
            int status = response.code();
            DLog.d("PIAAPI Expire", "status = " + status);
            String res = response.body().string();
            DLog.d("PIAAPI Expire", "body = " + res);
            if (status == 200) {
                etRes.setSuccess(true);
            }
            else {
                etRes.setSuccess(false);
            }

            cleanAuthenticator();
            return etRes;
        }
        catch (Exception e) {
            e.printStackTrace();

            ExpireTokenResponse etRes = new ExpireTokenResponse();
            etRes.setSuccess(false);

            return etRes;
        }
    }


    public TrialResponse createTrialAccount(TrialData data, TrialTestingData testingData){
        TrialResponse response = new TrialResponse(0);
        DLog.d(TAG,"trialAccountTesting = " + testingData.isTesting());
        DLog.d(TAG,"data = " + data.toString());
        if (!testingData.isTesting()) {
            try {
                StringBuilder sb = new StringBuilder();
                sb.append("email=").append(URLEncoder.encode(String.format("%s", data.getEmail()), "UTF-8"));
                sb.append("&");
                sb.append("pin=").append(URLEncoder.encode(String.format("%s", data.getPin()), "UTF-8"));

                DLog.d(TAG,sb.toString());

                RequestBody body = MultipartBody.create(MediaType.parse("application/x-www-form-urlencoded"),
                        sb.toString());

                Request request = new Request.Builder()
                        .url(getClientURL(context,"giftcard_redeem"))
                        .post(body)
                        .addHeader("User-Agent", PiaApi.ANDROID_HTTP_CLIENT)
                        .addHeader("Content-Type", "application/x-www-form-urlencoded")
                        .build();

                DLog.d(TAG, "path = " + getClientURL(context, "giftcard_redeem"));

                Response res = getOkHttpClient().newCall(request).execute();
                int status = res.code();
                DLog.d(TAG,"status = " + status);

                JSONObject json = null;
                String serverResponse = null;
                String username = null;
                String password = null;
                try {
                    json = new JSONObject(res.body().string());
                    serverResponse = json.optString("code");
                    username = json.optString("username");
                    password = json.optString("password");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                DLog.d(TAG,"json = " + json.toString());
                DLog.d(TAG,"response = " + serverResponse);

                response.setStatus(status);
                response.setResponse(serverResponse);
                response.setUsername(username);
                response.setPassword(password);

            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            response.setStatus(testingData.getStatus());
            response.setResponse(testingData.getMessage());
            response.setUsername(testingData.getUsername());
            response.setPassword(testingData.getPassword());
        }
        return response;
    }
}