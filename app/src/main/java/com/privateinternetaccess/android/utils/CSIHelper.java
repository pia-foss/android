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

package com.privateinternetaccess.android.utils;

import android.content.Context;
import android.text.TextUtils;

import com.privateinternetaccess.android.R;
import com.privateinternetaccess.android.model.states.VPNProtocol;
import com.privateinternetaccess.android.pia.handlers.PiaPrefHandler;
import com.privateinternetaccess.android.pia.handlers.ThemeHandler;
import com.privateinternetaccess.android.pia.utils.Prefs;
import com.privateinternetaccess.regions.RegionsUtils;
import com.privateinternetaccess.regions.model.RegionsResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

import static com.privateinternetaccess.android.pia.handlers.PiaPrefHandler.GEN4_LAST_SERVER_BODY;
import static com.privateinternetaccess.android.pia.vpn.PiaOvpnConfig.DEFAULT_AUTH;
import static com.privateinternetaccess.android.pia.vpn.PiaOvpnConfig.DEFAULT_CIPHER;


public class CSIHelper {

    public static String getProtocol(Context context) {
        Prefs prefs = Prefs.with(context);
        StringBuilder sb = new StringBuilder();
        sb.append("~~ Connection Settings ~~").append("\n");
        sb.append("Connection Type: " + (prefs.getBoolean(PiaPrefHandler.USE_TCP) ? "TCP": "UDP")).append("\n");
        sb.append("Port Forwarding: " + prefs.getBoolean(PiaPrefHandler.PORTFORWARDING)).append("\n");
        sb.append("Remote Port: " + prefs.get(PiaPrefHandler.RPORT, "auto")).append("\n");
        sb.append("Local Port: " + prefs.get(PiaPrefHandler.LPORT, "auto")).append("\n");
        sb.append("Use Small Packets: " + prefs.get(PiaPrefHandler.PACKET_SIZE, context.getResources().getBoolean(R.bool.usemssfix))).append("\n").append("\n");
        sb.append("Protocol: " + (VPNProtocol.activeProtocol(context) == VPNProtocol.Protocol.OpenVPN ? "OpenVPN" : "WireGuard")).append("\n");
        sb.append("~~ Proxy Settings ~~").append("\n");
        sb.append("Proxy Enabled: " + prefs.get(PiaPrefHandler.PROXY_ENABLED, false)).append("\n");
        sb.append("Proxy App: " + prefs.get(PiaPrefHandler.PROXY_APP, "")).append("\n");
        sb.append("Proxy Port: " + prefs.get(PiaPrefHandler.PROXY_PORT, "")).append("\n").append("\n");
        sb.append("~~ Blocking Settings ~~").append("\n");
        sb.append("MACE: " + prefs.get(PiaPrefHandler.PIA_MACE, false)).append("\n");
        sb.append("Killswitch: " + prefs.get(PiaPrefHandler.KILLSWITCH, false)).append("\n");
        sb.append("Ipv6 Blocking: " + prefs.get(PiaPrefHandler.IPV6, context.getResources().getBoolean(R.bool.useblockipv6))).append("\n");
        sb.append("Block Local Network: " + prefs.get(PiaPrefHandler.BLOCK_LOCAL_LAN, true)).append("\n").append("\n");
        sb.append("~~ Encryption Settings ~~").append("\n");
        String cipher = prefs.get(PiaPrefHandler.CIPHER, DEFAULT_CIPHER);
        sb.append("Data Encryption: " + cipher).append("\n");
        if(!TextUtils.isEmpty(cipher))
            sb.append("Data Authentication: " + (cipher.toLowerCase(Locale.ENGLISH).contains("gcm") ? prefs.get(PiaPrefHandler.AUTH, DEFAULT_AUTH) : "")).append("\n");
        sb.append("Handshake: " + prefs.get(PiaPrefHandler.TLSCIPHER, "RSA-2048")).append("\n").append("\n");
        sb.append("~~ App Settings ~~").append("\n");
        sb.append("1 click connect: " + prefs.get(PiaPrefHandler.AUTOCONNECT, false)).append("\n");
        sb.append("Connect on Boot: " + prefs.get(PiaPrefHandler.AUTOSTART, false)).append("\n");
        sb.append("Connect on App Updated: " + prefs.get(PiaPrefHandler.CONNECT_ON_APP_UPDATED, false)).append("\n");
        sb.append("Haptic Feedback: " + prefs.get(PiaPrefHandler.HAPTIC_FEEDBACK, true)).append("\n");
        sb.append("Dark theme: " + prefs.get(ThemeHandler.PREF_THEME, false)).append("\n");
        sb.append("\n~~~~~ End User Settings ~~~~~\n\n");
        return redactIPsFromString(sb.toString());
    }

    public static String getRegions(Context context) {
        String gen4LastBody = Prefs.with(context).get(GEN4_LAST_SERVER_BODY, "");
        if (TextUtils.isEmpty(gen4LastBody)) {
            return "";
        }

        ArrayList<RegionsResponse.Region> redactedRegions = new ArrayList<>();
        RegionsResponse regionsResponse = RegionsUtils.INSTANCE.parse(gen4LastBody);
        for (RegionsResponse.Region region : regionsResponse.getRegions()) {
            redactedRegions.add(new RegionsResponse.Region(
                    region.getId(),
                    region.getName(),
                    region.getCountry(),
                    region.getDns(),
                    region.getGeo(),
                    region.getOffline(),
                    region.getLatitude(),
                    region.getLongitude(),
                    region.getAutoRegion(),
                    region.getPortForward(),
                    region.getProxy(),
                    Collections.emptyMap()
            ));
        }

        RegionsResponse redactedResponse = new RegionsResponse(
                regionsResponse.getGroups(),
                redactedRegions
        );
        return redactIPsFromString(RegionsUtils.INSTANCE.stringify(redactedResponse));
    }

    // region private
    private static String redactIPsFromString(String redact) {
        return redact.replaceAll("\\b(?:[0-9]{1,3}\\.){3}[0-9]{1,3}\\b", "REDACTED");
    }
    // endregion
}
