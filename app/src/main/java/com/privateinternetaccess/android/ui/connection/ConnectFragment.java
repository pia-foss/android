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

package com.privateinternetaccess.android.ui.connection;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.privateinternetaccess.android.PIAApplication;
import com.privateinternetaccess.android.R;
import com.privateinternetaccess.android.pia.PIAFactory;
import com.privateinternetaccess.android.pia.model.events.KillSwitchEvent;
import com.privateinternetaccess.android.pia.model.events.VpnStateEvent;
import com.privateinternetaccess.android.pia.utils.DLog;
import com.privateinternetaccess.android.pia.utils.Prefs;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.blinkt.openvpn.core.ConnectionStatus;
import de.blinkt.openvpn.core.VpnStatus;

/**
 * Created by half47 on 3/14/17.
 */

public class ConnectFragment extends Fragment {

    private static final String TAG = "StatusFragment";

    @BindView(R.id.fragment_connect_text) TextView tvStatus;
    @Nullable @BindView(R.id.fragment_connect_header) View headerView;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(PIAApplication.isAndroidTV(getContext()) ?
                R.layout.fragment_tv_connect : R.layout.fragment_connect,
                container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
        setConnectStatus();
    }

    @Override
    public void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
    }

    @Subscribe
    public void updateState(VpnStateEvent event) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    DLog.d("ConnectFragment", "Update State Event");
                    setConnectStatus();
                } catch (Exception e) {
                }
            }
        });
    }

    private void setConnectStatus() {
        VpnStateEvent event = EventBus.getDefault().getStickyEvent(VpnStateEvent.class);
        int lastStateResId = event.getLocalizedResId();
        if (lastStateResId != 0) {
            if(lastStateResId == de.blinkt.openvpn.R.string.state_waitconnectretry)
                tvStatus.setText(VpnStatus.getLastCleanLogMessage(tvStatus.getContext()));
            else
                tvStatus.setText(getString(lastStateResId).toUpperCase());
        }

        ConnectionStatus status = event.getLevel();
        boolean androidTV = PIAApplication.isAndroidTV(getContext());
        if (status == ConnectionStatus.LEVEL_CONNECTED) {
            if (androidTV) {
                headerView.setEnabled(true);
                headerView.setActivated(true);
            }
        } else if (status == ConnectionStatus.LEVEL_NOTCONNECTED ||
                status == ConnectionStatus.LEVEL_AUTH_FAILED || status == null) {
            if (androidTV) {
                if(PIAFactory.getInstance().getVPN(getActivity()).isKillswitchActive()){
                    headerView.setEnabled(false);
                    headerView.setActivated(true);
                    tvStatus.setText(getString(R.string.killswitchtext));
                } else {
                    headerView.setActivated(false);
                    headerView.setEnabled(true);
                }
            }
        } else {
            if (androidTV) {
                headerView.setEnabled(true);
                headerView.setActivated(false);
            }
        }

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void killswitchEvent(KillSwitchEvent event) {
        try {
            DLog.d("ConnectFragment", "Update Killswitch State Event");
            setConnectStatus();
        } catch (Exception e) {
        }
    }
}