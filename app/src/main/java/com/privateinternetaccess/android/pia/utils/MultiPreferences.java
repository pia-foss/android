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

package com.privateinternetaccess.android.pia.utils;

import android.content.ContentResolver;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static com.privateinternetaccess.android.pia.utils.MultiProvider.CODE_BOOLEAN;
import static com.privateinternetaccess.android.pia.utils.MultiProvider.CODE_INTEGER;
import static com.privateinternetaccess.android.pia.utils.MultiProvider.CODE_LONG;
import static com.privateinternetaccess.android.pia.utils.MultiProvider.CODE_PREFS;
import static com.privateinternetaccess.android.pia.utils.MultiProvider.CODE_REMOVE_KEY;
import static com.privateinternetaccess.android.pia.utils.MultiProvider.CODE_STRING;
import static com.privateinternetaccess.android.pia.utils.MultiProvider.createContentValues;
import static com.privateinternetaccess.android.pia.utils.MultiProvider.createQueryUri;
import static com.privateinternetaccess.android.pia.utils.MultiProvider.extractBooleanFromCursor;
import static com.privateinternetaccess.android.pia.utils.MultiProvider.extractIntFromCursor;
import static com.privateinternetaccess.android.pia.utils.MultiProvider.extractLongFromCursor;
import static com.privateinternetaccess.android.pia.utils.MultiProvider.extractStringFromCursor;
import static com.privateinternetaccess.android.pia.utils.MultiProvider.performQuery;

/**
 * Multi Preference class
 * <p>
 * - allows access to Shared Preferences across processes through a
 * Content Provider
 */
public class MultiPreferences {

    private ContentResolver resolver;
    private String mName;

    public MultiPreferences(String prefFileName, ContentResolver resolver) {
        this.mName = prefFileName;
        this.resolver = resolver;
    }

    public void setString(final String key, @NonNull final String value) {
        resolver.update(createQueryUri(mName, key, CODE_STRING), createContentValues(key, value), null, null);
    }

    @Nullable public String getString(final String key, final String defaultValue) {
        return extractStringFromCursor(performQuery(createQueryUri(mName, key, CODE_STRING), resolver), defaultValue);
    }

    public void setInt(final String key, final int value) {
        resolver.update(createQueryUri(mName, key, CODE_INTEGER), createContentValues(key, value), null, null);
    }

    public int getInt(final String key, final int defaultValue) {
        return extractIntFromCursor(performQuery(createQueryUri(mName, key, CODE_INTEGER), resolver), defaultValue);
    }

    public void setLong(final String key, final long value) {
        resolver.update(createQueryUri(mName, key, CODE_LONG), createContentValues(key, value), null, null);
    }

    public long getLong(final String key, final long defaultValue) {
        return extractLongFromCursor(performQuery(createQueryUri(mName, key, CODE_LONG), resolver), defaultValue);
    }

    public void setBoolean(final String key, final boolean value) {
        resolver.update(createQueryUri(mName, key, CODE_BOOLEAN), createContentValues(key, value), null, null);
    }

    public boolean getBoolean(final String key, final boolean defaultValue) {
        return extractBooleanFromCursor(performQuery(createQueryUri(mName, key, CODE_BOOLEAN), resolver), defaultValue);
    }

    public void removePreference(final String key) {
        resolver.delete(createQueryUri(mName, key, CODE_REMOVE_KEY), null, null);
    }

    public void clearPreferences() {
        resolver.delete(createQueryUri(mName, "", CODE_PREFS), null, null);
    }
}