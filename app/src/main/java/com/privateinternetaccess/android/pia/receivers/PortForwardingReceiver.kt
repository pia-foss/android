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

package com.privateinternetaccess.android.pia.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.privateinternetaccess.android.R
import com.privateinternetaccess.android.pia.api.Gen4PortForwardApi
import com.privateinternetaccess.android.pia.model.exceptions.PortForwardingError
import com.privateinternetaccess.android.pia.utils.DLog
import com.privateinternetaccess.android.tunnel.PIAVpnStatus
import com.privateinternetaccess.android.tunnel.PortForwardingStatus
import kotlinx.coroutines.*
import java.io.IOException
import java.lang.Exception
import java.lang.IllegalStateException
import kotlin.coroutines.CoroutineContext

class PortForwardingReceiver : BroadcastReceiver(), CoroutineScope {

    companion object {
        const val TAG = "PortForwardingReceiver"
    }

    private val gen4PortForwardApi = Gen4PortForwardApi()
    private val job = Job()

    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.IO

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null) {
            return
        }

        launch {
            try {
                val port = gen4PortForwardApi.bindPort(context)
                PIAVpnStatus.setPortForwardingStatus(PortForwardingStatus.SUCCESS, port.toString())
            } catch (exception: IOException) {
                errorBindingPort(context, exception)
            } catch (exception: IllegalStateException) {
                errorBindingPort(context, exception)
            } catch (exception: PortForwardingError) {
                errorBindingPort(context, exception)
            }
        }
    }

    // region private
    private fun errorBindingPort(context: Context, exception: Exception) = launch(Dispatchers.Main) {
        DLog.w(TAG, exception.message)
        PIAVpnStatus.setPortForwardingStatus(
                PortForwardingStatus.ERROR,
                context.getString(R.string.n_a_port_forwarding)
        )
    }
    // endregion
}