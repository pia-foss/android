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
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.privateinternetaccess.android.PIAApplication;
import com.privateinternetaccess.android.R;
import com.privateinternetaccess.android.model.states.VPNProtocol;
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

    @Nullable
    @BindView(R.id.fragment_connect_vpn_layout) LinearLayout aVpnLayout;

    @BindView(R.id.fragment_connect_ip) TextView tvIP;
    @Nullable
    @BindView(R.id.fragment_connect_ip_vpn) TextView tvIPVPN;

    @Nullable
    @BindView(R.id.fragment_connect_port_arrow) AppCompatImageView ivPortArrow;

    @BindView(R.id.fragment_connect_port) TextView tvPort;
    @BindView(R.id.fragment_connect_port_area) LinearLayout aPort;

    @Nullable
    @BindView(R.id.fragment_connect_port_available) AppCompatImageView ivPortAvailable;

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
        if (PIAApplication.isAndroidTV(getContext())) {
            inflate(context, R.layout.view_ip_port_display_tv, this);
        }
        else {
            inflate(context, R.layout.view_ip_port_display, this);
        }

        ButterKnife.bind(this, this);
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
            tvIP.setText(lastIp);

        if (!PIAApplication.isAndroidTV(getContext()) &&
                lastIpVpn != null && lastIpVpn.length() > 0 &&
                PIAFactory.getInstance().getVPN(getContext()).isVPNActive()) {
            tvIPVPN.setText(lastIpVpn);
        }

        setVisibilities();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        EventBus.getDefault().unregister(this);
    }

    @Subscribe
    public void newPortForward(final PortForwardEvent event){
        tvPort.post(new Runnable() {
            @Override
            public void run() {
                boolean isVpnActive = PIAFactory.getInstance().getVPN(getContext()).isVPNActive();

                if(PiaPrefHandler.isPortForwardingEnabled(tvPort.getContext()) &&
                        VPNProtocol.activeProtocol(getContext()) == VPNProtocol.Protocol.OpenVPN) {
                    aPort.setVisibility(View.VISIBLE);
                    PIAServerHandler handler = PIAServerHandler.getInstance(getContext());
                    PIAServer selectedServer = handler.getSelectedRegion(getContext(), true);

                    if (!PIAApplication.isAndroidTV(getContext()))
                        ivPortAvailable.setImageResource(R.drawable.ic_port_forwarding);

                    if (selectedServer != null && !selectedServer.isAllowsPF()) {
                        tvPort.setText(getContext().getString(R.string.port_not_available));

                        if (!PIAApplication.isAndroidTV(getContext()))
                            ivPortAvailable.setImageResource(R.drawable.ic_port_forwarding_unavailable_lighter);
                    }
                    else if (event.getStatus() == PortForwardingStatus.NO_PORTFWD) {
                        tvPort.setText("---");
                    } else {
                        if (isVpnActive && event.getArg().length() > 0) {
                            tvPort.setText(event.getArg());
                        } else {
                            tvPort.setText("---");
                        }
                    }
                } else {
                    if (PIAApplication.isAndroidTV(getContext())) {
                        aPort.setVisibility(View.INVISIBLE);
                    }
                    else {
                        aPort.setVisibility(View.GONE);
                    }
                }
            }
        });
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void updateState(final VpnStateEvent event) {
        tvPort.post(new Runnable() {
            @Override
            public void run() {
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
                        if (tvPort != null) {
                            tvPort.setText("---");
                        }
                        if (tvIPVPN != null) {
                            tvIPVPN.setText("---");
                        }
                        break;
                }
            }
        });
    }

    @Subscribe
    public void onIPReceive(final FetchIPEvent event){
        tvIP.post(new Runnable() {
            public void run() {
                setVisibilities();
                boolean isVpnActive = PIAFactory.getInstance().getVPN(getContext()).isVPNActive();

                if (!TextUtils.isEmpty(event.getIp())) {
                    tvIP.setVisibility(View.VISIBLE);
                    if (PIAApplication.isAndroidTV(getContext())) {
                        tvIP.setText(event.getIp());
                    }
                    else {
                        String lastIp = PiaPrefHandler.getLastIP(getContext());
                        String lastIpVpn = PiaPrefHandler.getLastIPVPN(getContext());

                        tvIP.setText(lastIp);
                        if (isVpnActive) {
                            tvIPVPN.setText(lastIpVpn);
                        }
                        else {
                            tvIPVPN.setText("---");
                        }
                    }
                } else if (TextUtils.isEmpty(event.getIp())) {
                    if (!isVpnActive) {
                        tvIP.setText("---");
                    }
                } else {
                    tvIP.setText("---");
                }
            }
        });
    }

    private void setVisibilities() {
        if (!PIAApplication.isAndroidTV(getContext())) {
            if (PiaPrefHandler.isPortForwardingEnabled(getContext()) &&
                    VPNProtocol.activeProtocol(getContext()) == VPNProtocol.Protocol.OpenVPN) {
                aPort.setVisibility(View.VISIBLE);
            }
            else {
                aPort.setVisibility(View.GONE);
            }
        }
    }
}
