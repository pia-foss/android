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

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.widget.FrameLayout;

import com.privateinternetaccess.android.R;
import com.privateinternetaccess.android.pia.model.events.InviteEvent;
import com.privateinternetaccess.android.pia.model.response.InvitesResponse;
import com.privateinternetaccess.android.ui.adapters.InviteListAdapter;
import com.privateinternetaccess.android.ui.superclasses.BaseActivity;

import org.greenrobot.eventbus.Subscribe;

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
    }

    private void addSnippetToView() {
        FrameLayout container = findViewById(R.id.activity_secondary_container);
        View view = getLayoutInflater().inflate(R.layout.fragment_referrals_pending, container, false);
        container.addView(view);
    }

    @Subscribe(sticky = true)
    public void onReceivedInvites(InviteEvent event) {
        if (event.getResponse() != null) {
            InvitesResponse data = event.getResponse();
            mAdapter = new InviteListAdapter(
                    showAccepted ? data.getSignupInvites() : data.getSentInvites(),
                    this);

            layoutManager = new LinearLayoutManager(this);
            layoutManager.setOrientation(LinearLayoutManager.VERTICAL);

            recyclerView.setLayoutManager(layoutManager);
            recyclerView.setAdapter(mAdapter);
        }
    }
}
