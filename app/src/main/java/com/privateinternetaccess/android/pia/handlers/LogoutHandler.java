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

package com.privateinternetaccess.android.pia.handlers;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import com.privateinternetaccess.android.PIAKillSwitchStatus;
import com.privateinternetaccess.android.R;
import com.privateinternetaccess.android.pia.IPIACallback;
import com.privateinternetaccess.android.pia.PIAFactory;
import com.privateinternetaccess.android.pia.interfaces.IAccount;
import com.privateinternetaccess.android.pia.interfaces.IVPN;
import com.privateinternetaccess.android.pia.model.events.KillSwitchEvent;
import com.privateinternetaccess.android.pia.model.events.VpnStateEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import de.blinkt.openvpn.core.ConnectionStatus;
import de.blinkt.openvpn.core.VpnStatus;

/**
 * Handles the logout of a PIA Account using {@link IAccount#logout()} and a listener can be added for when the user agrees the log out by adding a {@link #callback} with a {@link IPIACallback}.
 *
 * Also will shut down the vpn and killswitch on success.
 *
 * Created by half47 on 4/7/17.
 */

public class LogoutHandler {

    private Activity activity;

    private IPIACallback<Boolean> callback;

    private boolean loggingout;

    public LogoutHandler(Activity activity, IPIACallback<Boolean> callback) {
        this.activity = activity;
        this.callback = callback;
        EventBus.getDefault().register(this);
    }

    public void logout() {
        Context context = activity;
        AlertDialog.Builder ab = new AlertDialog.Builder(context);
        ab.setTitle(R.string.logout_confirmation);
        ab.setMessage(R.string.logout_confirmation_text);
        ab.setPositiveButton(R.string.logout, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                logoutLogic(false);
            }
        });
        ab.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        ab.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                onDestroy();
            }
        });
        ab.show();
    }

    public void logoutLogic(boolean goToPurchasing) {
        loggingout = true;
        if(!EventBus.getDefault().isRegistered(this)){
            EventBus.getDefault().register(this);
        }
        IVPN vpn = PIAFactory.getInstance().getVPN(activity);
        if(!vpn.isVPNActive()) {
            if(vpn.isKillswitchActive()){
                vpn.stopKillswitch();
            }
            removeInfoAndListener();
            callback.apiReturn(goToPurchasing);
        } else {
            vpn.stop();
        }
    }

    private void removeInfoAndListener() {
        PIAFactory.getInstance().getAccount(activity).logout(PiaPrefHandler.getAuthToken(activity));
        EventBus.getDefault().unregister(this);
    }

    @Subscribe
    public void updateState(VpnStateEvent event) {
        if(event.getLevel() == ConnectionStatus.LEVEL_NOTCONNECTED) {
            IVPN vpn = PIAFactory.getInstance().getVPN(activity);
            if(vpn.isKillswitchActive())
                vpn.stopKillswitch();
            if(loggingout) {
                removeInfoAndListener();
                callback.apiReturn(false);
                loggingout = false;
            }
        }
    }

    public void onDestroy() {
        EventBus.getDefault().unregister(this);
    }
}
