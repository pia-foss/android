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

/**
 */
public class ParseException extends Exception {
    private final Class<?> parsingClass;
    private final CharSequence text;

    public ParseException(final Class<?> parsingClass, final CharSequence text,
                          @Nullable final String message, @Nullable final Throwable cause) {
        super(message, cause);
        this.parsingClass = parsingClass;
        this.text = text;
    }

    public ParseException(final Class<?> parsingClass, final CharSequence text,
                          @Nullable final String message) {
        this(parsingClass, text, message, null);
    }

    public ParseException(final Class<?> parsingClass, final CharSequence text,
                          @Nullable final Throwable cause) {
        this(parsingClass, text, null, cause);
    }

    public ParseException(final Class<?> parsingClass, final CharSequence text) {
        this(parsingClass, text, null, null);
    }

    public Class<?> getParsingClass() {
        return parsingClass;
    }

    public CharSequence getText() {
        return text;
    }
}
