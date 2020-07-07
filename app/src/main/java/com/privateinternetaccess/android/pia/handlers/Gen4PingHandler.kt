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

package com.privateinternetaccess.android.pia.handlers

import com.privateinternetaccess.regions.PingRequest
import com.privateinternetaccess.regions.PingRequest.PlatformPingResult
import java.io.IOException
import java.net.InetAddress
import kotlinx.coroutines.*
import kotlin.system.measureTimeMillis

class Gen4PingHandler : PingRequest {

    companion object {
        const val TIMEOUT = 1500
    }

    override fun platformPingRequest(
            endpoints: Map<String, List<String>>,
            callback: (result: Map<String, List<PlatformPingResult>>) -> Unit
    ) = runBlocking {
        val result = mutableMapOf<String, List<PlatformPingResult>>()
        val requests = async {
            for ((region, endpointsInRegion) in endpoints) {
                val regionEndpointsResults = mutableListOf<PlatformPingResult>()
                endpointsInRegion.forEach {
                    val latency = measureTimeMillis {
                        ping(it)
                    }
                    regionEndpointsResults.add(PlatformPingResult(it, latency))
                    result[region] = regionEndpointsResults
                }
            }
        }
        requests.await()
        callback(result)
    }

    // region private
    private fun ping(endpoint: String) {
        try {
            InetAddress.getByName(endpoint).isReachable(TIMEOUT)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
    // endregion
}
