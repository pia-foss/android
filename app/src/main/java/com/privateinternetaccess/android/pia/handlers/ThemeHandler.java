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

package com.privateinternetaccess.android.pia.handlers;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.privateinternetaccess.android.PIAApplication;
import com.privateinternetaccess.android.pia.utils.Prefs;

/**
 * Helps the app know what theme state it is in and make sure the app is in the correct state.
 *
 * Created by hfrede on 1/22/18.
 */

public class ThemeHandler {

    public static String PREF_THEME = "darktheme";

    /**
     * Gets the current theme of the app overall.
     *
     * @param context
     * @return
     */
    public static Theme getCurrentTheme(Context context){
        int currentNightMode = context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        Theme current = Theme.DAY;
        if(currentNightMode == Configuration.UI_MODE_NIGHT_YES)
            current = Theme.NIGHT;
        return current;
    }

    /**
     * Call this before onCreate to set the theme.
     *
     *
     * @param activity
     */
    public static void setTheme(AppCompatActivity activity){
        Theme theme = getPrefTheme(activity);
        int mode = AppCompatDelegate.MODE_NIGHT_NO;
        switch (theme){
            case NIGHT:
                mode = AppCompatDelegate.MODE_NIGHT_YES;
                break;
        }
        activity.getDelegate().setDefaultNightMode(mode);
    }

    public static int getThemeMode(AppCompatActivity activity) {
        Theme theme = getPrefTheme(activity);
        int mode = AppCompatDelegate.MODE_NIGHT_NO;
        switch (theme) {
            case NIGHT:
                mode = AppCompatDelegate.MODE_NIGHT_YES;
                break;
        }
        return mode;
    }

    /**
     * Use this to update the app theme
     *
     * @param activity
     */
    public static void updateActivityTheme(AppCompatActivity activity){
        Theme theme = getPrefTheme(activity);
        int mode = AppCompatDelegate.MODE_NIGHT_NO;
        switch (theme){
            case NIGHT:
                mode = AppCompatDelegate.MODE_NIGHT_YES;
                break;
        }
        activity.getDelegate().setLocalNightMode(mode);
        AppCompatDelegate.setDefaultNightMode(mode);
    }

    /**
     * Call this in application class.
     *
     * @param app
     */
    public static void setAppTheme(Application app){
        Theme theme = getPrefTheme(app);
        int mode = AppCompatDelegate.MODE_NIGHT_NO;
        switch (theme){
            case NIGHT:
                mode = AppCompatDelegate.MODE_NIGHT_YES;
                break;
        }
        AppCompatDelegate.setDefaultNightMode(mode);
    }

    public static Theme getPrefTheme(Context context){
        if (PIAApplication.isAndroidTV(context)) {
            return Theme.NIGHT;
        }

        boolean theme = Prefs.with(context).get(PREF_THEME, false);
        Theme t = Theme.DAY;
        if(theme)
            t = Theme.NIGHT;
        return t;
    }

    public enum Theme{
        DAY,
        NIGHT
//        AUTO
    }
}
