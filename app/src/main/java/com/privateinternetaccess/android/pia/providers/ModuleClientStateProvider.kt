package com.privateinternetaccess.android.pia.providers

import android.content.Context
import com.privateinternetaccess.account.AccountClientStateProvider
import com.privateinternetaccess.account.AccountEndpoint
import com.privateinternetaccess.android.BuildConfig
import com.privateinternetaccess.android.pia.PIAFactory
import com.privateinternetaccess.android.pia.handlers.PIAServerHandler
import com.privateinternetaccess.android.pia.handlers.PIAServerHandler.ServerSortingType
import com.privateinternetaccess.android.pia.handlers.PiaPrefHandler
import com.privateinternetaccess.android.pia.utils.Prefs
import com.privateinternetaccess.common.regions.RegionClientStateProvider
import com.privateinternetaccess.common.regions.RegionEndpoint
import com.privateinternetaccess.core.model.PIAServer
import kotlin.random.Random

class ModuleClientStateProvider(
        private val context: Context
)  : AccountClientStateProvider, RegionClientStateProvider {

    companion object {
        private const val MAX_META_ENDPOINTS = 2
        private const val GATEWAY = "10.0.0.1"
        private const val ACCOUNT_BASE_ENDPOINT = "www.privateinternetaccess.com"
        private const val ACCOUNT_PROXY_ENDPOINT = "piaproxy.net"
        private const val REGION_BASE_ENDPOINT = "serverlist.piaservers.net"
    }

    private val serverHandler = PIAServerHandler.getInstance(context)

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
        if (PiaPrefHandler.useStaging(context)) {
            endpoints.clear()
            endpoints.add(
                    RegionEndpoint(
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

    // region private
    private fun metaEndpoints(): List<GenericEndpoint> {
        val endpoints = mutableListOf<GenericEndpoint>()
        val selectedRegion = serverHandler.getSelectedRegion(context, false)
        val vpnConnected = PIAFactory.getInstance().getVPN(context).isVPNActive

        // If the VPN is connected. Add the meta gateway and return.
        if (vpnConnected) {
            val commonNames = selectedRegion.commonNames[PIAServer.Protocol.META]
            if (!commonNames.isNullOrEmpty()) {
                endpoints.add(
                        GenericEndpoint(
                                GATEWAY,
                                isProxy = true,
                                usePinnedCertificate = true,
                                certificateCommonName = commonNames.first().second
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
            val commonNames = region.commonNames[PIAServer.Protocol.META]
            if (!commonNames.isNullOrEmpty()) {
                endpoints.add(
                        GenericEndpoint(
                                region.metaHost,
                                isProxy = true,
                                usePinnedCertificate = true,
                                certificateCommonName = commonNames.first().second
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