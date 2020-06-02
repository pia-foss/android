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

package com.privateinternetaccess.android.pia.vpn;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.privateinternetaccess.android.R;
import com.privateinternetaccess.android.pia.handlers.PIAServerHandler;
import com.privateinternetaccess.android.pia.handlers.PiaPrefHandler;
import com.privateinternetaccess.android.pia.model.PIAServer;
import com.privateinternetaccess.android.pia.utils.DLog;
import com.privateinternetaccess.android.pia.utils.Prefs;
import com.privateinternetaccess.android.tunnel.PIAVpnStatus;
import com.privateinternetaccess.android.ui.drawer.settings.DeveloperActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.HashSet;
import java.util.Locale;
import java.util.Vector;

import de.blinkt.openvpn.VpnProfile;
import de.blinkt.openvpn.core.ConfigParser;
import de.blinkt.openvpn.core.Connection;
import de.blinkt.openvpn.core.ProfileManager;

public class PiaOvpnConfig {

    public static final String DEFAULT_CIPHER = "AES-128-GCM";
    public static final String DEFAULT_AUTH = "SHA1";

    @NonNull
    public static VpnProfile generateVpnProfile(Context c) throws IOException, ConfigParser.ConfigParseError {
        String token = PiaPrefHandler.getAuthToken(c);
        String user = token.substring(0, token.length() / 2);
        String pw = token.substring(token.length() / 2);

        ConfigParser cp = new ConfigParser();
        cp.parseConfig(new StringReader(PiaOvpnConfig.genConfig(c, user, pw)));
        final VpnProfile vp = cp.convertProfile();
        Prefs prefs = new Prefs(c);
        if (vp.checkProfile(c) != R.string.no_error_found)
            DLog.d("PIA", c.getString(vp.checkProfile(c)));

        PIAServer region = PIAServerHandler.getInstance(c).getSelectedRegion(c, false);
        vp.mName = region.getName();
        vp.mAllowedAppsVpnAreDisallowed = !prefs.getBoolean(PiaPrefHandler.VPN_PER_APP_ARE_ALLOWED);
        vp.mAllowedAppsVpn = new HashSet<>(prefs.getStringSet(PiaPrefHandler.VPN_PER_APP_PACKAGES));
            /* Always include PIA itself, so the current IP mechanism works */
        if (!vp.mAllowedAppsVpnAreDisallowed)
            vp.mAllowedAppsVpn.add(c.getPackageName());

        PIAVpnStatus.setLastConnectedRegion(region);

        DLog.d("MainActivity", "apps disallowed = " + vp.mAllowedAppsVpnAreDisallowed);

        vp.mAllowLocalLAN = !PiaPrefHandler.getBlockLocal(c);

        if (prefs.get("proxyisorbot", false))
            for (Connection conn : vp.mConnections)
                conn.mProxyType = Connection.ProxyType.ORBOT;

        vp.mUseCustomConfig = true;
        vp.mCustomConfigOptions += "\nauth-nocache\n";
        vp.mCustomConfigOptions +="forget-token-reconnect\n";

        // Unlimited number of retries
        vp.mConnectRetryMax = "-1";

        vp.mDNS1 = "";
        vp.mDNS2 = "";

        String dns = prefs.get(PiaPrefHandler.DNS, "");
        DLog.d("PiaOvpnConfig", "Custom DNS: " + dns);
        if(!TextUtils.isEmpty(dns)){
            vp.mDNS1 = dns;
            vp.mOverrideDNS = true;
        }

        String secondaryDns = prefs.get(PiaPrefHandler.DNS_SECONDARY, "");
        if (!TextUtils.isEmpty(secondaryDns)) {
            vp.mDNS2 = secondaryDns;
        }

        ProfileManager.setTemporaryProfile(c, vp);

        DLog.d("PiaOvpnConfig", "Primary: " + vp.mDNS1);
        DLog.d("PiaOvpnConfig", "Secondary: " + vp.mDNS2);

        return vp;
    }


    public static String genConfig(Context a, String user, String password) throws IOException {
        Prefs prefs = new Prefs(a);

        InputStream conf = a.getAssets().open("vpn.conf");
        InputStreamReader isr = new InputStreamReader(conf);
        BufferedReader br = new BufferedReader(isr);
        StringBuilder config = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null){
            config.append(line).append("\n");
        }
        PIAServerHandler handler = PIAServerHandler.getInstance(a);
        PIAServer ps = handler.getSelectedRegion(a, false);

        String autoserver;
        boolean useTCP = prefs.getBoolean(PiaPrefHandler.USE_TCP);
        if (useTCP) {
            config.append("proto tcp\n");
            autoserver = ps.getTcpbest();
        } else {
            config.append("proto udp\n");
            autoserver = ps.getUdpbest();
        }

        config.append(getInlineCa(a, prefs.get("tlscipher", "rsa2048")));

        config.append("cipher ").append(prefs.get("cipher", DEFAULT_CIPHER)).append("\n");
        config.append("auth ").append(prefs.get("auth", DEFAULT_AUTH)).append("\n");


        config.append("pia-signal-settings\n");


        String remoteip = autoserver.split(":")[0];
        String remoteport = autoserver.split(":")[1];

        String rport = prefs.get("rport", "auto");
        if (!rport.equals("") && !rport.equals("auto")) {
            remoteport = rport;
            config.append(String.format(Locale.ENGLISH, "remote %s %s\n", remoteip, remoteport));
        } else {
            Vector<Integer> ports = null;

            if(useTCP) {
                ports = handler.getInfo().getTcpPorts();
            } else {
                ports = handler.getInfo().getUdpPorts();
            }

            config.append(String.format(Locale.ENGLISH, "remote %s %s\n", remoteip, remoteport));

            for(Integer i : ports)
                if(!i.toString().equals(remoteport))
                    config.append(String.format(Locale.ENGLISH, "remote %s %s\n", remoteip, i));

            config.append("connect-timeout 5\n");
            config.append("connect-retry-max 1\n");
        }

        if (prefs.getBoolean("useproxy"))
            config.append(String.format(Locale.ENGLISH, "socks-proxy 127.0.0.1 %s\n", prefs.get("proxyport", "8080")));

        // Might be null when autoconnect is on and an old Config (ver 28) is still in the cache
        if (!TextUtils.isEmpty(ps.getTlsRemote())) {
            if (VpnProfile.mIsOpenVPN22)
                config.append(String.format("tls-remote %s\n", ps.getTlsRemote()));
            else
                config.append(String.format("verify-x509-name \"%s\" name\n", ps.getTlsRemote()));
        }

        String lport = prefs.get("lport", "auto");
        if (lport.equals("") || lport.equals("auto"))
            config.append("nobind\n");
        else
            config.append(String.format("lport %s\nbind\n", lport));


        config.append(String.format("<auth-user-pass>\n%s\n%s\n</auth-user-pass>\n",
                user, password));

        if (prefs.getBoolean("killswitch")) {
            // The tun fd is hold by the app
            //config += "\npersist-tun\n";
        }

        if (prefs.get("mssfix", a.getResources().getBoolean(R.bool.usemssfix)))
            config.append("mssfix 1250\n");


        // IPv6 kill
        if (prefs.get("blockipv6", a.getResources().getBoolean(R.bool.useblockipv6))) {
            if (!VpnProfile.mIsOpenVPN22) {
                config.append("ifconfig-ipv6 fd15:53b6:dead::2/64 fd15:53b6:dead::1\n");
                config.append("route-ipv6 ::/0 ::1\n");
            }
            config.append("block-ipv6\n");
        }

        Prefs p = new Prefs(a);

        String devConfigurations = p.getString(DeveloperActivity.PREF_DEVELOPER_CONFIGURATION);
        DLog.d("PIAOVPNConfig", "dev = " + devConfigurations);
        if (!TextUtils.isEmpty(devConfigurations)) {
//            config += "# Developer configurations\n";
//            config += "# Hopefully I'm doing this right\n";
            config.append("\n").append(devConfigurations).append("\n");
        }

        return config.toString();
    }

    private static String getInlineCa(Context c, String s) throws IOException {
        InputStream conf = c.getAssets().open(s + ".pem");
        InputStreamReader isr = new InputStreamReader(conf);
        BufferedReader br = new BufferedReader(isr);
        StringBuilder cafile = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            cafile.append(line).append("\n");
        }


        return String.format("<ca>\n%s\n</ca>\n", cafile.toString());
    }
}
