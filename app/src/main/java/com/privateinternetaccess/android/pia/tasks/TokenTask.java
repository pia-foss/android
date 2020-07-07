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

package com.privateinternetaccess.android.pia.tasks;

import android.content.Context;
import android.os.AsyncTask;

import com.privateinternetaccess.android.pia.api.AccountApi;
import com.privateinternetaccess.android.pia.handlers.PiaPrefHandler;
import com.privateinternetaccess.android.pia.model.LoginInfo;
import com.privateinternetaccess.android.pia.model.enums.LoginResponseStatus;
import com.privateinternetaccess.android.pia.model.events.LoginEvent;
import com.privateinternetaccess.android.pia.model.events.TokenEvent;
import com.privateinternetaccess.android.pia.model.response.TokenResponse;
import com.privateinternetaccess.core.utils.IPIACallback;

import org.greenrobot.eventbus.EventBus;

public class TokenTask extends AsyncTask<String, Void, TokenResponse> {
    private Context context;

    private LoginInfo info;
    private IPIACallback<TokenResponse> callback;

    public TokenTask(Context context, LoginInfo info) {
        this.context = context;
        this.info = info;
    }

    @Override
    protected TokenResponse doInBackground(String... voids) {
        AccountApi api = new AccountApi(context);
        return api.authenticate(info.getUsername(), info.getPassword());
    }

    @Override
    protected void onPostExecute(TokenResponse authResponse) {
        super.onPostExecute(authResponse);
        if(authResponse.getStatus() == LoginResponseStatus.CONNECTED){
            PiaPrefHandler.setUserIsLoggedIn(context, true);
            PiaPrefHandler.saveUser(context, info.getUsername());
            PiaPrefHandler.saveAuthToken(context, authResponse.getToken());
        }
        if(callback != null) {
            callback.apiReturn(authResponse);
        }

        EventBus.getDefault().postSticky(new TokenEvent(authResponse));
    }

    public void setCallback(IPIACallback<TokenResponse> callback) {
        this.callback = callback;
    }
}
