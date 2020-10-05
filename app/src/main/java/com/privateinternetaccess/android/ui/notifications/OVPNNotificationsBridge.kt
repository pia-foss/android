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

package com.privateinternetaccess.android.ui.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.privateinternetaccess.android.R
import com.privateinternetaccess.android.receivers.NotificationReceiver
import com.privateinternetaccess.android.receivers.OnRevokeReceiver
import com.privateinternetaccess.android.ui.LauncherActivity
import com.privateinternetaccess.android.ui.connection.MainActivity
import de.blinkt.openvpn.core.ConnectionStatus
import de.blinkt.openvpn.core.DeviceStateReceiver
import de.blinkt.openvpn.core.OpenVPNService
import de.blinkt.openvpn.core.VPNNotifications


class OVPNNotificationsBridge : VPNNotifications {

    override fun showRevokeNotification(context: Context) {
        context.sendBroadcast(Intent(context, OnRevokeReceiver::class.java))
        val openSettings =
                PendingIntent.getActivity(context, 0, Intent(Settings.ACTION_WIRELESS_SETTINGS), 0)
        val action = NotificationCompat.Action(
                0,
                context.getString(R.string.open_android_settings),
                openSettings
        )
        val style = NotificationCompat.BigTextStyle().bigText(
                context.getString(R.string.vpn_revoke_text)
        )
        val notificationIntent = Intent(context, LauncherActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val intent = PendingIntent.getActivity(context, 0, notificationIntent, 0)
        PIANotifications.sharedInstance.showNotification(
                context = context,
                notificationId = OpenVPNService.NOTIFICATION_CHANNEL_NEWSTATUS_ID.hashCode(),
                contentTitle = context.getString(R.string.vpn_revoke_title),
                contentText = context.getString(R.string.vpn_revoke_text),
                ticker = context.getString(R.string.vpn_revoke_text),
                style = style,
                intent = intent,
                action = action
        )
    }

    override fun showKillSwitchNotification(context: Context) {
        val style = NotificationCompat.BigTextStyle().bigText(
                context.getString(R.string.killswitchstatus_description)
        )
        val notificationIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val intent = PendingIntent.getActivity(context, 0, notificationIntent, 0)
        PIANotifications.sharedInstance.showNotification(
                context = context,
                notificationId = PIANotifications.NOTIFICATION_CHANNEL_ID.hashCode(),
                contentTitle = context.getString(R.string.killswitch_notification_title),
                contentText = context.getString(R.string.killswitchstatus_description),
                ticker = context.getString(R.string.killswitch_notification_title),
                style = style,
                intent = intent
        )
    }

    override fun stopKillSwitchNotification(context: Context) {
        PIANotifications.sharedInstance.hideNotification(
                context,
                PIANotifications.NOTIFICATION_CHANNEL_ID.hashCode()
        )
    }

    override fun addPiaNotificationExtra(
            builder: NotificationCompat.Builder,
            context: Context,
            deviceStateReceiver: DeviceStateReceiver?
    ) {
        val disconnectVPN = Intent(context, NotificationReceiver::class.java).apply {
            action = NotificationReceiver.ACTION_DISCONNECT
        }
        builder.addAction(
                0,
                context.getString(de.blinkt.openvpn.R.string.cancel_connection),
                PendingIntent.getBroadcast(context, 0, disconnectVPN, 0)
        )
        val pauseVPN = Intent(context, NotificationReceiver::class.java)
        if (deviceStateReceiver == null || !deviceStateReceiver.isUserPaused) {
            pauseVPN.action = NotificationReceiver.ACTION_PAUSE
            val pauseVPNPending = PendingIntent.getBroadcast(context, 0, pauseVPN, 0)
            builder.addAction(
                    0,
                    context.getString(de.blinkt.openvpn.R.string.pauseVPN),
                    pauseVPNPending
            )
        } else {
            pauseVPN.action = NotificationReceiver.ACTION_RESUME
            val resumeVPNPending = PendingIntent.getBroadcast(context, 0, pauseVPN, 0)
            builder.addAction(
                    0,
                    context.getString(de.blinkt.openvpn.R.string.resumevpn),
                    resumeVPNPending
            )
        }
        val changeServer = Intent(context, MainActivity::class.java).apply {
            action = MainActivity.CHANGE_VPN_SERVER
        }
        val changeServerPI = PendingIntent.getActivity(context, 0, changeServer, 0)
        val changeServerA = NotificationCompat.Action(
                0,
                context.getString(R.string.change_server),
                changeServerPI
        )
        builder.addAction(changeServerA)
    }

    override fun getIconByConnectionStatus(level: ConnectionStatus): Int {
        return when (level) {
            ConnectionStatus.LEVEL_START,
            ConnectionStatus.LEVEL_CONNECTING_SERVER_REPLIED,
            ConnectionStatus.LEVEL_CONNECTING_NO_SERVER_REPLY_YET ->
                R.drawable.ic_notification_connecting
            ConnectionStatus.LEVEL_CONNECTED,
            ConnectionStatus.LEVEL_VPNPAUSED,
            ConnectionStatus.LEVEL_NONETWORK,
            ConnectionStatus.LEVEL_NOTCONNECTED,
            ConnectionStatus.LEVEL_AUTH_FAILED,
            ConnectionStatus.LEVEL_WAITING_FOR_USER_INPUT,
            ConnectionStatus.UNKNOWN_LEVEL ->
                R.drawable.ic_stat_pia_robot_white
        }
    }

    /**
     * @param context
     * @param level
     * @return int This is either a color, or -1. -1 is handled as no tint in our openvpn wrapper.
     */
    override fun getColorByConnectionStatus(context: Context, level: ConnectionStatus): Int {
        return when (level) {
            ConnectionStatus.LEVEL_CONNECTING_SERVER_REPLIED,
            ConnectionStatus.LEVEL_CONNECTING_NO_SERVER_REPLY_YET,
            ConnectionStatus.LEVEL_START ->
                ContextCompat.getColor(context, R.color.connecting_yellow)
            ConnectionStatus.LEVEL_CONNECTED ->
                ContextCompat.getColor(context, R.color.pia_notification_green)
            ConnectionStatus.LEVEL_VPNPAUSED,
            ConnectionStatus.LEVEL_NONETWORK,
            ConnectionStatus.LEVEL_NOTCONNECTED,
            ConnectionStatus.LEVEL_AUTH_FAILED,
            ConnectionStatus.LEVEL_WAITING_FOR_USER_INPUT,
            ConnectionStatus.UNKNOWN_LEVEL ->
                -1
        }
    }
}