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

import android.content.Context;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import com.privateinternetaccess.android.PIAApplication;
import com.privateinternetaccess.android.R;
import com.privateinternetaccess.android.pia.PIAFactory;
import com.privateinternetaccess.android.pia.handlers.PIAServerHandler;
import com.privateinternetaccess.android.pia.model.PIAServer;
import com.privateinternetaccess.android.pia.model.events.VpnStateEvent;
import com.privateinternetaccess.android.wireguard.backend.GoBackend;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.blinkt.openvpn.core.ConnectionStatus;

/**
 * Created by hfrede on 12/18/17.
 */

public class ConnectionSlider extends FrameLayout {

    public static GoBackend wireguard;
    public static final String TAG = "ConnectionSlider";
    @BindView(R.id.connection_background) AppCompatImageView background;
    @Nullable @BindView(R.id.connect_progress) ProgressBar progressBar;

    private boolean isScaled = false;

    public ConnectionSlider(Context context) {
        super(context);
        init(context);
    }

    public ConnectionSlider(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ConnectionSlider(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        EventBus.getDefault().unregister(this);
    }

    public void init(Context context){
        if (wireguard == null) {
            wireguard = PIAApplication.getWireguard();
        }

        inflate(context, R.layout.view_connection_slider, this);
        ButterKnife.bind(this, this);

        background.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleVPN();
            }
        });

        updateState();
    }

    private void toggleVPN() {
        if(!PIAFactory.getInstance().getVPN(getContext()).isVPNActive()) {
            PIAFactory.getInstance().getVPN(getContext()).start();
        } else {
            PIAFactory.getInstance().getVPN(getContext()).stop();
        }
    }

    public void updateState(){
        VpnStateEvent event = EventBus.getDefault().getStickyEvent(VpnStateEvent.class);
        ConnectionStatus status = event.getLevel();

        if (status == ConnectionStatus.LEVEL_CONNECTED) {
            background.setImageDrawable(getResources().getDrawable(R.drawable.ic_connection_on));
        } else if (status == ConnectionStatus.LEVEL_NOTCONNECTED || status == null) {
            background.setImageDrawable(getResources().getDrawable(R.drawable.ic_connection_off));
        } else if (status == ConnectionStatus.LEVEL_AUTH_FAILED ||
                status == ConnectionStatus.LEVEL_NONETWORK) {
            background.setImageDrawable(getResources().getDrawable(R.drawable.ic_connection_error));
        }
        else {
            background.setImageDrawable(getResources().getDrawable(R.drawable.ic_connection_connecting));
        }
        Context context = getContext();

        int lastStateResId = event.getLocalizedResId();
        if (lastStateResId != 0) {
            if (lastStateResId == de.blinkt.openvpn.R.string.state_waitconnectretry) {

            }
            else if (event.getLevel() == ConnectionStatus.LEVEL_CONNECTED){
                PIAServer server = PIAServerHandler.getInstance(context).getSelectedRegion(context, false);
                StringBuilder sb = new StringBuilder();
                sb.append(context.getString(R.string.state_connected));
                sb.append(": ");
                sb.append(server.getName());
            } else {

            }
        }
    }

    public void animateFocus(boolean focused) {
        float scale = focused ? 1.1f : 1f;
        isScaled = focused;

        setScaleX(scale);
        setScaleY(scale);

        updateState();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void updateState(VpnStateEvent event) {
        updateState();
    }
}
