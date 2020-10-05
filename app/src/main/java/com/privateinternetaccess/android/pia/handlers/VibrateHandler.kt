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

import android.content.Context
import android.content.Context.VIBRATOR_SERVICE
import android.os.Build
import android.os.VibrationEffect
import android.os.VibrationEffect.DEFAULT_AMPLITUDE
import android.os.Vibrator
import com.privateinternetaccess.android.pia.model.events.VpnStateEvent
import com.privateinternetaccess.android.pia.utils.DLog
import de.blinkt.openvpn.core.ConnectionStatus
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

class VibrateHandler(val context: Context) {

    private var vibrator: Vibrator?
    private var vibrated = false

    companion object {
        private const val TAG = "VibrateHandler"
        private const val DURATION_MS = 100L
    }

    init {
        EventBus.getDefault().register(this)
        vibrator = context.getSystemService(VIBRATOR_SERVICE)?.let {
            it as Vibrator
        } ?: run {
            DLog.e(TAG, "Error getting the Vibrator Service")
            null
        }
    }

    @Subscribe
    @Synchronized
    public fun updateState(event: VpnStateEvent)
    {
        if (event.level == null || vibrator == null) {
            vibrated = false
            return
        }

        if (!PiaPrefHandler.isHapticFeedbackEnabled(context)) {
            vibrated = false
            return
        }

        // The events are being over reported. Thus, the need for a state to check
        // if the vibration already happened.
        if (vibrated) {
            return
        }

        @Suppress("WHEN_ENUM_CAN_BE_NULL_IN_JAVA")
        when(event.level) {
            ConnectionStatus.LEVEL_CONNECTED -> {
                vibrated = true
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(DURATION_MS, DEFAULT_AMPLITUDE))
                } else {
                    vibrator?.vibrate(DURATION_MS)
                }
            }
            ConnectionStatus.LEVEL_VPNPAUSED,
            ConnectionStatus.LEVEL_CONNECTING_SERVER_REPLIED,
            ConnectionStatus.LEVEL_CONNECTING_NO_SERVER_REPLY_YET,
            ConnectionStatus.LEVEL_NONETWORK,
            ConnectionStatus.LEVEL_NOTCONNECTED,
            ConnectionStatus.LEVEL_START,
            ConnectionStatus.LEVEL_AUTH_FAILED,
            ConnectionStatus.LEVEL_WAITING_FOR_USER_INPUT,
            ConnectionStatus.UNKNOWN_LEVEL -> {
                vibrated = false
            }
        }
    }
}