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
import android.content.Intent;
import android.os.AsyncTask;

import com.privateinternetaccess.android.pia.api.PiaApi;
import com.privateinternetaccess.android.pia.api.PortForwardApi;
import com.privateinternetaccess.android.pia.handlers.PIAServerHandler;
import com.privateinternetaccess.android.pia.model.exceptions.PortForwardingError;
import com.privateinternetaccess.android.pia.model.response.PortForwardResponse;
import com.privateinternetaccess.android.pia.utils.DLog;
import com.privateinternetaccess.android.tunnel.PIAVpnStatus;
import com.privateinternetaccess.android.tunnel.PortForwardingStatus;
import com.privateinternetaccess.core.model.PIAServer;

import java.io.IOException;

import de.blinkt.openvpn.core.VpnStatus;


/**
 * Quick task to get port forwarding setup for the user over the VPN connection.
 *
 * Responds with otto with type of PortForwardEvent
 *
 * -999 means port forwarding isn't enabled. If you see this, you are doing something wrong.
 *
 * Returns {@link com.privateinternetaccess.android.pia.model.events.PortForwardEvent} via Eventbus and callback
 *
 * Created by half47 on 9/1/16.
 */
public class PortForwardTask extends AsyncTask<String, Void, PortForwardResponse> {

    private Context mContext;
    private int errorResId;
    private int n_a_port_forwarding;

    public PortForwardTask(Context mContext, int errorResId, int n_a_port_forwarding) {
        this.mContext = mContext;
        this.errorResId = errorResId;
        this.n_a_port_forwarding = n_a_port_forwarding;
    }

    @Override
    protected PortForwardResponse doInBackground(String... strings) {
        PortForwardResponse port = new PortForwardResponse(-1);
        PIAServer currentServer = PIAServerHandler.getInstance(mContext).getSelectedRegion(mContext, false);
        if(currentServer.isAllowsPF()) {
            // Even we are already connected to the VPN, directly querying the server results in
            // a race condition and gets us the non VPN IP, wait 1,234s
            try {
                Thread.sleep(PiaApi.VPN_DELAY_TIME);
            } catch (InterruptedException ignored) {
            }
            for (int tries = 0; tries < 3 && port.getPort() == -1; tries++) {
                if (!isCancelled()) {
                    DLog.d("PortForwarding", "tries = " + tries);
                    String expMessage = null;
                    try {
                        if (VpnStatus.isVPNActive()) {
                            PortForwardApi api = new PortForwardApi();
                            port = api.getPort(mContext);
                        }
                    } catch (IOException e) {
                        expMessage = mContext.getString(errorResId, e.getLocalizedMessage());
                    } catch (PortForwardingError e) {
                        expMessage = e.getLocalizedMessage();
                        // PortForwardingError is a real error from server, no use to try again
                        port.setPort(-2);
                    } catch (Exception e) {
                        expMessage = "";
                    }
                    DLog.d("PortForwarding", "port = " + port + " msg = " + expMessage);
                    if (port.getPort() > 0) {
                        break;
                    } else if (tries <= 1 && port.getPort() >= -1) {
                        //remove the error message here is to prevent an error before we are done trying.
                    } else {
                        PIAVpnStatus.setPortForwardingStatus(PortForwardingStatus.ERROR, expMessage);
                    }
                }
            }
        } else {
            port.setPort(-999);
        }

        return port;
    }

    @Override
    protected void onPostExecute(PortForwardResponse response) {
        super.onPostExecute(response);
        if (response.getPort() > 0 && !isCancelled() && response.getPort() != -999) {
            PIAVpnStatus.setPortForwardingStatus(PortForwardingStatus.SUCCESS, "" + response.getPort());
            sendSuccessfullPortForward(response.getPort(), mContext);
        } else {
            PIAVpnStatus.setPortForwardingStatus(PortForwardingStatus.ERROR, mContext.getString(n_a_port_forwarding));
        }
    }

    private static void sendSuccessfullPortForward(int port, Context c) {
        Intent intent = new Intent();
        intent.setAction("com.privateinternetaccess.com.PORTFORWARD");
        intent.putExtra("port", port);
        c.sendBroadcast(intent);
    }
}
