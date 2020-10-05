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

package com.privateinternetaccess.android.ui.drawer;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;

import com.google.android.material.appbar.AppBarLayout;
import com.privateinternetaccess.android.PIAApplication;
import com.privateinternetaccess.android.R;
import com.privateinternetaccess.android.model.events.SeverListUpdateEvent;
import com.privateinternetaccess.android.pia.handlers.PIAServerHandler;
import com.privateinternetaccess.android.pia.utils.DLog;
import com.privateinternetaccess.android.ui.connection.MainActivity;
import com.privateinternetaccess.android.ui.superclasses.BaseActivity;

import org.greenrobot.eventbus.Subscribe;

/**
 * Created by half47 on 8/3/16.
 */
public class SettingsActivity extends BaseActivity {

    private static final String TAG = "SettingsActivity";
    private static boolean changedTheme;
    private SettingsFragment fragment;
    private AppBarLayout appBar;
    private View loadingView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(PIAApplication.isAndroidTV(this) ?
                R.layout.activity_tv_secondary : R.layout.activity_secondary);

        if (!PIAApplication.isAndroidTV(this)) {
            initHeader(true, true);
            setTitle(getString(R.string.menu_settings));
            setBackground();
            setSecondaryGreenBackground();
            appBar = findViewById(R.id.appbar);
            loadingView = findViewById(R.id.activity_settings_loading);
            updateUiForGen4FetchingState(PIAServerHandler.getServerListFetchState());
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home){
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_DPAD_LEFT){
            onBackPressed();
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Fragment frag = getSupportFragmentManager().findFragmentById(R.id.activity_secondary_container);
        if(fragment == null){
            fragment = new SettingsFragment();
            getSupportFragmentManager().beginTransaction().replace(R.id.activity_secondary_container, fragment).commit();
        } else {
            if(frag instanceof SettingsFragment)
                fragment = (SettingsFragment) getSupportFragmentManager().findFragmentById(R.id.activity_secondary_container);
        }
    }

    @Override
    public void onBackPressed() {
        SeverListUpdateEvent.ServerListUpdateState state = PIAServerHandler.getServerListFetchState();
        boolean shouldIgnoreBackPress = false;
        switch (state) {
            case STARTED:
                shouldIgnoreBackPress = true;
                break;
            case FETCH_SERVERS_FINISHED:
            case GEN4_PING_SERVERS_FINISHED:
                break;
        }

        if (shouldIgnoreBackPress) {
            DLog.d(TAG, "Ignoring onBackPressed as we are updating the core list of servers");
            return;
        }

        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            setTitle(R.string.menu_settings);
            getSupportFragmentManager().popBackStack();
        } else {
            if(changedTheme)
                setResult(MainActivity.THEME_CHANGED);
            else
                setResult(RESULT_OK);
            finish();
            overridePendingTransition(R.anim.right_to_left_exit, R.anim.left_to_right_exit);
        }
    }

    public void setChangedTheme(boolean changed) {
        changedTheme = changed;
    }

    public void showHideActionBar(boolean show){
        appBar.setExpanded(show);
    }

    @Subscribe
    public void serverListUpdateEvent(SeverListUpdateEvent event) {
        updateUiForGen4FetchingState(event.getState());
    }

    private void updateUiForGen4FetchingState(SeverListUpdateEvent.ServerListUpdateState state) {
        if (loadingView == null) {
            return;
        }

        int uiOptions = View.SYSTEM_UI_FLAG_VISIBLE;
        switch (state) {
            case STARTED:
                uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
                loadingView.setVisibility(View.VISIBLE);
                break;
            case FETCH_SERVERS_FINISHED:
            case GEN4_PING_SERVERS_FINISHED:
                loadingView.setVisibility(View.GONE);
                break;
        }
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(uiOptions);
    }
}