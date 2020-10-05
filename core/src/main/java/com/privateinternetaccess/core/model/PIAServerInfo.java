package com.privateinternetaccess.core.model;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.Vector;

public class PIAServerInfo {

    private Vector<String> webIps;
    private int pollInterval;
    private Vector<String> autoRegions;
    private Vector<Integer> udpPorts;
    private Vector<Integer> tcpPorts;

    public PIAServerInfo() { }

    public PIAServerInfo(
            List<String> webIps,
            List<String> autoRegions,
            List<Integer> udpPorts,
            List<Integer> tcpPorts
    ) {
        this.webIps = new Vector<String>(webIps);
        this.autoRegions = new Vector<String>(autoRegions);
        this.udpPorts = new Vector<Integer>(udpPorts);
        this.tcpPorts = new Vector<Integer>(tcpPorts);
    }

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
        if(auto_regions != null) {
            for (int i = 0; i < auto_regions.length(); i++) {
                autoRegions.add(auto_regions.optString(i));
            }
        }
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
