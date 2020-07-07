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
import android.os.Handler;


import com.privateinternetaccess.android.pia.PIAFactory;
import com.privateinternetaccess.android.pia.api.MaceApi;
import com.privateinternetaccess.android.pia.api.PiaApi;
import com.privateinternetaccess.android.pia.connection.ConnectionResponder;
import com.privateinternetaccess.android.pia.handlers.PiaPrefHandler;
import com.privateinternetaccess.android.pia.model.events.HitMaceEvent;
import com.privateinternetaccess.android.pia.model.response.MaceResponse;
import com.privateinternetaccess.android.pia.utils.DLog;
import com.privateinternetaccess.core.utils.IPIACallback;

import org.greenrobot.eventbus.EventBus;

/**
 * Hit the mace server twice for every connection and you are good. Don't worry about it after that.
 * This was not my decision and I hope I can change it in the future.
 *
 * Responds {@link HitMaceEvent} via Eventbus, {@link MaceResponse} via callback
 */
public class HitMaceTask extends AsyncTask<Integer, Void, MaceResponse> {


    private Context context;
    boolean sendAgain;

    private IPIACallback<MaceResponse> callback;

    public HitMaceTask(Context context, boolean sendAgain){
        this.context = context;
        this.sendAgain = sendAgain;
    }

    @Override
    protected MaceResponse doInBackground(Integer... params) {
        ConnectionResponder.MACE_IS_RUNNING = true;
        try {
            Thread.sleep(PiaApi.VPN_DELAY_TIME);
        } catch (InterruptedException ignored) {
        }
        MaceApi api = new MaceApi();
        MaceResponse hitUrl = api.hitMace(context);
        return hitUrl;
    }

    @Override
    protected void onPostExecute(MaceResponse success) {
        super.onPostExecute(success);
        DLog.d("HitMaceTask", "postExecute = " + success);
        if(PIAFactory.getInstance().getVPN(context).isVPNActive()) {
            PiaPrefHandler.setMaceActive(context, true);
            sendAgain = false;
        }
        EventBus.getDefault().post(new HitMaceEvent());
        if(callback != null) {
            callback.apiReturn(success);
        }
        if(sendAgain) {
            Handler h = new Handler(context.getMainLooper());
            h.postDelayed(new Runnable() {
                @Override
                public void run() {
                    new HitMaceTask(context, false).execute(0);
                }
            }, 5000);
        }
        ConnectionResponder.MACE_IS_RUNNING = sendAgain;
    }

    public void setCallback(IPIACallback<MaceResponse> callback) {
        this.callback = callback;
    }
}
