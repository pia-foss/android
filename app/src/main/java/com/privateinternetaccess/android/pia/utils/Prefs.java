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

import android.content.Context;
import android.content.SharedPreferences;

import com.privateinternetaccess.android.pia.handlers.PiaPrefHandler;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by half47 on 6/9/16.
 *
 * quick method to handle preferences
 */
public class Prefs {

    private MultiPreferences preferences;
    private SharedPreferences oldPreferences;


    /**
     * Sets up using the pref file PiaServerDatabase.PREFNAME
     */
    public Prefs(Context context){
        setUp(context, PiaPrefHandler.PREFNAME);
    }

    /**
     * Sets up using the pref file name you sent in filename
     *
     */
    public Prefs(Context context, String fileName){
        setUp(context, fileName);
    }

    private void setUp(Context context, String fileName){
        preferences = new MultiPreferences(fileName, context.getContentResolver());
        oldPreferences = context.getSharedPreferences(fileName, 0);
    }

    /**
     * quick factory method.
     *
     */
    public static Prefs with(Context context){
        return new Prefs(context);
    }

    /**
     * quick factory method with added ability to change file name.
     *
     */
    public static Prefs with(Context context, String fileName){
        return new Prefs(context, fileName);
    }

    /**
     * remove any item from stored preferences
     *
     */
    public void remove(String key){
        preferences.removePreference(key);
    }

    // getters with defaults

    public String get(String key, String defaultValue){
        return preferences.getString(key, defaultValue);
    }

    public boolean get(String key, boolean defaultValue){
        return preferences.getBoolean(key, defaultValue);
    }

    public int get(String key, int defaultValue){
        return preferences.getInt(key, defaultValue);
    }

    public long get(String key, long defaultValue){
        return preferences.getLong(key, defaultValue);
    }

    public Set<String> get(String key, Set<String> defaultValue){
        return oldPreferences.getStringSet(key, defaultValue);
    }

    // quick getters

    /**
     *
     * @return string value or null
     */
    public String getString(String key){
        String defaultValue = null;
        return get(key, defaultValue);
    }

    /**
     *
     * @return int value or 0
     */
    public int getInt(String key){
        return get(key, 0);
    }

    /**
     *
     * @return boolean value or false
     */
    public boolean getBoolean(String key){
        return get(key, false);
    }

    /**
     *
     * @return long value or 0l
     */
    public long getLong(String key){
        return get(key, 0L);
    }

    /**
     *
     * @return set or empty set
     */
    public Set<String> getStringSet(String key){
        return get(key, new HashSet<String>());
    }

    // Setters

    public void set(String key, String value){
        preferences.setString(key, value);
    }

    public void set(String key, boolean value){
        preferences.setBoolean(key, value);
    }

    public void set(String key, int value){
        preferences.setInt(key, value);
    }

    public void set(String key, long value){
        preferences.setLong(key, value);
    }

    public void set(String key, Set<String> value){
        SharedPreferences.Editor edit = oldPreferences.edit();
        edit.putStringSet(key, value);
        edit.apply();
    }
}
