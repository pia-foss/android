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

import com.privateinternetaccess.android.BuildConfig;
import com.privateinternetaccess.android.R;
import com.privateinternetaccess.android.pia.IPIACallback;
import com.privateinternetaccess.android.pia.api.ServerAPI;
import com.privateinternetaccess.android.pia.model.PIAServer;
import com.privateinternetaccess.android.pia.model.PIAServerInfo;
import com.privateinternetaccess.android.pia.model.response.ServerResponse;
import com.privateinternetaccess.android.pia.tasks.FetchServersTask;
import com.privateinternetaccess.android.pia.utils.DLog;
import com.privateinternetaccess.android.pia.utils.Prefs;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
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
import java.util.Locale;
import java.util.Map;
import java.util.Vector;

/**
 * Handler class for helping with the servers and pings to those servers.
 *
 */
public class PIAServerHandler {

    public static final String LAST_SERVER_BODY = "LAST_SERVER_BODY";
    private static final String TAG = "PIAServerHandler";
    public static final String LAST_SERVER_GRAB = "LAST_SERVER_GRAB";
    public static final String SELECTEDREGION = "selectedregion";
    public static final long SERVER_TIME_DIFFERENCE = 600000L; //10m

    private static PIAServerHandler instance;
    private static IPIACallback<ServerResponse> CALLBACK;
    private static Map<String, Integer> serverImageMap;

    public static PIAServerHandler getInstance(Context context){
        if(instance == null){
            startup(context);
        }
        return instance;
    }

    public static void startup(Context context) {
        instance = new PIAServerHandler();
        EventBus.getDefault().register(instance);
        instance.context = context;
        instance.loadEmbeddedServers(context);
        instance.fetchServers(context);
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

    private Map<String, PIAServer> servers;
    private PIAServerInfo info;
    private Context context;

    @Subscribe
    public void onServerReceive(ServerResponse response){
        offLoadResponse(response, true);
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
                loadExcessServers(context);
            }
            if(fromWeb && isValid){
                Prefs.with(context).set(LAST_SERVER_BODY, response.getBody());
                PingHandler.getInstance(context).fetchPings(PingHandler.PING_TIME_INSTANT);
            } else if (fromWeb && !isValid && handler.servers != null && handler.info != null) {
                PingHandler.getInstance(context).fetchPings(PingHandler.PING_TIME_INSTANT);
            }
        }
    }

    public void loadEmbeddedServers(Context context){
        try {
            String lastBody = Prefs.with(context).get(LAST_SERVER_BODY, "");
            DLog.d(TAG, "");
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
                offLoadResponse(response, false);
            } else {
                ServerResponse response = parseServers(lastBody);
                offLoadResponse(response, false);
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

    public void fetchServers(Context context){
        Prefs prefs = Prefs.with(context);
        long lastGrab = prefs.get(LAST_SERVER_GRAB, 0L);
        long now = Calendar.getInstance().getTimeInMillis();
        long minDiff = SERVER_TIME_DIFFERENCE;
        if(now - lastGrab > minDiff || ServerAPI.forceUpdate(context)) {
            FetchServersTask task = new FetchServersTask(context, CALLBACK);
            task.execute("");
            Prefs.with(context).set(LAST_SERVER_GRAB, Calendar.getInstance().getTimeInMillis());
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
                    case PING:
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

        Map<String, Long> pingMap = PingHandler.getInstance(c).getPings();

        if (servers.containsKey(region))
            return servers.get(region);
        else if (returnNullonAuto)
            return null;
        else
            return getSortedServer(getAutoRegionServers(), new PingComperator(pingMap))[0];
    }


    public PIAServer[] getSortedServer() {
        return getSortedServer(servers.values(), new ServerNameComperator());
    }

    private PIAServer[] getSortedServer(Collection<PIAServer> tosort, Comparator<PIAServer> comperator) {
        PIAServer[] servers = new PIAServer[tosort.size()];
        int i = 0;
        for (PIAServer ps : tosort)
            servers[i++] = ps;

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

    public static void setCallback(IPIACallback<ServerResponse> CALLBACK) {
        PIAServerHandler.CALLBACK = CALLBACK;
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
            if(!lhs.isTesting() && !rhs.isTesting()) {
                Long lhsPing = pings.get(lhs.getKey());
                Long rhsPing = pings.get(rhs.getKey());
                if(lhsPing == null)
                    lhsPing = 999L;
                if(rhsPing == null)
                    rhsPing = 999L;
                return lhsPing.compareTo(rhsPing);
            } else if(rhs.isTesting() && !lhs.isTesting())
                return 1;
            else if(lhs.isTesting() && !rhs.isTesting())
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
        PING,
        FAVORITES
    }
}