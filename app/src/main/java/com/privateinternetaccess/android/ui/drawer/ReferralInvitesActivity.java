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

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.privateinternetaccess.android.R;
import com.privateinternetaccess.android.pia.model.events.InviteEvent;
import com.privateinternetaccess.android.pia.model.response.InviteResponse;
import com.privateinternetaccess.android.pia.model.response.InvitesResponse;
import com.privateinternetaccess.android.ui.superclasses.BaseActivity;

import org.greenrobot.eventbus.Subscribe;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class ReferralInvitesActivity extends BaseActivity {

    @BindView(R.id.fragment_invites_free_days) TextView tvFreeDays;
    @BindView(R.id.fragment_invites_pending_count) TextView tvPending;
    @BindView(R.id.fragment_invites_sent_count) TextView tvSent;
    @BindView(R.id.fragment_invites_signup_count) TextView tvSignups;

    private InvitesResponse activeResponse;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_secondary);

        initHeader(true, true);
        setTitle(getString(R.string.refer_invites_title));
        setBackground();
        setSecondaryGreenBackground();

        addSnippetToView();

        ButterKnife.bind(this);
    }

    private void addSnippetToView() {
        FrameLayout container = findViewById(R.id.activity_secondary_container);
        View view = getLayoutInflater().inflate(R.layout.fragment_referrals_sent, container, false);
        container.addView(view);
    }

    @Subscribe(sticky = true)
    public void onReceivedInvites(InviteEvent event) {
        InvitesResponse response = event.getResponse();

        if (response != null) {
            tvFreeDays.setText(String.format(getResources().getString(R.string.refer_free_count),
                    Integer.toString(response.getFreeDays())));

            if (response.getNumberInvites() == 1) {
                tvSent.setText(String.format(getResources().getString(R.string.refer_sent_invite),
                        Integer.toString(response.getNumberInvites())));
            }
            else {
                tvSent.setText(String.format(getResources().getString(R.string.refer_sent_invites),
                        Integer.toString(response.getNumberInvites())));
            }

            tvPending.setText(String.format(getResources().getString(R.string.refer_pending_invites),
                    Integer.toString(response.getSentInvites().size())));
            tvSignups.setText(String.format(getResources().getString(R.string.refer_signed_up),
                    Integer.toString(response.getSignupInvites().size())));
        }

        activeResponse = response;
    }

    @OnClick(R.id.fragment_invites_pending_layout)
    public void onPendingClicked() {
        if (activeResponse != null && activeResponse.getSentInvites().size() > 0) {
            Intent intent = new Intent(this, ReferralInvitesListActivity.class);
            intent.putExtra("showAccepted", false);
            startActivity(intent);
            overridePendingTransition(R.anim.left_to_right, R.anim.right_to_left);
        }
    }

    @OnClick(R.id.fragment_invites_signup_layout)
    public void onSignupsClicked() {
        if (activeResponse != null && activeResponse.getSignupInvites().size() > 0) {
            Intent intent = new Intent(this, ReferralInvitesListActivity.class);
            intent.putExtra("showAccepted", true);
            startActivity(intent);
            overridePendingTransition(R.anim.left_to_right, R.anim.right_to_left);
        }
    }
}
