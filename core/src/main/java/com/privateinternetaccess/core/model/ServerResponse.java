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

import java.util.Map;

public class ServerResponse {

    private Map<String, PIAServer> servers;
    private PIAServerInfo info;
    private String body;

    public ServerResponse() {
    }

    public ServerResponse(Map<String, PIAServer> servers, PIAServerInfo info) {
        this.servers = servers;
        this.info = info;
    }

    public ServerResponse(Map<String, PIAServer> servers, PIAServerInfo info, String body) {
        this.servers = servers;
        this.info = info;
        this.body = body;
    }

    public boolean isValid(){
        return servers != null && servers.size() > 0
                && info != null && info.getAutoRegions() != null && info.getAutoRegions().size() > 0
                && info.getTcpPorts() != null && info.getTcpPorts().size() > 0
                && info.getUdpPorts() != null && info.getUdpPorts().size() > 0
                && info.getWebIps() != null && info.getWebIps().size() > 0;
    }

    public Map<String, PIAServer> getServers() {
        return servers;
    }

    public void setServers(Map<String, PIAServer> servers) {
        this.servers = servers;
    }

    public PIAServerInfo getInfo() {
        return info;
    }

    public void setInfo(PIAServerInfo info) {
        this.info = info;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}
