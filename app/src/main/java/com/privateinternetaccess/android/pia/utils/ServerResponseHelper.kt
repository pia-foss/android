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

import com.privateinternetaccess.common.regions.RegionsProtocol
import com.privateinternetaccess.common.regions.model.RegionsResponse
import com.privateinternetaccess.core.model.PIAServer
import com.privateinternetaccess.core.model.PIAServerInfo

class ServerResponseHelper {
    companion object {
        fun adaptServers(regionsResponse: RegionsResponse): Map<String, PIAServer> {
            val servers = mutableMapOf<String, PIAServer>()
            for (region in regionsResponse.regions) {
                val wireguardDetails = region.servers[RegionsProtocol.WIREGUARD.protocol]
                val ovpnTcpDetails = region.servers[RegionsProtocol.OPENVPN_TCP.protocol]
                val ovpnUdpDetails = region.servers[RegionsProtocol.OPENVPN_UDP.protocol]
                val metaDetails = region.servers[RegionsProtocol.META.protocol]

                val bestWireguard = wireguardDetails?.firstOrNull()?.ip
                val bestOvpnTcp = ovpnTcpDetails?.firstOrNull()?.ip
                val bestOvpnUdp = ovpnUdpDetails?.firstOrNull()?.ip
                val bestMeta = metaDetails?.firstOrNull()?.ip

                if (bestWireguard == null &&
                        bestOvpnTcp == null &&
                        bestOvpnUdp == null &&
                        bestMeta == null
                ) {
                    continue
                }

                val commonNames = mutableMapOf<PIAServer.Protocol, List<Pair<String, String>>>()
                val wireguardCommonNames = mutableListOf<Pair<String, String>>()
                wireguardDetails?.forEach {
                    wireguardCommonNames.add(Pair(it.ip, it.cn))
                }
                commonNames[PIAServer.Protocol.WIREGUARD] = wireguardCommonNames

                val ovpnTcpCommonNames = mutableListOf<Pair<String, String>>()
                ovpnTcpDetails?.forEach {
                    ovpnTcpCommonNames.add(Pair(it.ip, it.cn))
                }
                commonNames[PIAServer.Protocol.OPENVPN_TCP] = ovpnTcpCommonNames

                val ovpnUdpCommonNames = mutableListOf<Pair<String, String>>()
                ovpnUdpDetails?.forEach {
                    ovpnUdpCommonNames.add(Pair(it.ip, it.cn))
                }
                commonNames[PIAServer.Protocol.OPENVPN_UDP] = ovpnUdpCommonNames

                val metaCommonNames = mutableListOf<Pair<String, String>>()
                metaDetails?.forEach {
                    metaCommonNames.add(Pair(it.ip, it.cn))
                }
                commonNames[PIAServer.Protocol.META] = metaCommonNames

                // Application does not support the user option to choose wg ports and expect the
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
                        bestMeta,
                        null,
                        null,
                        commonNames,
                        bestOvpnTcp,
                        bestOvpnUdp,
                        region.id,
                        null,
                        region.latitude,
                        region.longitude,
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