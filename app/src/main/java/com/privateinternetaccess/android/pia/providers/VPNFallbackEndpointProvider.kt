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

import android.content.Context
import com.privateinternetaccess.android.R
import com.privateinternetaccess.android.pia.handlers.PIAServerHandler
import com.privateinternetaccess.android.pia.handlers.PiaPrefHandler
import com.privateinternetaccess.android.pia.model.events.VpnStateEvent
import com.privateinternetaccess.android.pia.utils.DLog
import com.privateinternetaccess.android.utils.ServerUtils
import com.privateinternetaccess.core.model.PIAServer
import de.blinkt.openvpn.core.ConnectionStatus
import kotlinx.coroutines.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.util.*
import kotlin.coroutines.CoroutineContext


class VPNFallbackEndpointProvider : CoroutineScope {

    data class VPNEndpoint(
            val key: String,
            val name: String,
            val endpoint: String,
            val commonName: String,
            val usesVanillaOpenVPN: Boolean,
            val wireguardServer: PIAServer
    )

    companion object {
        private const val TAG = "VPNFallbackEndpointProvider"
        private const val ATTEMPT_TIMEOUT_MS = 10000L
        public val sharedInstance = VPNFallbackEndpointProvider()
    }

    private var attemptNumber = 0
    private var attemptTimeoutJob: Job? = null
    private var endpointsPerProtocol = mutableListOf<VPNEndpoint>()
    private var attemptEndpointCallback: ((endpoint: VPNEndpoint?, error: Error?) -> Unit)? = null

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main

    fun start(
            context: Context,
            attemptEndpointCallback: (endpoint: VPNEndpoint?, error: Error?) -> Unit
    ) {
        DLog.d(TAG, "start")
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }
        this.attemptEndpointCallback = attemptEndpointCallback
        endpointsPerProtocol = adaptEndpointsPerProtocol(context)
        attemptNextAvailableEndpoint()
    }

    fun stop() {
        DLog.d(TAG, "stop")
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this)
        }
        endpointsPerProtocol = mutableListOf()
        attemptEndpointCallback = null

        launch {
            attemptTimeoutJob?.cancelAndJoin()
            attemptTimeoutJob = null
            attemptNumber = 0
        }
    }

    // region VpnStateEvent
    @Subscribe
    fun vpnStateEvent(event: VpnStateEvent?) {
        val eventType = event?.level ?: return

        DLog.d(TAG, "VPN State Event for latest endpoint attempt ${event.level}")
        when (eventType) {
            ConnectionStatus.LEVEL_CONNECTED -> {
                // Fallback provider did its job. Clear it
                stop()
            }
            ConnectionStatus.LEVEL_START,
            ConnectionStatus.LEVEL_CONNECTING_SERVER_REPLIED,
            ConnectionStatus.LEVEL_CONNECTING_NO_SERVER_REPLY_YET,
            ConnectionStatus.LEVEL_WAITING_FOR_USER_INPUT,
            ConnectionStatus.LEVEL_VPNPAUSED,
            ConnectionStatus.LEVEL_NOTCONNECTED,
            ConnectionStatus.UNKNOWN_LEVEL -> {
                // Do nothing
            }
            ConnectionStatus.LEVEL_NONETWORK,
            ConnectionStatus.LEVEL_AUTH_FAILED -> {
                // Try next endpoint
                attemptNextAvailableEndpoint()
            }
        }
    }
    // endregion

    // region private
    private fun attemptNextAvailableEndpoint() {
        attemptEndpointCallback?.let {
            val nextEndpoint = endpointsPerProtocol.removeFirstOrNull()
            if (nextEndpoint == null) {
                DLog.d(TAG, "No endpoints left to try")
                stop()
                it(null, Error("There are no available endpoints left"))
            } else {
                DLog.d(TAG, "Attempting endpoint $nextEndpoint")
                startAttemptTimeout()
                it(nextEndpoint, null)
            }
        } ?: DLog.d(TAG, "Callback undefined")
    }

    private fun startAttemptTimeout() {
        updateState(attemptNumber++)
        launch(Dispatchers.IO) {
            attemptTimeoutJob?.cancelAndJoin()
            attemptTimeoutJob = async {
                delay(ATTEMPT_TIMEOUT_MS)
                launch(Dispatchers.Main) {
                    attemptNextAvailableEndpoint()
                }
            }
        }
    }

    private fun updateState(attemptNumber: Int) {
        val stringId = when {
            attemptNumber == 0 -> {
                R.string.wg_connecting
            }
            attemptNumber == 1 -> {
                R.string.please_wait
            }
            attemptNumber >= 2 -> {
                R.string.still_connecting
            }
            else -> {
                R.string.wg_connecting
            }
        }

        EventBus.getDefault().postSticky(VpnStateEvent(
                "CONNECT",
                "",
                stringId,
                ConnectionStatus.LEVEL_CONNECTING_NO_SERVER_REPLY_YET
        ))
    }

    private fun adaptEndpointsPerProtocol(context: Context): MutableList<VPNEndpoint> {
        val result = mutableListOf<VPNEndpoint>()
        val region = PIAServerHandler.getInstance(context).getSelectedRegion(context, false)
        region?.let {
            val protocol = ServerUtils.getUserSelectedProtocol(context)
            region.endpoints[protocol]?.forEach {
                var endpoint = it.ip

                // If we want to force fail the connection attempt for testing. Return an invalid value.
                if (PiaPrefHandler.getRegionInitialConnectionRandomizerTesting(context)) {
                    if (Random().nextInt(10) < 7) {
                        endpoint = "1.1.1.1:1337"
                    }
                }

                result.add(VPNEndpoint(
                        region.key,
                        region.name,
                        endpoint,
                        it.cn,
                        it.usesVanillaOpenVPN,
                        region
                ))
            }
        }
        return result
    }
    // endregion
}