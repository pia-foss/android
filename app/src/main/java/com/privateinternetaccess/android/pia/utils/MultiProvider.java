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

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Multi Preference provider class
 */
public class MultiProvider extends ContentProvider {

    private static final String PROVIDER_NAME = "com.privateinternetaccess.multipreferences.MultiProvider";

    private static final String URL_STRING = "content://" + PROVIDER_NAME + "/string/";
    private static final String URL_INT = "content://" + PROVIDER_NAME + "/integer/";
    private static final String URL_LONG = "content://" + PROVIDER_NAME + "/long/";
    private static final String URL_BOOLEAN = "content://" + PROVIDER_NAME + "/boolean/";
    private static final String URL_PREFERENCES = "content://" + PROVIDER_NAME + "/prefs/";
    private static final String URL_REMOVE = "content://" + PROVIDER_NAME + "/remove/";

    static final int CODE_STRING = 1;
    static final int CODE_INTEGER = 2;
    static final int CODE_LONG = 3;
    static final int CODE_BOOLEAN = 4;
    static final int CODE_PREFS = 5;
    static final int CODE_REMOVE_KEY = 6;
    static final String KEY = "key";
    static final String VALUE = "value";

    /**
     * Create UriMatcher to match all requests
     */
    private static final UriMatcher mUriMatcher;

    static {
        mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        // */* = wildcard  (name or file name / key)
        mUriMatcher.addURI(PROVIDER_NAME, "string/*/*", CODE_STRING);
        mUriMatcher.addURI(PROVIDER_NAME, "integer/*/*", CODE_INTEGER);
        mUriMatcher.addURI(PROVIDER_NAME, "long/*/*", CODE_LONG);
        mUriMatcher.addURI(PROVIDER_NAME, "boolean/*/*", CODE_BOOLEAN);
        mUriMatcher.addURI(PROVIDER_NAME, "prefs/*/", CODE_PREFS);
    }

    /**
     * Map to hold all current Inter actors with shared preferences
     */
    private Map<String, PreferenceInteractor> mPreferenceMap = new HashMap<>();

    @Override public boolean onCreate() {
        return true;
    }

    /**
     * Get a new Preference Interactor, or return a previously used Interactor
     *
     * @param preferenceName the name of the preference file
     * @return a new interactor, or current one in the map
     */
    PreferenceInteractor getPreferenceInteractor(String preferenceName) {
        if (mPreferenceMap.containsKey(preferenceName)) {
            return mPreferenceMap.get(preferenceName);
        } else {
            final PreferenceInteractor interactor = new PreferenceInteractor(getContext(), preferenceName);
            mPreferenceMap.put(preferenceName, interactor);
            return interactor;
        }
    }

    @Nullable @Override public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        final PreferenceInteractor interactor = getPreferenceInteractor(uri.getPathSegments().get(1));

        switch (mUriMatcher.match(uri)) {
            case CODE_STRING:
                final String s = uri.getPathSegments().get(2);
                return interactor.hasKey(s) ? preferenceToCursor(interactor.getString(s)) : null;
            case CODE_INTEGER:
                final String i = uri.getPathSegments().get(2);
                return interactor.hasKey(i) ? preferenceToCursor(interactor.getInt(i)) : null;
            case CODE_LONG:
                final String l = uri.getPathSegments().get(2);
                return interactor.hasKey(l) ? preferenceToCursor(interactor.getLong(l)) : null;
            case CODE_BOOLEAN:
                final String b = uri.getPathSegments().get(2);
                return interactor.hasKey(b) ? preferenceToCursor(interactor.getBoolean(b) ? 1 : 0) : null;
        }
        return null;
    }


    @Override public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        if (values != null) {
            final PreferenceInteractor interactor = getPreferenceInteractor(uri.getPathSegments().get(1));
            final String key = values.getAsString(KEY);

            switch (mUriMatcher.match(uri)) {
                case CODE_STRING:
                    final String s = values.getAsString(VALUE);
                    interactor.setString(key, s);
                    break;
                case CODE_INTEGER:
                    final int i = values.getAsInteger(VALUE);
                    interactor.setInt(key, i);
                    break;
                case CODE_LONG:
                    final long l = values.getAsLong(VALUE);
                    interactor.setLong(key, l);
                    break;
                case CODE_BOOLEAN:
                    final boolean b = values.getAsBoolean(VALUE);
                    interactor.setBoolean(key, b);
                    break;
            }

        } else {
            throw new IllegalArgumentException("Content Values are null!");
        }
        return 0;
    }

    @Override public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        final PreferenceInteractor interactor = getPreferenceInteractor(uri.getPathSegments().get(1));
        interactor.removePref(uri.getPathSegments().get(2));
        return 0;
    }

    @Nullable @Override public String getType(@NonNull Uri uri) {
        throw new UnsupportedOperationException("not supported");
    }

    @Nullable @Override public Uri insert(@NonNull Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("not supported");
    }

    static String extractStringFromCursor(Cursor cursor, String defaultVal) {
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                return cursor.getString(cursor.getColumnIndex(MultiProvider.VALUE));
            }
            cursor.close();
        }
        return defaultVal;
    }

    static int extractIntFromCursor(Cursor cursor, int defaultVal) {
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                return cursor.getInt(cursor.getColumnIndex(MultiProvider.VALUE));
            }
            cursor.close();
        }
        return defaultVal;
    }

    static long extractLongFromCursor(Cursor cursor, long defaultVal) {
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                return cursor.getLong(cursor.getColumnIndex(MultiProvider.VALUE));
            }
            cursor.close();
        }
        return defaultVal;
    }

    static boolean extractBooleanFromCursor(Cursor cursor, boolean defaultVal) {
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                return cursor.getInt(cursor.getColumnIndex(MultiProvider.VALUE)) == 1;
            }
            cursor.close();
        }
        return defaultVal;
    }

    static Uri createQueryUri(String prefFileName, String key, int prefType) {
        switch (prefType) {
            case CODE_STRING:
                return Uri.parse(URL_STRING + prefFileName + "/" + key);
            case CODE_INTEGER:
                return Uri.parse(URL_INT + prefFileName + "/" + key);
            case CODE_LONG:
                return Uri.parse(URL_LONG + prefFileName + "/" + key);
            case CODE_BOOLEAN:
                return Uri.parse(URL_BOOLEAN + prefFileName + "/" + key);
            case CODE_PREFS:
                return Uri.parse(URL_PREFERENCES + prefFileName + "/" + key);
            case CODE_REMOVE_KEY:
                return Uri.parse(URL_REMOVE + prefFileName + "/" + key);
            default:
                throw new IllegalArgumentException("Not Supported Type : " + prefType);
        }
    }

    static <T> ContentValues createContentValues(String key, T value) {
        final ContentValues contentValues = new ContentValues();
        contentValues.put(MultiProvider.KEY, key);

        if (value instanceof String) {
            contentValues.put(MultiProvider.VALUE, (String) value);
        } else if (value instanceof Integer) {
            contentValues.put(MultiProvider.VALUE, (Integer) value);
        } else if (value instanceof Long) {
            contentValues.put(MultiProvider.VALUE, (Long) value);
        } else if (value instanceof Boolean) {
            contentValues.put(MultiProvider.VALUE, (Boolean) value);
        } else {
            throw new IllegalArgumentException("Unsupported type " + value.getClass());
        }
        return contentValues;
    }

    @Nullable static Cursor performQuery(Uri uri, ContentResolver resolver) {
        return resolver.query(uri, null, null, null, null, null);
    }

    /**
     * Convert a value into a cursor object using a Matrix Cursor
     *
     * @param value the value to be converetd
     * @param <T>   generic object type
     * @return a Cursor object
     */
    private <T> MatrixCursor preferenceToCursor(T value) {
        final MatrixCursor matrixCursor = new MatrixCursor(new String[]{MultiProvider.VALUE}, 1);
        final MatrixCursor.RowBuilder builder = matrixCursor.newRow();
        builder.add(value);
        return matrixCursor;
    }
}