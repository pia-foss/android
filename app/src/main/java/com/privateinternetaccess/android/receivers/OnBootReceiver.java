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
import com.privateinternetaccess.android.ui.LauncherActivity;
import com.privateinternetaccess.android.ui.connection.MainActivity;


/**
 * If we add this to other apps, copy the code and build it in the app. This is going to be app specific.
 *
 */
public class OnBootReceiver extends BroadcastReceiver {

    // Debug: am broadcast -a android.intent.action.BOOT_COMPLETED
    @Override
    public void onReceive(Context context, Intent intent) {

        final String action = intent.getAction();

        if (Intent.ACTION_BOOT_COMPLETED.equals(action) && PiaPrefHandler.doAutoStart(context)) {
            launchVPN(context);
        }
    }

    private void launchVPN(Context context) {
        Intent intent = VpnService.prepare(context);

        // `intent` null means its prepared and not null means go to the VPNPermission activity
        if (intent == null) {
            PIAServerHandler.getInstance(context).triggerLatenciesUpdate(error -> {
                PIAFactory.getInstance().getVPN(context).start();
                return null;
            });
        } else {
            Intent i = new Intent(context.getApplicationContext(), LauncherActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            i.setAction(MainActivity.START_VPN_SHORTCUT);
            context.startActivity(i);
        }
    }
}
