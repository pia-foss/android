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

package com.privateinternetaccess.android.pia.providers

import android.content.Context
import com.privateinternetaccess.account.AccountClientStateProvider
import com.privateinternetaccess.account.AccountEndpoint
import com.privateinternetaccess.android.BuildConfig
import com.privateinternetaccess.android.pia.PIAFactory
import com.privateinternetaccess.android.pia.handlers.PIAServerHandler
import com.privateinternetaccess.android.pia.handlers.PIAServerHandler.ServerSortingType
import com.privateinternetaccess.android.pia.handlers.PiaPrefHandler
import com.privateinternetaccess.common.regions.RegionClientStateProvider
import com.privateinternetaccess.common.regions.RegionEndpoint
import com.privateinternetaccess.core.model.PIAServer
import com.privateinternetaccess.csi.CSIClientStateProvider
import com.privateinternetaccess.csi.CSIEndpoint
import kotlin.random.Random


class ModuleClientStateProvider(
        private val context: Context
)  : AccountClientStateProvider, RegionClientStateProvider, CSIClientStateProvider {

    companion object {
        private const val MAX_META_ENDPOINTS = 2
        private const val GATEWAY = "10.0.0.1"
        private const val ACCOUNT_BASE_ENDPOINT = "www.privateinternetaccess.com"
        private const val ACCOUNT_PROXY_ENDPOINT = "www.piaproxy.net"
        private const val REGION_BASE_ENDPOINT = "serverlist.piaservers.net"
        private const val CSI_BASE_ENDPOINT = "csi.supreme.tools"
    }

    // region AccountClientStateProvider
    override fun accountEndpoints(): List<AccountEndpoint> {
        val endpoints = mutableListOf<AccountEndpoint>()
        for (metaEndpoint in metaEndpoints()) {
            endpoints.add(AccountEndpoint(
                    metaEndpoint.endpoint,
                    metaEndpoint.isProxy,
                    metaEndpoint.usePinnedCertificate,
                    metaEndpoint.certificateCommonName)
            )
        }
        endpoints.add(
                AccountEndpoint(
                        ACCOUNT_BASE_ENDPOINT,
                        isProxy = false,
                        usePinnedCertificate = false,
                        certificateCommonName = null
                )
        )
        endpoints.add(
                AccountEndpoint(
                        ACCOUNT_PROXY_ENDPOINT,
                        isProxy = true,
                        usePinnedCertificate = false,
                        certificateCommonName = null
                )
        )
        if (PiaPrefHandler.useStaging(context)) {
            endpoints.clear()
            endpoints.add(
                    AccountEndpoint(
                            BuildConfig.STAGEINGHOST.replace("https://", ""),
                            isProxy = false,
                            usePinnedCertificate = false,
                            certificateCommonName = null
                    )
            )
        }
        return endpoints
    }
    // endregion

    // region RegionClientStateProvider
    override fun regionEndpoints(): List<RegionEndpoint> {
        val endpoints = mutableListOf<RegionEndpoint>()
        for (metaEndpoint in metaEndpoints()) {
            endpoints.add(RegionEndpoint(
                    metaEndpoint.endpoint,
                    metaEndpoint.isProxy,
                    metaEndpoint.usePinnedCertificate,
                    metaEndpoint.certificateCommonName)
            )
        }
        endpoints.add(
                RegionEndpoint(
                        REGION_BASE_ENDPOINT,
                        isProxy = false,
                        usePinnedCertificate = false,
                        certificateCommonName = null
                )
        )
        return endpoints
    }
    // endregion

    // region CSIClientStateProvider
    override fun csiEndpoints(): List<CSIEndpoint> {
        return listOf(CSIEndpoint(CSI_BASE_ENDPOINT, false, false, null))
    }
    // endregion

    // region private
    private fun metaEndpoints(): List<GenericEndpoint> {
        val endpoints = mutableListOf<GenericEndpoint>()
        val serverHandler = PIAServerHandler.getInstance(context)
        val selectedRegion = serverHandler.getSelectedRegion(context, false)
        val vpnConnected = PIAFactory.getInstance().getVPN(context).isVPNActive

        // If the VPN is connected. Add the meta gateway and return.
        if (vpnConnected) {
            val selectedEndpoints = selectedRegion.endpoints[PIAServer.Protocol.META]
            if (!selectedEndpoints.isNullOrEmpty()) {
                endpoints.add(
                        GenericEndpoint(
                                GATEWAY,
                                isProxy = true,
                                usePinnedCertificate = true,
                                certificateCommonName = selectedEndpoints.first().second
                        )
                )
            }
            return endpoints
        }

        // Get the list of known regions sorted by latency.
        val sortedLatencyRegions = serverHandler.getServers(context, ServerSortingType.LATENCY)
        if (sortedLatencyRegions.isNullOrEmpty()) {
            return endpoints
        }

        // Filter out invalid latencies. e.g. nil, zero, etc.
        val regionsWithValidLatency = sortedLatencyRegions.filterNot {
            it.latency.isNullOrEmpty() || it.latency == "0"
        }.toMutableList()

        // If there were no regions with valid latencies yet or less than what we need to. Pick random.
        if (regionsWithValidLatency.isEmpty() || regionsWithValidLatency.size < MAX_META_ENDPOINTS) {

            // Starting from 2 because the automatic/selected region occupies one slot of the max.
            for ( i in 2..MAX_META_ENDPOINTS) {
                val region = sortedLatencyRegions[Random.nextInt(0, sortedLatencyRegions.size)]
                regionsWithValidLatency.add(region)
            }
        }

        // Add the selected region.
        regionsWithValidLatency.add(0, selectedRegion)

        // Add the MAX_META_ENDPOINTS regions with the lowest latencies.
        for (region in regionsWithValidLatency.subList(0, MAX_META_ENDPOINTS)) {
            // We want different meta regions. Provide just one meta per region region.
            val selectedEndpoint = region.endpoints[PIAServer.Protocol.META]?.first()
            if (selectedEndpoint != null) {
                endpoints.add(
                        GenericEndpoint(
                                selectedEndpoint.first,
                                isProxy = true,
                                usePinnedCertificate = true,
                                certificateCommonName = selectedEndpoint.second
                        )
                )
            }
        }
        return endpoints
    }

    private data class GenericEndpoint(
            val endpoint: String,
            val isProxy: Boolean,
            val usePinnedCertificate: Boolean,
            val certificateCommonName: String?
    )
    // endregion
}