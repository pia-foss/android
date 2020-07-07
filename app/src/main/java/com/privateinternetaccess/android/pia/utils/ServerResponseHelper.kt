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

package com.privateinternetaccess.android.pia.utils

import com.privateinternetaccess.core.model.PIAServer
import com.privateinternetaccess.core.model.PIAServerInfo
import com.privateinternetaccess.regions.RegionsProtocol
import com.privateinternetaccess.regions.model.RegionsResponse

class ServerResponseHelper {
    companion object {
        fun adaptServers(regionsResponse: RegionsResponse): Map<String, PIAServer> {
            val servers = mutableMapOf<String, PIAServer>()
            for (region in regionsResponse.regions) {
                val bestWireguard =
                        region.servers[RegionsProtocol.WIREGUARD.protocol]?.firstOrNull()?.ip
                val bestOvpnTcp =
                        region.servers[RegionsProtocol.OPENVPN_TCP.protocol]?.firstOrNull()?.ip
                val bestOvpnUdp =
                        region.servers[RegionsProtocol.OPENVPN_UDP.protocol]?.firstOrNull()?.ip

                if (bestWireguard == null && bestOvpnTcp == null && bestOvpnUdp == null) {
                    continue
                }

                // Application does not supporting the user option to choose wg ports and expect the
                // format `endpoint:port`, as it is not aware of wg ports.
                val wireguardPortEndpoint = bestWireguard?.let {
                    regionsResponse.groups[RegionsProtocol.WIREGUARD.protocol]?.let {
                        val port = it.first().ports.first().toString()
                        "$bestWireguard:$port"
                    }
                }

                val server = PIAServer(
                        region.name,
                        region.country,
                        region.dns,
                        wireguardPortEndpoint,
                        null,
                        mapOf<PIAServer.Protocol, String>(),
                        bestOvpnTcp,
                        bestOvpnUdp,
                        region.id,
                        null,
                        region.geo,
                        region.portForward,
                        false
                )
                servers[region.id] = server
            }
            return servers
        }

        fun adaptServersInfo(regionsResponse: RegionsResponse): PIAServerInfo {
            val autoRegions = mutableListOf<String>()
            regionsResponse.regions.filter { it.autoRegion }.forEach { region ->
                autoRegions.add(region.id)
            }
            val ovpntcp = mutableListOf<Int>()
            regionsResponse.groups[RegionsProtocol.OPENVPN_TCP.protocol]?.forEach { protocolPorts ->
                ovpntcp.addAll(protocolPorts.ports)
            }
            val ovpnudp = mutableListOf<Int>()
            regionsResponse.groups[RegionsProtocol.OPENVPN_UDP.protocol]?.forEach { protocolPorts ->
                ovpnudp.addAll(protocolPorts.ports)
            }
            return PIAServerInfo(
                    listOf("www.privateinternetaccess.com"),
                    autoRegions,
                    ovpnudp,
                    ovpntcp
            )
        }
    }
}