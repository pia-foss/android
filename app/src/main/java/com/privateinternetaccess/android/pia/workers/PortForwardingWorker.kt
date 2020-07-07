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

package com.privateinternetaccess.android.pia.workers

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.privateinternetaccess.android.R
import com.privateinternetaccess.android.pia.api.Gen4PortForwardApi
import com.privateinternetaccess.android.pia.model.exceptions.PortForwardingError
import com.privateinternetaccess.android.tunnel.PIAVpnStatus
import com.privateinternetaccess.android.tunnel.PortForwardingStatus
import java.io.IOException

class PortForwardingWorker(
        appContext: Context,
        workerParams: WorkerParameters
) : Worker(appContext, workerParams) {

    private val gen4PortForwardApi = Gen4PortForwardApi()

    override fun doWork(): Result {
        try {
            val port = gen4PortForwardApi.bindPort(applicationContext)
            PIAVpnStatus.setPortForwardingStatus(PortForwardingStatus.SUCCESS, port.toString())
        } catch (e: IOException) {
            errorBindingPort()
        } catch (e: PortForwardingError) {
            errorBindingPort()
        }
        return Result.success()
    }

    override fun onStopped() {
        super.onStopped()
        gen4PortForwardApi.clearBindPort(applicationContext)
    }

    // region private
    private fun errorBindingPort() {
        PIAVpnStatus.setPortForwardingStatus(
                PortForwardingStatus.ERROR,
                applicationContext.getString(R.string.n_a_port_forwarding)
        )
    }
    // endregion
}