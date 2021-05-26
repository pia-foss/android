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

package com.privateinternetaccess.android.ui.tv.views;

import android.content.Context;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.privateinternetaccess.android.PIAApplication;
import com.privateinternetaccess.android.R;
import com.privateinternetaccess.android.pia.PIAFactory;
import com.privateinternetaccess.android.pia.handlers.PIAServerHandler;
import com.privateinternetaccess.android.pia.handlers.PiaPrefHandler;
import com.privateinternetaccess.android.pia.model.events.FetchIPEvent;
import com.privateinternetaccess.android.pia.model.events.PortForwardEvent;
import com.privateinternetaccess.android.pia.model.events.VpnStateEvent;
import com.privateinternetaccess.android.tunnel.PortForwardingStatus;
import com.privateinternetaccess.core.model.PIAServer;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import butterknife.BindView;
import butterknife.ButterKnife;

public class IPPortView extends FrameLayout {

    @BindView(R.id.fragment_connect_port) TextView port;
    @BindView(R.id.fragment_connect_port_area) LinearLayout portContainer;
    @BindView(R.id.fragment_connect_ip) TextView ip;
    @BindView(R.id.fragment_connect_ip_vpn) TextView ipVpn;
    @Nullable @BindView(R.id.fragment_connect_port_arrow) AppCompatImageView ivPortArrow;
    @Nullable @BindView(R.id.fragment_connect_port_available) AppCompatImageView ivPortAvailable;
    @Nullable @BindView(R.id.fragment_connect_vpn_layout) LinearLayout aVpnLayout;



    public IPPortView(Context context) {
        super(context);
        init(context);
    }

    public IPPortView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public IPPortView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public void init(Context context) {
        if (!PIAApplication.isAndroidTV(getContext())) {
            int padding = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    16f,
                    context.getResources().getDisplayMetrics()
            );
            setPadding(padding, 0, padding, 0);
        }
        inflate(context, R.layout.view_ip_port_display, this);
        ButterKnife.bind(this, this);
    }

    public void updateState() {
        updatePortForwardingVisibility();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        EventBus.getDefault().register(this);

        PortForwardEvent portForwardEvent = EventBus.getDefault().getStickyEvent(PortForwardEvent.class);

        if (portForwardEvent != null)
            newPortForward(portForwardEvent);

        String lastIp = PiaPrefHandler.getLastIP(getContext());
        String lastIpVpn = PiaPrefHandler.getLastIPVPN(getContext());

        if (lastIp != null && lastIp.length() > 0)
            ip.setText(lastIp);

        if (!PIAApplication.isAndroidTV(getContext()) &&
                lastIpVpn != null && lastIpVpn.length() > 0 &&
                PIAFactory.getInstance().getVPN(getContext()).isVPNActive()) {
            ipVpn.setText(lastIpVpn);
        }

        updatePortForwardingVisibility();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        EventBus.getDefault().unregister(this);
    }

    @Subscribe
    public void newPortForward(final PortForwardEvent event){
        port.post(new Runnable() {
            @Override
            public void run() {
                boolean isVpnActive = PIAFactory.getInstance().getVPN(getContext()).isVPNActive();
                updatePortForwardingVisibility();

                if (PiaPrefHandler.isPortForwardingEnabled(port.getContext())) {
                    PIAServerHandler handler = PIAServerHandler.getInstance(getContext());
                    PIAServer selectedServer = handler.getSelectedRegion(getContext(), true);

                    if (!PIAApplication.isAndroidTV(getContext()))
                        ivPortAvailable.setImageResource(R.drawable.ic_port_forwarding);

                    if (selectedServer != null && !selectedServer.isAllowsPF()) {
                        port.setText(getContext().getString(R.string.port_not_available));

                        if (!PIAApplication.isAndroidTV(getContext()))
                            ivPortAvailable.setImageResource(R.drawable.ic_port_forwarding_unavailable_lighter);
                    }
                    else if (event.getStatus() == PortForwardingStatus.NO_PORTFWD) {
                        port.setText("---");
                    } else {
                        if (isVpnActive && event.getArg().length() > 0) {
                            port.setText(event.getArg());
                        } else {
                            port.setText("---");
                        }
                    }
                }
            }
        });
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
                if (port != null) {
                    port.setText("---");
                }
                if (ipVpn != null) {
                    ipVpn.setText("---");
                }
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onIPReceive(final FetchIPEvent event){
        updatePortForwardingVisibility();

        if (event.getConnected()) {
            ipVpn.setText(event.getIp());
        } else {
            ip.setText(event.getIp());
        }
    }

    private void updatePortForwardingVisibility() {
        if (PiaPrefHandler.isPortForwardingEnabled(getContext())) {
            portContainer.setVisibility(View.VISIBLE);
        } else {
            portContainer.setVisibility(View.GONE);
        }
    }
}
