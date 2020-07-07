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


import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.Nullable;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.privateinternetaccess.android.BuildConfig;
import com.privateinternetaccess.android.R;
import com.privateinternetaccess.android.model.events.SeverListUpdateEvent;
import com.privateinternetaccess.android.model.events.SeverListUpdateEvent.ServerListUpdateState;
import com.privateinternetaccess.android.pia.PIAFactory;
import com.privateinternetaccess.android.pia.api.ServerAPI;
import com.privateinternetaccess.android.pia.tasks.FetchServersTask;
import com.privateinternetaccess.android.pia.utils.DLog;
import com.privateinternetaccess.android.pia.utils.Prefs;
import com.privateinternetaccess.android.pia.utils.ServerResponseHelper;
import com.privateinternetaccess.android.pia.workers.FetchServersWorker;
import com.privateinternetaccess.android.pia.workers.PingWorker;
import com.privateinternetaccess.android.utils.ServerUtils;
import com.privateinternetaccess.android.utils.SystemUtils;
import com.privateinternetaccess.core.model.PIAServer;
import com.privateinternetaccess.core.model.PIAServerInfo;
import com.privateinternetaccess.core.model.ServerResponse;
import com.privateinternetaccess.core.utils.IPIACallback;
import com.privateinternetaccess.regions.RegionLowerLatencyInformation;
import com.privateinternetaccess.regions.RegionsAPI;
import com.privateinternetaccess.regions.RegionsBuilder;
import com.privateinternetaccess.regions.RegionsProtocol;
import com.privateinternetaccess.regions.internals.RegionsUtils;
import com.privateinternetaccess.regions.model.RegionsResponse;

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
import java.util.Vector;
import java.util.concurrent.TimeUnit;

import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;

/**
 * Handler class for helping with the servers and pings to those servers.
 *
 */
public class PIAServerHandler {

    public static final String LAST_SERVER_BODY = "LAST_SERVER_BODY";
    public static final String GEN4_LAST_SERVER_BODY = "GEN4_LAST_SERVER_BODY";
    private static final String TAG = "PIAServerHandler";
    public static final String LAST_SERVER_GRAB = "LAST_SERVER_GRAB";
    public static final String SELECTEDREGION = "selectedregion";
    public static final long SERVER_TIME_DIFFERENCE = 600000L; //10m

    private static PIAServerHandler instance;
    private static Map<String, Integer> serverImageMap;

    private static Prefs prefs;

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
        instance.context = context;
        instance.preparePreferences();
        instance.regionModule = new RegionsBuilder().
                setPingRequestDependency(new Gen4PingHandler()).
                setMessageVerificatorDependency(new Gen4MessageVerificationHandler()).
                build();
        instance.loadEmbeddedServers(context);
        instance.fetchServers(context, true);
        setupServerImageMap();
    }

    private static void setupServerImageMap() {
        serverImageMap = new HashMap<>();
        serverImageMap.put("US", R.drawable.flag_usa);
        serverImageMap.put("CA", R.drawable.flag_canada);
        serverImageMap.put("AU", R.drawable.flag_australia);
        serverImageMap.put("FR", R.drawable.flag_france);
        serverImageMap.put("DE", R.drawable.flag_germany);
        serverImageMap.put("HK", R.drawable.flag_hongkong);
        serverImageMap.put("IL", R.drawable.flag_israel);
        serverImageMap.put("JP", R.drawable.flag_japan);
        serverImageMap.put("NL", R.drawable.flag_netherlands);
        serverImageMap.put("RO", R.drawable.flag_romania);
        serverImageMap.put("MX", R.drawable.flag_mexico);
        serverImageMap.put("SE", R.drawable.flag_sweden);
        serverImageMap.put("CH", R.drawable.flag_switzerland);
        serverImageMap.put("GB", R.drawable.flag_uk);
        serverImageMap.put("NZ", R.drawable.flag_new_zealand);
        serverImageMap.put("NO", R.drawable.flag_norway);
        serverImageMap.put("DK", R.drawable.flag_denmark);
        serverImageMap.put("FI", R.drawable.flag_finland);
        serverImageMap.put("BE", R.drawable.flag_belgium);
        serverImageMap.put("AT", R.drawable.flag_austria);
        serverImageMap.put("CZ", R.drawable.flag_czech_republic);
        serverImageMap.put("IE", R.drawable.flag_ireland);
        serverImageMap.put("IT", R.drawable.flag_italy);
        serverImageMap.put("ES", R.drawable.flag_spain);
        serverImageMap.put("TR", R.drawable.flag_turkey);
        serverImageMap.put("SG", R.drawable.flag_singapore);
        serverImageMap.put("BR", R.drawable.flag_brazil);
        serverImageMap.put("IN", R.drawable.flag_india);
    }

    private RegionsAPI regionModule;
    private PeriodicWorkRequest pingWorker;
    private PeriodicWorkRequest serversWorker;
    private Map<String, PIAServer> servers;
    private PIAServerInfo info;
    private Context context;

    private void preparePreferences() {
        if (prefs == null) {
            prefs = Prefs.with(context);
        }
    }

    private void offLoadResponse(ServerResponse response, boolean fromWeb, boolean gen4Enabled){
        if (Prefs.with(context).getBoolean(PiaPrefHandler.GEN4_ACTIVE) != gen4Enabled) {
            DLog.e(TAG, "The next generation state changed in between requests. Return.");
            return;
        }

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
                loadExcessServers(context);
            }
            if(fromWeb && isValid){
                if (gen4Enabled) {
                    Prefs.with(context).set(GEN4_LAST_SERVER_BODY, response.getBody());
                } else {
                    Prefs.with(context).set(LAST_SERVER_BODY , response.getBody());
                }
                PingHandler.getInstance(context).fetchPings(PingHandler.PING_TIME_INSTANT);
            } else if (fromWeb && !isValid && handler.servers != null && handler.info != null) {
                PingHandler.getInstance(context).fetchPings(PingHandler.PING_TIME_INSTANT);
            }
        }
    }

    public void loadEmbeddedServers(Context context){
        try {
            boolean gen4Enabled = Prefs.with(context).getBoolean(PiaPrefHandler.GEN4_ACTIVE);
            String lastBody = Prefs.with(context).get(LAST_SERVER_BODY, "");

            if (Prefs.with(context).getBoolean(PiaPrefHandler.GEN4_ACTIVE)) {
                lastBody = Prefs.with(context).get(GEN4_LAST_SERVER_BODY, "");
                if (TextUtils.isEmpty(lastBody)) {
                    lastBody = RegionsUtils.GEN4_DEFAULT_RESPONSE;
                }
                RegionsResponse regionsResponse = RegionsUtils.INSTANCE.parse(lastBody);
                Map<String, PIAServer> serverMap =
                        ServerResponseHelper.Companion.adaptServers(regionsResponse);
                PIAServerInfo serverInfo =
                        ServerResponseHelper.Companion.adaptServersInfo(regionsResponse);
                offLoadResponse(
                        new ServerResponse(
                                serverMap,
                                serverInfo,
                                RegionsUtils.INSTANCE.stringify(regionsResponse)
                        ),
                        false,
                        gen4Enabled
                );
                return;
            }

            if(TextUtils.isEmpty(lastBody)) {
                InputStream serverjson = context.getAssets().open("servers.json");
                BufferedReader r = new BufferedReader(new InputStreamReader(serverjson));
                StringBuilder total = new StringBuilder();
                String line;
                while ((line = r.readLine()) != null) {
                    total.append(line).append('\n');
                }
                String body = total.toString();
                ServerResponse response = parseServers(body);
                offLoadResponse(response, false, gen4Enabled);
            } else {
                ServerResponse response = parseServers(lastBody);
                offLoadResponse(response, false, gen4Enabled);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadExcessServers(Context context){
        try {
            InputStream serverjson = context.getAssets().open("testing_servers.json");
            BufferedReader r = new BufferedReader(new InputStreamReader(serverjson));
            StringBuilder total = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) {
                total.append(line).append('\n');
            }
            String body = total.toString();
            ServerResponse response = parseServers(body);
            for(String key : response.getServers().keySet()){
                response.getServers().get(key).setTesting(true);
            }
            getServers().putAll(response.getServers());
        } catch (IOException e) {
            e.printStackTrace();
        }
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

        if (!Prefs.with(context).getBoolean(PiaPrefHandler.GEN4_ACTIVE)) {
            DLog.e(TAG, "Error when updating latencies. GEN4 is disabled.");
            DLog.e(TAG, "PingWorker state " + pingWorker);
            if (callback != null) {
                callback.invoke(new Error("GEN4 is disabled"));
            }
            return;
        }

        PIAServer.Protocol protocol = ServerUtils.getUserSelectedProtocol(context);
        RegionsProtocol regionsProtocol = null;
        switch (protocol) {
            case WIREGUARD:
                regionsProtocol = RegionsProtocol.WIREGUARD;
                break;
            case OPENVPN_TCP:
                regionsProtocol = RegionsProtocol.OPENVPN_TCP;
                break;
            case OPENVPN_UDP:
                regionsProtocol = RegionsProtocol.OPENVPN_UDP;
                break;
        }
        regionModule.pingRequests(regionsProtocol, new Function2<List<RegionLowerLatencyInformation>, Error, Unit>() {
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

                HashMap<String, PIAServer> serversCopy = new HashMap(servers);
                for (RegionLowerLatencyInformation latencyInformation : response) {
                    PIAServer server = serversCopy.get(latencyInformation.getRegion());
                    if (server != null) {
                        HashMap<PIAServer.Protocol, String> regionLatencies =
                                new HashMap(server.getLatencies());
                        regionLatencies.put(
                                protocol,
                                String.valueOf(latencyInformation.getLatency())
                        );
                        server.setLatencies(regionLatencies);
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

        boolean gen4Enabled = Prefs.with(context).getBoolean(PiaPrefHandler.GEN4_ACTIVE);
        if (!gen4Enabled) {
            DLog.e(TAG, "Error when updating list of servers. GEN4 is disabled.");
            DLog.e(TAG, "ServerWorker state " + serversWorker);
            if (callback != null) {
                callback.invoke(new Error("GEN4 is disabled"));
            }
            return;
        }

        regionModule.fetch(new Function2<RegionsResponse, Error, Unit>() {
            @Override
            public Unit invoke(@Nullable RegionsResponse regionsResponse, @Nullable Error error) {
                if (error != null) {
                    DLog.e(TAG, "Error fetching the list of servers" + error.getMessage());
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
                        Map<PIAServer.Protocol, String> knownLatencies =
                                entry.getValue().getLatencies();
                        if (knownLatencies == null) {
                            continue;
                        }
                        PIAServer serverDetails = serverMap.get(entry.getKey());
                        serverDetails.setLatencies(knownLatencies);
                        serverMap.put(entry.getKey(), serverDetails);
                    }
                }

                offLoadResponse(
                        new ServerResponse(
                                serverMap,
                                serverInfo,
                                RegionsUtils.INSTANCE.stringify(regionsResponse)
                        ),
                        true,
                        gen4Enabled
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
        Prefs prefs = Prefs.with(context);
        long lastGrab = prefs.get(LAST_SERVER_GRAB, 0L);
        long now = Calendar.getInstance().getTimeInMillis();
        if (force || (now - lastGrab > SERVER_TIME_DIFFERENCE || ServerAPI.forceUpdate(context))) {
            setServerListFetchState(ServerListUpdateState.STARTED);
            boolean gen4Enabled = Prefs.with(context).getBoolean(PiaPrefHandler.GEN4_ACTIVE);
            if (gen4Enabled) {
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
                if (serversWorker == null) {
                    serversWorker = new PeriodicWorkRequest.Builder(
                            FetchServersWorker.class,
                            24,
                            TimeUnit.HOURS
                    ).setInitialDelay(24, TimeUnit.HOURS).build();
                    WorkManager.getInstance(context).enqueue(serversWorker);
                }

                if (pingWorker == null) {
                    pingWorker = new PeriodicWorkRequest.Builder(
                            PingWorker.class,
                            1,
                            TimeUnit.HOURS
                    ).setInitialDelay(1, TimeUnit.HOURS).build();
                    WorkManager.getInstance(context).enqueue(pingWorker);
                }
            } else {
                FetchServersTask task = new FetchServersTask(context, new IPIACallback<ServerResponse>() {
                    @Override
                    public void apiReturn(ServerResponse response) {
                        offLoadResponse(response, true, gen4Enabled);
                        setServerListFetchState(ServerListUpdateState.FETCH_SERVERS_FINISHED);
                        if (callback != null) {
                            callback.invoke(null);
                        }
                    }
                });
                task.execute("");
            }
            Prefs.with(context).set(LAST_SERVER_GRAB, Calendar.getInstance().getTimeInMillis());
        }

        // Regardless of the time condition above. If GEN4 is disabled. Cancel its workers.
        if (!Prefs.with(context).getBoolean(PiaPrefHandler.GEN4_ACTIVE)) {
            if (pingWorker != null) {
                WorkManager.getInstance(context).cancelWorkById(pingWorker.getId());
                pingWorker = null;
            }
            if (serversWorker != null) {
                WorkManager.getInstance(context).cancelWorkById(serversWorker.getId());
                serversWorker = null;
            }
        }
    }

    public Vector<PIAServer> getAutoRegionServers() {
        Vector<PIAServer> as = new Vector<>();
        if (info != null) {
            for (String autoregion : info.getAutoRegions()) {
                PIAServer ps = servers.get(autoregion);
                if (ps == null)
                    DLog.d("PIA", "No server entry for autoregion: " + autoregion);
                else
                    as.add(ps);
            }
        } else { // rare case after the validation
            Prefs.with(context).set(LAST_SERVER_BODY, "");
            Prefs.with(context).set(GEN4_LAST_SERVER_BODY, "");
            loadEmbeddedServers(context);
            if (info != null) {
                as = getAutoRegionServers();
            }
        }
        return as;
    }

    public Vector<PIAServer> getServers(Context context, ServerSortingType... types){
        Vector<PIAServer> servers = new Vector<>(getServers().values());

        if(types != null){
            for(ServerSortingType type : types){
                switch(type) {
                    case NAME:
                        Collections.sort(servers, new ServerNameComperator());
                        break;
                    case LATENCY:
                        Collections.sort(servers, new PingComperator(PingHandler.getInstance(context).getPings()));
                        break;
                    case FAVORITES:
                        Collections.sort(servers, new FavoriteComperator((HashSet<String>) PiaPrefHandler.getFavorites(context)));
                        break;
                }
            }
        }
        return servers;
    }

    public boolean isSelectedRegionAuto(Context a) {
        // Server region
        String region = Prefs.with(a).get(SELECTEDREGION, "");
        return !servers.containsKey(region);
    }

    public PIAServer getSelectedRegion(Context c, boolean returnNullonAuto) {
        // Server region
        String region = Prefs.with(c).get(SELECTEDREGION, "");

        if (servers.containsKey(region)) {
            return servers.get(region);
        } else if (returnNullonAuto) {
            return null;
        } else {
            if (Prefs.with(context).getBoolean(PiaPrefHandler.GEN4_ACTIVE)) {
                PIAServer.Protocol protocol = ServerUtils.getUserSelectedProtocol(context);
                Collection<PIAServer> autoRegionServers = getAutoRegionServers();
                PIAServer lowestKnownLatencyServer = null;
                for (PIAServer server : autoRegionServers) {
                    if (lowestKnownLatencyServer == null) {
                        lowestKnownLatencyServer = server;
                        continue;
                    }

                    Map<PIAServer.Protocol, String> lowestKnownServerLatencies =
                            lowestKnownLatencyServer.getLatencies();
                    if (lowestKnownServerLatencies == null) {
                        continue;
                    }

                    Map<PIAServer.Protocol, String> serverLatencies = server.getLatencies();
                    if (serverLatencies == null) {
                        continue;
                    }

                    String serverLatency = serverLatencies.get(protocol);
                    String lowestKnownServerLatency = lowestKnownServerLatencies.get(protocol);
                    if (serverLatency != null && lowestKnownServerLatency != null && !server.isGeo()) {
                        Long latency = Long.valueOf(serverLatency);
                        Long lowestKnownLatency = Long.valueOf(lowestKnownServerLatency);
                        if (latency > 0 && latency < lowestKnownLatency) {
                            lowestKnownLatencyServer = server;
                        }
                    }
                }
                return lowestKnownLatencyServer;
            }
            Map<String, Long> pingMap = PingHandler.getInstance(c).getPings();
            return getSortedServer(getAutoRegionServers(), new PingComperator(pingMap))[0];
        }
    }

    private PIAServer[] getSortedServer(Collection<PIAServer> tosort, Comparator<PIAServer> comperator) {
        PIAServer[] servers = new PIAServer[tosort.size()];
        int i = 0;
        for (PIAServer ps : tosort) {
            if (!ps.isGeo()) {
                servers[i++] = ps;
            }
        }

        Arrays.sort(servers, comperator);
        DLog.d(TAG, Arrays.toString(servers));

        return servers;
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
        Prefs.with(context).set(SELECTEDREGION, region);
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

        Map<String, Long> pings;

        public PingComperator(Map<String, Long> pings) {
            this.pings = pings;
        }

        @Override
        public int compare(PIAServer lhs, PIAServer rhs) {
            if (prefs.getBoolean(PiaPrefHandler.GEN4_ACTIVE)) {
                return compareGEN4(lhs, rhs);
            } else {
                return compareLegacy(lhs, rhs);
            }
        }

        private int compareLegacy(PIAServer lhs, PIAServer rhs) {
            if (lhs == null || rhs == null) {
                return 0;
            }

            if (!lhs.isTesting() && !rhs.isTesting()) {
                Long lhsPing = null;
                Long rhsPing = null;
                if (pings != null) {
                    String lhsKey = lhs.getKey();
                    String rhsKey = rhs.getKey();
                    if (lhsKey != null) {
                        lhsPing = pings.get(lhsKey);
                    }
                    if (rhsKey != null) {
                        rhsPing = pings.get(rhsKey);
                    }
                }
                if (lhsPing == null)
                    lhsPing = 999L;
                if (rhsPing == null)
                    rhsPing = 999L;
                return lhsPing.compareTo(rhsPing);
            } else if (rhs.isTesting() && !lhs.isTesting())
                return 1;
            else if (lhs.isTesting() && !rhs.isTesting())
                return -1;
            else
                return 0;
        }

        private int compareGEN4(PIAServer lhs, PIAServer rhs) {
            if (lhs == null || rhs == null) {
                return 0;
            }

            if (!lhs.isTesting() && !rhs.isTesting()) {
                Long lhsPing = 999L;
                Long rhsPing = 999L;
                String lhsPingString = ServerUtils.getLatencyForActiveSetting(
                        instance.context,
                        lhs.getLatencies()
                );
                if (lhsPingString != null && !lhsPingString.isEmpty()) {
                    lhsPing = Long.valueOf(lhsPingString);
                }
                String rhsPingString = ServerUtils.getLatencyForActiveSetting(
                        instance.context,
                        rhs.getLatencies()
                );
                if (rhsPingString != null && !rhsPingString.isEmpty()) {
                    rhsPing = Long.valueOf(rhsPingString);
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
        Integer flagResource = serverImageMap.get(server.getIso());
        if(flagResource == null){
            String resName = server.getName();
            if(server.isTesting())
                resName = resName.replace("Test Server", "").trim();
            resName = String.format(Locale.US, "flag_%s", resName.replace(" ", "_").replace(",", "").toLowerCase(Locale.US));
            flagResource = context.getResources().getIdentifier(resName, "drawable", context.getPackageName());
            if(flagResource == 0){
                flagResource = R.drawable.flag_world;
            }
        }
        return flagResource;
    }

    public int getFlagResource(String serverName){
        PIAServer server = new PIAServer();
        server.setName(serverName);
        server.setIso(serverName.toUpperCase());
        return getFlagResource(server);
    }

    public enum ServerSortingType {
        NAME,
        LATENCY,
        FAVORITES
    }
}