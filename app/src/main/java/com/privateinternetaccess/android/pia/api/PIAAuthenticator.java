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

import androidx.annotation.Nullable;

import com.privateinternetaccess.android.pia.utils.DLog;

import java.io.IOException;

import okhttp3.Credentials;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;

/**
 * Authenticator class for Okhttp.
 *
 * Created by hfrede on 11/14/17.
 */

public class PIAAuthenticator implements okhttp3.Authenticator{

    private static final String TAG = "PIAAuthenticator";
    private static final String AUTHORIZATION = "Authorization";
    private String username;
    private String password;

    private String token;

    @Nullable
    @Override
    public Request authenticate(Route route, Response response) {
        if (response.request().header(AUTHORIZATION) != null) {
            DLog.d(TAG, "Authentication aborted. Already tried to authenticate with proper header.");
            return null; // Give up, we've already failed to authenticate.
        }

        if (token != null) {
            DLog.d(TAG, "Authenticating with token");
            String credential = "Token " + token;
            return response.request().newBuilder()
                    .header(AUTHORIZATION, credential)
                    .build();
        }

        DLog.d(TAG, "Authentication aborted. No available credentials.");
        return null;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getToken() { return token; }

    public void setToken(String token) { this.token = token; }
}