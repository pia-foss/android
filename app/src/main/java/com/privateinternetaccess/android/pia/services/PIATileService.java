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

package com.privateinternetaccess.android.pia.services;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import com.privateinternetaccess.android.R;
import com.privateinternetaccess.android.pia.PIAFactory;
import com.privateinternetaccess.android.pia.interfaces.IAccount;
import com.privateinternetaccess.android.pia.interfaces.IVPN;
import com.privateinternetaccess.android.pia.model.events.VpnStateEvent;
import com.privateinternetaccess.android.pia.utils.DLog;
import com.privateinternetaccess.android.ui.LauncherActivity;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import de.blinkt.openvpn.VpnProfile;
import de.blinkt.openvpn.core.ConnectionStatus;
import de.blinkt.openvpn.core.ProfileManager;
import de.blinkt.openvpn.core.VpnStatus;

@TargetApi(Build.VERSION_CODES.N)
public class PIATileService extends TileService {

    @Override
    public void onClick() {
        super.onClick();
        if (!isLocked())
            clickAction();
        else
            unlockAndRun(new Runnable() {
                @Override
                public void run() {
                    clickAction();
                }
            });
    }


    private void clickAction() {
        IAccount account = PIAFactory.getInstance().getAccount(this);
        IVPN vpn = PIAFactory.getInstance().getVPN(this);
        if(account.isLoggedIn()) {
            if (vpn.isVPNActive()) {
                vpn.stop();
            } else if (vpn.isKillswitchActive()) {
                vpn.stopKillswitch();
            } else
                vpn.start();
        } else {
            Intent i = new Intent(getApplicationContext(), LauncherActivity.class);
            startActivity(i);
        }
    }


    @TargetApi(Build.VERSION_CODES.N)
    @Override
    public void onTileAdded() {
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        EventBus.getDefault().register(this);
    }

    @Subscribe(sticky = true)
    public void updateState(VpnStateEvent event){
        ConnectionStatus status = event.getLevel();

        Tile t = getQsTile();
        if(t != null) {
            IVPN ivpn = PIAFactory.getInstance().getVPN(this);
            if (status != ConnectionStatus.LEVEL_CONNECTED) {
                if(!ivpn.isKillswitchActive()) {
                    IAccount account = PIAFactory.getInstance().getAccount(this);
                    // No login
                    if (!account.isLoggedIn()) {
                        t.setLabel(getString(R.string.not_logged_in));
                        t.setState(Tile.STATE_UNAVAILABLE);
                    } else {
                        if (event.getLocalizedResId() == R.string.status_server_ping)
                            t.setLabel(getString(event.getLocalizedResId()));
                        else
                            t.setLabel(getString(R.string.qs_title));
                        t.setState(Tile.STATE_INACTIVE);
                    }
                } else {
                    t.setLabel(getString(R.string.tile_killswitch));
                }
            } else {
                VpnProfile vpn = ProfileManager.getLastConnectedVpn();
                String name;
                if (vpn == null) {
                    t.setLabel(getString(R.string.qs_disconnect_nolocation));
                } else {
                    name = vpn.getName().replace(" ", "\u00A0");
                    t.setLabel(getString(R.string.qs_disconnect, name));
                }
                t.setState(Tile.STATE_ACTIVE);
            }
            DLog.d("TilesService", "state = " + (t.getState() == Tile.STATE_ACTIVE));
            t.updateTile();
        }
    }

    @Override
    public void onStopListening() {
        DLog.d("PIATileService", "Stop Listening");
        EventBus.getDefault().unregister(this);
        super.onStopListening();
    }
}
