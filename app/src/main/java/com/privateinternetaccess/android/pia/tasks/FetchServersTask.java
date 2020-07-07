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

import com.privateinternetaccess.android.pia.api.ServerAPI;
import com.privateinternetaccess.core.model.ServerResponse;
import com.privateinternetaccess.core.utils.IPIACallback;

public class FetchServersTask extends AsyncTask<String, Void, ServerResponse> {

    private IPIACallback<ServerResponse> callback;

    private Context context;

    public FetchServersTask(Context context, IPIACallback<ServerResponse> callback) {
        this.callback = callback;
        this.context = context;
    }

    @Override
    protected ServerResponse doInBackground(String... strings) {
        ServerAPI api = new ServerAPI(context);
        return api.fetchServers();
    }

    @Override
    protected void onPostExecute(ServerResponse serverResponse) {
        super.onPostExecute(serverResponse);
        if(callback != null){
            callback.apiReturn(serverResponse);
        }
    }
}
