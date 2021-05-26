package com.privateinternetaccess.core.model;

import java.util.List;
import java.util.Vector;


public class PIAServerInfo {

    private Vector<String> autoRegions;
    private Vector<Integer> udpPorts;
    private Vector<Integer> tcpPorts;

    public PIAServerInfo(
            List<String> autoRegions,
            List<Integer> udpPorts,
            List<Integer> tcpPorts
    ) {
        this.autoRegions = new Vector<>(autoRegions);
        this.udpPorts = new Vector<>(udpPorts);
        this.tcpPorts = new Vector<>(tcpPorts);
    }

    @Override
    public String toString() {
        return "PIAServerInfo{" +
                "autoRegions=" + autoRegions +
                ", udpPorts=" + udpPorts +
                ", tcpPorts=" + tcpPorts +
                '}';
    }

    public Vector<String> getAutoRegions() { return autoRegions; }

    public Vector<Integer> getUdpPorts() {
        return udpPorts;
    }

    public Vector<Integer> getTcpPorts() {
        return tcpPorts;
    }
}
