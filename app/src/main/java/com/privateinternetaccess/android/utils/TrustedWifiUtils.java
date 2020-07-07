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
import android.net.wifi.WifiManager;

import com.privateinternetaccess.android.pia.handlers.PiaPrefHandler;

public class TrustedWifiUtils {

    public static boolean isEnabledAndConnected(Context context) {
        boolean connectedToTrustedWifi = false;
        if (PiaPrefHandler.getTrustWifi(context)) {
            WifiManager wifiManager =
                    (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            String ssid = wifiManager.getConnectionInfo().getSSID();
            String sanitizedSSID = ssid.substring(1, ssid.length() - 1);
            connectedToTrustedWifi =
                    PiaPrefHandler.getTrustedNetworks(context).contains(sanitizedSSID);
        }
        return connectedToTrustedWifi;
    }
}
