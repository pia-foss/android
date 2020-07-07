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
import com.privateinternetaccess.android.pia.model.response.ExpireTokenResponse;
import com.privateinternetaccess.android.pia.utils.DLog;
import com.privateinternetaccess.core.utils.IPIACallback;

public class ExpireTokenTask extends AsyncTask<Void, Void, ExpireTokenResponse> {
    private Context context;

    private IPIACallback<ExpireTokenResponse> callback;
    private String token;

    public ExpireTokenTask(Context context, IPIACallback<ExpireTokenResponse> callback, String token) {
        this.context = context;
        this.callback = callback;
        this.token = token;
    }

    @Override
    protected ExpireTokenResponse doInBackground(Void... voids) {
        if (TextUtils.isEmpty(token)) {
            ExpireTokenResponse res = new ExpireTokenResponse();
            DLog.d("ExpireTokenTask", "Token is empty");
            res.setSuccess(false);
            return res;
        }

        AccountApi api = new AccountApi(context);
        return api.expireToken(token);
    }

    @Override
    protected void onPostExecute(ExpireTokenResponse res) {
        super.onPostExecute(res);

        if(callback != null) {
            callback.apiReturn(res);
        }
    }
}
