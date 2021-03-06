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

import com.privateinternetaccess.android.pia.utils.DLog;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Vector;

public class PIAServerInfo {

    private Vector<String> webIps;

    private int pollInterval;

    private Vector<String> autoRegions;

    private Vector<Integer> udpPorts;

    private Vector<Integer> tcpPorts;

    public void parse(JSONObject json){
        pollInterval = json.optInt("poll_interval");

        webIps = new Vector<>();

        JSONArray array = json.optJSONArray("web_ips");
        if(array != null)
            for(int i = 0; i < array.length(); i++){
                webIps.add(array.optString(i));
            }

        JSONObject vpn_ports = json.optJSONObject("vpn_ports");
        if(vpn_ports != null) {
            JSONArray udp_ports = vpn_ports.optJSONArray("udp");
            JSONArray tcp_ports = vpn_ports.optJSONArray("tcp");

            udpPorts = new Vector<>();
            if(udp_ports != null)
                for (int i = 0; i < udp_ports.length(); i++) {
                    udpPorts.add(udp_ports.optInt(i));
                }


            tcpPorts = new Vector<>();
            if(tcp_ports != null)
                for (int i = 0; i < tcp_ports.length(); i++) {
                    tcpPorts.add(tcp_ports.optInt(i));
                }
        }
        autoRegions = new Vector<>();
        JSONArray auto_regions = json.optJSONArray("auto_regions");
        if(auto_regions != null)
            for(int i = 0; i < auto_regions.length(); i++){
                autoRegions.add(auto_regions.optString(i));
            }
        DLog.d("PIAServerInfo", toString());
    }

    @Override
    public String toString() {
        return "PIAServerInfo{" +
                "webIps=" + webIps +
                ", pollInterval=" + pollInterval +
                ", autoRegions=" + autoRegions +
                ", udpPorts=" + udpPorts +
                ", tcpPorts=" + tcpPorts +
                '}';
    }

    public Vector<String> getWebIps() {
        return webIps;
    }

    public void setWebIps(Vector<String> webIps) {
        this.webIps = webIps;
    }

    public int getPollInterval() {
        return pollInterval;
    }

    public void setPollInterval(int pollInterval) {
        this.pollInterval = pollInterval;
    }

    public Vector<String> getAutoRegions() {
        return autoRegions;
    }

    public void setAutoRegions(Vector<String> autoRegions) {
        this.autoRegions = autoRegions;
    }

    public Vector<Integer> getUdpPorts() {
        return udpPorts;
    }

    public void setUdpPorts(Vector<Integer> udpPorts) {
        this.udpPorts = udpPorts;
    }

    public Vector<Integer> getTcpPorts() {
        return tcpPorts;
    }

    public void setTcpPorts(Vector<Integer> tcpPorts) {
        this.tcpPorts = tcpPorts;
    }
}
