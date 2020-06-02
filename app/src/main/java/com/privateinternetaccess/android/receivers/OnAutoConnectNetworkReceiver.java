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

package com.privateinternetaccess.android.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import com.privateinternetaccess.android.PIAApplication;
import com.privateinternetaccess.android.R;
import com.privateinternetaccess.android.pia.PIAFactory;
import com.privateinternetaccess.android.pia.handlers.PiaPrefHandler;
import com.privateinternetaccess.android.pia.interfaces.IVPN;
import com.privateinternetaccess.android.pia.utils.DLog;
import com.privateinternetaccess.android.utils.SnoozeUtils;
import com.privateinternetaccess.android.wireguard.model.Tunnel;

import de.blinkt.openvpn.core.VpnStatus;

public class OnAutoConnectNetworkReceiver extends BroadcastReceiver {

    private static final long CONNECTION_DELAY = 5000;
    private static final long DISCONNECTION_DELAY = 5000;
    private static final long CHANGE_DELAY = 2000;

    @Override
    public void onReceive(final Context context, Intent intent) {
//        DLog.d("NetworkSettings", "Change Time: " + PiaPrefHandler.getLastNetworkChange(context));
//        DLog.d("NetworkSettings", "System Time: " + System.currentTimeMillis());
//        if (PiaPrefHandler.getLastNetworkChange(context) + CHANGE_DELAY  > System.currentTimeMillis()) {
//            return;
//        }

        PiaPrefHandler.setLastNetworkChange(context, System.currentTimeMillis());

        IVPN vpn = PIAFactory.getInstance().getVPN(context);

        ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        boolean vpnConnected = VpnStatus.isVPNActive() || (PIAApplication.getWireguard() != null && PIAApplication.getWireguard().lastState == Tunnel.State.UP);

        if (isConnected) {
            if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI && PiaPrefHandler.shouldConnectOnWifi(context)) {
                WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                WifiInfo info = wifiManager.getConnectionInfo();

                String ssid  = info.getSSID();
                ssid = ssid.substring(1, ssid.length() - 1);

                DLog.d("NetworkSettings", "Last Connect: " + PiaPrefHandler.getLastConnection(context));
                DLog.d("NetworkSettings", "Last Disconnect: " + PiaPrefHandler.getLastDisconnection(context));
                DLog.d("NetworkSettings", "System Time: " + System.currentTimeMillis());
                DLog.d("NetworkSettings", "VPN Active: " + vpn.isVPNActive());
                DLog.d("NetworkSettings", "Has Alarm: " + SnoozeUtils.hasActiveAlarm(context));

                if (PiaPrefHandler.getTrustedNetworks(context).contains(ssid)) {
                    if (vpnConnected && PiaPrefHandler.getLastConnection(context) + CONNECTION_DELAY < System.currentTimeMillis()) {
                        vpn.stop();
                        PIAApplication.getWireguard().stopVpn();
                        DLog.d("NetworkSettings", "Disabling connection trusted network");
                    }
                }
                else if (!vpnConnected &&
                        PiaPrefHandler.getLastDisconnection(context) + DISCONNECTION_DELAY < System.currentTimeMillis() &&
                        !SnoozeUtils.hasActiveAlarm(context)) {
                    DLog.d("NetworkSettings", "Starting VPN");
                    vpn.start();
                }

            }
        }
    }
}
