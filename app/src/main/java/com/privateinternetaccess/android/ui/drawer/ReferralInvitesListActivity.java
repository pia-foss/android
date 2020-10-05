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

import android.content.Context;
import android.os.Bundle;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.widget.FrameLayout;

import com.privateinternetaccess.account.model.response.InvitesDetailsInformation;
import com.privateinternetaccess.android.R;
import com.privateinternetaccess.android.pia.handlers.PiaPrefHandler;
import com.privateinternetaccess.android.ui.adapters.InviteListAdapter;
import com.privateinternetaccess.android.ui.superclasses.BaseActivity;
import com.privateinternetaccess.android.utils.InvitesUtils;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ReferralInvitesListActivity extends BaseActivity {

    @BindView(R.id.fragment_invites_list) RecyclerView recyclerView;

    private boolean showAccepted = false;

    private InviteListAdapter mAdapter;
    private LinearLayoutManager layoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_secondary);

        initHeader(true, true);
        setBackground();
        setSecondaryGreenBackground();

        addSnippetToView();

        ButterKnife.bind(this);

        showAccepted = getIntent().getExtras().getBoolean("showAccepted");

        if (showAccepted) {
            setTitle(getString(R.string.refer_signups_title));
        }
        else {
            setTitle(getString(R.string.refer_pending_title));
        }

        processInvites();
    }

    private void addSnippetToView() {
        FrameLayout container = findViewById(R.id.activity_secondary_container);
        View view = getLayoutInflater().inflate(R.layout.fragment_referrals_pending, container, false);
        container.addView(view);
    }

    public void processInvites() {
        Context context = getBaseContext();
        InvitesDetailsInformation invitesDetails = PiaPrefHandler.invitesDetails(context);
        if (invitesDetails != null) {
            List<InvitesDetailsInformation.Invite> invitesList =
                    InvitesUtils.INSTANCE.pendingInvitesFromInvitesList(invitesDetails.getInvites());
            if (showAccepted) {
                invitesList =
                        InvitesUtils.INSTANCE.acceptedInvitesFromInvitesList(invitesDetails.getInvites());
            }
            mAdapter = new InviteListAdapter(invitesList, this);
            layoutManager = new LinearLayoutManager(this);
            layoutManager.setOrientation(LinearLayoutManager.VERTICAL);

            recyclerView.setLayoutManager(layoutManager);
            recyclerView.setAdapter(mAdapter);
        }
    }
}
