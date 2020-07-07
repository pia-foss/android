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

package com.privateinternetaccess.android.pia.impl;

import android.content.Context;
import android.os.AsyncTask;

import com.privateinternetaccess.android.R;
import com.privateinternetaccess.android.pia.connection.ConnectionResponder;
import com.privateinternetaccess.android.pia.handlers.PiaPrefHandler;
import com.privateinternetaccess.android.pia.interfaces.IConnection;
import com.privateinternetaccess.android.pia.model.events.FetchIPEvent;
import com.privateinternetaccess.android.pia.model.events.PortForwardEvent;
import com.privateinternetaccess.android.pia.model.response.MaceResponse;
import com.privateinternetaccess.android.pia.tasks.FetchIPTask;
import com.privateinternetaccess.android.pia.tasks.HitMaceTask;
import com.privateinternetaccess.android.pia.tasks.PortForwardTask;
import com.privateinternetaccess.android.tunnel.PIAVpnStatus;
import com.privateinternetaccess.core.utils.IPIACallback;

import org.greenrobot.eventbus.EventBus;


/**
 * Handles all methods going over the VPN or attributes of a users connection to the internet.
 *
 * Implementation for {@link IConnection} and can be accessed by {@link com.privateinternetaccess.android.pia.PIAFactory}
 *
 * Created by hfrede on 9/6/17.
 */

public class ConnectionImpl implements IConnection {

    private final Context context;

    public ConnectionImpl(Context context) {
        this.context = context;
    }

    @Override
    public void setConnectionResponderCallbacks(IPIACallback<FetchIPEvent> ipEvent, IPIACallback<PortForwardEvent> port, IPIACallback<MaceResponse> mace) {
        ConnectionResponder.setupCallbacks(ipEvent, port, mace);
    }

    @Override
    public void fetchIP( IPIACallback<FetchIPEvent> callback) {
        FetchIPTask.execute(context, callback);
    }

    @Override
    public void resetFetchIP() {
        FetchIPTask.resetValues(context);
    }

    @Override
    public HitMaceTask hitMace(IPIACallback<MaceResponse> callback) {
        HitMaceTask task = new HitMaceTask(context, true);
        task.setCallback(callback);
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        return task;
    }

    @Override
    public PortForwardTask fetchPort(IPIACallback<PortForwardEvent> callback) {
        PortForwardTask task = new PortForwardTask(context, R.string.portfwderror, R.string.n_a_port_forwarding);
        PIAVpnStatus.setCallback(callback);
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        return task;
    }

    @Override
    public String getPort() {
        String port = "";
        PortForwardEvent event = EventBus.getDefault().getStickyEvent(PortForwardEvent.class);
        if(event != null){
            port = event.getArg();
        }
        return port;
    }

    @Override
    public String getSavedIP() {
        return PiaPrefHandler.getLastIP(context);
    }

    @Override
    public boolean hasHitMace() {
        return PiaPrefHandler.isMaceActive(context);
    }
}
