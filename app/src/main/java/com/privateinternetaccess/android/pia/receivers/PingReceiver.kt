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
import com.privateinternetaccess.android.PIAApplication
import com.privateinternetaccess.android.pia.handlers.PIAServerHandler
import com.privateinternetaccess.android.pia.utils.DLog

class PingReceiver : BroadcastReceiver() {

    companion object {
        const val TAG = "PingReceiver"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (!PIAApplication.isNetworkAvailable(context)) {
            DLog.d(TAG, "Ping cancelled. Network not available")
            return
        }
        PIAServerHandler.getInstance(context).triggerLatenciesUpdate()
    }
}