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

package com.privateinternetaccess.android.ui.views;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.Switch;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.privateinternetaccess.android.R;
import com.privateinternetaccess.android.model.states.VPNProtocol;
import com.privateinternetaccess.android.pia.handlers.PiaPrefHandler;
import com.privateinternetaccess.android.ui.superclasses.BaseActivity;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;

public class QuickSettingsSettings extends BaseActivity {

    @BindView(R.id.snippet_quick_settings_browser_switch) Switch sBrowser;
    @BindView(R.id.snippet_quick_settings_killswitch_switch) Switch sKillswitch;
    @BindView(R.id.snippet_quick_settings_network_switch) Switch sNetwork;

    @BindView(R.id.snippet_quick_settings_killswitch_layout) ConstraintLayout lKillswitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_secondary);
        initHeader(true, true);
        setBackground();
        setSecondaryGreenBackground();

        addSnippetToView();

        ButterKnife.bind(this);

        showTopExtraArea();
    }

    private void addSnippetToView() {
        FrameLayout container = findViewById(R.id.activity_secondary_container);
        View view = getLayoutInflater().inflate(R.layout.snippet_quick_settings, container, false);
        container.addView(view);
    }

    @Override
    protected void onResume() {
        super.onResume();

        setStatus();
    }

    private void setStatus() {
        sNetwork.setChecked(PiaPrefHandler.getQuickSettingsNetwork(this));
        sKillswitch.setChecked(PiaPrefHandler.getQuickSettingsKillswitch(this));
        sBrowser.setChecked(PiaPrefHandler.getQuickSettingsPrivateBrowser(this));

        if (VPNProtocol.activeProtocol(this) == VPNProtocol.Protocol.Wireguard) {
            lKillswitch.setVisibility(View.GONE);
        }
    }

    @OnCheckedChanged(R.id.snippet_quick_settings_killswitch_switch)
    public void onKillswitchChanged(CompoundButton button, boolean checked) {
        PiaPrefHandler.setQuickSettingsKillswitch(this, checked);
    }

    @OnCheckedChanged(R.id.snippet_quick_settings_network_switch)
    public void onNetworkChanged(CompoundButton button, boolean checked) {
        PiaPrefHandler.setQuickSettingsNetwork(this, checked);
    }

    @OnCheckedChanged(R.id.snippet_quick_settings_browser_switch)
    public void onBrowserChanged(CompoundButton button, boolean checked) {
        PiaPrefHandler.setQuickSettingsPrivateBrowser(this, checked);
    }
}
