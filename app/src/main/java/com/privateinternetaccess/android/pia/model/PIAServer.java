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

package com.privateinternetaccess.android.pia.model;

import android.content.Context;

import com.privateinternetaccess.android.R;
import com.privateinternetaccess.android.pia.utils.DLog;

import org.json.JSONObject;

import java.util.Locale;
import java.util.Vector;

public class PIAServer {

    public static final int NUM_PINGS = 10;
    private String name;
    private String iso;
    private Object dns;
    private String wgHost;
    private String ping;
    private String tcpbest;
    private String udpbest;
    private String key;
    private String tlsRemote;
    private boolean allowsPF;
    private boolean testing;

//    "us_california": {
//        "name": "US California",
//                "country": "US",
//                "dns": "us-california.privateinternetaccess.com",
//                "port_forward": false,
//                "ping": "198.8.80.54:8888",
//                "openvpn_udp": {
//            "best": "198.8.80.54:8080"
//        },
//        "openvpn_tcp": {
//            "best": "198.8.80.54:500"
//        },
//        "serial": "528993c9148e23182cbf83de58aaadae"
//        "wireguard": {
//            "host":"107.181.189.80:1337",
//            "serial":"b21ab1a2b7c1a2f0879b5d5e7dac09e8"
//        }
//    }

    public void parse(JSONObject json, String key){
        setKey(key);
        name = json.optString("name");
        dns = json.optString("dns");
        allowsPF = json.optBoolean("port_forward");
        ping = json.optString("ping");
        tlsRemote = json.optString("serial");
        iso = json.optString("country");
        JSONObject udp = json.optJSONObject("openvpn_udp");
        if(udp != null)
            udpbest = udp.optString("best");
        JSONObject tcp = json.optJSONObject("openvpn_tcp");
        if(tcp != null)
            tcpbest = tcp.optString("best");
        JSONObject wg = json.optJSONObject("wireguard");
        if (wg != null)
            wgHost = wg.optString("host");
        DLog.d("PIAServer",toString());
    }

    @Override
    public String toString() {
        return "PIAServer{" +
                "name='" + name + '\'' +
                ", dns=" + dns +
                ", ping='" + ping + '\'' +
                ", tcpbest='" + tcpbest + '\'' +
                ", udpbest='" + udpbest + '\'' +
                ", key='" + key + '\'' +
                ", tlsRemote='" + tlsRemote + '\'' +
                ", allowsPF=" + allowsPF +
                ", wgHost=" + wgHost +
                '}';
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIso() {
        return iso;
    }

    public void setIso(String iso) {
        this.iso = iso;
    }

    public Object getDns() {
        return dns;
    }

    public void setDns(Object dns) {
        this.dns = dns;
    }

    public String getPing() {
        return ping;
    }

    public void setPing(String ping) {
        this.ping = ping;
    }

    public String getTcpbest() {
        return tcpbest;
    }

    public void setTcpbest(String tcpbest) {
        this.tcpbest = tcpbest;
    }

    public String getUdpbest() {
        return udpbest;
    }

    public void setUdpbest(String udpbest) {
        this.udpbest = udpbest;
    }

    public String getWgHost() { return wgHost; }

    public void setWgHost(String host) { this.wgHost = host; }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getTlsRemote() {
        return tlsRemote;
    }

    public void setTlsRemote(String tlsRemote) {
        this.tlsRemote = tlsRemote;
    }

    public boolean isAllowsPF() {
        return allowsPF;
    }

    public void setAllowsPF(boolean allowsPF) {
        this.allowsPF = allowsPF;
    }

    public boolean isTesting() {
        return testing;
    }

    public void setTesting(boolean testing) {
        this.testing = testing;
    }
}