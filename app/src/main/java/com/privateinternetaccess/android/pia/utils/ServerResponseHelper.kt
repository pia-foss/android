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

import android.content.Context
import com.privateinternetaccess.android.pia.handlers.PiaPrefHandler
import com.privateinternetaccess.common.regions.RegionsProtocol
import com.privateinternetaccess.common.regions.model.RegionsResponse
import com.privateinternetaccess.core.model.PIAServer
import com.privateinternetaccess.core.model.PIAServerInfo


class ServerResponseHelper {
    companion object {
        fun adaptServers(context: Context, regionsResponse: RegionsResponse): Map<String, PIAServer> {
            val servers = mutableMapOf<String, PIAServer>()
            for (region in regionsResponse.regions) {
                val wireguardEndpoints = region.servers[RegionsProtocol.WIREGUARD.protocol]
                val ovpnTcpEndpoints = region.servers[RegionsProtocol.OPENVPN_TCP.protocol]
                val ovpnUdpEndpoints = region.servers[RegionsProtocol.OPENVPN_UDP.protocol]
                val metaEndpoints = region.servers[RegionsProtocol.META.protocol]

                val regionEndpoints = mutableMapOf<PIAServer.Protocol, List<Pair<String, String>>>()

                regionsResponse.groups[RegionsProtocol.WIREGUARD.protocol]?.let { group ->
                    val port = group.first().ports.first().toString()
                    wireguardEndpoints?.let {
                        val mappedEndpoints = mutableListOf<Pair<String, String>>()
                        for (wireguardEndpoint in it) {
                            // Application does not support the user option to choose wg ports and
                            // expect the format `endpoint:port`, as it is not aware of wg ports.
                            mappedEndpoints.add(
                                    Pair("${wireguardEndpoint.ip}:$port", wireguardEndpoint.cn)
                            )

                        }
                        regionEndpoints[PIAServer.Protocol.WIREGUARD] = mappedEndpoints
                    }
                }

                ovpnTcpEndpoints?.let {
                    val mappedEndpoints = mutableListOf<Pair<String, String>>()
                    for (ovpnTcpEndpoint in it) {
                        mappedEndpoints.add(Pair(ovpnTcpEndpoint.ip, ovpnTcpEndpoint.cn))

                    }
                    regionEndpoints[PIAServer.Protocol.OPENVPN_TCP] = mappedEndpoints
                }

                ovpnUdpEndpoints?.let {
                    val mappedEndpoints = mutableListOf<Pair<String, String>>()
                    for (ovpnUdpEndpoint in it) {
                        mappedEndpoints.add(Pair(ovpnUdpEndpoint.ip, ovpnUdpEndpoint.cn))

                    }
                    regionEndpoints[PIAServer.Protocol.OPENVPN_UDP] = mappedEndpoints
                }

                metaEndpoints?.let {
                    val mappedEndpoints = mutableListOf<Pair<String, String>>()
                    for (metaEndpoint in it) {
                        mappedEndpoints.add(Pair(metaEndpoint.ip, metaEndpoint.cn))

                    }
                    regionEndpoints[PIAServer.Protocol.META] = mappedEndpoints
                }

                // Randomize offline state for testing
                var offline = region.offline
                if (PiaPrefHandler.getRegionOfflineRandomizerTesting(context)) {
                    offline = (1..10).random() <= 2
                }

                val server = PIAServer(
                        region.name,
                        region.country,
                        region.dns,
                        null,
                        regionEndpoints,
                        region.id,
                        region.latitude,
                        region.longitude,
                        region.geo,
                        offline,
                        region.portForward,
                        null,
                        null
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