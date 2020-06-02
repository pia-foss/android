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

import java.net.Inet4Address;
import java.net.InetAddress;

/**
 * An Internet network, denoted by its address and netmask
 * <p>
 * Instances of this class are immutable.
 */
public final class InetNetwork {
    private final InetAddress address;
    private final int mask;

    private InetNetwork(final InetAddress address, final int mask) {
        this.address = address;
        this.mask = mask;
    }

    public static InetNetwork parse(final String network) throws ParseException {
        final int slash = network.lastIndexOf('/');
        final String maskString;
        final int rawMask;
        final String rawAddress;
        if (slash >= 0) {
            maskString = network.substring(slash + 1);
            try {
                rawMask = Integer.parseInt(maskString, 10);
            } catch (final NumberFormatException ignored) {
                throw new ParseException(Integer.class, maskString);
            }
            rawAddress = network.substring(0, slash);
        } else {
            maskString = "";
            rawMask = -1;
            rawAddress = network;
        }
        final InetAddress address = InetAddresses.parse(rawAddress);
        final int maxMask = (address instanceof Inet4Address) ? 32 : 128;
        if (rawMask > maxMask)
            throw new ParseException(InetNetwork.class, maskString, "Invalid network mask");
        final int mask = rawMask >= 0 && rawMask <= maxMask ? rawMask : maxMask;
        return new InetNetwork(address, mask);
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof InetNetwork))
            return false;
        final InetNetwork other = (InetNetwork) obj;
        return address.equals(other.address) && mask == other.mask;
    }

    public InetAddress getAddress() {
        return address;
    }

    public int getMask() {
        return mask;
    }

    @Override
    public int hashCode() {
        return address.hashCode() ^ mask;
    }

    @Override
    public String toString() {
        return address.getHostAddress() + '/' + mask;
    }
}
