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

package com.privateinternetaccess.android.ui.drawer;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.privateinternetaccess.account.model.response.DedicatedIPInformationResponse;
import com.privateinternetaccess.android.R;
import com.privateinternetaccess.android.pia.PIAFactory;
import com.privateinternetaccess.android.pia.handlers.PIAServerHandler;
import com.privateinternetaccess.android.pia.handlers.PiaPrefHandler;
import com.privateinternetaccess.android.pia.interfaces.IAccount;
import com.privateinternetaccess.android.pia.interfaces.IVPN;
import com.privateinternetaccess.android.pia.model.enums.RequestResponseStatus;
import com.privateinternetaccess.android.pia.utils.Toaster;
import com.privateinternetaccess.android.ui.adapters.DedicatedIPAdapter;
import com.privateinternetaccess.android.ui.superclasses.BaseActivity;
import com.privateinternetaccess.android.utils.DedicatedIpUtils;
import com.privateinternetaccess.core.model.PIAServer;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class DedicatedIPActivity extends BaseActivity {

    @BindView(R.id.snippet_dip_entry_field) EditText etDipToken;
    @BindView(R.id.snippet_dip_list) RecyclerView recyclerView;
    @BindView(R.id.snippet_dip_list_layout) LinearLayout lList;

    private DedicatedIPAdapter mAdapter;
    private LinearLayoutManager layoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_secondary);

        initHeader(true, true);
        setTitle(getString(R.string.dip_menu_title));
        setBackground();
        setSecondaryGreenBackground();

        addSnippetToView();

        ButterKnife.bind(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupList();
    }

    private void addSnippetToView() {
        FrameLayout container = findViewById(R.id.activity_secondary_container);
        View view = getLayoutInflater().inflate(R.layout.snippet_dedicated_ip, container, false);
        container.addView(view);
    }

    private void setupList() {
        List<DedicatedIPInformationResponse.DedicatedIPInformation> ipList = PiaPrefHandler.getDedicatedIps(this);
        List<PIAServer> serverList = new ArrayList<>();

        if (ipList.size() <= 0) {
            lList.setVisibility(View.GONE);
        }
        else {
            lList.setVisibility(View.VISIBLE);

            for (DedicatedIPInformationResponse.DedicatedIPInformation dip : ipList) {
                serverList.add(DedicatedIpUtils.serverForDip(dip, this));
            }

            mAdapter = new DedicatedIPAdapter(this, serverList);
            layoutManager = new LinearLayoutManager(this);

            recyclerView.setLayoutManager(layoutManager);
            recyclerView.setAdapter(mAdapter);
        }
    }

    public void removeDip(PIAServer ps) {
        PIAServerHandler handler = PIAServerHandler.getInstance(this);
        final PIAServer server = handler.getSelectedRegion(this, false);
        IVPN vpnImpl = PIAFactory.getInstance().getVPN(this);

        if (server.isDedicatedIp() && server.getDipToken().equals(ps.getDipToken())) {
            if (vpnImpl.isVPNActive()) {
                vpnImpl.stop(true);
            }
        }

        PiaPrefHandler.removeDedicatedIps(this, ps);
        PiaPrefHandler.removeFavorite(this, ps.getDedicatedIp());
        setupList();
    }

    @OnClick(R.id.snippet_dip_activate_button)
    public void onActivatePressed() {
        String token = etDipToken.getText().toString();
        String authToken = PiaPrefHandler.getAuthToken(this);

        if (!TextUtils.isEmpty(token)) {
            List<String> ipTokens = new ArrayList<>();
            ipTokens.add(token);

            IAccount account = PIAFactory.getInstance().getAccount(this);
            account.dedicatedIPs(authToken, ipTokens, (details, requestResponseStatus) -> {
                if (requestResponseStatus != RequestResponseStatus.SUCCEEDED) {
                    etDipToken.setText("");
                    Toaster.l(this, R.string.dip_invalid);
                    return null;
                }

                List<DedicatedIPInformationResponse.DedicatedIPInformation> dedicatedIps = details;

                for (DedicatedIPInformationResponse.DedicatedIPInformation ip : dedicatedIps) {
                    if (ip.getStatus() == DedicatedIPInformationResponse.Status.active) {
                        PiaPrefHandler.addDedicatedIp(this, ip);
                        Toaster.l(this, R.string.dip_success);
                    }
                    else if (ip.getStatus() == DedicatedIPInformationResponse.Status.expired) {
                        Toaster.l(this, R.string.dip_expired_warning);
                    }
                    else if (ip.getStatus() == DedicatedIPInformationResponse.Status.invalid) {
                        Toaster.l(this, R.string.dip_invalid);
                    }
                }

                etDipToken.setText("");

                setupList();

                return null;
            });
        }
    }
}
