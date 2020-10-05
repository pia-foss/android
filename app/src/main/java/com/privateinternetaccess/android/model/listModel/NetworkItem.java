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

package com.privateinternetaccess.android.model.listModel;

import android.content.Context;
import android.net.Network;

import com.privateinternetaccess.android.R;
import com.privateinternetaccess.android.pia.handlers.PIAServerHandler;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;

public class NetworkItem {

    public enum NetworkBehavior {
        ALWAYS_CONNECT,
        ALWAYS_DISCONNECT,
        RETAIN_STATE
    }

    public enum NetworkType {
        MOBILE_DATA,
        WIFI_OPEN,
        WIFI_SECURE,
        WIFI_CUSTOM
    }

    public String networkName;
    public NetworkBehavior behavior;
    public NetworkType type;

    public boolean isDefaultMobile = false;
    public boolean isDefaultOpen = false;
    public boolean isDefaultSecure = false;

    private static final String BEHAVIOR = "behavior";
    private static final String TYPE = "type";
    private static final String NETWORK_NAME = "network_name";
    private static final String DEFAULT_MOBILE = "default_mobile";
    private static final String DEFAULT_OPEN = "default_open";
    private static final String DEFAULT_SECURE = "default_secure";

    @Nullable
    public String toString() {
        JSONObject json = new JSONObject();
        try {
            json.put(BEHAVIOR, behavior.ordinal());
            json.put(TYPE, type.ordinal());
            json.put(NETWORK_NAME, networkName);
            json.put(DEFAULT_MOBILE, isDefaultMobile);
            json.put(DEFAULT_OPEN,isDefaultOpen);
            json.put(DEFAULT_SECURE, isDefaultSecure);

            return json.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    public boolean isDefault() {
        return isDefaultMobile || isDefaultOpen || isDefaultSecure;
    }

    @Nullable
    public static NetworkItem fromString(String data) {
        try {
            JSONObject json = new JSONObject(data);
            NetworkItem networkItem = new NetworkItem();
            networkItem.behavior = NetworkBehavior.values()[json.getInt(BEHAVIOR)];
            networkItem.type = NetworkType.values()[json.getInt(TYPE)];
            networkItem.networkName = json.getString(NETWORK_NAME);
            networkItem.isDefaultMobile = json.getBoolean(DEFAULT_MOBILE);
            networkItem.isDefaultOpen = json.getBoolean(DEFAULT_OPEN);
            networkItem.isDefaultSecure = json.getBoolean(DEFAULT_SECURE);

            return networkItem;
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static List<NetworkItem> defaultList(Context context) {
        List<NetworkItem> items = new ArrayList<>();

//        NetworkItem secureWifi = new NetworkItem();
//        secureWifi.networkName = context.getResources().getString(R.string.nmt_secure_wifi);
//        secureWifi.behavior = NetworkBehavior.ALWAYS_CONNECT;
//        secureWifi.type = NetworkType.WIFI_SECURE;
//        secureWifi.isDefaultSecure = true;

        NetworkItem openWifi = new NetworkItem();
        openWifi.networkName = context.getResources().getString(R.string.nmt_open_wifi);
        openWifi.behavior = NetworkBehavior.ALWAYS_CONNECT;
        openWifi.type = NetworkType.WIFI_OPEN;
        openWifi.isDefaultOpen = true;

        NetworkItem mobileData = new NetworkItem();
        mobileData.networkName = context.getResources().getString(R.string.nmt_mobile_data);
        mobileData.behavior = NetworkBehavior.ALWAYS_CONNECT;
        mobileData.type = NetworkType.MOBILE_DATA;
        mobileData.isDefaultMobile = true;

//        items.add(secureWifi);
        items.add(openWifi);
        items.add(mobileData);

        return items;
    }

    public static NetworkBehavior getBehaviorFromId(int id) {
        NetworkBehavior selectedBehavior = NetworkBehavior.ALWAYS_CONNECT;

        for (NetworkBehavior behaviorType : NetworkBehavior.values()) {
            if (behaviorType.name().hashCode() == id) {
                selectedBehavior = behaviorType;
                break;
            }
        }
        return selectedBehavior;
    }
}
