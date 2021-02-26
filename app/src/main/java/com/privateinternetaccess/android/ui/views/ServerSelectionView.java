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

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.privateinternetaccess.android.PIAApplication;
import com.privateinternetaccess.android.R;
import com.privateinternetaccess.android.model.events.DedicatedIPUpdatedEvent;
import com.privateinternetaccess.android.model.events.ServerClickedEvent;
import com.privateinternetaccess.android.model.events.SeverListUpdateEvent;
import com.privateinternetaccess.android.model.events.SeverListUpdateEvent.ServerListUpdateState;
import com.privateinternetaccess.android.pia.handlers.PIAServerHandler;
import com.privateinternetaccess.android.pia.model.events.VpnStateEvent;
import com.privateinternetaccess.android.tunnel.PIAVpnStatus;
import com.privateinternetaccess.android.ui.connection.MainActivity;
import com.privateinternetaccess.android.ui.drawer.ServerListActivity;
import com.privateinternetaccess.core.model.PIAServer;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.blinkt.openvpn.core.ConnectionStatus;
import de.blinkt.openvpn.core.VpnStatus;

public class ServerSelectionView extends FrameLayout {

    @BindView(R.id.fragment_connect_flag_area) View aServer;
    @BindView(R.id.fragment_connect_server_name) TextView tvServer;
    @BindView(R.id.connect_server_list_progress_bar) View progressBar;
    @BindView(R.id.fragment_connect_server_map) RegionMapView mapView;
    @BindView(R.id.fragment_server_geo) View serverGeoImageView;

    @BindView(R.id.list_server_dip_layout) LinearLayout lDipLayout;
    @BindView(R.id.list_server_dip) TextView tvDip;

    public ServerSelectionView(Context context) {
        super(context);
        init(context);
    }

    public ServerSelectionView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ServerSelectionView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public void init(Context context) {
        inflate(context, R.layout.view_server_select, this);
        ButterKnife.bind(this, this);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        EventBus.getDefault().register(this);
        updateUiForFetchingState(PIAServerHandler.getServerListFetchState());
        aServer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Activity activity = getActivity();

                if (activity != null) {
                    Intent i = new Intent(v.getContext(), ServerListActivity.class);
                    getActivity().startActivityForResult(i, MainActivity.START_SERVER_LIST);
                    getActivity().overridePendingTransition(R.anim.left_to_right, R.anim.right_to_left);
                }
            }
        });

        setGeoDisplay();
        setRegionDisplay();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        EventBus.getDefault().unregister(this);
    }

    private void setGeoDisplay() {
        PIAServerHandler handler = PIAServerHandler.getInstance(getActivity());
        PIAServer selectedServer = handler.getSelectedRegion(tvServer.getContext(), true);
        int targetVisibility = View.GONE;
        if (selectedServer != null && selectedServer.isGeo()) {
            targetVisibility = View.VISIBLE;
        }
        serverGeoImageView.setVisibility(targetVisibility);
    }

    private void setRegionDisplay() {
        lDipLayout.setVisibility(View.GONE);

        if (PIAServerHandler.getInstance(getActivity()).isSelectedRegionAuto(tvServer.getContext()) && VpnStatus.isVPNActive()) {
            PIAServer currentServer = PIAVpnStatus.getLastConnectedRegion();
            VpnStateEvent event = EventBus.getDefault().getStickyEvent(VpnStateEvent.class);
            if (!(event.getLevel() == ConnectionStatus.LEVEL_NOTCONNECTED || event.getLevel() == ConnectionStatus.LEVEL_AUTH_FAILED) && currentServer != null) {
                String name = currentServer.getName();
                tvServer.setText(getContext().getString(R.string.automatic_server_selection_main_region, name));
                mapView.setServer(currentServer);
            } else {
                setServerName();
            }
        } else {
            setServerName();
        }
    }

    private void setServerName() {
        lDipLayout.setVisibility(View.GONE);
        String name = getContext().getString(R.string.automatic_server_selection_main);
        PIAServerHandler serverHandler = PIAServerHandler.getInstance(getContext());
        PIAServer nonNullSelectedServer = serverHandler.getSelectedRegion(getContext(), false);
        PIAServer selectedServer = serverHandler.getSelectedRegion(getContext(), true);

        if (selectedServer != null) {
            if (selectedServer.isDedicatedIp()) {
                lDipLayout.setVisibility(View.VISIBLE);
                tvDip.setText(selectedServer.getDedicatedIp());
            }
            name = selectedServer.getName();
        }

        tvServer.setText(name);
        mapView.setServer(nonNullSelectedServer);
    }

    private Activity getActivity() {
        Context context = getContext();
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                return (Activity)context;
            }
            context = ((ContextWrapper)context).getBaseContext();
        }
        return null;
    }

    @Subscribe
    public void serverSelected(ServerClickedEvent event) {
        setServerName();
    }

    @Subscribe
    public void serverListUpdateEvent(SeverListUpdateEvent event) {
        updateUiForFetchingState(event.getState());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void updateState(final VpnStateEvent event) {
        switch (event.level) {
            case LEVEL_CONNECTED:
            case LEVEL_START:
            case LEVEL_WAITING_FOR_USER_INPUT:
            case LEVEL_CONNECTING_SERVER_REPLIED:
            case LEVEL_CONNECTING_NO_SERVER_REPLY_YET:
                break;
            case UNKNOWN_LEVEL:
            case LEVEL_NONETWORK:
            case LEVEL_VPNPAUSED:
            case LEVEL_AUTH_FAILED:
            case LEVEL_NOTCONNECTED:
                setServerName();
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void dedicatedIPUpdatedEvent(DedicatedIPUpdatedEvent event) {
        setServerName();
    }

    private void updateUiForFetchingState(ServerListUpdateState state) {
        switch (state) {
            case STARTED:
                aServer.setEnabled(false);
                progressBar.setVisibility(VISIBLE);
                break;
            case FETCH_SERVERS_FINISHED:
            case GEN4_PING_SERVERS_FINISHED:
                aServer.setEnabled(true);
                progressBar.setVisibility(GONE);
                setServerName();
                break;
        }
    }
}
