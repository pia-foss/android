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

package com.privateinternetaccess.core.model;

import org.json.JSONObject;

import java.util.Map;

public class PIAServer {

    public enum Protocol {
        OPENVPN_TCP {
            public String toString() {
                return "ovpntcp";
            }
        },
        OPENVPN_UDP {
            public String toString() {
                return "ovpnudp";
            }
        },
        WIREGUARD {
            public String toString() {
                return "wg";
            }
        }
    }

    private String name;
    private String iso;
    private String dns;
    private String wgHost;
    private String pingEndpoint;
    private Map<Protocol, String> latencies;
    private String tcpbest;
    private String udpbest;
    private String key;
    private String tlsRemote;
    private boolean geo;
    private boolean allowsPF;
    private boolean testing;

    public PIAServer() { }

    public PIAServer(
            String name,
            String iso,
            String dns,
            String wgHost,
            String pingEndpoint,
            Map<Protocol, String> latencies,
            String tcpbest,
            String udpbest,
            String key,
            String tlsRemote,
            boolean geo,
            boolean allowsPF,
            boolean testing
    ) {
        this.name = name;
        this.iso = iso;
        this.dns = dns;
        this.wgHost = wgHost;
        this.pingEndpoint = pingEndpoint;
        this.latencies = latencies;
        this.tcpbest = tcpbest;
        this.udpbest = udpbest;
        this.key = key;
        this.tlsRemote = tlsRemote;
        this.geo = geo;
        this.allowsPF = allowsPF;
        this.testing = testing;
    }

    public void parse(JSONObject json, String key){
        setKey(key);
        name = json.optString("name");
        dns = json.optString("dns");
        allowsPF = json.optBoolean("port_forward");
        geo = json.optBoolean("geo");
        pingEndpoint = json.optString("ping");
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
    }

    @Override
    public String toString() {
        return "PIAServer{" +
                "name='" + name + '\'' +
                ", dns=" + dns +
                ", pingEndpoint='" + pingEndpoint + '\'' +
                ", tcpbest='" + tcpbest + '\'' +
                ", udpbest='" + udpbest + '\'' +
                ", key='" + key + '\'' +
                ", tlsRemote='" + tlsRemote + '\'' +
                ", geo='" + geo + '\'' +
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

    public String getDns() {
        return dns;
    }

    public void setDns(String dns) {
        this.dns = dns;
    }

    public String getPingEndpoint() {
        return pingEndpoint;
    }

    public void setPingEndpoint(String ping) {
        this.pingEndpoint = ping;
    }

    public Map<Protocol, String> getLatencies() {
        return latencies;
    }

    public void setLatencies(Map<Protocol, String> latencies) {
        this.latencies = latencies;
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

    public boolean isGeo() {
        return geo;
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