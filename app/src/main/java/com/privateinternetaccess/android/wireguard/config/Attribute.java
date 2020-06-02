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

import android.text.TextUtils;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Attribute {
    private static final Pattern LINE_PATTERN = Pattern.compile("(\\w+)\\s*=\\s*([^\\s#][^#]*)");
    private static final Pattern LIST_SEPARATOR = Pattern.compile("\\s*,\\s*");

    private final String key;
    private final String value;

    private Attribute(final String key, final String value) {
        this.key = key;
        this.value = value;
    }

    public static String join(final Iterable<?> values) {
        return TextUtils.join(", ", values);
    }

    public static Optional<Attribute> parse(final CharSequence line) {
        final Matcher matcher = LINE_PATTERN.matcher(line);
        if (!matcher.matches())
            return Optional.empty();
        return Optional.of(new Attribute(matcher.group(1), matcher.group(2)));
    }

    public static String[] split(final CharSequence value) {
        return LIST_SEPARATOR.split(value);
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }
}
