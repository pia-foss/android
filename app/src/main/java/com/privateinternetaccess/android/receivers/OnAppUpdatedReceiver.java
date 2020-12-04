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
import android.net.VpnService;

import com.privateinternetaccess.android.pia.PIAFactory;
import com.privateinternetaccess.android.pia.handlers.PIAServerHandler;
import com.privateinternetaccess.android.pia.handlers.PiaPrefHandler;
import com.privateinternetaccess.android.pia.interfaces.IVPN;
import com.privateinternetaccess.android.ui.LauncherActivity;
import com.privateinternetaccess.android.ui.connection.MainActivity;

public class OnAppUpdatedReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        boolean isPrepared = false;
        try {
            Intent vpnIntent = VpnService.prepare(context);
            isPrepared = vpnIntent == null; // Null means its prepared and not null means go to the VPNPermission activity
        } catch (Exception e) {
            e.printStackTrace();
        }
        boolean isLoggedIn = PIAFactory.getInstance().getAccount(context).loggedIn();
        IVPN vpn = PIAFactory.getInstance().getVPN(context);
        if(isLoggedIn && !vpn.isVPNActive() && PiaPrefHandler.isConnectOnAppUpdate(context)) {
            // if pia is updated, if logged in, if vpn not already active, if option is active.
            if (isPrepared) {
                PIAServerHandler.getInstance(context).triggerLatenciesUpdate(error -> {
                    PIAFactory.getInstance().getVPN(context).start();
                    return null;
                });
            } else {
                // if all of it is on, make sure we have permissions
                Intent i = new Intent(context, LauncherActivity.class);
                i.putExtra(MainActivity.START_VPN_SHORTCUT, true);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(i);
            }
        }
    }
}