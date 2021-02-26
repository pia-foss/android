package com.privateinternetaccess.android.handlers;

import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.NotificationCompat;

import com.privateinternetaccess.android.BuildConfig;
import com.privateinternetaccess.android.R;
import com.privateinternetaccess.android.pia.handlers.PiaPrefHandler;
import com.privateinternetaccess.android.pia.model.response.CheckUpdateResponse;
import com.privateinternetaccess.android.pia.tasks.CheckUpdateTask;
import com.privateinternetaccess.android.pia.utils.Prefs;
import com.privateinternetaccess.android.receivers.UpdateCheckReceiver;
import com.privateinternetaccess.android.ui.notifications.PIANotifications;
import com.privateinternetaccess.core.utils.IPIACallback;

public class UpdateHandler {

    public enum UpdateDisplayType{
        SHOW_NOTIFICATION,
        SHOW_DIALOG
    };

    private static String DOWNLOAD_URL = "https://www.privateinternetaccess.com/installer/download_installer_android";

    private static String UPDATE_NOTIFICATION_CHANNEL = "pia_update_channel";
    private static int UPDATE_NOTIFICATION_ID = 1235;
    private static int LAUNCH_UPDATE_ID = 12356;

    private static String PREF_TRY_LATER = "retry_later";
    private static String PREF_UPDATE_RETRY = "retry_update_count";
    private static String PREF_LAST_VERSION = "last_update_version";

    private static int RETRY_AT_COUNT = 5;

    public static void checkUpdates(final Context context, final UpdateDisplayType displayType) {
        CheckUpdateTask updateTask = new CheckUpdateTask(context);

        updateTask.setCallback(new IPIACallback<CheckUpdateResponse>() {
            @Override
            public void apiReturn(CheckUpdateResponse checkUpdateResponse) {
                if (context == null)
                    return;

                if (displayType == UpdateDisplayType.SHOW_DIALOG) {
                    if (shouldShowUpdateDialog(context, checkUpdateResponse.getVersionCode())) {
                        showDialog(context);
                    }
                }
                else if (displayType == UpdateDisplayType.SHOW_NOTIFICATION) {
                    if (shouldShowUpdateNotification(context, checkUpdateResponse.getVersionCode())) {
                        showNotification(context);
                    }
                }
            }
        });
        updateTask.execute();

        UpdateCheckReceiver.setupUpdateCheck(context);
    }

    public static void showDialog(final Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder
                .setMessage(R.string.updater_notification_body)
                .setTitle(R.string.updater_notification_title)
                .setPositiveButton(R.string.updater_alert_confirm, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(DOWNLOAD_URL));
                        context.startActivity(browserIntent);
                    }
                })
                .setNegativeButton(R.string.updater_alert_dismiss, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Prefs.with(context).set(PREF_TRY_LATER, true);
                        Prefs.with(context).set(PREF_UPDATE_RETRY, 0);
                    }
                });

        builder.create();
        builder.show();
    }

    public static void showNotification(final Context context) {
        String title = context.getResources().getString(R.string.updater_notification_title);
        String message = context.getResources().getString(R.string.updater_notification_body);
        String download = context.getResources().getString(R.string.updater_alert_confirm);

        Intent updateIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(DOWNLOAD_URL));
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                LAUNCH_UPDATE_ID,
                updateIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PIANotifications.Companion.getSharedInstance().createNotificationChannel(
                    context,
                    UPDATE_NOTIFICATION_CHANNEL,
                    context.getResources().getString(R.string.pia_update_channel_title),
                    context.getResources().getString(R.string.pia_update_channel_description)
            );
        }

        PIANotifications.Companion.getSharedInstance().showNotification(
                context,
                UPDATE_NOTIFICATION_ID,
                UPDATE_NOTIFICATION_CHANNEL,
                title,
                message,
                R.drawable.ic_update,
                new NotificationCompat.Action(R.drawable.ic_download_white, download, pendingIntent)
        );
    }

    private static boolean shouldShowUpdateDialog(Context context, int nextVersionCode) {
        if (isNewVersion(context, nextVersionCode)) {
            if (BuildConfig.FLAVOR_pia.equals("qa") &&
                    PiaPrefHandler.getUpdaterTesting(context)) {
                if (Prefs.with(context).get(PiaPrefHandler.TESTING_UPDATER_SHOW_DIALOG, false)) {
                    return true;
                }
            }

            if (Prefs.with(context).get(PREF_TRY_LATER, false) &&
                    Prefs.with(context).get(PREF_UPDATE_RETRY, 0) < RETRY_AT_COUNT) {
                int count = Prefs.with(context).get(PREF_UPDATE_RETRY, 0) + 1;
                Prefs.with(context).set(PREF_UPDATE_RETRY, count);

                return false;
            }

            return true;
        }
        else {
            Prefs.with(context).set(PREF_TRY_LATER, false);
            Prefs.with(context).set(PREF_UPDATE_RETRY, 0);
        }

        return false;
    }

    private static boolean shouldShowUpdateNotification(Context context, int nextVersion) {
        if (isNewVersion(context, nextVersion)) {
            if (BuildConfig.FLAVOR_pia.equals("qa") &&
                    PiaPrefHandler.getUpdaterTesting(context)) {
                if (Prefs.with(context).get(PiaPrefHandler.TESTING_UPDATER_SHOW_NOTIFICATION, false)) {
                    return true;
                }
            }

            Prefs prefs = Prefs.with(context);

            if (prefs.get(PREF_LAST_VERSION, 0) == nextVersion) {
                return false;
            }
            else {
                prefs.set(PREF_LAST_VERSION, nextVersion);
                return true;
            }
        }

        return false;
    }

    private static boolean isNewVersion(Context context, int nextVersionCode) {
        int versionCode = BuildConfig.VERSION_CODE;

        if (BuildConfig.FLAVOR_pia.equals("qa") &&
                PiaPrefHandler.getUpdaterTesting(context) &&
                Prefs.with(context).get(PiaPrefHandler.TESTING_UPDATER_BUILD, 0) != 0) {
            versionCode = Prefs.with(context).get(PiaPrefHandler.TESTING_UPDATER_BUILD, versionCode);
        }

        if (versionCode < nextVersionCode) {
            return true;
        }

        return false;
    }
}