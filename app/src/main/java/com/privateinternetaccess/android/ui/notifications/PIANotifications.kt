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

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.graphics.Color
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.privateinternetaccess.android.R

class PIANotifications {

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "pia_notification_channel"
        val sharedInstance = PIANotifications()
    }

    public fun showNotification(
            context: Context,
            notificationId: Int,
            contentTitle: String,
            contentText: String,
            intent: PendingIntent
    ) = showNotification(
            context,
            notificationId,
            null,
            contentTitle,
            contentText,
            null,
            null,
            null,
            null,
            null,
            intent,
            null
    )

    public fun showNotification(
            context: Context,
            notificationId: Int,
            channelId: String,
            contentTitle: String,
            contentText: String,
            smallIcon: Int,
            action: NotificationCompat.Action
    ) = showNotification(
            context,
            notificationId,
            channelId,
            contentTitle,
            contentText,
            null,
            smallIcon,
            null,
            null,
            null,
            null,
            action
    )

    public fun showNotification(
            context: Context,
            notificationId: Int,
            contentTitle: String,
            contentText: String,
            ticker: String,
            style: NotificationCompat.Style,
            intent: PendingIntent
    ) = showNotification(
            context,
            notificationId,
            null,
            contentTitle,
            contentText,
            ticker,
            null,
            null,
            null,
            style,
            intent,
            null
    )

    public fun showNotification(
            context: Context,
            notificationId: Int,
            contentTitle: String,
            contentText: String,
            ticker: String,
            style: NotificationCompat.Style,
            intent: PendingIntent,
            action: NotificationCompat.Action?
    ) = showNotification(
            context,
            notificationId,
            null,
            contentTitle,
            contentText,
            ticker,
            null,
            null,
            null,
            style,
            intent,
            action
    )

    public fun showNotification(
            context: Context,
            notificationId: Int,
            channelId: String,
            contentTitle: String,
            contentText: String,
            ticker: String,
            smallIcon: Int,
            color: Int,
            ongoing: Boolean,
            intent: PendingIntent
    ) = showNotification(
            context,
            notificationId,
            channelId,
            contentTitle,
            contentText,
            ticker,
            smallIcon,
            color,
            ongoing,
            null,
            intent,
            null
    )

    public fun showNotification(
            context: Context,
            notificationId: Int,
            channelId: String?,
            contentTitle: String,
            contentText: String,
            ticker: String?,
            smallIcon: Int?,
            color: Int?,
            ongoing: Boolean?,
            style: NotificationCompat.Style?,
            intent: PendingIntent?,
            action: NotificationCompat.Action?
    ): Notification {
        val unwrappedChannelId = channelId ?: NOTIFICATION_CHANNEL_ID
        val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationBuilder =
                NotificationCompat.Builder(context, unwrappedChannelId)
                        .setContentText(contentText)
                        .setOnlyAlertOnce(true)
                        .setSmallIcon(R.drawable.ic_stat_pia_robot_white)
                        .setAutoCancel(true)
                        .setContentTitle(contentTitle)

        smallIcon?.let {
            notificationBuilder.setSmallIcon(smallIcon)
        }
        color?.let {
            notificationBuilder.setColor(it)
        }
        ongoing?.let {
            notificationBuilder.setAutoCancel(false)
            notificationBuilder.setOngoing(it)
        }
        ticker?.let {
            notificationBuilder.setTicker(it)
        }
        style?.let {
            notificationBuilder.setStyle(it)
        }
        action?.let {
            notificationBuilder.addAction(it)
        }
        intent?.let {
            notificationBuilder.setContentIntent(it)
        }
        val notification = notificationBuilder.build()
        notificationManager.notify(notificationId, notification)
        return notification
    }

    public fun hideNotification(context: Context, id: Int) {
        val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(id)
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public fun createNotificationChannel(
            context: Context,
            title: String,
            description: String
    ) {
        createNotificationChannel(context, NOTIFICATION_CHANNEL_ID, title, description)
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public fun createNotificationChannel(
            context: Context,
            id: String,
            title: String,
            description: String
    ) {
        val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(id, title, NotificationManager.IMPORTANCE_MIN)
        channel.enableLights(false)
        channel.enableVibration(false)
        channel.lightColor = Color.GREEN
        channel.lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
        channel.description = description
        channel.setSound(null, null)
        notificationManager.createNotificationChannel(channel)
    }
}