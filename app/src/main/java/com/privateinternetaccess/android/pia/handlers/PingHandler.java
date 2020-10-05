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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ReceiverCallNotAllowedException;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.privateinternetaccess.android.pia.PIAFactory;
import com.privateinternetaccess.android.pia.model.events.ServerPingEvent;
import com.privateinternetaccess.android.pia.model.response.PingResponse;
import com.privateinternetaccess.android.pia.tasks.FetchPingTask;
import com.privateinternetaccess.android.pia.utils.DLog;
import com.privateinternetaccess.android.pia.utils.Prefs;
import com.privateinternetaccess.core.model.PIAServer;
import com.privateinternetaccess.core.utils.IPIACallback;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import de.blinkt.openvpn.core.IOpenVPNServiceInternal;
import de.blinkt.openvpn.core.OpenVPNService;

public class PingHandler {

    private static final String TAG = "PingHandler";
    public static final String LAST_PING_GRAB = "LAST_PING_GRAB";
    public static final String PREFS_PINGS = "pings";
    // instant
    public static final int PING_TIME_INSTANT = 0;
    // 3 min
    public static final int PING_TIME_3_DIFFERENCE = 180000;
    // 10 min
    public static final int PING_TIME_10_DIFFERENCE = 600000;
    // 15 min
    public static final int PING_TIME_15_DIFFERENCE = 900000;
    // 30 min
    public static final int PING_TIME_30_DIFFERENCE = 1800000;

    private static PingHandler instance;
    private Context context;
    private Map<String, Long> pings;
    private static IOpenVPNServiceInternal rmService;
    private IPIACallback<ServerPingEvent> callback;
    private boolean serviceConnected;

    //Grabbing ping items
    BlockingQueue<Runnable> workQueue;
    ThreadPoolExecutor executor;

    private List<FetchPingTask> taskList;
    private List<PingResponse> responseList;

    public static PingHandler getInstance(Context context){
        if(instance == null){
            instance = new PingHandler();
            instance.pings = new HashMap<>();
            loadPings(context);
        }
        instance.connectHandler(context);
        instance.context = context;
        return instance;
    }

    private static void loadPings(Context context) {
        Prefs prefs = Prefs.with(context, PREFS_PINGS);
        Map<String, PIAServer> serverMap = PIAServerHandler.getInstance(context).getServers();
        for(String key : serverMap.keySet()){
            Long lastPing = prefs.getLong(key);
            if(lastPing != 0L)
                instance.getPings().put(key, lastPing);
        }
    }

    public boolean fetchPings(){
        return fetchPings(PING_TIME_3_DIFFERENCE);
    }

    public boolean fetchPings(long difference) {
        if (Prefs.with(context).get(PiaPrefHandler.GEN4_ACTIVE, true)) {
            DLog.d(TAG, "Next generation network enabled. Cancelling ping to legacy.");
            return false;
        }

        if (PIAFactory.getInstance().getVPN(context).isVPNActive()) {
            DLog.e(TAG, "Error when updating latencies. Connected to the VPN.");
            return false;
        }

        Prefs prefs = Prefs.with(context, PREFS_PINGS);
        long lastGrab = prefs.get(LAST_PING_GRAB, 0L);
        long now = Calendar.getInstance().getTimeInMillis();
        boolean pingServers = now - lastGrab > difference;
        DLog.d(TAG, "ping diff = " + pingServers);
        if(pingServers) {
            int number_of_cores = Runtime.getRuntime().availableProcessors();
            workQueue = new LinkedBlockingQueue<>();
            executor = new ThreadPoolExecutor(number_of_cores, number_of_cores, 7, TimeUnit.SECONDS, workQueue);

            taskList = new ArrayList<>();
            responseList = new ArrayList<>();

            if(!EventBus.getDefault().isRegistered(this))
                EventBus.getDefault().register(this);

            PIAServerHandler handler = PIAServerHandler.getInstance(context);
            for(PIAServer server : handler.getServers().values()) {
                if(!executor.isTerminating() || !executor.isShutdown() || !executor.isTerminated()) {
                    FetchPingTask task = new FetchPingTask(rmService, prefs.get(PiaPrefHandler.USE_TCP, false), server);
                    task.executeOnExecutor(executor, "");
                    taskList.add(task);
                }
            }
            prefs.set(LAST_PING_GRAB, Calendar.getInstance().getTimeInMillis());
        }
        return pingServers;
    }

    private void savePings(){
        Prefs prefs = Prefs.with(context, PREFS_PINGS);
        for(String key : pings.keySet()){
            prefs.set(key, pings.get(key));
        }
    }

    @Subscribe
    public void onReceive(PingResponse response){
        if(response.getPing() != -1L){
            pings.put(response.getName(), response.getPing());
        }
        responseList.add(response);
        if(responseList.size() == taskList.size()){
            ServerPingEvent event2 = new ServerPingEvent();
            EventBus.getDefault().post(event2);
            savePings();
            if(callback != null){
                callback.apiReturn(event2);
            }
            EventBus.getDefault().unregister(this);
            DLog.d(TAG, "Done");
            executor.shutdown();
            try {
                if(executor.awaitTermination(5, TimeUnit.SECONDS)){
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
            }
            executor = null;
            taskList = null;
            responseList = null;
        }
    }

    public Map<String, Long> getPings() {
        return pings;
    }

    public static void setRmService(IOpenVPNServiceInternal rmService) {
        PingHandler.rmService = rmService;
    }

    public void setCallback(IPIACallback<ServerPingEvent> callback) {
        this.callback = callback;
    }

    public ServiceConnection getConnection() {
        return mConnection;
    }

    private void connectHandler(Context context){
        if(!serviceConnected) {
            try {
                Intent intent = new Intent(context, OpenVPNService.class);
                intent.setAction(OpenVPNService.START_SERVICE);
                context.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
            } catch (ReceiverCallNotAllowedException e) {
                e.printStackTrace();
            }
        }
    }

    public ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {

            DLog.d("PingHandler.ServiceConnection","onServiceConnected");
            PingHandler.setRmService(IOpenVPNServiceInternal.Stub.asInterface(service));
            PingHandler.getInstance(context).fetchPings();
            serviceConnected = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            DLog.d("PingHandler.ServiceConnection","onServiceDisconnected");
            PingHandler.setRmService(null);
            try {
                context.unbindService(getConnection());
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
            serviceConnected = false;
        }

    };
}