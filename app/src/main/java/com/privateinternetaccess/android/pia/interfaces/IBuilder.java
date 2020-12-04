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

package com.privateinternetaccess.android.pia.interfaces;

import com.privateinternetaccess.android.PIACallbacks;

import java.io.File;

import de.blinkt.openvpn.core.VPNNotifications;
import de.blinkt.openvpn.core.VpnStatus;

/**
 * Interface class for {@link com.privateinternetaccess.android.pia.PIABuilder}
 *
 * Created by hfrede on 11/22/17.
 */

public interface IBuilder {

    /**
     * init the vpn library required notifications and callbacks.
     *
     * @param notifications
     * @param callbacks
     * @param listener - Default to use is the {@link com.privateinternetaccess.android.pia.connection.ConnectionResponder}
     * @return this
     */
    IBuilder initVPNLibrary(VPNNotifications notifications, PIACallbacks callbacks, VpnStatus.StateListener listener);

    /**
     * Creates a notification channel with a channel name and channel description.
     *
     *
     * @param channelName
     * @param channelDescription
     * @return
     */
    IBuilder createNotificationChannel(String channelName, String channelDescription);

    /**
     * inits the tile service for pia application.
     *
     * @return IBuilder
     */
    IBuilder enabledTileService();

    /**
     * sets {@link com.privateinternetaccess.android.pia.utils.DLog} peremeters so that it will log, log to a file or log only certain things to a file.
     *
     * @param debugMode - whether or not to save to a file.
     * @param debugLevel - what debug level and above to save to a file.
     * @param filesDir - file to place the dlog file
     * @return this
     */
    IBuilder setDebugParameters(boolean debugMode, int debugLevel, File filesDir);

    /**
     * sets {@link com.privateinternetaccess.android.pia.utils.DLog} peremeters so that it will log
     *
     * @param logging - the current flavor of the app compared to the release flavor of the app.
     * @return this
     */
    IBuilder enableLogging(boolean logging);
}