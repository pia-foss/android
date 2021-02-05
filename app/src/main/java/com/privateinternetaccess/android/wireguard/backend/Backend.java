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

package com.privateinternetaccess.android.wireguard.backend;

import com.privateinternetaccess.android.pia.providers.VPNFallbackEndpointProvider;
import com.privateinternetaccess.android.wireguard.model.Tunnel;
import com.privateinternetaccess.android.wireguard.model.Tunnel.Statistics;
import java.util.Set;


/**
 * Interface for implementations of the WireGuard secure network tunnel.
 */
public interface Backend {
    /**
     * Enumerate names of currently-running tunnels.
     *
     * @return The set of running tunnel names.
     */
    Set<String> getRunningTunnelNames();

    /**
     * Get the state of a tunnel.
     *
     * @param tunnel The tunnel to examine the state of.
     * @return The state of the tunnel.
     */
    Tunnel.State getState(Tunnel tunnel) throws Exception;

    /**
     * Get statistics about traffic and errors on this tunnel. If the tunnel is not running, the
     * statistics object will be filled with zero values.
     *
     * @param tunnel The tunnel to retrieve statistics for.
     * @return The statistics for the tunnel.
     */
    Statistics getStatistics(Tunnel tunnel) throws Exception;

    /**
     * Determine version of underlying backend.
     *
     * @return The version of the backend.
     * @throws Exception
     */
    String getVersion() throws Exception;

    /**
     * Set the state of a tunnel, updating it's configuration. If the tunnel is already up, config
     * may update the running configuration; config may be null when setting the tunnel down.
     *
     * @param endpoint The endpoint data we are currently working on.
     * @param tunnel The tunnel to control the state of.
     * @param state  The new state for this tunnel. Must be {@code UP}, {@code DOWN}, or
     *               {@code TOGGLE}.
     * @return The updated state of the tunnel.
     */
    Tunnel.State setState(
            VPNFallbackEndpointProvider.VPNEndpoint endpoint,
            Tunnel tunnel,
            Tunnel.State state
    ) throws Exception;
}
