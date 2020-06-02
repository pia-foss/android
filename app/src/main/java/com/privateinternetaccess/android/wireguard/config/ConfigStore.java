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

package com.privateinternetaccess.android.wireguard.config;

import com.privateinternetaccess.android.wireguard.config.Config;

import java.util.Set;

/**
 * Interface for persistent storage providers for WireGuard configurations.
 */

public interface ConfigStore {
    /**
     * Create a persistent tunnel, which must have a unique name within the persistent storage
     * medium.
     *
     * @param name   The name of the tunnel to create.
     * @param config Configuration for the new tunnel.
     * @return The configuration that was actually saved to persistent storage.
     */
    Config create(final String name, final Config config) throws Exception;

    /**
     * Delete a persistent tunnel.
     *
     * @param name The name of the tunnel to delete.
     */
    void delete(final String name) throws Exception;

    /**
     * Enumerate the names of tunnels present in persistent storage.
     *
     * @return The set of present tunnel names.
     */
    Set<String> enumerate();

    /**
     * Load the configuration for the tunnel given by {@code name}.
     *
     * @param name The identifier for the configuration in persistent storage (i.e. the name of the
     *             tunnel).
     * @return An in-memory representation of the configuration loaded from persistent storage.
     */
    Config load(final String name) throws Exception;

    /**
     * Rename the configuration for the tunnel given by {@code name}.
     *
     * @param name        The identifier for the existing configuration in persistent storage.
     * @param replacement The new identifier for the configuration in persistent storage.
     */
    void rename(String name, String replacement) throws Exception;

    /**
     * Save the configuration for an existing tunnel given by {@code name}.
     *
     * @param name   The identifier for the configuration in persistent storage (i.e. the name of
     *               the tunnel).
     * @param config An updated configuration object for the tunnel.
     * @return The configuration that was actually saved to persistent storage.
     */
    Config save(final String name, final Config config) throws Exception;
}
