package com.privateinternetaccess.core.model;

import org.json.JSONObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kotlin.Pair;

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
        },
        META {
            public String toString() {
                return "meta";
            }
        }
    }

    private String name;
    private String iso;
    private String dns;
    private String wgHost;
    private String metaHost;
    private String pingEndpoint;
    private String latency;
    private Map<Protocol, List<Pair<String, String>>> certCommonNames;
    private String tcpbest;
    private String udpbest;
    private String key;
    private String tlsRemote;
    private String latitude;
    private String longitude;
    private boolean geo;
    private boolean allowsPF;
    private boolean testing;

    public PIAServer() { }

    public PIAServer(
            String name,
            String iso,
            String dns,
            String wgHost,
            String metaHost,
            String pingEndpoint,
            String latency,
            Map<Protocol, List<Pair<String, String>>> certCommonNames,
            String tcpbest,
            String udpbest,
            String key,
            String tlsRemote,
            String latitude,
            String longitude,
            boolean geo,
            boolean allowsPF,
            boolean testing
    ) {
        this.name = name;
        this.iso = iso;
        this.dns = dns;
        this.wgHost = wgHost;
        this.metaHost = metaHost;
        this.pingEndpoint = pingEndpoint;
        this.latency = latency;
        this.certCommonNames = certCommonNames;
        this.tcpbest = tcpbest;
        this.udpbest = udpbest;
        this.key = key;
        this.tlsRemote = tlsRemote;
        this.latitude = latitude;
        this.longitude = longitude;
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
        certCommonNames = new HashMap<>();
        JSONObject udp = json.optJSONObject("openvpn_udp");
        if (udp != null) {
            udpbest = udp.optString("best");
            certCommonNames.put(
                    Protocol.OPENVPN_UDP,
                    Collections.singletonList(
                            new Pair<>(udpbest.split(":")[0], tlsRemote)
                    )
            );
        }
        JSONObject tcp = json.optJSONObject("openvpn_tcp");
        if (tcp != null) {
            tcpbest = tcp.optString("best");
            certCommonNames.put(
                    Protocol.OPENVPN_TCP,
                    Collections.singletonList(
                            new Pair<>(tcpbest.split(":")[0], tlsRemote)
                    )
            );
        }
        JSONObject wg = json.optJSONObject("wireguard");
        if (wg != null) {
            wgHost = wg.optString("host");
            certCommonNames.put(
                    Protocol.WIREGUARD,
                    Collections.singletonList(
                            new Pair<>(wgHost.split(":")[0], tlsRemote)
                    )
            );
        }
    }

    public boolean isValid() {
        return tcpbest != null && !tcpbest.equals("") &&
                udpbest != null && !udpbest.equals("") &&
                wgHost != null && !wgHost.equals("");
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

    public Map<Protocol, List<Pair<String, String>>> getCommonNames() {
        return certCommonNames;
    }

    public String getPingEndpoint() {
        return pingEndpoint;
    }

    public void setPingEndpoint(String ping) {
        this.pingEndpoint = ping;
    }

    public String getLatency() {
        return latency;
    }

    public void setLatency(String latency) {
        this.latency = latency;
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

    public String getMetaHost() { return metaHost; }

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

    public String getLatitude() {
        return latitude;
    }

    public String getLongitude() {
        return longitude;
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