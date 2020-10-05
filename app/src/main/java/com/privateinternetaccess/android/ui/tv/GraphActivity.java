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

package com.privateinternetaccess.android.ui.tv;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;

import com.privateinternetaccess.android.R;
import com.privateinternetaccess.android.model.events.VPNTrafficDataPointEvent;
import com.privateinternetaccess.android.pia.handlers.PIAServerHandler;
import com.privateinternetaccess.android.pia.handlers.PiaPrefHandler;
import com.privateinternetaccess.android.pia.model.events.VpnStateEvent;
import com.privateinternetaccess.android.ui.connection.GraphFragmentHandler;
import com.privateinternetaccess.android.ui.superclasses.BaseActivity;
import com.privateinternetaccess.core.model.PIAServer;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.blinkt.openvpn.core.ConnectionStatus;
import de.blinkt.openvpn.core.VpnStatus;

public class GraphActivity extends BaseActivity {

    private static final String TAG = "GraphActivity";

    @BindView(R.id.activity_graph_connected_status) TextView tvConnectedStatus;
    @BindView(R.id.activity_graph_ip_status) TextView tvIPStatus;
    @BindView(R.id.activity_graph_download_amount) TextView tvDownload;
    @BindView(R.id.activity_graph_download_image) View ivDown;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graph);
        ButterKnife.bind(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        String lastIP = PiaPrefHandler.getLastIP(getApplicationContext());
        if (!TextUtils.isEmpty(lastIP)) {
            tvIPStatus.setText(lastIP);
        }

        VpnStateEvent vpnEvent = EventBus.getDefault().getStickyEvent(VpnStateEvent.class);
        setVPNTextAndTrafficVisibility(vpnEvent);
        VPNTrafficDataPointEvent event = EventBus.getDefault().getStickyEvent(VPNTrafficDataPointEvent.class);
        onVPNSpeedReceived(event);
    }

    private void setVPNTextAndTrafficVisibility(VpnStateEvent vpnEvent) {
        String connectionText = getString(R.string.state_exiting);
        int lastStateResId = vpnEvent.getLocalizedResId();
        if (lastStateResId != 0) {
            if(lastStateResId == de.blinkt.openvpn.R.string.state_waitconnectretry)
                connectionText = VpnStatus.getLastCleanLogMessage(getApplicationContext());
            else
                connectionText = getString(lastStateResId);
        }

        PIAServerHandler handler =  PIAServerHandler.getInstance(getApplicationContext());
        if(vpnEvent.getLevel() == ConnectionStatus.LEVEL_CONNECTED){
            PIAServer server = handler.getSelectedRegion(getApplicationContext(), false);
            tvDownload.setVisibility(View.VISIBLE);
            ivDown.setVisibility(View.VISIBLE);
            tvConnectedStatus.setText(new StringBuilder()
                    .append(connectionText)
                    .append(":")
                    .append(server != null ? server.getName() : getString(R.string.automatic_server_selection_main)).toString());
        } else if(vpnEvent.getLevel() == ConnectionStatus.LEVEL_NOTCONNECTED){
            tvDownload.setVisibility(View.GONE);
            ivDown.setVisibility(View.GONE);
            tvConnectedStatus.setText(R.string.state_disconnected);
            tvIPStatus.setText("");
        } else {
            tvConnectedStatus.setText(connectionText);
            String[] array = getResources().getStringArray(R.array.preference_graph_values);
            String graphUnit = PiaPrefHandler.getGraphUnit(getApplicationContext());
            tvDownload.setText(GraphFragmentHandler.getFormattedString(0, graphUnit, array));
            ivDown.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_BACK) {
            onBackPressed();
        }
        return super.onKeyDown(keyCode, event);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onVPNReceive(VpnStateEvent event){
        setVPNTextAndTrafficVisibility(event);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onVPNSpeedReceived(VPNTrafficDataPointEvent event){
        String graphUnit = PiaPrefHandler.getGraphUnit(getApplicationContext());
        String[] array = getResources().getStringArray(R.array.preference_graph_values);
        tvDownload.setText(GraphFragmentHandler.getFormattedString(event.getDiffIn(), graphUnit, array));
    }
}
