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

import com.privateinternetaccess.android.PIAKillSwitchStatus;
import com.privateinternetaccess.android.model.events.VPNTrafficDataPointEvent;
import com.privateinternetaccess.android.pia.model.events.KillSwitchEvent;
import com.privateinternetaccess.android.pia.model.events.VpnStateEvent;

import org.greenrobot.eventbus.EventBus;

import de.blinkt.openvpn.core.ConnectionStatus;
import de.blinkt.openvpn.core.LogItem;
import de.blinkt.openvpn.core.VpnStatus;

/**
 * Removes the need for multiple listeners and callbacks and allows the use of only the Eventbus.
 *
 * Created by arne on 16.11.17.
 */

// Item commented can be reused when we switch to EventBus again

public class EventBusBridge implements PIAKillSwitchStatus.KillSwitchStateListener, VpnStatus.ByteCountListener, VpnStatus.StateListener, VpnStatus.LogListener {
    public void init()
    {
        VpnStatus.addLogListener(this);
        VpnStatus.addStateListener(this);
        VpnStatus.addByteCountListener(this);
        PIAKillSwitchStatus.addKillSwitchListener(this);
    }

    void deinit()
    {
        VpnStatus.removeLogListener(this);
        VpnStatus.removeStateListener(this);
        VpnStatus.removeByteCountListener(this);
        PIAKillSwitchStatus.removeKillSwitchListener(this);
    }

    @Override
    public void killSwitchUpdate(boolean isInKillSwitch) {
        EventBus.getDefault().postSticky(new KillSwitchEvent(isInKillSwitch));
    }

    @Override
    public void updateByteCount(long in, long out, long diffIn, long diffOut) {
        EventBus.getDefault().postSticky(new VPNTrafficDataPointEvent(in, out, diffIn, diffOut));
    }

    @Override
    public void updateState(String state, String logmessage, int localizedResId, ConnectionStatus level) {
        EventBus.getDefault().postSticky(new VpnStateEvent(state, logmessage, localizedResId, level));
    }

    @Override
    public void setConnectedVPN(String uuid) {
        // Pia does not need this, there is only one VPN
    }

    @Override
    public void newLog(LogItem logItem) {
//        EventBus.getDefault().post(logItem);
    }
}
