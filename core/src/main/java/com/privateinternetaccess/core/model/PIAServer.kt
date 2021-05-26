package com.privateinternetaccess.core.model

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

data class PIAServer(
        val name: String,
        val iso: String,
        val dns: String,
        val latency: String?,
        val endpoints: Map<Protocol, List<PIAServerEndpointDetails>>,
        val key: String,
        val latitude: String?,
        val longitude: String?,
        val isGeo: Boolean,
        val isOffline: Boolean,
        val isAllowsPF: Boolean,
        val dipToken: String?,
        val dedicatedIp: String?
) {
    data class PIAServerEndpointDetails(
            val ip: String,
            val cn: String,
            val usesVanillaOpenVPN: Boolean
    )

    enum class Protocol {
        OPENVPN_TCP {
            override fun toString(): String {
                return "ovpntcp"
            }
        },
        OPENVPN_UDP {
            override fun toString(): String {
                return "ovpnudp"
            }
        },
        WIREGUARD {
            override fun toString(): String {
                return "wg"
            }
        },
        META {
            override fun toString(): String {
                return "meta"
            }
        }
    }

    fun isDedicatedIp() = dedicatedIp != null && dedicatedIp.isNotEmpty()
}