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

package com.privateinternetaccess.android.pia.services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import com.privateinternetaccess.android.BuildConfig;
import com.privateinternetaccess.android.R;
import com.privateinternetaccess.android.pia.PIAFactory;
import com.privateinternetaccess.android.pia.handlers.PiaPrefHandler;
import com.privateinternetaccess.android.pia.interfaces.IAccount;
import com.privateinternetaccess.android.pia.model.AccountInformation;
import com.privateinternetaccess.android.pia.utils.DLog;
import com.privateinternetaccess.android.ui.loginpurchasing.LoginPurchaseActivity;
import com.privateinternetaccess.android.ui.notifications.PIANotifications;

public class ExpiryNotificationService extends Service {
    private static final int SERVICE_NOTIFY_EXPIRY = 7231;
    private static final long DAY_MS = 24 * 3600 * 1000;
    private static final long MONTH_MS = 31 * DAY_MS;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        DLog.i("ExpiryNotificationService", "On Start Command");

        int iconId = R.drawable.ic_stat_pia_robot_white;
        String title = getString(R.string.expiry_notification_title);

        IAccount account = PIAFactory.getInstance().getAccount(this);
        AccountInformation accountInformation = account.persistedAccountInformation();

        // We should arm the timers for the next alert
        armReminders(this);

        if (accountInformation.getTimeLeftMs() > MONTH_MS && !BuildConfig.DEBUG) {
            // More than one moth left?! ignore and rearm timers
            return START_NOT_STICKY;
        }

        // If we have shown a reminder recently don't annoy the user
        if (!PiaPrefHandler.showExpiryNotifcation(this))
            return START_NOT_STICKY;

        String text = getString(R.string.expiry_notification_onnemonth);
        if (accountInformation.getTimeLeftMs() > 7 * DAY_MS)
            text = getString(R.string.expiry_notification_onnemonth);
        else if (accountInformation.getTimeLeftMs() > 3 * DAY_MS)
            // More than one day left
            text = getString(R.string.expiry_notification_oneweek);

        else if (accountInformation.getTimeLeftMs() > DAY_MS)
            // More than one day left
            text = getString(R.string.expirty_notification_threedays);
        else if (accountInformation.getTimeLeftMs() <= DAY_MS)
            text = getString(R.string.expirty_notification_oneday);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                new Intent(this, LoginPurchaseActivity.class),
                0
        );
        PIANotifications.Companion.getSharedInstance().showNotification(
                this,
                SERVICE_NOTIFY_EXPIRY,
                title,
                text,
                pendingIntent
        );

        PiaPrefHandler.setLastExpiryNotifcationShown(this);
        return START_NOT_STICKY;
    }

    public static void armReminders(Context c) {
        DLog.i("ExpiryNotificationService", "Arm Reminders");
        Intent serviceIntent = new Intent(c, ExpiryNotificationService.class);
        PendingIntent pIntent = PendingIntent.getService(c, SERVICE_NOTIFY_EXPIRY, serviceIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarmManager = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);

        // First cancel any armed event
        alarmManager.cancel(pIntent);

        IAccount account = PIAFactory.getInstance().getAccount(c);
        // Check if notifications should be displayed
        AccountInformation accountInformation = account.persistedAccountInformation();

        if (account.loggedIn() && accountInformation.getExpirationTime() > 0) {

            long timeLeft = accountInformation.getTimeLeftMs();
            long expirationTime = accountInformation.getExpirationTime();

            if (AccountInformation.PLAN_YEARLY.equals(accountInformation.getPlan()) && timeLeft > MONTH_MS) {
                long wakeTime = expirationTime - MONTH_MS;
                alarmManager.set(AlarmManager.RTC, wakeTime, pIntent);
            } else if (timeLeft > 7 * DAY_MS) {
                // Still more than a week left arm timer for one week expiry
                long wakeTime = expirationTime - 7 * DAY_MS;
                alarmManager.set(AlarmManager.RTC, wakeTime, pIntent);
            } else if (timeLeft > 3 * DAY_MS) {
                long wakeTime = expirationTime - 3 * DAY_MS;
                alarmManager.set(AlarmManager.RTC, wakeTime, pIntent);

            } else if (timeLeft > DAY_MS) {
                long wakeTime = expirationTime - DAY_MS;
                alarmManager.set(AlarmManager.RTC, wakeTime, pIntent);
            }
        }
    }
}
