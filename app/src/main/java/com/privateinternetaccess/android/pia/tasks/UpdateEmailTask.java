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
import com.privateinternetaccess.android.pia.model.UpdateAccountInfo;
import com.privateinternetaccess.android.pia.model.events.UpdateEmailEvent;
import com.privateinternetaccess.android.pia.model.response.UpdateEmailResponse;
import com.privateinternetaccess.core.utils.IPIACallback;

import org.greenrobot.eventbus.EventBus;

/**
 * Created by hfrede on 9/6/17.
 */

public class UpdateEmailTask extends AsyncTask<Void, Void, UpdateEmailResponse> {

    private Context context;
    private IPIACallback<UpdateEmailResponse> callback;
    private UpdateAccountInfo accountInfo;

    public UpdateEmailTask(Context context, UpdateAccountInfo account) {
        this.context = context;
        this.accountInfo = account;
    }

    @Override
    protected UpdateEmailResponse doInBackground(Void... voids) {
        AccountApi api = new AccountApi(context);
        return api.changeEmail(PiaPrefHandler.getLogin(context),
                accountInfo);
    }

    @Override
    protected void onPostExecute(UpdateEmailResponse response) {
        super.onPostExecute(response);
        EventBus.getDefault().post(new UpdateEmailEvent(response));
        if(callback != null)
            callback.apiReturn(response);
    }

    public void setCallback(IPIACallback<UpdateEmailResponse> callback) {
        this.callback = callback;
    }
}