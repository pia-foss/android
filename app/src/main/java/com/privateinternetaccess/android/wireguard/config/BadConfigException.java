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

import androidx.annotation.Nullable;

import com.privateinternetaccess.android.wireguard.crypto.KeyFormatException;

public class BadConfigException extends Exception {
    private final Location location;
    private final Reason reason;
    private final Section section;
    @Nullable
    private final CharSequence text;

    private BadConfigException(final Section section, final Location location,
                               final Reason reason, @Nullable final CharSequence text,
                               @Nullable final Throwable cause) {
        super(cause);
        this.section = section;
        this.location = location;
        this.reason = reason;
        this.text = text;
    }

    public BadConfigException(final Section section, final Location location,
                              final Reason reason, @Nullable final CharSequence text) {
        this(section, location, reason, text, null);
    }

    public BadConfigException(final Section section, final Location location,
                              final KeyFormatException cause) {
        this(section, location, Reason.INVALID_KEY, null, cause);
    }

    public BadConfigException(final Section section, final Location location,
                              @Nullable final CharSequence text,
                              final NumberFormatException cause) {
        this(section, location, Reason.INVALID_NUMBER, text, cause);
    }

    public BadConfigException(final Section section, final Location location,
                              final ParseException cause) {
        this(section, location, Reason.INVALID_VALUE, cause.getText(), cause);
    }

    public Location getLocation() {
        return location;
    }

    public Reason getReason() {
        return reason;
    }

    public Section getSection() {
        return section;
    }

    @Nullable
    public CharSequence getText() {
        return text;
    }

    public enum Location {
        TOP_LEVEL(""),
        ADDRESS("Address"),
        ALLOWED_IPS("AllowedIPs"),
        DNS("DNS"),
        ENDPOINT("Endpoint"),
        EXCLUDED_APPLICATIONS("ExcludedApplications"),
        LISTEN_PORT("ListenPort"),
        MTU("MTU"),
        PERSISTENT_KEEPALIVE("PersistentKeepalive"),
        PRE_SHARED_KEY("PresharedKey"),
        PRIVATE_KEY("PrivateKey"),
        PUBLIC_KEY("PublicKey");

        private final String name;

        Location(final String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    public enum Reason {
        INVALID_KEY,
        INVALID_NUMBER,
        INVALID_VALUE,
        MISSING_ATTRIBUTE,
        MISSING_SECTION,
        MISSING_VALUE,
        SYNTAX_ERROR,
        UNKNOWN_ATTRIBUTE,
        UNKNOWN_SECTION
    }

    public enum Section {
        CONFIG("Config"),
        INTERFACE("Interface"),
        PEER("Peer");

        private final String name;

        Section(final String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
}
