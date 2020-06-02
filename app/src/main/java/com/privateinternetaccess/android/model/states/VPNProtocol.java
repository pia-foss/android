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

package com.privateinternetaccess.android.model.states;

import android.content.Context;

import com.privateinternetaccess.android.R;
import com.privateinternetaccess.android.pia.handlers.PiaPrefHandler;

public class VPNProtocol {

    public enum Protocol {
        OpenVPN,
        Wireguard
    }

    public static Protocol activeProtocol(Context context) {
        String protocols[] = context.getResources().getStringArray(R.array.protocol_options);
        String protocol = PiaPrefHandler.getProtocol(context);

        if (protocol.equals(protocols[0])) {
            return Protocol.OpenVPN;
        }
        else {
            return Protocol.Wireguard;
        }
    }
}
