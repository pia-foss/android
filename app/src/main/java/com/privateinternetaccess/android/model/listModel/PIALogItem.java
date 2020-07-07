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

package com.privateinternetaccess.android.model.listModel;

import android.content.Context;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import de.blinkt.openvpn.core.LogItem;

public class PIALogItem {
    public String timeString;
    public String message;

    public PIALogItem(LogItem ovpnItem, Context context) {
        message = ovpnItem.getString(context).trim();
        timeString = getTime(ovpnItem);
    }

    public PIALogItem(String message, String time) {
        this.message = message;
        this.timeString = time;
    }

    private String getTime(LogItem le) {
        Date d = new Date(le.getLogtime());
        java.text.DateFormat timeformat;
        timeformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return timeformat.format(d) + " ";
    }
}
