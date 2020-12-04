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

package com.privateinternetaccess.android.pia.connection;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import com.privateinternetaccess.android.BuildConfig;
import com.privateinternetaccess.android.PIAApplication;
import com.privateinternetaccess.android.PIAKillSwitchStatus;
import com.privateinternetaccess.android.PIAOpenVPNTunnelLibrary;
import com.privateinternetaccess.android.pia.PIAFactory;
import com.privateinternetaccess.android.pia.handlers.PiaPrefHandler;
import com.privateinternetaccess.android.pia.interfaces.IVPN;
import com.privateinternetaccess.android.pia.model.events.VpnStateEvent;
import com.privateinternetaccess.android.pia.receivers.PortForwardingReceiver;
import com.privateinternetaccess.android.pia.utils.DLog;
import com.privateinternetaccess.android.tunnel.PIAVpnStatus;
import com.privateinternetaccess.android.tunnel.PortForwardingStatus;

import org.greenrobot.eventbus.EventBus;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import de.blinkt.openvpn.core.ConnectionStatus;
import de.blinkt.openvpn.core.VpnStatus;

import static de.blinkt.openvpn.core.OpenVpnManagementThread.GATEWAY;

/**
 *
 * Use this to handle all connection features. This will handle port fowarding, MACE and fetching the new IP by using {@link PortForwardTask} and {@link HitMaceTask}.
 *
 * You can toggle all of these features with {@link com.privateinternetaccess.android.pia.PIABuilder} or {@link PiaPrefHandler} methods.
 *
 *
 * Change log:
 *
 * Created by arne on 10.10.13.
 * Updated by half47 a while later.
 * Completely changed by half47 an even while later.
 * Fixed by arne on 13.6.17.
 * Fixed mace not working by half47 4/18 and documented more.
 *
 *
 */
public class ConnectionResponder implements VpnStatus.StateListener, PIAKillSwitchStatus.KillSwitchStateListener {

    public static final String TAG = "ConnectionResponder";
    public static boolean MACE_IS_RUNNING;

    private Context context;
    private static ConnectionResponder mInstance;

    private AlarmManager alarmManager;
    private PendingIntent portForwardingIntent;

    static ThreadPoolExecutor executor;
    static BlockingQueue<Runnable> workQueue;

    private static int REQUESTING_PORT_STRING;
    public static boolean VPN_REVOKED;
    private Runnable REVIVE_MECHANIC;
    private int connectionAttempts;

    private ConnectionResponder(Context c, int resId) {
        context = c;
        REQUESTING_PORT_STRING = resId;
        alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        PIAKillSwitchStatus.addKillSwitchListener(this);
    }

    public static ConnectionResponder initConnection(Context c, int resID) {
        if (mInstance == null)
            mInstance = new ConnectionResponder(c, resID);
        return mInstance;
    }

    @Override
    public void killSwitchUpdate(boolean isInKillSwitch) { }

    @Override
    public void setConnectedVPN(String uuid) { }

    @Override
    public void updateState(String state, String message, int localizedResId, final ConnectionStatus level) {
        new Thread(new Runnable() { // Threading this as the amount of work has bloated overtime.
            public void run() {
                handleStateChange(state, message, level);
            }
        }).start();
    }

    private synchronized void handleStateChange(String state, String message, ConnectionStatus level) {
        DLog.d(TAG, level + "");
        if (level == ConnectionStatus.LEVEL_CONNECTED) {
            if(executor == null || (executor != null && executor.isShutdown())) {
                int number_of_cores = Runtime.getRuntime().availableProcessors();
                workQueue = new LinkedBlockingQueue<>();
                executor = new ThreadPoolExecutor(number_of_cores, number_of_cores, 30, TimeUnit.SECONDS, workQueue);
            }

            if (state.equals(GATEWAY)) {
                PiaPrefHandler.setGatewayEndpoint(context, message);
            }

            if (PiaPrefHandler.isPortForwardingEnabled(context)) {
                startPortForwarding();
            }

            if(PiaPrefHandler.isKillswitchEnabled(context)){
                PIAOpenVPNTunnelLibrary.mNotifications.stopKillSwitchNotification(context);
            }

            resetRevivalMechanic();
        } else if(level == ConnectionStatus.LEVEL_NOTCONNECTED) {
            DLog.d(TAG, "Not connected Clear");
            PiaPrefHandler.clearLastIPVPN(context);
            MACE_IS_RUNNING = false;
            PIAVpnStatus.clearOldData();
            cleanupExecutor();
            PiaPrefHandler.setVPNConnecting(context, false);
            clearPortForwarding();
        } else if(level == ConnectionStatus.LEVEL_CONNECTING_NO_SERVER_REPLY_YET || level == ConnectionStatus.LEVEL_NONETWORK){
            PiaPrefHandler.setVPNConnecting(context, true);
            PiaPrefHandler.clearLastIPVPN(context);
            VPN_REVOKED = false;
            clearPortForwarding();
        }
    }

    private boolean isVPNReviveNeeded(){
        boolean reviveNeeded = false;
        String processName = PIAApplication.getProcessName(context);
        boolean isCorrectProcess = processName != null && processName.equals("com.privateinternetaccess.android");
        // Has the VPN been started before
        // && thread is null, which means revival isn't already started
        // && is this the regular process
        DLog.d(TAG, "executor = " + (executor != null) + " mechanic = " + REVIVE_MECHANIC + " correctProcess = " + isCorrectProcess);
        if((executor != null || PiaPrefHandler.wasVPNConnecting(context)) && REVIVE_MECHANIC == null && isCorrectProcess) {
            // Was the connection ended by the user, if so, reset the condition
            boolean connectionEndedByUser = PiaPrefHandler.wasConnectionEndedByUser(context, true);
            // Also check if the VPN wasn't revoked by Samsung
            DLog.d(TAG,"EndedByUser = " + connectionEndedByUser + " VPN Revoked = " + VPN_REVOKED);
            if(!connectionEndedByUser){
                Handler h = new Handler(Looper.getMainLooper());
                REVIVE_MECHANIC = new Runnable() {
                    @Override
                    public void run() {
                        IVPN vpn = PIAFactory.getInstance().getVPN(context);
                        // 3s have past and the vpn is still not up. Restart
                        DLog.d(TAG,"vpn_revoked = " + VPN_REVOKED);
                        if (!vpn.isVPNActive() && !VPN_REVOKED) {
                            vpn.start();
                        } else {
                            VPN_REVOKED = false;
                        }
                        REVIVE_MECHANIC = null; // Clear out REVIVE_MECHANIC to let another REVIVE HAPPEN
                    }
                };
                // create delay
                int delay = getDelay();
                h.postDelayed(REVIVE_MECHANIC, delay);
                if(connectionAttempts <= 7) // Maxing out at 10s delay. Starting off with 3s.
                    connectionAttempts++;
                // Send alert to the rest of the app alerting of retrying connection after x seconds
                VpnStateEvent event = new VpnStateEvent("CONNECTRETRY", String.valueOf(getDelay() / 1000),
                        de.blinkt.openvpn.R.string.state_waitconnectretry, ConnectionStatus.LEVEL_CONNECTING_NO_SERVER_REPLY_YET);
                EventBus.getDefault().postSticky(event);

                reviveNeeded = true;
            }
        }
        return reviveNeeded;
    }

    private int getDelay(){
        int delay = 3000;
        delay += delay + (1000 * connectionAttempts);
        return delay;
    }

    private void resetRevivalMechanic() {
        PiaPrefHandler.setUserEndedConnection(context, false);
        PiaPrefHandler.setVPNConnecting(context, false);
        REVIVE_MECHANIC = null;
        VPN_REVOKED = false;
        connectionAttempts = 0;
    }

    private void cleanupExecutor() {
        try {
            if (executor != null) {
                executor.shutdown();
                try {
                    if (executor.awaitTermination(5, TimeUnit.SECONDS)) {
                        executor.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    executor.shutdownNow();
                }
                executor = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startPortForwarding() {
        PIAVpnStatus.setPortForwardingStatus(PortForwardingStatus.REQUESTING, context.getString(REQUESTING_PORT_STRING));
        if (portForwardingIntent != null) {
            return;
        }

        Intent intent = new Intent(context, PortForwardingReceiver.class);
        portForwardingIntent = PendingIntent.getBroadcast(
                context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT
        );
        alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                0,
                AlarmManager.INTERVAL_FIFTEEN_MINUTES,
                portForwardingIntent
        );
    }

    private void clearPortForwarding() {
        PiaPrefHandler.clearGatewayEndpoint(context);
        PIAVpnStatus.setPortForwardingStatus(PortForwardingStatus.NO_PORTFWD, "");
        if (portForwardingIntent == null) {
            return;
        }
        PiaPrefHandler.clearBindPortForwardInformation(context);
        alarmManager.cancel(portForwardingIntent);
        portForwardingIntent = null;
    }
}
