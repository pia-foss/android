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

import android.os.AsyncTask;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;

import com.privateinternetaccess.android.pia.model.response.PingResponse;
import com.privateinternetaccess.android.pia.utils.DLog;
import com.privateinternetaccess.core.model.PIAServer;
import com.privateinternetaccess.core.utils.IPIACallback;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.blinkt.openvpn.core.IOpenVPNServiceInternal;

/**
 *
 */
public class FetchPingTask extends AsyncTask<String, Void, PingResponse> {

    private static final String TAG = "FetchPingTask";
    public static final int TIMEOUT = 3000;

    private IOpenVPNServiceInternal rmService;
    private boolean useTCP;
    private PIAServer server;
    private IPIACallback<PingResponse> callback;

    public FetchPingTask(IOpenVPNServiceInternal rmService, boolean useTCP, PIAServer server) {
        this.rmService = rmService;
        this.useTCP = useTCP;
        this.server = server;
    }

    @Override
    protected PingResponse doInBackground(String... addresses) {
        PingResponse response = new PingResponse();
        if (server.getPingEndpoint() == null) {
            return response;
        }

        String[] parts = server.getPingEndpoint().split(":");
        if(parts.length == 2){
            int port = 0;
            try {
                port = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
            }
            response.setName(server.getKey());
            if(port > 0) {
                InetSocketAddress socket = new InetSocketAddress(parts[0], port);
                Long ping = grabPing(socket);
                DLog.d(TAG, "Ping " + ping + " time for " + server.getKey());
                response.setPing(ping);
            } else {
                response.setPing(-1L);
            }
        }
        return response;
    }

    @Override
    protected void onPostExecute(PingResponse response) {
        super.onPostExecute(response);
        EventBus.getDefault().post(response);
        if(callback != null) {
            callback.apiReturn(response);
        }
    }

    private long grabPing(InetSocketAddress address){
        Long lat = -1L;
        String p = "";
        try {
            long before, after;
            if(!useTCP){
                DatagramSocket s = new DatagramSocket();
                s.connect(address);
                if (rmService != null) {
                    boolean protectWorked = rmService.protect( ParcelFileDescriptor.fromDatagramSocket(s));
                    if (protectWorked)
                        p = "(p)";
                    else
                        p = "(f)";
                }
                DatagramPacket pingPacket = new DatagramPacket(new byte[]{0x55}, 1, address);
                before = System.currentTimeMillis();
                s.setSoTimeout(TIMEOUT); // 3s
                s.send(pingPacket);
                try {
                    s.receive(pingPacket);
                } catch (IOException e) {
                    DLog.d(TAG, "Pinging server " + address + " failed: " + e.getMessage());
                }
                after = System.currentTimeMillis();
                lat = after - before;
            } else {
                Socket s = new Socket();
                s.setTcpNoDelay(true);
                if (rmService != null) {
                    boolean protectWorked = rmService.protect(ParcelFileDescriptor.fromSocket(s));
                    if (protectWorked)
                        p = "(p)";
                    else
                        p = "(f)";
                }
                before = System.currentTimeMillis();
                s.connect(address, 60 * 1000);
                after = System.currentTimeMillis();
                lat = after - before;
                s.close();
            }
        } catch (IOException e) {
            DLog.d(TAG, "Pinging " + p + " server " + address + " failed:" + e.getMessage());
            e.printStackTrace();
        } catch (RemoteException | NullPointerException e) {
            e.printStackTrace();
        }
        return lat;
    }

}
