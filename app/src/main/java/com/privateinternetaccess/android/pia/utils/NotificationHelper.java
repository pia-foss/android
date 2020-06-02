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

package com.privateinternetaccess.android.pia.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

/**
 * New utility methods for Android O for channels.
 *
 * Created by half47 on 4/7/17.
 */
public class NotificationHelper {

    public static final String NOTIFICATION_CHANNEL_ID = "pia_notification_channel";

    /**
     * creates the PIA notification channel and sends it to the NotificationManager.
     *
     * @param context
     * @return
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static NotificationChannel createPiaNotificationChannel(Context context, String channelId, String title, String description){
        NotificationChannel mChannel = new NotificationChannel(channelId, title, NotificationManager.IMPORTANCE_MIN);
        // Sets whether notifications posted to this channel should display notification lights
        mChannel.enableLights(false);
        // Sets whether notification posted to this channel should vibrate.
        mChannel.enableVibration(false);
        // Sets the notification light color for notifications posted to this channel
        mChannel.setLightColor(Color.GREEN);
        // Sets whether notifications posted to this channel appear on the lockscreen or not
        mChannel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        mChannel.setDescription(description);
        mChannel.setSound(null, null);

        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.createNotificationChannel(mChannel);

        return mChannel;
    }

    /**
     * creates a notification and a notification channel for Android O.
     *
     * @param context
     * @param id
     * @param title
     * @param icon
     * @param autoCancel
     * @param text
     * @param intent
     * @param channelId PIAApplication.NOTIFICATION_CHANNEL_ID is standard
     */
    public static void createNotification(Context context, int id, String title, int icon, boolean autoCancel, String text, PendingIntent intent, String channelId){
        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationCompat.Builder nBuilder = new NotificationCompat.Builder(context, channelId);

        nBuilder.setSmallIcon(icon);
        nBuilder.setAutoCancel(autoCancel);
        nBuilder.setContentTitle(title);

        nBuilder.setContentText(text);

        nBuilder.setContentIntent(intent);
        nBuilder.setSound(null);

        mNotificationManager.notify(id, nBuilder.build());
    }

    public static void createNotification(Context context, int id, String title, int icon, boolean autoCancel, String text, String action, int actionIcon, PendingIntent intent, String channelId){
        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationCompat.Builder nBuilder = new NotificationCompat.Builder(context, channelId);

        nBuilder.setSmallIcon(icon);
        nBuilder.setAutoCancel(autoCancel);
        nBuilder.setContentTitle(title);

        nBuilder.addAction(actionIcon, action, intent);

        nBuilder.setContentText(text);

        nBuilder.setSound(null);

        mNotificationManager.notify(id, nBuilder.build());
    }

    public static void deleteChannel(Context context, String name){
        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mNotificationManager.deleteNotificationChannel(name);
        }
    }
}
