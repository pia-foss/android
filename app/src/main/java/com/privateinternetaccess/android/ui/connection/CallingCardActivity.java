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
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.privateinternetaccess.android.BuildConfig;
import com.privateinternetaccess.android.R;
import com.privateinternetaccess.android.pia.PIAFactory;
import com.privateinternetaccess.android.pia.handlers.PIAServerHandler;
import com.privateinternetaccess.android.pia.handlers.PiaPrefHandler;
import com.privateinternetaccess.android.pia.interfaces.IVPN;
import com.privateinternetaccess.android.pia.utils.DLog;
import com.privateinternetaccess.android.pia.utils.Prefs;
import com.privateinternetaccess.android.pia.utils.Toaster;
import com.privateinternetaccess.android.ui.drawer.SettingsActivity;
import com.privateinternetaccess.android.ui.features.WebviewActivity;
import com.privateinternetaccess.android.ui.superclasses.BaseActivity;
import com.privateinternetaccess.android.wireguard.backend.GoBackend;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import androidx.annotation.Nullable;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class CallingCardActivity extends BaseActivity {

    public static final String SHOW_CTA1 = "show_cta1";

    @BindView(R.id.update_cta1_button) Button bCta1;
    @BindView(R.id.update_cta2_button) Button bCta2;

    @BindView(R.id.update_description_text) TextView tvDescription;
    @BindView(R.id.update_header_text) TextView tvHeader;

    private boolean showCta1 = true;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_calling_card);

        if(getIntent() != null){
            showCta1 = getIntent().getBooleanExtra(SHOW_CTA1, true);
        }

        FrameLayout contentLayout = findViewById(R.id.update_content_layout);
        LayoutInflater li = LayoutInflater.from(this);
        li.inflate(R.layout.update_3_7_1_layout, contentLayout);

        ButterKnife.bind(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        bCta1.setVisibility(showCta1 ? View.VISIBLE : View.INVISIBLE);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(SHOW_CTA1, showCta1);
    }

    @OnClick(R.id.update_close_button)
    public void closeClicked() {
        finish();
    }

    @OnClick(R.id.update_cta1_button)
    public void onCta1Clicked() {
        setWireGuard();

        Intent i = new Intent(this, SettingsActivity.class);
        startActivity(i);
        finish();
    }

    @OnClick(R.id.update_cta2_button)
    public void onCta2Clicked() {
        Intent i = new Intent(this, WebviewActivity.class);
        i.putExtra(WebviewActivity.EXTRA_URL, "https://www.privateinternetaccess.com/blog/wireguide-all-about-the-wireguard-vpn-protocol/");
        startActivity(i);
        finish();
    }

    public static boolean hasCallingCard(String version) {
        Set<String> versionSet = new HashSet<>();
        versionSet.add("3.7.1");

        DLog.d("CallingCard", "Version: " + version);

        return versionSet.contains(version);
    }

    /*
    Temporary method to turn on WG when try WG is pressed on initial calling card. This will be
    changed later to be more dynamic
     */
    private void setWireGuard() {
        final String[] protocolArray = getResources().getStringArray(R.array.protocol_options);
        String previousProtocol = Prefs.with(this).get(PiaPrefHandler.VPN_PROTOCOL, protocolArray[0]);

        if(PIAFactory.getInstance().getVPN(this).isVPNActive()) {
            Toaster.l(getApplicationContext(), R.string.reconnect_vpn);
        }

        if (previousProtocol.equals(protocolArray[1])) {
            if (GoBackend.VpnService.backend != null) {
                GoBackend.VpnService.backend.stopVpn();
            }
        }
        else {
            IVPN vpn = PIAFactory.getInstance().getVPN(this);

            if(vpn.isVPNActive()) {
                vpn.stop();

                Handler handler = new Handler(Looper.getMainLooper());
                handler.postDelayed(() -> {
                    if(vpn.isKillswitchActive()){
                        vpn.stopKillswitch();
                    }
                }, 1000);
            }

        }

        Prefs.with(this).set(PiaPrefHandler.VPN_PROTOCOL, protocolArray[1]);
        PIAServerHandler.getInstance(getBaseContext()).triggerLatenciesUpdate();
    }
}
