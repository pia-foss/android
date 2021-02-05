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
import com.privateinternetaccess.android.pia.handlers.PIAServerHandler;
import com.privateinternetaccess.android.pia.handlers.PiaPrefHandler;
import com.privateinternetaccess.android.pia.interfaces.IVPN;
import com.privateinternetaccess.android.pia.model.events.ConnectionAttemptsExhaustedEvent;
import com.privateinternetaccess.android.pia.model.events.VpnStateEvent;
import com.privateinternetaccess.android.pia.providers.VPNFallbackEndpointProvider;
import com.privateinternetaccess.android.pia.utils.DLog;
import com.privateinternetaccess.android.pia.vpn.PiaOvpnConfig;
import com.privateinternetaccess.android.tunnel.PIAVpnStatus;
import com.privateinternetaccess.android.ui.widgets.WidgetBaseProvider;
import com.privateinternetaccess.android.utils.SnoozeUtils;
import com.privateinternetaccess.core.model.PIAServer;

import org.greenrobot.eventbus.EventBus;

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

    private static final String TAG = "VPNImpl";
    private Context context;

    public VPNImpl(Context context) {
        this.context = context;
    }

    @Override
    public void start(boolean connectPressed) {
        PIAServer region = PIAServerHandler.getInstance(context).getSelectedRegion(context, false);
        if (region.isOffline()) {
            DLog.d(TAG, "Unable to start VPN. Selected region is offline");
            EventBus.getDefault().postSticky(
                    new VpnStateEvent(
                            "CONNECT",
                            "Region offline",
                            R.string.failed_connect_status,
                            ConnectionStatus.LEVEL_NONETWORK
                    )
            );
            return;
        }

        VPNFallbackEndpointProvider.Companion.getSharedInstance().start(context, (vpnEndpoint, error) -> {
            if (error != null) {
                DLog.d(TAG, error.getMessage());
                EventBus.getDefault().post(new ConnectionAttemptsExhaustedEvent());
                stop();
                return null;
            }

            PIAVpnStatus.setLastConnectedRegion(region);
            SnoozeUtils.resumeVpn(context, false);
            PiaPrefHandler.setLastConnection(context, System.currentTimeMillis());
            PiaPrefHandler.clearLastIPVPN(context);
            WidgetBaseProvider.updateWidget(context, true);
            if (VPNProtocol.activeProtocol(context) == Protocol.OpenVPN) {
                try {
                    VpnProfile profile = PiaOvpnConfig.generateVpnProfile(context, vpnEndpoint);
                    PIAApplication.getAsyncWorker().runAsync(() ->
                            VPNLaunchHelper.startOpenVpn(profile, context));
                } catch (IOException | ConfigParser.ConfigParseError e) {
                    e.printStackTrace();
                }
            }
            else {
                PIAApplication.getAsyncWorker().runAsync(() ->
                        PIAApplication.getWireguard().startVpn(vpnEndpoint));
            }
            return null;
        });
    }

    @Override
    public void start() {
        start(false);
    }

    @Override
    public void stop(boolean disconnectPressed) {
        VPNFallbackEndpointProvider.Companion.getSharedInstance().stop();
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