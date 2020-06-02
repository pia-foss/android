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

public final class BackendException extends Exception {
    private final Object[] format;
    private final Reason reason;

    public BackendException(final Reason reason, final Object... format) {
        this.reason = reason;
        this.format = format;
    }

    public Object[] getFormat() {
        return format;
    }

    public Reason getReason() {
        return reason;
    }

    public enum Reason {
        UNKNOWN_KERNEL_MODULE_NAME,
        WG_QUICK_CONFIG_ERROR_CODE,
        TUNNEL_MISSING_CONFIG,
        VPN_NOT_AUTHORIZED,
        UNABLE_TO_START_VPN,
        TUN_CREATION_ERROR,
        GO_ACTIVATION_ERROR_CODE
    }
}
