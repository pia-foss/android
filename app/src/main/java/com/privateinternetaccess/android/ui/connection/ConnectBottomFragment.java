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

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.privateinternetaccess.android.PIAKillSwitchStatus;
import com.privateinternetaccess.android.R;
import com.privateinternetaccess.android.pia.PIAFactory;
import com.privateinternetaccess.android.pia.handlers.PIAServerHandler;
import com.privateinternetaccess.android.pia.handlers.PiaPrefHandler;
import com.privateinternetaccess.android.pia.interfaces.IConnection;
import com.privateinternetaccess.android.pia.interfaces.IVPN;
import com.privateinternetaccess.android.pia.model.events.FetchIPEvent;
import com.privateinternetaccess.android.pia.model.events.KillSwitchEvent;
import com.privateinternetaccess.android.pia.model.events.PortForwardEvent;
import com.privateinternetaccess.android.pia.model.events.VpnStateEvent;
import com.privateinternetaccess.android.pia.utils.DLog;
import com.privateinternetaccess.android.tunnel.PIAVpnStatus;
import com.privateinternetaccess.android.tunnel.PortForwardingStatus;
import com.privateinternetaccess.android.ui.drawer.ServerListActivity;
import com.privateinternetaccess.core.model.PIAServer;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import de.blinkt.openvpn.core.ConnectionStatus;

/**
 * Created by half47 on 3/14/17.
 */

public class ConnectBottomFragment extends Fragment {

    private TextView tvIP;
    private View pbIP;

    private View aPort;
    private TextView tvPort;

    private View aServer;
    private TextView tvServer;
    private ImageView ivServer;

    private String normalIP;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_bottom_connect, container, false);
        bindView(view);
        if(savedInstanceState != null)
            normalIP = savedInstanceState.getString("normalIP");
        DLog.d("ConnectBottomFragment", "normalIP = " + normalIP);
        return view;
    }

    private void bindView(View view) {
        tvIP = view.findViewById(R.id.fragment_connect_ip);
        pbIP = view.findViewById(R.id.fragment_connect_ip_progress);

        aPort = view.findViewById(R.id.fragment_connect_port_area);
        tvPort = view.findViewById(R.id.fragment_connect_port);

        aServer = view.findViewById(R.id.fragment_connect_flag_area);
        tvServer = view.findViewById(R.id.fragment_connect_server_name);
        ivServer = view.findViewById(R.id.fragment_connect_server_icon);

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("normalIP", normalIP);
    }

    @Override
    public void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
        initView();
        onUpdateEvent(EventBus.getDefault().getStickyEvent(VpnStateEvent.class));
    }

    @Override
    public void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
    }

    private void initView() {
        aServer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(v.getContext(), ServerListActivity.class);
                getActivity().startActivityForResult(i, MainActivity.START_SERVER_LIST);
                getActivity().overridePendingTransition(R.anim.left_to_right, R.anim.right_to_left);
            }
        });
    }

    private void refreshViews(ConnectionStatus event){
        refreshIPInformation(event);

        setRegionDisplay();

        PortForwardEvent portForwardEvent = EventBus.getDefault().getStickyEvent(PortForwardEvent.class);

        if(portForwardEvent != null) {
            newPortForward(portForwardEvent);
        }

        if(PiaPrefHandler.isPortForwardingEnabled(aServer.getContext())){
            if(!isLargerScreen())
                aPort.setVisibility(View.VISIBLE);
            else {
                aPort.setVisibility(View.INVISIBLE);
            }
        } else {
            aPort.setVisibility(View.INVISIBLE);
        }
    }

    private void refreshIPInformation(ConnectionStatus event) {
        IConnection connection = PIAFactory.getInstance().getConnection(tvIP.getContext());
        IVPN vpn = PIAFactory.getInstance().getVPN(tvIP.getContext());
        String savedIP = connection.getSavedIP();
        DLog.d("ConnectBottom", "event = " + event + " savedIP = "+ savedIP + " normalIP = " + normalIP);
        if(!TextUtils.isEmpty(savedIP) && event == ConnectionStatus.LEVEL_CONNECTED) {
            tvIP.setText(savedIP);
            tvIP.setVisibility(View.VISIBLE);
            pbIP.setVisibility(View.GONE);
            normalIP = null;
        } else {
            if(event == ConnectionStatus.LEVEL_NOTCONNECTED){
                if(!TextUtils.isEmpty(normalIP)){
                    tvIP.setText(normalIP);
                    tvIP.setVisibility(View.VISIBLE);
                    pbIP.setVisibility(View.GONE);
                } else {
                    if(!vpn.isKillswitchActive()) {
                        tvIP.setText("");
                        tvIP.setVisibility(View.INVISIBLE);
                        pbIP.setVisibility(View.VISIBLE);
                    } else {
                        tvIP.setText("");
                        tvIP.setVisibility(View.INVISIBLE);
                        pbIP.setVisibility(View.GONE);
                    }
                }
            } else if(event != ConnectionStatus.LEVEL_CONNECTED){
                tvIP.setText("");
                tvIP.setVisibility(View.INVISIBLE);
                pbIP.setVisibility(View.VISIBLE);
                normalIP = null;
            }
        }
    }

    @Subscribe
    public void onUpdateEvent(final VpnStateEvent event){
        //Update port and IP
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    refreshViews(event.getLevel());
                } catch (IllegalStateException e) {
                    //prevent illegal state issues 
                }
            }
        });
    }

    @Subscribe
    public void onIPReceive(final FetchIPEvent event){
        tvIP.post(new Runnable() {
            public void run() {
                DLog.d("ConnectBottom", "ip = " + event.getIp() + " searching = " + event.isSearching());
                if (!TextUtils.isEmpty(event.getIp()) && !event.isSearching()) {
                    if(!PIAFactory.getInstance().getVPN(getContext()).isVPNActive()){
                        normalIP = event.getIp();
                    }
                    tvIP.setText(event.getIp());
                    tvIP.setVisibility(View.VISIBLE);
                    pbIP.setVisibility(View.GONE);
                } else if (TextUtils.isEmpty(event.getIp()) && event.isSearching()) {
                    pbIP.setVisibility(PIAKillSwitchStatus.isKillSwitchActive() ? View.GONE : View.VISIBLE);
                    tvIP.setText("");
                    tvIP.setVisibility(View.INVISIBLE);
                } else if (TextUtils.isEmpty(event.getIp()) && !event.isSearching()) {
                    if (!PIAFactory.getInstance().getVPN(getContext()).isVPNActive()) {
                        tvIP.setText("");
                        tvIP.setVisibility(View.INVISIBLE);
                    }
                    pbIP.setVisibility(View.GONE);
                } else {
                    tvIP.setVisibility(View.INVISIBLE);
                    tvIP.setText("");
                    pbIP.setVisibility(View.GONE);
                }
            }
        });
    }

    @Subscribe
    public void onKillswitchEvent(final KillSwitchEvent event){
        pbIP.post(new Runnable() {
            @Override
            public void run() {
                if(event.isKillSwitchActive){
                    pbIP.setVisibility(View.GONE);
                    tvIP.setText("");
                    tvIP.setVisibility(View.INVISIBLE);
                } else {
                    pbIP.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    @Subscribe
    public void newPortForward(final PortForwardEvent event){
        tvPort.post(new Runnable() {
            @Override
            public void run() {
                DLog.d("StatusFragment", "newPortForward = " + event.getStatus());
                if(PiaPrefHandler.isPortForwardingEnabled(tvPort.getContext())) {
                    if (event.getStatus() == PortForwardingStatus.NO_PORTFWD) {
                        if (isLargerScreen())
                            aPort.setVisibility(View.INVISIBLE);
                        tvPort.setText("");
                    } else {
                        if (PIAFactory.getInstance().getVPN(getContext()).isVPNActive()) {
                            tvPort.setText(event.getArg());
                            if (isLargerScreen())
                                aPort.setVisibility(View.VISIBLE);
                        } else {
                            if (isLargerScreen())
                                aPort.setVisibility(View.INVISIBLE);
                            tvPort.setText("");
                        }
                    }
                } else {
                    aPort.setVisibility(View.INVISIBLE);
                }
            }
        });
    }

    private boolean isLargerScreen() {
        return ivServer == null;
    }

    public void setRegionDisplay() {
        if (PIAServerHandler.getInstance(getActivity()).isSelectedRegionAuto(tvServer.getContext()) && PIAFactory.getInstance().getVPN(getContext()).isVPNActive()) {
            PIAServer currentServer = PIAVpnStatus.getLastConnectedRegion();
            VpnStateEvent event = EventBus.getDefault().getStickyEvent(VpnStateEvent.class);
            if (!(event.getLevel() == ConnectionStatus.LEVEL_NOTCONNECTED ||
                    event.getLevel() == ConnectionStatus.LEVEL_AUTH_FAILED) && currentServer != null) {
                int flag = PIAServerHandler.getInstance(getActivity()).getFlagResource(currentServer);
                String name = currentServer.getName();
                tvServer.setText(getString(R.string.automatic_server_selection_main_region, name));
                if(ivServer != null)
                    ivServer.setBackgroundResource(flag);
            } else {
                setServerName();
            }
        } else {
            setServerName();
        }
    }

    private void setServerName() {
        int flag = R.drawable.flag_world;
        String name = getString(R.string.automatic_server_selection_main);

        PIAServerHandler handler = PIAServerHandler.getInstance(getActivity());
        PIAServer selectedServer = handler.getSelectedRegion(tvServer.getContext(), true);
        if (selectedServer != null) {
            flag = handler.getFlagResource(selectedServer);
            name = selectedServer.getName();
        }
        tvServer.setText(name);
        ivServer.setBackgroundResource(flag);

    }

    private void setUpCard(View parent){
        CardView cardView = (CardView) parent;
        int backgroundInt = R.color.windowBackground;
        cardView.setCardBackgroundColor(ContextCompat.getColor(parent.getContext(), backgroundInt));
        cardView.setRadius(6.0f);
        cardView.setCardElevation(4f);
    }
}
