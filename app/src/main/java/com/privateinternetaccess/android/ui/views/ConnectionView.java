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
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import com.privateinternetaccess.android.R;
import com.privateinternetaccess.android.model.states.VPNProtocol;
import com.privateinternetaccess.android.pia.handlers.PiaPrefHandler;
import com.privateinternetaccess.android.pia.model.events.VpnStateEvent;
import com.privateinternetaccess.android.pia.utils.Prefs;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.blinkt.openvpn.core.ConnectionStatus;

public class ConnectionView extends FrameLayout {

    @BindView(R.id.connection_authentication_text) TextView tvAuthentication;
    @BindView(R.id.connection_connection_text) TextView tvConnection;
    @BindView(R.id.connection_encryption_text) TextView tvEncryption;
    @BindView(R.id.connection_handshake_text) TextView tvHandshake;
    @BindView(R.id.connection_port_text) TextView tvPort;
    @BindView(R.id.connection_socket_text) TextView tvSocket;

    private final String WG_PORT = "1337";
    private final String WG_ENCRYPTION = "ChaCha20";
    private final String WG_AUTH = "Poly1305";
    private final String WG_HANDSHAKE = "Noise_IK";
    private final String WG_SOCKET = "UDP";

    public ConnectionView(Context context) {
        super(context);
        init(context);
    }

    public ConnectionView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ConnectionView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public void init(Context context) {
        inflate(context, R.layout.view_connection_tile, this);
        ButterKnife.bind(this, this);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        EventBus.getDefault().register(this);

        VpnStateEvent event = EventBus.getDefault().getStickyEvent(VpnStateEvent.class);
        setupValues(event.getLevel() == ConnectionStatus.LEVEL_CONNECTED);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        EventBus.getDefault().unregister(this);
    }

    private void setupValues(boolean isConnected) {
        tvConnection.setText(PiaPrefHandler.getProtocol(getContext()));

        if (VPNProtocol.activeProtocol(getContext()) == VPNProtocol.Protocol.Wireguard) {
            tvSocket.setText(WG_SOCKET);
            tvEncryption.setText(WG_ENCRYPTION);
            tvAuthentication.setText(WG_AUTH);
            tvHandshake.setText(WG_HANDSHAKE);
            tvPort.setText(WG_PORT);
        }
        else {
            boolean usesTCP = Prefs.with(getContext()).get(PiaPrefHandler.USE_TCP, false);
            tvSocket.setText(usesTCP ?
                    getResources().getString(R.string.connection_tile_tcp) :
                    getResources().getString(R.string.connection_tile_udp));
            tvEncryption.setText(Prefs.with(getContext()).get(PiaPrefHandler.CIPHER,
                    getResources().getStringArray(R.array.ciphers_values)[0]));
            tvAuthentication.setText(Prefs.with(getContext()).get(PiaPrefHandler.AUTH,
                    getResources().getStringArray(R.array.auth_values)[0]));
            tvHandshake.setText(Prefs.with(getContext()).get(PiaPrefHandler.TLSCIPHER,
                    "rsa2048"));
            tvPort.setText("-");
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void updateState(VpnStateEvent event) {
        setupValues(event.getLevel() == ConnectionStatus.LEVEL_CONNECTED);
    }

}
