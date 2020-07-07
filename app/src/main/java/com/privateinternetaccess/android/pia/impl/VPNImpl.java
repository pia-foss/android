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
import android.content.Intent;

import com.privateinternetaccess.android.PIAApplication;
import com.privateinternetaccess.android.PIAKillSwitchStatus;
import com.privateinternetaccess.android.PIAOpenVPNTunnelLibrary;
import com.privateinternetaccess.android.R;
import com.privateinternetaccess.android.model.states.VPNProtocol;
import com.privateinternetaccess.android.model.states.VPNProtocol.Protocol;
import com.privateinternetaccess.android.pia.handlers.PiaPrefHandler;
import com.privateinternetaccess.android.pia.interfaces.IVPN;
import com.privateinternetaccess.android.pia.tasks.FetchIPTask;
import com.privateinternetaccess.android.pia.vpn.PiaOvpnConfig;
import com.privateinternetaccess.android.ui.widgets.WidgetBaseProvider;
import com.privateinternetaccess.android.utils.SnoozeUtils;

import java.io.IOException;

import de.blinkt.openvpn.VpnProfile;
import de.blinkt.openvpn.core.ConfigParser;
import de.blinkt.openvpn.core.ConnectionStatus;
import de.blinkt.openvpn.core.OpenVPNService;
import de.blinkt.openvpn.core.VPNLaunchHelper;
import de.blinkt.openvpn.core.VpnStatus;

/**
 * Handles interaction with the VPN's lifecycle.
 *
 * Implementation for {@link IVPN} and can be accessed by {@link com.privateinternetaccess.android.pia.PIAFactory}
 *
 * Created by hfrede on 9/6/17.
 */

public class VPNImpl implements IVPN {

    private Context context;

    public VPNImpl(Context context) {
        this.context = context;
    }

    @Override
    public void start(boolean connectPressed) {
        SnoozeUtils.resumeVpn(context, false);
        PiaPrefHandler.setLastConnection(context, System.currentTimeMillis());

        if (VPNProtocol.activeProtocol(context) == Protocol.OpenVPN) {
            try {
                FetchIPTask.resetValues(context);

                VpnStatus.updateStateString("GEN_CONFIG", "", R.string.state_gen_config,
                        ConnectionStatus.LEVEL_START);

                final VpnProfile vp = PiaOvpnConfig.generateVpnProfile(context);

                WidgetBaseProvider.updateWidget(context, true);

                new Thread() {
                    @Override
                    public void run() {
                        VPNLaunchHelper.startOpenVpn(vp, context);
                    }
                }.start();

            } catch (IOException | ConfigParser.ConfigParseError e) {
                e.printStackTrace();
            }
        }
        else {
            PIAApplication.getAsyncWorker().runAsync(PIAApplication.getWireguard()::startVpn);
        }
    }

    @Override
    public void start() {
        start(false);
    }

    @Override
    public void stop(boolean disconnectPressed) {
        PiaPrefHandler.setUserEndedConnection(context, true);
        PiaPrefHandler.setLastDisconnection(context, System.currentTimeMillis());

        if (VPNProtocol.activeProtocol(context) == Protocol.OpenVPN) {
            if (VpnStatus.isVPNActive()) {
                Intent i = new Intent(context, OpenVPNService.class);
                i.setAction(OpenVPNService.DISCONNECT_VPN);
                context.startService(i);
            }
        }
        else {
            if (PIAApplication.getWireguard() != null && PIAApplication.getWireguard().isConnecting) {
                PIAApplication.getAsyncWorker().runAsync(PIAApplication.getWireguard()::cancel);
            }
            else if (PIAApplication.getWireguard() != null) {
                PIAApplication.getAsyncWorker().runAsync(() -> PIAApplication.getWireguard().stopVpn(disconnectPressed));
            }
        }
    }

    @Override
    public void stop() {
        stop(false);
    }

    @Override
    public void pause() {
        if (VpnStatus.isVPNActive()) {
            Intent i = new Intent(context, OpenVPNService.class);
            i.setAction(OpenVPNService.PAUSE_VPN);
            context.startService(i);
        }
    }

    @Override
    public void resume() {
        if (VpnStatus.isVPNActive()) {
            Intent i = new Intent(context.getApplicationContext(), OpenVPNService.class);
            i.setAction(OpenVPNService.RESUME_VPN);
            context.startService(i);
        }
    }

    @Override
    public boolean isKillswitchActive() {
        return PIAKillSwitchStatus.isKillSwitchActive();
    }

    @Override
    public void stopKillswitch() {
        PIAKillSwitchStatus.stopKillSwitch(context);
        //Callbacks aren't working here so putting the logic here to make sure everything is taken care of.
        PIAKillSwitchStatus.triggerUpdateKillState(false);
        PIAOpenVPNTunnelLibrary.mNotifications.stopKillSwitchNotification(context);
    }

    @Override
    public boolean isVPNActive() {
        if (VPNProtocol.activeProtocol(context) == Protocol.Wireguard &&
                PIAApplication.getWireguard() != null && PIAApplication.getWireguard().isActive()) {
            return true;
        }

        return VpnStatus.isVPNActive();
    }

}