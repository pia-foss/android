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

import com.privateinternetaccess.android.PIAApplication;
import com.privateinternetaccess.android.model.events.TrustedWifiEvent;
import com.privateinternetaccess.android.model.listModel.NetworkItem;
import com.privateinternetaccess.android.pia.PIAFactory;
import com.privateinternetaccess.android.pia.handlers.PiaPrefHandler;
import com.privateinternetaccess.android.pia.interfaces.IVPN;
import com.privateinternetaccess.android.pia.utils.Prefs;
import com.privateinternetaccess.android.utils.SnoozeUtils;
import com.privateinternetaccess.android.utils.TrustedWifiUtils;
import com.privateinternetaccess.android.wireguard.model.Tunnel;

import org.greenrobot.eventbus.EventBus;

import de.blinkt.openvpn.core.VpnStatus;

import static android.net.ConnectivityManager.TYPE_WIFI;

public class OnAutoConnectNetworkReceiver extends BroadcastReceiver {

    private static final long CONNECTION_DELAY = 5000;
    private static final long DISCONNECTION_DELAY = 5000;

    @Override
    public void onReceive(final Context context, Intent intent) {
        if (!Prefs.with(context).getBoolean(PiaPrefHandler.NETWORK_MANAGEMENT)) {
            return;
        }

        PiaPrefHandler.setLastNetworkChange(context, System.currentTimeMillis());

        IVPN vpn = PIAFactory.getInstance().getVPN(context);

        ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        boolean vpnConnected = VpnStatus.isVPNActive() || (PIAApplication.getWireguard() != null && PIAApplication.getWireguard().lastState == Tunnel.State.UP);

        if (isConnected) {
            NetworkItem bestRule;
            if (activeNetwork.getType() == TYPE_WIFI) {
                bestRule = TrustedWifiUtils.getBestRule(context);
            }
            else  {
                bestRule = TrustedWifiUtils.getMobileRule(context);
            }

            if (bestRule == null)
                return;

            if (bestRule.behavior == NetworkItem.NetworkBehavior.ALWAYS_CONNECT && !vpnConnected
                    && PiaPrefHandler.getLastDisconnection(context) + DISCONNECTION_DELAY < System.currentTimeMillis() &&
                    !SnoozeUtils.hasActiveAlarm(context)) {
                vpn.start();
            }
            else if (bestRule.behavior == NetworkItem.NetworkBehavior.ALWAYS_DISCONNECT && vpnConnected
                    && PiaPrefHandler.getLastConnection(context) + CONNECTION_DELAY < System.currentTimeMillis()) {
                vpn.stop();
            }
        }

        EventBus.getDefault().post(new TrustedWifiEvent());
    }
}
