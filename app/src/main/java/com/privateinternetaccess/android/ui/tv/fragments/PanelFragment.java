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

package com.privateinternetaccess.android.ui.tv.fragments;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.privateinternetaccess.android.BuildConfig;
import com.privateinternetaccess.android.R;
import com.privateinternetaccess.android.model.states.VPNProtocol;
import com.privateinternetaccess.android.pia.handlers.PiaPrefHandler;
import com.privateinternetaccess.android.ui.drawer.SettingsActivity;
import com.privateinternetaccess.android.ui.tv.DashboardActivity;
import com.privateinternetaccess.android.ui.tv.views.IPPortView;
import com.privateinternetaccess.android.ui.tv.views.PanelItem;
import com.privateinternetaccess.android.ui.tv.views.TVToggleView;

import butterknife.BindView;
import butterknife.ButterKnife;

public class PanelFragment extends Fragment {

    @BindView(R.id.panel_port_view) IPPortView portWidget;

    @BindView(R.id.panel_port_item) PanelItem portPanelItem;
    @BindView(R.id.panel_graph_item) PanelItem graphPanelItem;
    @BindView(R.id.panel_favorites_item) PanelItem favoritesPanelItem;
    @BindView(R.id.panel_logout_item) PanelItem logoutPanelItem;
    @BindView(R.id.panel_settings_item) PanelItem settingsPanelItem;
    @BindView(R.id.panel_per_app_item) PanelItem perAppSettingsPanelItem;

    @Nullable
    @BindView(R.id.panel_mace_toggle) TVToggleView maceToggle;
    @Nullable
    @BindView(R.id.panel_mace_item) PanelItem macePanelItem;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        int panelLayout = BuildConfig.FLAVOR_store.equals("playstore") ?
                R.layout.fragment_tv_panel_no_mace :
                R.layout.fragment_tv_panel;

        View view = inflater.inflate(panelLayout, container, false);
        ButterKnife.bind(this, view);

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        graphPanelItem.setPanelClickListener(v -> {});

        perAppSettingsPanelItem.setPanelClickListener(v -> {
            DashboardActivity dashboardActivity = (DashboardActivity) getActivity();
            dashboardActivity.showAllowedAppsFragment();
        });

        favoritesPanelItem.setPanelClickListener(v -> {
            DashboardActivity dashboardActivity = (DashboardActivity) getActivity();
            dashboardActivity.showFavoritesFragment();
        });

        logoutPanelItem.setPanelClickListener(v -> {
            DashboardActivity dashboardActivity = (DashboardActivity) getActivity();
            dashboardActivity.logout();
        });

        settingsPanelItem.setPanelClickListener(v -> startActivity(new Intent(getContext(), SettingsActivity.class)));

        if(!BuildConfig.FLAVOR_store.equals("playstore")){
            maceToggle.setPrefKey(PiaPrefHandler.PIA_MACE);
            maceToggle.setTitle(getResources().getString(R.string.mace_toggle).toUpperCase());

            macePanelItem.setPanelClickListener(v -> maceToggle.toggle());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        portWidget.updateState();

        if (VPNProtocol.activeProtocol(getContext()) == VPNProtocol.Protocol.OpenVPN) {
            perAppSettingsPanelItem.setVisibility(View.VISIBLE);
        }
        else {
            perAppSettingsPanelItem.setVisibility(View.GONE);
        }
    }
}
