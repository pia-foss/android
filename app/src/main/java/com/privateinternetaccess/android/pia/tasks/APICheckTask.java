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

import com.privateinternetaccess.android.pia.api.IpApi;
import com.privateinternetaccess.android.pia.model.events.APICheckEvent;
import com.privateinternetaccess.android.pia.model.response.APICheckResponse;
import com.privateinternetaccess.android.pia.model.response.IPResponse;
import com.privateinternetaccess.core.utils.IPIACallback;

import org.greenrobot.eventbus.EventBus;

public class APICheckTask extends AsyncTask<String, Void, APICheckResponse>{

    private IPIACallback<APICheckResponse> callback;
    private Context context;

    public APICheckTask(IPIACallback<APICheckResponse> callback, Context context) {
        this.callback = callback;
        this.context = context;
    }

    @Override
    protected APICheckResponse doInBackground(String... strings) {
        IpApi api = new IpApi(context);
        IPResponse response = api.getIPAddress();
        APICheckResponse ret = new APICheckResponse();
        ret.setCanConnect(response != null);
        return ret;
    }

    @Override
    protected void onPostExecute(APICheckResponse apiCheckResponse) {
        super.onPostExecute(apiCheckResponse);
        if(callback != null){
            callback.apiReturn(apiCheckResponse);
        }
        EventBus.getDefault().post(new APICheckEvent(apiCheckResponse));
    }
}

