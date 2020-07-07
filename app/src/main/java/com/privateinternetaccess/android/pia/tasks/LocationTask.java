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

import com.privateinternetaccess.android.pia.api.LocationApi;
import com.privateinternetaccess.android.pia.handlers.PiaPrefHandler;
import com.privateinternetaccess.android.pia.model.events.FetchLocationEvent;
import com.privateinternetaccess.android.pia.model.response.LocationResponse;
import com.privateinternetaccess.core.utils.IPIACallback;

import org.greenrobot.eventbus.EventBus;

/**
 * Quick task for grabbing the users location. Not used in app yet
 *
 * returns {@link FetchLocationEvent} via otto and {@link LocationResponse} via callback
 *
 * Created by half47 on 2/9/17.
 */

public class LocationTask extends AsyncTask<String, Void, LocationResponse> {

    private Context context;

    private IPIACallback<LocationResponse> callback;

    @Override
    protected LocationResponse doInBackground(String... params) {
        LocationApi api = new LocationApi();
        return api.getLocation(context);
    }

    @Override
    protected void onPostExecute(LocationResponse ipInfo) {
        super.onPostExecute(ipInfo);
        PiaPrefHandler.saveLastIPInfo(context, ipInfo.getBody());
        if(callback != null) {
            callback.apiReturn(ipInfo);
        }
        EventBus.getDefault().post(new FetchLocationEvent(ipInfo));
    }

    public void setCtx(Context context) {
        this.context = context;
    }

    public void setCallback(IPIACallback<LocationResponse> callback) {
        this.callback = callback;
    }
}
