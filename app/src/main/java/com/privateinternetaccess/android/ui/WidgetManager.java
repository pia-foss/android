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

package com.privateinternetaccess.android.ui;

import android.content.Context;
import android.view.View;

import com.privateinternetaccess.android.pia.handlers.PiaPrefHandler;
import com.privateinternetaccess.android.ui.tv.views.IPPortView;
import com.privateinternetaccess.android.ui.views.ConnectionSlider;
import com.privateinternetaccess.android.ui.views.ConnectionView;
import com.privateinternetaccess.android.ui.views.InAppMessageView;
import com.privateinternetaccess.android.ui.views.QuickConnectView;
import com.privateinternetaccess.android.ui.views.QuickSettingsView;
import com.privateinternetaccess.android.ui.views.ServerSelectionView;
import com.privateinternetaccess.android.ui.views.SnoozeView;
import com.privateinternetaccess.android.ui.views.UsageView;

import java.util.ArrayList;
import java.util.List;

public class WidgetManager {

    public enum WidgetType {
        WIDGET_CONNECTION,
        WIDGET_SERVER,
        WIDGET_IP,
        WIDGET_PERFORMANCE,
        WIDGET_SNOOZE,
        WIDGET_QUICK_SETTINGS,
        WIDGET_QUICK_CONNECT,
        WIDGET_USAGE,
        WIDGET_CONNECTION_INFO,
        WIDGET_IN_APP_MESSAGE
    }

    private Context mContext;
    private List<WidgetItem> widgetList;

    public WidgetManager(Context context) {
        mContext = context;
        widgetList = new ArrayList<>();
    }

    public static View getView(Context context, WidgetType type) {
        switch (type) {
            case WIDGET_CONNECTION:
                return new ConnectionSlider(context);
            case WIDGET_IN_APP_MESSAGE:
                return new InAppMessageView(context);
            case WIDGET_SERVER:
                return new ServerSelectionView(context);
            case WIDGET_IP:
                return new IPPortView(context);
            case WIDGET_PERFORMANCE:
                return null;
            case WIDGET_QUICK_SETTINGS:
                return new QuickSettingsView(context);
            case WIDGET_QUICK_CONNECT:
                return new QuickConnectView(context);
            case WIDGET_SNOOZE:
                return new SnoozeView(context);
            case WIDGET_USAGE:
                return new UsageView(context);
            case WIDGET_CONNECTION_INFO:
                return new ConnectionView(context);
            default:
                return null;
        }
    }

    public List<WidgetItem> getWidgets(boolean isOrganizing) {
        getWidgets();

        List<WidgetItem> items = new ArrayList<>();

        for (int i = 0; i < widgetList.size(); i++) {
            WidgetItem item = widgetList.get(i);

            if (getView(mContext, item.widgetType) == null) {
                continue;
            }
            else if (isOrganizing && item.widgetType != WidgetType.WIDGET_CONNECTION && item.widgetType != WidgetType.WIDGET_IN_APP_MESSAGE) {
                items.add(item);
            }
            else if (!isOrganizing && item.isVisible) {
                items.add(item);
            }
        }

        return items;
    }

    private List<WidgetItem> createDefaultList() {
        List<WidgetItem> defaultList = new ArrayList<>();
        defaultList.add(new WidgetItem(WidgetType.WIDGET_CONNECTION, true));
        defaultList.add(new WidgetItem(WidgetType.WIDGET_IN_APP_MESSAGE, true));
        defaultList.add(new WidgetItem(WidgetType.WIDGET_SERVER, true));
        defaultList.add(new WidgetItem(WidgetType.WIDGET_IP, true));
        defaultList.add(new WidgetItem(WidgetType.WIDGET_PERFORMANCE, true));
        defaultList.add(new WidgetItem(WidgetType.WIDGET_QUICK_SETTINGS, true));
        defaultList.add(new WidgetItem(WidgetType.WIDGET_QUICK_CONNECT, true));
        defaultList.add(new WidgetItem(WidgetType.WIDGET_SNOOZE, true));
        defaultList.add(new WidgetItem(WidgetType.WIDGET_USAGE, true));
        defaultList.add(new WidgetItem(WidgetType.WIDGET_CONNECTION_INFO, true));

        return defaultList;
    }

    private void getWidgets() {
        List<String> serializedItems = PiaPrefHandler.getWidgetOrder(mContext);
        List<WidgetItem> defaultList = createDefaultList();
        widgetList.clear();

        if (serializedItems.size() == 0) {
            widgetList.addAll(defaultList);
        }
        else {
            for (int i = 0; i < serializedItems.size(); i++) {
                String[] fields = serializedItems.get(i).split(" ");
                WidgetItem item = new WidgetItem(WidgetType.values()[Integer.parseInt(fields[0])], Boolean.parseBoolean(fields[1]));

                widgetList.add(item);
            }

            for (WidgetItem item : defaultList) {
                boolean itemExists = false;

                for (WidgetItem oldItem : widgetList) {
                    if (oldItem.widgetType == item.widgetType)
                        itemExists = true;
                }

                if (!itemExists && item.widgetType == WidgetType.WIDGET_IN_APP_MESSAGE) {
                    widgetList.add(1 ,item);
                }
                else if (!itemExists) {
                    widgetList.add(item);
                }
            }
        }
    }

    public void saveWidgets(List<WidgetItem> items) {
        List<WidgetItem> savedCopy = new ArrayList<>(items);
        savedCopy.add(0, new WidgetItem(WidgetType.WIDGET_CONNECTION, true));

        List<String> serializedItems = new ArrayList<>();
        for (int i = 0; i < savedCopy.size(); i++) {
            WidgetItem item = savedCopy.get(i);
            serializedItems.add(String.format("%d %b", item.widgetType.ordinal(), item.isVisible));
        }

        PiaPrefHandler.saveWidgetOrder(mContext, serializedItems);
    }

    public class WidgetItem {
        public WidgetType widgetType;
        public boolean isVisible;

        public WidgetItem(WidgetType type, boolean visible) {
            widgetType = type;
            isVisible = visible;
        }
    }
}
