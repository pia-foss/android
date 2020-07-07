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
import android.text.TextUtils;

import com.privateinternetaccess.android.pia.api.AccountApi;
import com.privateinternetaccess.android.pia.handlers.PiaPrefHandler;
import com.privateinternetaccess.android.pia.model.enums.LoginResponseStatus;
import com.privateinternetaccess.android.pia.model.events.LoginEvent;
import com.privateinternetaccess.android.pia.model.response.LoginResponse;
import com.privateinternetaccess.android.pia.utils.DLog;
import com.privateinternetaccess.core.utils.IPIACallback;

import org.greenrobot.eventbus.EventBus;

/**
 * Quick task to grab account information. OnPostExecute is where you can check if the user information is still valid
 *
 * Grabs user and password saved in {@link PiaPrefHandler#getLogin(Context)} & {@link PiaPrefHandler#getSavedPassword(Context)}
 *
 * if your logic is custom, use {@link #callback} to handle how you want. This will still log the user out.
 *
 * Created by half47 on 8/8/16.
 */
public class FetchAccountTask extends AsyncTask<Void, Void, LoginResponse> {

    private Context context;

    private IPIACallback<LoginResponse> callback;

    public FetchAccountTask(Context context, IPIACallback<LoginResponse> callback) {
        this.context = context;
        this.callback = callback;
    }

    @Override
    protected LoginResponse doInBackground(Void... voids) {
        String token = PiaPrefHandler.getAuthToken(context);

        if (TextUtils.isEmpty(token)) {
            LoginResponse res = new LoginResponse();
            res.setException(new Exception("Password is empty"));
            res.setLrStatus(LoginResponseStatus.AUTH_FAILED);
            return res;
        }

        AccountApi api = new AccountApi(context);
        return api.getAccountInformation(token);
    }

    @Override
    protected void onPostExecute(LoginResponse ai) {
        super.onPostExecute(ai);
        DLog.d("FetchAccountTask", "ai = " + ai.toString());
        EventBus.getDefault().post(new LoginEvent(ai));
        if(callback != null) {
            callback.apiReturn(ai);
        }
    }
}