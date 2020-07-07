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

import com.privateinternetaccess.android.pia.model.events.PortForwardEvent;
import com.privateinternetaccess.android.pia.utils.DLog;
import com.privateinternetaccess.core.model.PIAServer;
import com.privateinternetaccess.core.utils.IPIACallback;

import org.greenrobot.eventbus.EventBus;

/**
 * Convenience class for port forwarding and last server information.
 *
 * Created by hfrede on 10/4/17.
 */

public class PIAVpnStatus {

    protected static PIAServer mLastConnectedRegion;

    public static void setLastConnectedRegion(PIAServer region) {
        mLastConnectedRegion = region;
    }

    public static PIAServer getLastConnectedRegion() {
        return mLastConnectedRegion;
    }

    protected static IPIACallback<PortForwardEvent> callback;

    public static void setCallback(IPIACallback<PortForwardEvent> callback2) {
        callback = callback2;
    }

    protected static PortForwardingStatus mLastFwdStatus = PortForwardingStatus.NO_PORTFWD;
    protected static String mLastFwdStatusArg = null;

    synchronized public static void setPortForwardingStatus(PortForwardingStatus status, String arg) {
        mLastFwdStatus = status;
        mLastFwdStatusArg = arg;
        DLog.d("Portforward", " status = " + status + " message = " + arg);
        EventBus.getDefault().postSticky(new PortForwardEvent(status, arg));
        if (callback != null)
            callback.apiReturn(new PortForwardEvent(status, arg));
    }

    public static void clearOldData() {
        mLastFwdStatus = null;
        mLastFwdStatus = PortForwardingStatus.NO_PORTFWD;
    }

    public static String getFwdArgument() {
        return mLastFwdStatusArg;
    }

    public static String getFwdStatus() {
        return mLastFwdStatus.name();
    }
}