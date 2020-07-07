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

package com.privateinternetaccess.regions

import com.privateinternetaccess.regions.internals.Regions
import com.privateinternetaccess.regions.model.RegionsResponse

public enum class RegionsProtocol(val protocol: String) {
    OPENVPN_TCP("ovpntcp"),
    OPENVPN_UDP("ovpnudp"),
    WIREGUARD("wg")
}

/**
 * Interface defining the API to be offered by the module.
 */
public interface RegionsAPI {

    /**
     * Fetch all servers information on GEN4.
     *
     * @param callback `(response: RegionsResponse?, error: Error?)`. Invoked on the main thread.
     */
    fun fetch(callback: (response: RegionsResponse?, error: Error?) -> Unit)

    /**
     * Starts the process of ping requests and return the updated `ServerResponse` object as a
     * callback parameter.
     *
     * @param protocol `RegionsProtocol`. The protocol the application is working with.
     * @param callback `(response: RegionsResponse?, error: Error?)`. Invoked on the main thread.
     */
    fun pingRequests(
            protocol: RegionsProtocol,
            callback: (response: List<RegionLowerLatencyInformation>, error: Error?) -> Unit
    )
}

/**
 * Builder class responsible for creating an instance of an object conforming to
 * the `RegionsAPI` interface.
 */
public class RegionsBuilder {
    private var pingRequestDependency: PingRequest? = null
    private var messageVerificatorDependency: MessageVerificator? = null

    fun setPingRequestDependency(pingRequestDependency: PingRequest): RegionsBuilder =
            apply { this.pingRequestDependency = pingRequestDependency }

    fun setMessageVerificatorDependency(messageVerificatorDependency: MessageVerificator): RegionsBuilder =
            apply { this.messageVerificatorDependency = messageVerificatorDependency }

    /**
     * @return `RegionsAPI` instance.
     */
    fun build(): RegionsAPI {
        val pingDependency = pingRequestDependency
                ?: throw Exception("Essential ping request dependency missing.")
        val messageVerificator = messageVerificatorDependency
                ?: throw Exception("Essential message verification dependency missing.")
        return Regions(pingDependency, messageVerificator)
    }
}

/**
 * Class defining the information needed for the platform to perform a ping request.
 * Platform specific request.
 */
public interface PingRequest {

    /**
     * @param endpoints Map<String, List<String>>. Key: Region. List<String>: Endpoints within the
     * region.
     * @param callback Map<String, List<PlatformPingResult>>. Key: Region. List<PlatformPingResult>:
     * Endpoint and latencies within the region.
     */
    fun platformPingRequest(
            endpoints: Map<String, List<String>>,
            callback: (result: Map<String, List<PlatformPingResult>>) -> Unit
    )

    /**
     * @param endpoint String. Endpoint.
     * @param latency Long. Latency to the endpoint.
     */
    data class PlatformPingResult(val endpoint: String, val latency: Long)
}

/**
 *
 */
public interface MessageVerificator {

    /**
     *
     */
    fun verifyMessage(message: String, key: String): Boolean
}

/**
 * Data class defining the response object for a ping request. @see `fun pingRequests(...)`.
 *
 * @param region `String`.
 * @param endpoint `String`.
 * @param latency `String`.
 */
public data class RegionLowerLatencyInformation(
        val region: String,
        val endpoint: String,
        val latency: Long
)
