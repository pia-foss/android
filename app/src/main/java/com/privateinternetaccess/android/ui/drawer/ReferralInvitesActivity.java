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
import android.content.Intent;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.privateinternetaccess.account.model.response.InvitesDetailsInformation;
import com.privateinternetaccess.android.PIAApplication;
import com.privateinternetaccess.android.R;
import com.privateinternetaccess.android.pia.handlers.PiaPrefHandler;
import com.privateinternetaccess.android.ui.superclasses.BaseActivity;
import com.privateinternetaccess.android.utils.InvitesUtils;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class ReferralInvitesActivity extends BaseActivity {

    @BindView(R.id.fragment_invites_free_days) TextView tvFreeDays;
    @BindView(R.id.fragment_invites_pending_count) TextView tvPending;
    @BindView(R.id.fragment_invites_sent_count) TextView tvSent;
    @BindView(R.id.fragment_invites_signup_count) TextView tvSignups;

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

    @Override
    protected void onResume() {
        super.onResume();
        processInvites();
    }

    private void addSnippetToView() {
        FrameLayout container = findViewById(R.id.activity_secondary_container);
        View view = getLayoutInflater().inflate(R.layout.fragment_referrals_sent, container, false);
        container.addView(view);
    }

    private void processInvites() {
        Context context = getBaseContext();
        InvitesDetailsInformation invitesDetails = PiaPrefHandler.invitesDetails(context);
        if (invitesDetails != null) {
            tvFreeDays.setText(
                    String.format(getResources().getString(R.string.refer_free_count), Integer.toString(invitesDetails.getTotalFreeDaysGiven()))
            );

            int invitesSent = invitesDetails.getTotalInvitesSent();
            if (invitesSent == 1) {
                tvSent.setText(String.format(getResources().getString(R.string.refer_sent_invite), Integer.toString(invitesSent)));
            } else {
                tvSent.setText(String.format(getResources().getString(R.string.refer_sent_invites), Integer.toString(invitesSent)));
            }

            List<InvitesDetailsInformation.Invite> pendingInvites =
                    InvitesUtils.INSTANCE.pendingInvitesFromInvitesList(invitesDetails.getInvites());
            tvPending.setText(String.format(getResources().getString(R.string.refer_pending_invites),
                    Integer.toString(pendingInvites.size())));

            List<InvitesDetailsInformation.Invite> acceptedInvites =
                    InvitesUtils.INSTANCE.acceptedInvitesFromInvitesList(invitesDetails.getInvites());
            tvSignups.setText(String.format(getResources().getString(R.string.refer_signed_up),
                    Integer.toString(acceptedInvites.size())));
        }
    }

    @OnClick(R.id.fragment_invites_pending_layout)
    public void onPendingClicked() {
        Context context = getBaseContext();
        InvitesDetailsInformation invitesDetails = PiaPrefHandler.invitesDetails(context);
        List<InvitesDetailsInformation.Invite> pendingInvites =
                InvitesUtils.INSTANCE.pendingInvitesFromInvitesList(invitesDetails.getInvites());
        if (pendingInvites.size() > 0) {
            Intent intent = new Intent(this, ReferralInvitesListActivity.class);
            intent.putExtra("showAccepted", false);
            startActivity(intent);
            overridePendingTransition(R.anim.left_to_right, R.anim.right_to_left);
        }
    }

    @OnClick(R.id.fragment_invites_signup_layout)
    public void onSignupsClicked() {
        Context context = getBaseContext();
        InvitesDetailsInformation invitesDetails = PiaPrefHandler.invitesDetails(context);
        List<InvitesDetailsInformation.Invite> acceptedInvites =
                InvitesUtils.INSTANCE.acceptedInvitesFromInvitesList(invitesDetails.getInvites());
        if (acceptedInvites.size() > 0) {
            Intent intent = new Intent(this, ReferralInvitesListActivity.class);
            intent.putExtra("showAccepted", true);
            startActivity(intent);
            overridePendingTransition(R.anim.left_to_right, R.anim.right_to_left);
        }
    }
}
