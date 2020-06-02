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

package com.privateinternetaccess.android.tunnel;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import androidx.core.content.ContextCompat;
import androidx.core.app.NotificationCompat;

import com.privateinternetaccess.android.R;
import com.privateinternetaccess.android.pia.utils.NotificationHelper;
import com.privateinternetaccess.android.receivers.NotificationReceiver;
import com.privateinternetaccess.android.receivers.OnRevokeReceiver;
import com.privateinternetaccess.android.ui.LauncherActivity;
import com.privateinternetaccess.android.ui.connection.MainActivity;

import de.blinkt.openvpn.core.ConnectionStatus;
import de.blinkt.openvpn.core.DeviceStateReceiver;
import de.blinkt.openvpn.core.OpenVPNService;

/**
 * Created by arne on 22.04.16.
 */
public class PIANotifications implements de.blinkt.openvpn.core.VPNNotifications {

    @Override
    public  void showRevokeNotification(Context context)
    {
        Intent i = new Intent(context, OnRevokeReceiver.class);
        context.sendBroadcast(i);

        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(ns);

        int icon = R.drawable.ic_stat_pia_robot_white;

        NotificationCompat.Builder nbuilder = new NotificationCompat.Builder(context);

        nbuilder.setContentText(context.getString(R.string.vpn_revoke_text));
        nbuilder.setOnlyAlertOnce(true);
        nbuilder.setSmallIcon(icon);
        nbuilder.setAutoCancel(true);
        nbuilder.setContentTitle(context.getString(R.string.vpn_revoke_title));
        nbuilder.setChannelId(NotificationHelper.NOTIFICATION_CHANNEL_ID);

        PendingIntent openSettings = PendingIntent.getActivity(context, 0, new Intent(Settings.ACTION_WIRELESS_SETTINGS), 0);
        nbuilder.addAction(0,  context.getString(R.string.open_android_settings),  openSettings);

        nbuilder.setTicker(context.getString(R.string.vpn_revoke_text));

        NotificationCompat.BigTextStyle style = new NotificationCompat.BigTextStyle();
        style.bigText(context.getString(R.string.vpn_revoke_text));
        nbuilder.setStyle(style);

        Intent notificationIntent = new Intent(context, LauncherActivity.class);

        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent intent = PendingIntent.getActivity(context, 0,
                notificationIntent, 0);

        nbuilder.setContentIntent(intent);

        Notification notification = nbuilder.build();

        mNotificationManager.notify(OpenVPNService.NOTIFICATION_CHANNEL_NEWSTATUS_ID.hashCode(), notification);

    }

    @Override
    public int getIconByConnectionStatus(ConnectionStatus level) {
        if (level == ConnectionStatus.LEVEL_CONNECTING_NO_SERVER_REPLY_YET ||
                level == ConnectionStatus.LEVEL_CONNECTING_SERVER_REPLIED ||
                level == ConnectionStatus.LEVEL_START) {
            return R.drawable.ic_notification_connecting;
        }

        return R.drawable.ic_stat_pia_robot_white;
    }

    /**
     * @param context
     * @param level
     * @return int This is either a color, or -1. -1 is handled as no tint in our openvpn wrapper.
     */
    @Override
    public int getColorByConnectionStatus(Context context, ConnectionStatus level) {
        switch (level) {
            case LEVEL_CONNECTING_SERVER_REPLIED:
            case LEVEL_CONNECTING_NO_SERVER_REPLY_YET:
            case LEVEL_START:
                return ContextCompat.getColor(context, R.color.connecting_yellow);
            case LEVEL_CONNECTED:
                return ContextCompat.getColor(context, R.color.pia_notification_green);
        }

        return -1;
    }

    @Override
    public  void stopKillSwitchNotification(Context context)
    {
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(ns);
        mNotificationManager.cancel(NotificationHelper.NOTIFICATION_CHANNEL_ID.hashCode());
    }

    @Override
    public  void showKillSwitchNotification(Context context)
    {
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(ns);

        int icon = R.drawable.ic_stat_pia_robot_white;

        NotificationCompat.Builder nbuilder = new NotificationCompat.Builder(context);

        nbuilder.setContentText(context.getString(R.string.killswitchstatus_description));
        nbuilder.setOnlyAlertOnce(true);
        nbuilder.setSmallIcon(icon);
        nbuilder.setAutoCancel(true);
        nbuilder.setContentTitle(context.getString(R.string.killswitch_notification_title));
        nbuilder.setChannelId(NotificationHelper.NOTIFICATION_CHANNEL_ID);

        nbuilder.setTicker(context.getString(R.string.killswitch_notification_title));

        NotificationCompat.BigTextStyle style = new NotificationCompat.BigTextStyle();
        style.bigText(context.getString(R.string.killswitchstatus_description));
        nbuilder.setStyle(style);

        Intent notificationIntent = new Intent(context, MainActivity.class);

        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);


        PendingIntent intent = PendingIntent.getActivity(context, 0,
                notificationIntent, 0);

        nbuilder.setContentIntent(intent);

        Notification notification = nbuilder.build();

        mNotificationManager.notify(NotificationHelper.NOTIFICATION_CHANNEL_ID.hashCode(), notification);

    }

    @Override
    public  void addPiaNotificationExtra(androidx.core.app.NotificationCompat.Builder nbuilder, Context context, DeviceStateReceiver mDeviceStateReceiver) {

        Intent disconnectVPN = new Intent(context, NotificationReceiver.class);
        disconnectVPN.setAction(NotificationReceiver.ACTION_DISCONNECT);
        PendingIntent disconnectPendingIntent = PendingIntent.getBroadcast(context, 0, disconnectVPN, 0);

        nbuilder.addAction(0,
                context.getString(de.blinkt.openvpn.R.string.cancel_connection), disconnectPendingIntent);

        Intent pauseVPN = new Intent(context, NotificationReceiver.class);
        if (mDeviceStateReceiver == null || !mDeviceStateReceiver.isUserPaused()) {
            pauseVPN.setAction(NotificationReceiver.ACTION_PAUSE);
            PendingIntent pauseVPNPending = PendingIntent.getBroadcast(context, 0, pauseVPN, 0);
            nbuilder.addAction(0,
                    context.getString(de.blinkt.openvpn.R.string.pauseVPN), pauseVPNPending);
        } else {
            pauseVPN.setAction(NotificationReceiver.ACTION_RESUME);
            PendingIntent resumeVPNPending = PendingIntent.getBroadcast(context, 0, pauseVPN, 0);
            nbuilder.addAction(0,
                    context.getString(de.blinkt.openvpn.R.string.resumevpn), resumeVPNPending);
        }

        Intent changeServer = new Intent(context, MainActivity.class);
        changeServer.setAction(MainActivity.CHANGE_VPN_SERVER);
        PendingIntent changeServerPI = PendingIntent.getActivity(context, 0, changeServer, 0);
        androidx.core.app.NotificationCompat.Action changeServerA =
                new androidx.core.app.NotificationCompat.Action(0, context.getString(R.string.change_server), changeServerPI);
        nbuilder.addAction(changeServerA);

    }

}
