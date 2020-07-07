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
import com.privateinternetaccess.android.pia.model.TrialData;
import com.privateinternetaccess.android.pia.model.TrialTestingData;
import com.privateinternetaccess.android.pia.model.events.TrialEvent;
import com.privateinternetaccess.android.pia.model.response.TrialResponse;
import com.privateinternetaccess.core.utils.IPIACallback;

import org.greenrobot.eventbus.EventBus;

public class TrialCreationTask extends AsyncTask<TrialData, Void, TrialResponse> {

    private Context context;
    private IPIACallback<TrialResponse> callback;

    public TrialCreationTask(Context context) {
        this.context = context;
    }

    @Override
    protected TrialResponse doInBackground(TrialData... trialData) {
        TrialData data = PiaPrefHandler.getTempTrialData(context);
        TrialTestingData testingData = PiaPrefHandler.getTrialTestingData(context);
        AccountApi api = new AccountApi(context);
        return api.createTrialAccount(data, testingData);
    }

    @Override
    protected void onPostExecute(TrialResponse trialResponse) {
        super.onPostExecute(trialResponse);
        EventBus.getDefault().postSticky(new TrialEvent(trialResponse));
        if (trialResponse.getStatus() == 200) {
            PiaPrefHandler.cleanTempTrialData(context);
        }
        if(callback != null)
            callback.apiReturn(trialResponse);
    }

    public void setCallback(IPIACallback<TrialResponse> callback) {
        this.callback = callback;
    }
}
