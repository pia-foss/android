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

package com.privateinternetaccess.android.wireguard.backend;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArraySet;
import androidx.core.app.NotificationCompat;
import android.util.Log;

import com.privateinternetaccess.android.BuildConfig;
import com.privateinternetaccess.android.PIAApplication;
import com.privateinternetaccess.android.PIAOpenVPNTunnelLibrary;
import com.privateinternetaccess.android.R;
import com.privateinternetaccess.android.model.events.VPNTrafficDataPointEvent;
import com.privateinternetaccess.android.pia.api.PiaApi;
import com.privateinternetaccess.android.pia.handlers.PIAServerHandler;
import com.privateinternetaccess.android.pia.handlers.PiaPrefHandler;
import com.privateinternetaccess.android.pia.model.events.VpnStateEvent;
import com.privateinternetaccess.android.pia.tasks.FetchIPTask;
import com.privateinternetaccess.android.pia.tasks.HitMaceTask;
import com.privateinternetaccess.android.pia.utils.DLog;
import com.privateinternetaccess.android.pia.utils.Prefs;
import com.privateinternetaccess.android.ui.connection.MainActivity;
import com.privateinternetaccess.android.utils.SnoozeUtils;
import com.privateinternetaccess.android.wireguard.config.Interface;
import com.privateinternetaccess.android.wireguard.crypto.KeyPair;
import com.privateinternetaccess.android.wireguard.model.Tunnel;
import com.privateinternetaccess.android.wireguard.model.Tunnel.State;
import com.privateinternetaccess.android.wireguard.model.Tunnel.Statistics;
import com.privateinternetaccess.android.wireguard.util.SharedLibraryLoader;
import com.privateinternetaccess.android.wireguard.config.Config;
import com.privateinternetaccess.android.wireguard.config.InetNetwork;
import com.privateinternetaccess.android.wireguard.config.Peer;
import com.privateinternetaccess.android.wireguard.crypto.Key;
import com.privateinternetaccess.android.wireguard.crypto.KeyFormatException;
import com.privateinternetaccess.core.model.PIAServer;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import de.blinkt.openvpn.core.ConnectionStatus;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static com.privateinternetaccess.android.pia.api.MaceApi.GEN4_MACE_ENABLED_DNS;
import static de.blinkt.openvpn.core.OpenVPNService.NOTIFICATION_CHANNEL_NEWSTATUS_ID;

public final class GoBackend implements Backend {
    private String IPV4_PUBLIC_NETWORKS =
            "0.0.0.0/5, 8.0.0.0/7, 11.0.0.0/8, 12.0.0.0/6, 16.0.0.0/4, 32.0.0.0/3, " +
                    "64.0.0.0/2, 128.0.0.0/3, 160.0.0.0/5, 168.0.0.0/6, 172.0.0.0/12, " +
                    "172.32.0.0/11, 172.64.0.0/10, 172.128.0.0/9, 173.0.0.0/8, 174.0.0.0/7, " +
                    "176.0.0.0/4, 192.0.0.0/9, 192.128.0.0/11, 192.160.0.0/13, 192.169.0.0/16, " +
                    "192.170.0.0/15, 192.172.0.0/14, 192.176.0.0/12, 192.192.0.0/10, " +
                    "193.0.0.0/8, 194.0.0.0/7, 196.0.0.0/6, 200.0.0.0/5, 208.0.0.0/4";

    private static final String TAG = "WireGuard/" + GoBackend.class.getSimpleName();
    private static GhettoCompletableFuture<VpnService> vpnService = new GhettoCompletableFuture<>();
    @Nullable private static AlwaysOnCallback alwaysOnCallback;

    private final Context context;
    @Nullable private Tunnel currentTunnel;
    private int currentTunnelHandle = -1;
    public boolean isConnecting = false;

    public GoBackend(final Context context) {
        SharedLibraryLoader.loadSharedLibrary(context, "wg-go");
        this.context = context;
    }

    private static native String wgGetConfig(int handle);

    private static native int wgGetSocketV4(int handle);

    private static native int wgGetSocketV6(int handle);

    private static native void wgTurnOff(int handle);

    private static native int wgTurnOn(String ifName, int tunFd, String settings);

    private static native String wgVersion();

    static public State lastState = State.DOWN;

    public static void setAlwaysOnCallback(final AlwaysOnCallback cb) {
        alwaysOnCallback = cb;
    }

    @Override
    public Set<String> getRunningTunnelNames() {
        if (currentTunnel != null) {
            final Set<String> runningTunnels = new ArraySet<>();
            runningTunnels.add(currentTunnel.getName());
            return runningTunnels;
        }
        return Collections.emptySet();
    }

    @Override
    public State getState(final Tunnel tunnel) {
        return currentTunnel == tunnel ? State.UP : State.DOWN;
    }

    @Override
    public Statistics getStatistics(final Tunnel tunnel) {
        final Statistics stats = new Statistics();
        if (tunnel != currentTunnel) {
            return stats;
        }
        final String config = wgGetConfig(currentTunnelHandle);

        if (config == null) {
            return stats;
        }

        Key key = null;
        long rx = 0, tx = 0;
        for (final String line : config.split("\\n")) {
            if (line.startsWith("public_key=")) {
                if (key != null)
                    stats.add(key, rx, tx);
                rx = 0;
                tx = 0;
                try {
                    key = Key.fromHex(line.substring(11));
                } catch (final KeyFormatException ignored) {
                    key = null;
                }
            } else if (line.startsWith("rx_bytes=")) {
                if (key == null)
                    continue;
                try {
                    rx = Long.parseLong(line.substring(9));
                } catch (final NumberFormatException ignored) {
                    rx = 0;
                }
            } else if (line.startsWith("tx_bytes=")) {
                if (key == null)
                    continue;
                try {
                    tx = Long.parseLong(line.substring(9));
                } catch (final NumberFormatException ignored) {
                    tx = 0;
                }
            }
        }
        if (key != null)
            stats.add(key, rx, tx);
        return stats;
    }

    @Override
    public String getVersion() {
        return wgVersion();
    }

    @Override
    public State setState(final Tunnel tunnel, State state) throws Exception {
        final State originalState = getState(tunnel);

        if (state == State.TOGGLE)
            state = originalState == State.UP ? State.DOWN : State.UP;
        if (state == originalState)
            return originalState;
        if (state == State.UP && currentTunnel != null)
            throw new IllegalStateException("Only one userspace tunnel can run at a time");
        Log.d(TAG, "Changing tunnel " + tunnel.getName() + " to state " + state);
        setStateInternal(tunnel, tunnel.getConfig(), state);
        return getState(tunnel);
    }


    private void setStateInternal(final Tunnel tunnel, @Nullable final Config config, final State state)
            throws Exception {

        if (state == State.UP) {
            Log.i(TAG, "Bringing tunnel up");

            Objects.requireNonNull(config, "Trying to bring up a tunnel with no config");

            if (VpnService.prepare(context) != null)
                throw new Exception("VPN service not authorized by user");

            final VpnService service;
            if (!vpnService.isDone())
                startVpnService();

            try {
                DLog.d("Wireguard", "Waiting for service");
                service = vpnService.get(2, TimeUnit.SECONDS);
            } catch (final TimeoutException e) {
                throw new Exception("Unable to start Android VPN service", e);
            }

            if (currentTunnelHandle != -1) {
                Log.w(TAG, "Tunnel already up");
                return;
            }

            // Build config
            final String goConfig = config.toWgUserspaceString();

            // Create the vpn tunnel with android API
            final VpnService.Builder builder = service.getBuilder();
            builder.setSession(tunnel.getName());

            final Intent configureIntent = new Intent(context, MainActivity.class);
            configureIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            builder.setConfigureIntent(PendingIntent.getActivity(context, 0, configureIntent, 0));

            for (final String excludedApplication : config.getInterface().getExcludedApplications())
                builder.addDisallowedApplication(excludedApplication);

            for (final InetNetwork addr : config.getInterface().getAddresses())
                builder.addAddress(addr.getAddress(), addr.getMask());

            for (final InetAddress addr : config.getInterface().getDnsServers()) {
                DLog.d("Wireguard", "DNS: " + addr.getHostAddress());
                builder.addDnsServer(addr.getHostAddress());
            }

            for (final Peer peer : config.getPeers()) {
                for (final InetNetwork addr : peer.getAllowedIps())
                    builder.addRoute(addr.getAddress(), addr.getMask());
            }

            builder.setMtu(config.getInterface().getMtu().orElse(1280));

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                builder.setMetered(false);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                service.setUnderlyingNetworks(null);


            builder.setBlocking(true);
            try (final ParcelFileDescriptor tun = builder.establish()) {
                if (tun == null)
                    throw new BackendException(BackendException.Reason.TUN_CREATION_ERROR);
                Log.d(TAG, "Go backend v" + wgVersion());
                currentTunnelHandle = wgTurnOn(tunnel.getName(), tun.detachFd(), goConfig);
            }
            if (currentTunnelHandle < 0)
                throw new BackendException(BackendException.Reason.GO_ACTIVATION_ERROR_CODE, currentTunnelHandle);

            currentTunnel = tunnel;

            service.protect(wgGetSocketV4(currentTunnelHandle));
            service.protect(wgGetSocketV6(currentTunnelHandle));
            service.backend = this;

            //PIA Specific up logic
            service.setActiveTunnel(currentTunnel);

            VpnStateEvent event = new VpnStateEvent("CONNECT", "Wireguard Connect",
                    R.string.wg_connected, ConnectionStatus.LEVEL_CONNECTED);
            EventBus.getDefault().postSticky(event);
            SnoozeUtils.resumeVpn(context, false);

            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    FetchIPTask.resetValues(context);
                    FetchIPTask.execute(context, null);
                }
            });

            PiaPrefHandler.setGatewayEndpoint(context, currentTunnel.getConfig().getInterface().getGateway());
            boolean useMace = PiaPrefHandler.isMaceEnabled(context);

            if (useMace && !BuildConfig.FLAVOR_store.equals("playstore")) {
                HitMaceTask mace = new HitMaceTask(context, true);
                mace.execute();
            }

            service.showNotification("Connected",
                   "Connected", NOTIFICATION_CHANNEL_NEWSTATUS_ID, 0, ConnectionStatus.LEVEL_CONNECTED);

            lastState = state;
            isConnecting = false;
        } else {
            Log.i(TAG, "Bringing tunnel down");

            if (currentTunnelHandle == -1) {
                Log.w(TAG, "Tunnel already down");
                return;
            }

            wgTurnOff(currentTunnelHandle);
            currentTunnel = null;
            currentTunnelHandle = -1;

            //PIA Specific down logic

            VpnService.activeTunnel = null;

            VpnStateEvent event = new VpnStateEvent("CONNECT", "Wireguard Connect",
                    R.string.state_exiting, ConnectionStatus.LEVEL_NOTCONNECTED);
            EventBus.getDefault().postSticky(event);

            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    FetchIPTask.resetValues(context);
                    FetchIPTask.execute(context, null);
                }
            });

            PiaPrefHandler.clearGatewayEndpoint(context);
            PiaPrefHandler.setMaceActive(context, false);

            lastState = state;
            isConnecting = false;

            try {
                VpnService service = vpnService.get(2, TimeUnit.SECONDS);
                service.hideNotification();
            } catch (final TimeoutException e) {
                throw new Exception("Unable to start Android VPN service", e);
            }
        }
    }

    private void startVpnService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            context.startForegroundService(new Intent(context, VpnService.class));
        else
            context.startService(new Intent(context, VpnService.class));
    }

    public interface AlwaysOnCallback {
        void alwaysOnTriggered();
    }

    public void startVpn() {
        PIAServerHandler handler = PIAServerHandler.getInstance(context);
        final PIAServer server = handler.getSelectedRegion(context, false);

        if (PIAApplication.wireguardTunnel != null &&
                !PiaPrefHandler.hasDnsChanged(context)) {
            if (PIAApplication.wireguardServer != null &&
                server.getKey() != PIAApplication.wireguardServer.getKey()) {
                startWireguardService();
            }
            else {
                try {
                    VpnStateEvent event = new VpnStateEvent("CONNECT", "Wireguard Connect",
                            R.string.wg_connecting, ConnectionStatus.LEVEL_CONNECTING_NO_SERVER_REPLY_YET);
                    EventBus.getDefault().postSticky(event);
                    isConnecting = true;

                    setState(PIAApplication.wireguardTunnel, State.UP);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        else {
            startWireguardService();
        }
    }

    public void stopVpn() {
        stopVpn(false);
    }

    public void stopVpn(boolean killTunnel) {
        if (PIAApplication.wireguardTunnel != null) {
            try {
                setState(PIAApplication.wireguardTunnel, State.DOWN);

                if (killTunnel) {
                    PIAApplication.wireguardTunnel = null;
                    currentTunnel = null;
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void cancel() {
        VpnStateEvent event = new VpnStateEvent("CONNECT", "Wireguard Cancel",
                de.blinkt.openvpn.R.string.state_waitconnectretry, ConnectionStatus.LEVEL_NOTCONNECTED);
        EventBus.getDefault().postSticky(event);
        lastState = State.DOWN;
        isConnecting = false;
    }

    public boolean isActive() {
        if (lastState == State.UP || isConnecting) {
            return true;
        }

        return false;
    }

    public void startWireguardService() {
        PIAServerHandler handler = PIAServerHandler.getInstance(context);
        final PIAServer server = handler.getSelectedRegion(context, false);

        PiaPrefHandler.setDnsChanged(context, false);

        if (PIAApplication.wireguardTunnel != null &&
                PIAApplication.wireguardServer != null &&
                PIAApplication.wireguardServer.getKey() != server.getKey()) {
            try {
                setState(PIAApplication.wireguardTunnel, State.DOWN);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (server.getWgHost() == null || server.getWgHost().length() <= 0) {
            VpnStateEvent event = new VpnStateEvent("CONNECT", "Wireguard Connect",
                    R.string.failed_connect_status, ConnectionStatus.LEVEL_NONETWORK);
            EventBus.getDefault().postSticky(event);
            return;
        }

        String[] splitPort = server.getWgHost().split(":");
        int port = Integer.parseInt(splitPort[1]);

        final KeyPair wgKeyPair = new KeyPair();

        VpnStateEvent event = new VpnStateEvent("CONNECT", "Wireguard Connect",
                R.string.wg_connecting, ConnectionStatus.LEVEL_CONNECTING_NO_SERVER_REPLY_YET);
        EventBus.getDefault().postSticky(event);
        isConnecting = true;

        try {
            HttpUrl httpUrl = new HttpUrl.Builder()
                    .scheme("https")
                    .host(server.getDns())
                    .port(port)
                    .addPathSegment("addKey")
                    .addQueryParameter("pubkey", wgKeyPair.getPublicKey().toBase64())
                    .addQueryParameter("pt", PiaPrefHandler.getAuthToken(context))
                    .build();

            Request request = new Request.Builder().url(httpUrl).
                    build();

            PiaApi piaApi = new PiaApi();
            OkHttpClient client = piaApi.getOkHttpClient();

            DLog.d("Wireguard", "httpUrl: " + httpUrl.toString());
            DLog.d("Wireguard", "Attempting call");

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    DLog.d("Wireguard", "Failed call");
                    e.printStackTrace();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    int status = response.code();
                    DLog.d("Wireguard", "status = " + status);
                    String res = response.body().string();
                    DLog.d("Wireguard", "body = " + res);

                    try {
                        JSONObject jsonResponse = new JSONObject(res);
                        Config.Builder wgConfigBuilder = new Config.Builder();
                        wgConfigBuilder.addPeer(Peer.parse(generatePeer(wgKeyPair, jsonResponse, server)));
                        wgConfigBuilder.setInterface(Interface.parse(generateInterface(wgKeyPair, jsonResponse, server)));

                        Tunnel testTunnel = new Tunnel("PIATunnel", wgConfigBuilder.build(), State.DOWN);
                        PIAApplication.wireguardTunnel = testTunnel;
                        PIAApplication.wireguardServer = server;
                        setState(testTunnel, State.UP);
                    }
                    catch (Throwable e) {
                        VpnStateEvent event = new VpnStateEvent("CONNECT", "Wireguard Connect",
                                de.blinkt.openvpn.R.string.state_waitconnectretry, ConnectionStatus.LEVEL_NOTCONNECTED);
                        EventBus.getDefault().postSticky(event);
                        lastState = State.DOWN;
                        isConnecting = false;

                        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                        mNotificationManager.cancel(NOTIFICATION_CHANNEL_NEWSTATUS_ID.hashCode());

                        e.printStackTrace();
                    }
                }
            });
        }
        catch (Exception e) {
            e.printStackTrace();
            event = new VpnStateEvent("CONNECT", "Wireguard Connect",
                    de.blinkt.openvpn.R.string.state_waitconnectretry, ConnectionStatus.LEVEL_NOTCONNECTED);
            EventBus.getDefault().postSticky(event);
        }
    }

    private List<String> generateInterface(KeyPair keys, JSONObject response, PIAServer server) throws JSONException {
        List<String> wgSettings = new ArrayList<>();
        Object dns = response.getJSONArray("dns_servers").get(0);

        wgSettings.add("privatekey=" + keys.getPrivateKey().toBase64());

        Prefs prefs = new Prefs(context);
        if (prefs.get(PiaPrefHandler.CUSTOM_DNS_SELECTED, false)) {
            wgSettings.add("dns=" + prefs.get(PiaPrefHandler.DNS, dns.toString()));
        }
        else {
            if (prefs.getBoolean(PiaPrefHandler.GEN4_ACTIVE) && PiaPrefHandler.isMaceEnabled(context)) {
                dns = GEN4_MACE_ENABLED_DNS;
            }
            wgSettings.add("dns=" + dns);
        }

        wgSettings.add("mtu=1280");
        wgSettings.add("address=" + response.getString("peer_ip"));
        wgSettings.add("gateway=" + response.getString("server_vip"));

        return wgSettings;
    }

    private List<String> generatePeer(KeyPair keys, JSONObject response, PIAServer server) throws JSONException {
        List<String> wgSettings = new ArrayList<>();

        wgSettings.add("publickey=" + response.getString("server_key"));
        wgSettings.add("endpoint=" + response.getString("server_ip") + ":" + response.getString("server_port"));
        wgSettings.add("persistentkeepalive=25");

        Prefs prefs = new Prefs(context);
        if (PiaPrefHandler.getBlockLocal(context)) {
            wgSettings.add("allowedips=0.0.0.0/0");
        }
        else {
            if (prefs.getBoolean(PiaPrefHandler.GEN4_ACTIVE)) {
                if (PiaPrefHandler.isMaceEnabled(context)) {
                    wgSettings.add("allowedips=" + getAllowedIps(GEN4_MACE_ENABLED_DNS));
                }
                else {
                    wgSettings.add("allowedips=" + getAllowedIps(response.getJSONArray("dns_servers").get(0).toString()));
                }
            }
            else {
                wgSettings.add("allowedips=" + IPV4_PUBLIC_NETWORKS);
            }
        }

        return wgSettings;
    }

    private String getAllowedIps(String dnsServer) {
        if (dnsServer != null) {
            return IPV4_PUBLIC_NETWORKS + "," + dnsServer;
        }
        else {
            return IPV4_PUBLIC_NETWORKS + "10.0.0.240/29";
        }
    }

    // TODO: When we finally drop API 21 and move to API 24, delete this and replace with the ordinary CompletableFuture.
    public static final class GhettoCompletableFuture<V> {
        private final LinkedBlockingQueue<V> completion = new LinkedBlockingQueue<>(1);
        private final FutureTask<V> result = new FutureTask<>(completion::peek);

        public boolean complete(final V value) {
            if (value == null)
                return false;

            final boolean offered = completion.offer(value);
            if (offered)
                result.run();
            return offered;
        }

        public V get() throws ExecutionException, InterruptedException {
            return result.get();
        }

        public V get(final long timeout, final TimeUnit unit) throws ExecutionException, InterruptedException, TimeoutException {
            return result.get(timeout, unit);
        }

        public boolean isDone() {
            return !completion.isEmpty();
        }

        public GhettoCompletableFuture<V> newIncompleteFuture() {
            return new GhettoCompletableFuture<>();
        }
    }

    public static class VpnService extends android.net.VpnService {
        public Builder getBuilder() {
            return new Builder();
        }
        public static Tunnel activeTunnel;
        @Nullable public static GoBackend backend;

        private Handler usageHandler;
        private Runnable usageRunnable;

        static private Handler reconnectHandler;
        static private Runnable reconnectRunnable;

        private Statistics prevStats;

        private int staleCount = 0;

        @Override
        public void onCreate() {
            DLog.d("Wireguard", "Service created");
            vpnService.complete(this);
            super.onCreate();
        }

        @Override
        public void onDestroy() {
            if (backend != null) {
                final Tunnel tunnel = backend.currentTunnel;
                if (tunnel != null) {
                    if (backend.currentTunnelHandle != -1)
                        wgTurnOff(backend.currentTunnelHandle);
                    backend.currentTunnel = null;
                    backend.currentTunnelHandle = -1;
                    tunnel.onStateChange(State.DOWN);
                }
            }

            vpnService = vpnService.newIncompleteFuture();
            super.onDestroy();
        }

        @Override
        public int onStartCommand(@Nullable final Intent intent, final int flags, final int startId) {
            vpnService.complete(this);

            if (intent == null || intent.getComponent() == null || !intent.getComponent().getPackageName().equals(getPackageName())) {
                Log.d(TAG, "Service started by Always-on VPN feature");
                if (alwaysOnCallback != null)
                    alwaysOnCallback.alwaysOnTriggered();
            }

            return super.onStartCommand(intent, flags, startId);
        }

        public void setOwner(final GoBackend owner) {
            this.backend = owner;
        }

        public void setActiveTunnel(Tunnel tunnel) {
            activeTunnel = tunnel;

            if (reconnectHandler != null) {
                if (reconnectRunnable != null) {
                    reconnectHandler.removeCallbacks(reconnectRunnable);
                }

                reconnectHandler = null;
            }

            if (activeTunnel != null) {
                startUsageMonitor();
            }
            else {
                stopUsageMonitor();
            }
        }

        private void stopUsageMonitor() {
            if (usageRunnable != null)
                usageHandler.removeCallbacks(usageRunnable);

            usageRunnable = null;
            usageHandler = null;
        }

        private void startUsageMonitor() {
            usageRunnable = new Runnable() {
                @Override
                public void run() {
                    if (backend == null || activeTunnel == null) {
                        return;
                    }

                    Statistics stats = backend.getStatistics(activeTunnel);

                    if (prevStats == null) {
                        prevStats = stats;
                    }

                    VPNTrafficDataPointEvent traffic = new VPNTrafficDataPointEvent(
                            stats.totalRx(),
                            stats.totalTx(),
                            stats.totalRx() - prevStats.totalRx(),
                            stats.totalTx() - prevStats.totalTx());

                    if (stats.totalRx() - prevStats.totalRx() == 0) {
                        staleCount += 1;

                        if (staleCount > 3) {
                            PIAServerHandler handler = PIAServerHandler.getInstance(VpnService.this);
                            PIAServer server = handler.getSelectedRegion(VpnService.this, false);

                            Request request = new Request.Builder().url("http://" + server.getPingEndpoint()).
                                            build();

                            PiaApi piaApi = new PiaApi();
                            OkHttpClient client = piaApi.getOkHttpClient();
                            client.newCall(request).enqueue(new Callback() {
                                @Override
                                public void onFailure(Call call, IOException e) {
                                    e.printStackTrace();
                                }

                                @Override
                                public void onResponse(Call call, Response response) throws IOException {

                                }
                            });
                        }

                        if (staleCount > 5) {
                            staleCount = 0;
                            startReconnect();

                            prevStats = null;
                            return;
                        }
                    }
                    else {
                        staleCount = 0;
                    }

                    EventBus.getDefault().postSticky(traffic);

                    if (usageHandler != null) {
                        usageHandler.postDelayed(usageRunnable, 10000);
                    }

                    prevStats = stats;
                }
            };

            usageHandler = new Handler(Looper.getMainLooper());
            usageHandler.postDelayed(usageRunnable, 10000);
        }

        private void startReconnect() {
            if (backend != null && activeTunnel != null) {
                try {
                    backend.setState(activeTunnel, State.DOWN);
                    PIAApplication.wireguardTunnel = null;

                    stopUsageMonitor();

                    reconnectHandler = new Handler(Looper.getMainLooper());
                    reconnectRunnable = () -> {
                        try {
                            backend.startVpn();

                            reconnectHandler.postDelayed(reconnectRunnable, 30000);
                        }
                        catch(Exception e) {
                            e.printStackTrace();
                        }
                    };

                    reconnectHandler.postDelayed(reconnectRunnable, 5000);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }

        private int getIconByConnectionStatus(ConnectionStatus level) {
            return PIAOpenVPNTunnelLibrary.mNotifications.getIconByConnectionStatus(level);
        }

        private int getColorByConnectionStatus(ConnectionStatus level) {
            return PIAOpenVPNTunnelLibrary.mNotifications.getColorByConnectionStatus(this, level);
        }

        public void hideNotification() {
            stopUsageMonitor();
            stopForeground(true);
            stopSelf();
        }

        public void showNotification(final String msg, String tickerText, @NonNull String channel, long when, ConnectionStatus status) {
            NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            int icon = getIconByConnectionStatus(status);
            int color = getColorByConnectionStatus(status);

            PIAServer server = PIAApplication.wireguardServer;

            NotificationCompat.Builder nbuilder = new NotificationCompat.Builder(this);

            if (server != null)
                nbuilder.setContentTitle(getString(de.blinkt.openvpn.R.string.notifcation_title, server.getName()));
            else
                nbuilder.setContentTitle("");

            nbuilder.setContentText(msg);
            nbuilder.setOnlyAlertOnce(true);
            nbuilder.setOngoing(true);

            nbuilder.setSmallIcon(icon);
            nbuilder.setContentIntent(
                    PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0)
            );

            if (when != 0)
                nbuilder.setWhen(when);

            if (color != -1) {
                nbuilder.setColor(color);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                //noinspection NewApi
                nbuilder.setChannelId(channel);
            }

            if (tickerText != null && !tickerText.equals(""))
                nbuilder.setTicker(tickerText);

            @SuppressWarnings("deprecation")
            Notification notification = nbuilder.getNotification();

            int notificationId = channel.hashCode();

            mNotificationManager.notify(notificationId, notification);

            startForeground(notificationId, notification);
        }
    }
}
