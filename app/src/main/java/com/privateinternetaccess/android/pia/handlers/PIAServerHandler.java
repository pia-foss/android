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


import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.core.os.ConfigurationCompat;

import com.privateinternetaccess.android.BuildConfig;
import com.privateinternetaccess.android.R;
import com.privateinternetaccess.android.model.events.SeverListUpdateEvent;
import com.privateinternetaccess.android.model.events.SeverListUpdateEvent.ServerListUpdateState;
import com.privateinternetaccess.android.pia.PIAFactory;
import com.privateinternetaccess.android.pia.providers.ModuleClientStateProvider;
import com.privateinternetaccess.android.pia.receivers.FetchServersReceiver;
import com.privateinternetaccess.android.pia.receivers.PingReceiver;
import com.privateinternetaccess.android.pia.utils.DLog;
import com.privateinternetaccess.android.pia.utils.Prefs;
import com.privateinternetaccess.android.pia.utils.ServerResponseHelper;
import com.privateinternetaccess.android.utils.ServerUtils;
import com.privateinternetaccess.android.utils.SystemUtils;
import com.privateinternetaccess.common.regions.RegionLowerLatencyInformation;
import com.privateinternetaccess.common.regions.RegionsUtils;
import com.privateinternetaccess.common.regions.model.RegionsResponse;
import com.privateinternetaccess.core.model.PIAServer;
import com.privateinternetaccess.core.model.PIAServerInfo;
import com.privateinternetaccess.core.model.ServerResponse;
import com.privateinternetaccess.core.utils.IPIACallback;
import com.privateinternetaccess.regions.RegionsAPI;
import com.privateinternetaccess.regions.RegionsBuilder;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Vector;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;

/**
 * Handler class for helping with the servers and pings to those servers.
 *
 */
public class PIAServerHandler {

    public static final String GEN4_LAST_SERVER_BODY = "GEN4_LAST_SERVER_BODY";
    private static final String TAG = "PIAServerHandler";
    public static final String LAST_SERVER_GRAB = "LAST_SERVER_GRAB";
    public static final String SELECTEDREGION = "selectedregion";
    public static final long SERVER_TIME_DIFFERENCE = 600000L; //10m

    private static PIAServerHandler instance;

    private static Prefs prefs;
    private static AlarmManager alarmManager;
    private static RegionsAPI regionModule;

    private static PIAServer randomServer;

    public static PIAServerHandler getInstance(Context context){
        if(instance == null){
            startup(context);
        }
        return instance;
    }

    private static ServerListUpdateState serverListFetchState = ServerListUpdateState.FETCH_SERVERS_FINISHED;
    private static void setServerListFetchState(ServerListUpdateState state) {
        serverListFetchState = state;
        EventBus.getDefault().post(new SeverListUpdateEvent(serverListFetchState));
    }

    public static ServerListUpdateState getServerListFetchState() {
        return serverListFetchState;
    }

    public static void startup(Context context) {
        instance = new PIAServerHandler();
        instance.servers = new HashMap<>();
        instance.context = context;
        instance.preparePreferences();
        instance.prepareRegionModule();
        instance.loadPersistedServersIfAny();
        instance.fetchServers(context, true);
        instance.vibrateHandler = new VibrateHandler(context);
    }

    private VibrateHandler vibrateHandler;
    private PendingIntent pingIntent;
    private PendingIntent fetchServersIntent;
    private Map<String, PIAServer> servers;
    private PIAServerInfo info;
    private Context context;

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    public static void releaseInstance() {
        instance = null;
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    public PendingIntent getPingIntent() {
        return pingIntent;
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    public void setPingIntent(PendingIntent intent) {
        pingIntent = intent;
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    public PendingIntent getFetchServersIntent() {
        return fetchServersIntent;
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    public void setFetchServersIntent(PendingIntent intent) {
        fetchServersIntent = intent;
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    public static void setPrefs(Prefs preferences) {
        prefs = preferences;
    }

    private void preparePreferences() {
        if (prefs == null) {
            prefs = Prefs.with(context);
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    public static void setAlarmManager(AlarmManager manager) {
        alarmManager = manager;
    }

    private AlarmManager getAlarmManager() {
        if (alarmManager == null) {
            alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        }
        return alarmManager;
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    public static void setRegionModule(RegionsAPI module) {
        regionModule = module;
    }

    private void prepareRegionModule() {
        if (regionModule == null) {
            regionModule = new RegionsBuilder()
                    .setClientStateProvider(new ModuleClientStateProvider(context))
                    .build();
        }
    }

    private void offLoadResponse(ServerResponse response, boolean fromWeb){
        PIAServerHandler handler = getInstance(null);
        if(handler != null){
            boolean isValid = response.isValid();
            if(isValid) {
                handler.info = response.getInfo();
                handler.servers = response.getServers();
            }

            if(PiaPrefHandler.getServerTesting(context)) {
                PIAServer testServer = PiaPrefHandler.getTestServer(context);
                if(!TextUtils.isEmpty(testServer.getIso()))
                    removeTestingServer(testServer.getKey());
                addTestingServer(testServer);
            }
            if(BuildConfig.FLAVOR_pia.equals("qa")){
                loadExcessServers();
            }
            if(fromWeb && isValid){
                prefs.set(GEN4_LAST_SERVER_BODY, response.getBody());
            }
        }
    }

    public void loadPersistedServersIfAny() {
        String gen4LastBody = prefs.get(GEN4_LAST_SERVER_BODY, "");
        if (TextUtils.isEmpty(gen4LastBody)) {
            DLog.d(TAG, "No persisted servers for GEN4. Using default list.");
            try { gen4LastBody = readAssetsFile("regions.json").split("\n\n")[0]; }
            catch (IOException e) { e.printStackTrace(); }
        }

        RegionsResponse regionsResponse = RegionsUtils.INSTANCE.parse(gen4LastBody);
        Map<String, PIAServer> serverMap =
                ServerResponseHelper.Companion.adaptServers(regionsResponse);
        PIAServerInfo serverInfo =
                ServerResponseHelper.Companion.adaptServersInfo(regionsResponse);
        ServerResponse response =
                new ServerResponse(
                        serverMap,
                        serverInfo,
                        RegionsUtils.INSTANCE.stringify(regionsResponse)
                );
        offLoadResponse(response, false);
    }

    public void loadExcessServers(){
        try {
            String body = readAssetsFile("testing_servers.json");
            ServerResponse response = parseServers(body);
            for (String key : response.getServers().keySet()) {
                response.getServers().get(key).setTesting(true);
            }
            getServers().putAll(response.getServers());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String readAssetsFile(String filename) throws IOException {
        InputStream inputStream = context.getAssets().open(filename);
        BufferedReader r = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder stringBuilder = new StringBuilder();
        String receiveString;
        while ((receiveString = r.readLine()) != null) {
            stringBuilder.append(receiveString).append('\n');
        }
        inputStream.close();
        return stringBuilder.toString();
    }

    public void triggerLatenciesUpdate() {
        triggerLatenciesUpdate(null);
    }

    public void triggerLatenciesUpdate(@Nullable Function1<Error, Unit> callback) {
        if (PIAFactory.getInstance().getVPN(context).isVPNActive()) {
            DLog.e(TAG, "Error when updating latencies. Connected to the VPN");
            if (callback != null) {
                callback.invoke(new Error("Connected to the VPN"));
            }
            return;
        }

        if (SystemUtils.INSTANCE.isDozeModeEnabled(context)) {
            DLog.e(TAG, "Error when updating latencies. Doze mode enabled.");
            if (callback != null) {
                callback.invoke(new Error("Doze mode enabled"));
            }
            return;
        }

        regionModule.pingRequests(new Function2<List<RegionLowerLatencyInformation>, Error, Unit>() {
            @Override
            public Unit invoke(
                    List<RegionLowerLatencyInformation> response,
                    Error error
            ) {
                if (error != null) {
                    DLog.e(TAG, "Error when updating latencies " + error.getMessage());
                    if (callback != null) {
                        callback.invoke(error);
                    }
                    return null;
                }

                if (servers == null) {
                    DLog.e(TAG, "Error when updating latencies. Invalid List of servers.");
                    if (callback != null) {
                        callback.invoke(error);
                    }
                    return null;
                }

                HashMap<String, PIAServer> serversCopy = new HashMap(servers);
                for (RegionLowerLatencyInformation latencyInformation : response) {
                    PIAServer server = serversCopy.get(latencyInformation.getRegion());
                    if (server != null) {
                        server.setLatency(String.valueOf(latencyInformation.getLatency()));
                        serversCopy.put(latencyInformation.getRegion(), server);
                    }
                }
                servers = serversCopy;
                if (callback != null) {
                    callback.invoke(error);
                }
                return null;
            }
        });
    }

    public void triggerFetchServers() {
        triggerFetchServers(null);
    }

    public void triggerFetchServers(@Nullable Function1<Error, Unit> callback) {
        if (SystemUtils.INSTANCE.isDozeModeEnabled(context)) {
            DLog.e(TAG, "Error when updating list of servers. Doze mode enabled.");
            if (callback != null) {
                callback.invoke(new Error("Doze mode enabled"));
            }
            return;
        }

        Locale locale = ConfigurationCompat.getLocales(context.getResources().getConfiguration()).get(0);
        regionModule.fetchRegions(locale.getLanguage(), new Function2<RegionsResponse, Error, Unit>() {
            @Override
            public Unit invoke(@Nullable RegionsResponse regionsResponse, @Nullable Error error) {
                if (error != null) {
                    DLog.e(TAG, "Error fetching the list of servers " + error.getMessage());
                    if (callback != null) {
                        callback.invoke(error);
                    }
                    return null;
                }

                Map<String, PIAServer> serverMap =
                        ServerResponseHelper.Companion.adaptServers(regionsResponse);
                PIAServerInfo serverInfo =
                        ServerResponseHelper.Companion.adaptServersInfo(regionsResponse);

                // Keep latencies for the known ones
                for (Map.Entry<String, PIAServer> entry : servers.entrySet()) {
                    if (serverMap.containsKey(entry.getKey())) {
                        String knownLatency = entry.getValue().getLatency();
                        if (knownLatency == null) {
                            continue;
                        }
                        PIAServer serverDetails = serverMap.get(entry.getKey());
                        serverDetails.setLatency(knownLatency);
                        serverMap.put(entry.getKey(), serverDetails);
                    }
                }

                offLoadResponse(
                        new ServerResponse(
                                serverMap,
                                serverInfo,
                                RegionsUtils.INSTANCE.stringify(regionsResponse)
                        ),
                        true
                );
                if (callback != null) {
                    callback.invoke(error);
                }
                return null;
            }
        });
    }

    public void fetchServers(Context context, boolean force) {
        fetchServers(context, force, null);
    }

    public void fetchServers(Context context, boolean force, @Nullable Function1<Error, Unit> callback) {
        long lastGrab = prefs.get(LAST_SERVER_GRAB, 0L);
        long now = Calendar.getInstance().getTimeInMillis();
        if (force || (now - lastGrab > SERVER_TIME_DIFFERENCE)) {
            setServerListFetchState(ServerListUpdateState.STARTED);
            triggerFetchServers(new Function1<Error, Unit>() {
                @Override
                public Unit invoke(Error error) {
                    setServerListFetchState(ServerListUpdateState.FETCH_SERVERS_FINISHED);
                    triggerLatenciesUpdate(new Function1<Error, Unit>() {
                        @Override
                        public Unit invoke(Error error) {
                            setServerListFetchState(
                                    ServerListUpdateState.GEN4_PING_SERVERS_FINISHED
                            );
                            if (callback != null) {
                                callback.invoke(error);
                            }
                            return null;
                        }
                    });
                    return null;
                }
            });

            // We set an initial delay as the initial fetching above will handle the initial
            // update of the server's list and latencies.
            if (fetchServersIntent == null) {
                fetchServersIntent = PendingIntent.getBroadcast(
                        context, 0, new Intent(context, FetchServersReceiver.class), PendingIntent.FLAG_CANCEL_CURRENT
                );
                getAlarmManager().setRepeating(
                        AlarmManager.RTC,
                        AlarmManager.INTERVAL_DAY,
                        AlarmManager.INTERVAL_DAY,
                        fetchServersIntent
                );
            }

            if (pingIntent == null) {
                pingIntent = PendingIntent.getBroadcast(
                        context, 0, new Intent(context, PingReceiver.class), PendingIntent.FLAG_CANCEL_CURRENT
                );
                getAlarmManager().setRepeating(
                        AlarmManager.RTC,
                        AlarmManager.INTERVAL_HOUR,
                        AlarmManager.INTERVAL_HOUR,
                        pingIntent
                );
            }
            prefs.set(LAST_SERVER_GRAB, Calendar.getInstance().getTimeInMillis());
        }
    }

    public Vector<PIAServer> getAutoRegionServers() {
        Vector<PIAServer> as = new Vector<>();
        if (info == null) {
            return as;
        }

        for (String autoRegion : info.getAutoRegions()) {
            PIAServer ps = servers.get(autoRegion);
            if (ps == null)
                DLog.d("PIA", "No server entry for autoregion: " + autoRegion);
            else
                as.add(ps);
        }
        return as;
    }

    public Vector<PIAServer> getServers(Context context, ServerSortingType... types){
        Vector<PIAServer> servers = new Vector<>(getServers().values());
        if (types != null) {
            for(ServerSortingType type : types){
                switch(type) {
                    case NAME:
                        Collections.sort(servers, new ServerNameComperator());
                        break;
                    case LATENCY:
                        Collections.sort(servers, new PingComperator());
                        break;
                    case FAVORITES:
                        Collections.sort(servers, new FavoriteComperator((HashSet<String>) PiaPrefHandler.getFavorites(context)));
                        break;
                }
            }
        }
        return servers;
    }

    public boolean isSelectedRegionAuto(Context context) {
        if (servers == null) {
            return true;
        }

        String region = prefs.get(SELECTEDREGION, "");
        return !servers.containsKey(region);
    }

    public PIAServer getSelectedRegion(Context context, boolean returnNullonAuto) {
        // Server region
        String region = prefs.get(SELECTEDREGION, "");

        if (servers.containsKey(region)) {
            return servers.get(region);
        } else if (returnNullonAuto) {
            return null;
        } else {
            Vector<PIAServer> autoRegionServers = getAutoRegionServers();
            PIAServer lowestKnownLatencyServer = null;

            for (PIAServer server : autoRegionServers) {
                if (lowestKnownLatencyServer == null) {
                    lowestKnownLatencyServer = server;
                    continue;
                }

                String lowestKnownServerLatency = lowestKnownLatencyServer.getLatency();
                if (lowestKnownServerLatency == null) {
                    continue;
                }

                String serverLatency = server.getLatency();
                if (serverLatency == null) {
                    continue;
                }

                if (!server.isGeo()) {
                    Long latency = Long.valueOf(serverLatency);
                    Long lowestKnownLatency = Long.valueOf(lowestKnownServerLatency);
                    if (latency > 0 && latency < lowestKnownLatency) {
                        lowestKnownLatencyServer = server;
                    }
                }
            }

            if (lowestKnownLatencyServer.getLatency() == null) {
                lowestKnownLatencyServer = getRandomServer();
            }

            return lowestKnownLatencyServer;

        }
    }

    private PIAServer getRandomServer() {
        if (randomServer == null) {
            Vector<PIAServer> autoRegionServers = getAutoRegionServers();
            randomServer = getRandom(autoRegionServers);
        }

        return randomServer;
    }

    /**
     * Handy method to turn body from web or saved asset into a Server Response
     *
     * @param body
     * @return
     */
    public static ServerResponse parseServers(String body){
        PIAServerInfo info = null;
        Map<String, PIAServer> servers = null;
        try {
            String[] parts = body.split("\n\n", 2);
            String data = parts[0];

            JSONObject json = new JSONObject(data);
            Iterator<String> keyIter = json.keys();
            while (keyIter.hasNext()){
                String key = keyIter.next();
                if(!key.equals("info")) {
                    JSONObject serverJson = json.getJSONObject(key);
                    PIAServer server = new PIAServer();
                    server.parse(serverJson, key);
                    if(servers == null)
                        servers = new HashMap<>();
                    servers.put(key, server);
                } else {
                    if(info == null)
                        info = new PIAServerInfo();
                    info.parse(json.getJSONObject(key));
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return new ServerResponse(servers, info);
    }

    public Map<String, PIAServer> getServers() {
        if(servers == null)
            servers = new HashMap<>();
        return servers;
    }

    public PIAServerInfo getInfo() {
        return info;
    }

    public void addTestingServer(PIAServer testServer) {
        if(servers != null)
            servers.put(testServer.getKey(), testServer);
    }

    public void removeTestingServer(String testServerKey) {
        if(servers != null)
            servers.remove(testServerKey);
    }

    public void saveSelectedServer(Context context, String region) {
        prefs.set(SELECTEDREGION, region);
        PiaPrefHandler.addQuickConnectItem(context, region);
    }

    static public class ServerNameComperator implements Comparator<PIAServer> {
        @Override
        public int compare(PIAServer lhs, PIAServer rhs) {
            if(!lhs.isTesting() && !rhs.isTesting())
                return lhs.getName().compareTo(rhs.getName());
            else if(rhs.isTesting() && !lhs.isTesting())
                return 1;
            else if(lhs.isTesting() && !rhs.isTesting())
                return -1;
            else
                return 0;
        }
    }

    static public class PingComperator implements Comparator<PIAServer> {

        @Override
        public int compare(PIAServer lhs, PIAServer rhs) {
            if (lhs == null || rhs == null) {
                return 0;
            }

            if (!lhs.isTesting() && !rhs.isTesting()) {
                Long lhsPing = 999L;
                Long rhsPing = 999L;
                String lhsLatency = lhs.getLatency();
                if (lhsLatency != null && !lhsLatency.isEmpty()) {
                    lhsPing = Long.valueOf(lhsLatency);
                }
                String rhsLatency = rhs.getLatency();
                if (rhsLatency != null && !rhsLatency.isEmpty()) {
                    rhsPing = Long.valueOf(rhsLatency);
                }
                return lhsPing.compareTo(rhsPing);
            } else if (rhs.isTesting() && !lhs.isTesting())
                return 1;
            else if (lhs.isTesting() && !rhs.isTesting())
                return -1;
            else
                return 0;
        }
    }

    static public class FavoriteComperator implements Comparator<PIAServer> {

        HashSet<String> favorites;

        public FavoriteComperator(HashSet<String> favorites) {
            this.favorites = favorites;
        }

        @Override
        public int compare(PIAServer o1, PIAServer o2) {
            String name1 = o1.getName();
            String name2 = o2.getName();
            boolean server1 = favorites.contains(name1);
            boolean server2 = favorites.contains(name2);
            if(server1 && !server2){
                return -1;
            } else if(!server1 && server2){
                return 1;
            } else {
                return 0;
            }
        }
    }

    public int getFlagResource(PIAServer server){
        String resName = server.getIso();
        if (server.isTesting()) {
            resName = resName.replace("Test Server", "").trim();
        }
        resName = String.format(Locale.US, "flag_%s", resName.replace(" ", "_").replace(",", "").toLowerCase(Locale.US));
        int flagResource = context.getResources().getIdentifier(resName, "drawable", context.getPackageName());
        if(flagResource == 0){
            flagResource = R.drawable.flag_world;
        }
        return flagResource;
    }

    public int getFlagResource(String serverName){
        PIAServer server = new PIAServer();
        server.setName(serverName);
        server.setIso(serverName.toUpperCase());
        return getFlagResource(server);
    }

    public PIAServer getRandom(Vector<PIAServer> servers) {
        if (servers != null && servers.size() > 0) {
            int rnd = new Random().nextInt(servers.size());
            return servers.get(rnd);
        }

        return null;
    }

    public enum ServerSortingType {
        NAME,
        LATENCY,
        FAVORITES
    }
}