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

package com.privateinternetaccess.android.ui.widgets;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.widget.RemoteViews;

import com.privateinternetaccess.android.R;
import com.privateinternetaccess.android.model.events.VPNTrafficDataPointEvent;
import com.privateinternetaccess.android.pia.model.events.VpnStateEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import de.blinkt.openvpn.core.ConnectionStatus;
import de.blinkt.openvpn.core.OpenVPNManagement;
import de.blinkt.openvpn.core.OpenVPNService;

/**
 * Created by half47 on 9/20/16.
 */

public class WidgetProvider extends WidgetBaseProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        final int N = appWidgetIds.length;
        this.context = context;
        // Perform this loop procedure for each App Widget that belongs to this provider
        for (int i=0; i<N; i++) {
            int appWidgetId = appWidgetIds[i];
            AppWidgetProviderInfo info = appWidgetManager.getAppWidgetInfo(appWidgetId);
            if(info != null) {
                int resId = info.initialLayout;

                RemoteViews views = new RemoteViews(context.getPackageName(), resId);

                setupView(views, this, resId);

                // Tell the AppWidgetManager to perform an update on the current app widget
                appWidgetManager.updateAppWidget(appWidgetId, views);
            }
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
        this.context = null;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        this.context = context;
        onReceiveBroadcast(intent, context, this);
    }

    @Subscribe
    public void updateState(VpnStateEvent event){
        updateWidget(context, false);
    }

    @Subscribe
    public void updateByteCount(VPNTrafficDataPointEvent event) {
        VpnStateEvent stateEvent = EventBus.getDefault().getStickyEvent(VpnStateEvent.class);
        if(context != null && (stateEvent != null && stateEvent.getLevel() == ConnectionStatus.LEVEL_CONNECTED)) {
            Resources res= context.getResources();

            String down =  String.format(context.getString(R.string.shorthand_bytecount),
                    OpenVPNService.humanReadableByteCount(event.getIn(), false ,res),
                    OpenVPNService.humanReadableByteCount(event.getDiffIn() / OpenVPNManagement.mBytecountInterval, true, res));
            String up = String.format(context.getString(R.string.shorthand_bytecount),
                    OpenVPNService.humanReadableByteCount(event.getOut(), false, res),
                    OpenVPNService.humanReadableByteCount(event.getDiffOut() / OpenVPNManagement.mBytecountInterval, true, res));
            if(lastUpSpeed == null) {
                lastUpSpeed = up;
                lastDownSpeed = down;
                updateCCWidget(context, false);
            } else {
                if(!lastUpSpeed.equals(up) || !lastDownSpeed.equals(down)) {
                    lastUpSpeed = up;
                    lastDownSpeed = down;
                    updateCCWidget(context, false);
                }
            }
        }
    }
}