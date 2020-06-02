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

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.appcompat.widget.AppCompatImageView;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.privateinternetaccess.android.R;
import com.privateinternetaccess.android.pia.model.events.InviteEvent;
import com.privateinternetaccess.android.pia.model.events.SendInviteEvent;
import com.privateinternetaccess.android.pia.model.response.InvitesResponse;
import com.privateinternetaccess.android.pia.tasks.FetchInvitesTask;
import com.privateinternetaccess.android.pia.tasks.SendInviteTask;
import com.privateinternetaccess.android.ui.superclasses.BaseActivity;
import com.privateinternetaccess.android.ui.views.PiaxEditText;

import org.greenrobot.eventbus.Subscribe;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;

public class ReferralActivity extends BaseActivity {

    @BindView(R.id.fragment_refer_email) PiaxEditText etEmail;
    @BindView(R.id.fragment_refer_full_name) PiaxEditText etFullName;

    @BindView(R.id.fragment_referral_copied_layout) RelativeLayout lCopied;
    @BindView(R.id.fragment_referral_invite_layout) RelativeLayout lInvite;
    @BindView(R.id.fragment_referral_processing_layout) LinearLayout lProcessing;
    @BindView(R.id.fragment_referral_send_layout) LinearLayout lSend;
    @BindView(R.id.fragment_referral_status_layout) LinearLayout lStatus;

    @BindView(R.id.fragment_referral_status_button) Button bStatus;
    @BindView(R.id.fragment_referral_terms_check) AppCompatCheckBox cbTerms;

    @BindView(R.id.fragment_referral_status_icon) AppCompatImageView ivStatus;

    @BindView(R.id.fragment_referral_share_link) TextView tvShareLink;
    @BindView(R.id.fragment_referral_status_description) TextView tvStatusDescription;
    @BindView(R.id.fragment_referral_status_header) TextView tvStatusHeader;
    @BindView(R.id.fragment_referral_terms_required) TextView tvTermsRequired;

    private boolean processingInvite;

    private String referralUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_secondary);

        initHeader(true, true);
        setTitle(getString(R.string.drawer_refer_friend));
        setGreenBackground();
        setSecondaryGreenBackground();

        addSnippetToView();

        ButterKnife.bind(this);

        setupViews();

        new FetchInvitesTask(this).execute();
    }

    private void addSnippetToView() {
        FrameLayout container = findViewById(R.id.activity_secondary_container);
        View view = getLayoutInflater().inflate(R.layout.fragment_referrals, container, false);
        container.addView(view);
    }

    private void setupViews() {
        lStatus.setVisibility(View.GONE);
        lCopied.setVisibility(View.GONE);
        lProcessing.setVisibility(View.GONE);
        lSend.setVisibility(View.VISIBLE);

        etEmail.etMain.setInputType(InputType.TYPE_CLASS_TEXT);
        etFullName.etMain.setInputType(InputType.TYPE_CLASS_TEXT);
    }

    private void showProcessing() {
        lStatus.setVisibility(View.GONE);
        lSend.setVisibility(View.GONE);
        lProcessing.setVisibility(View.VISIBLE);
    }

    private void showSuccess() {
        lStatus.setVisibility(View.VISIBLE);
        lSend.setVisibility(View.GONE);
        lProcessing.setVisibility(View.GONE);

        ivStatus.setImageResource(R.drawable.ic_referral_success);
        tvStatusHeader.setText(R.string.refer_send_success_title);
        tvStatusDescription.setText(R.string.refer_send_success_desc);
        bStatus.setText(R.string.refer_send_success_button);
    }

    private void showFailure() {
        lStatus.setVisibility(View.VISIBLE);
        lSend.setVisibility(View.GONE);
        lProcessing.setVisibility(View.GONE);

        ivStatus.setImageResource(R.drawable.ic_referral_failure);
        tvStatusHeader.setText(R.string.refer_send_fail_title);
        tvStatusDescription.setText(R.string.refer_send_fail_desc);
        bStatus.setText(R.string.refer_send_fail_button);
    }

    @Subscribe(sticky = true)
    public void onReceivedInvites(InviteEvent event) {
        InvitesResponse response = event.getResponse();

        if (response != null) {
            if (response.getNumberInvites() > 0) {
                lInvite.setVisibility(View.VISIBLE);
            }
            else {
                lInvite.setVisibility(View.GONE);
            }

            tvShareLink.setText(response.getReferralLink());
            referralUrl = response.getReferralLink();
        }
    }

    @Subscribe
    public void onInviteSent(SendInviteEvent event) {
        processingInvite = false;

        if (event.getResponse().isSuccessStatus()) {
            new FetchInvitesTask(this).execute();
            showSuccess();
        }
        else {
            showFailure();
        }
    }

    @OnClick(R.id.fragment_referral_invite_layout)
    public void onViewInvitesClicked() {
        Intent i = new Intent(this, ReferralInvitesActivity.class);
        startActivity(i);
        overridePendingTransition(R.anim.left_to_right, R.anim.right_to_left);
    }

    @OnClick(R.id.fragment_referral_copy_button)
    public void onCopyClicked() {
        if (referralUrl != null && referralUrl.length() > 0) {
            ClipboardManager clipboard = (ClipboardManager)
                    getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("PIA Referral Code", referralUrl);
            clipboard.setPrimaryClip(clip);

            lCopied.setVisibility(View.VISIBLE);

            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    lCopied.post(new Runnable() {
                        @Override
                        public void run() {
                            lCopied.setVisibility(View.GONE);
                        }
                    });
                }
            }, 2000);
        }
    }

    @OnCheckedChanged(R.id.fragment_referral_terms_check)
    public void onCheckChanged() {
        tvTermsRequired.setVisibility(View.GONE);
    }

    @OnClick(R.id.fragment_referral_share_button)
    public void onShareClicked() {
        if (referralUrl == null || referralUrl.length() < 0)
            return;

        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, referralUrl);
        sendIntent.setType("text/plain");

        Intent shareIntent = Intent.createChooser(sendIntent, null);
        startActivity(shareIntent);
    }

    @OnClick(R.id.fragment_referral_status_button)
    public void onCompleteClicked() {
        setupViews();
    }

    @OnClick(R.id.fragment_refer_invite_button)
    public void onInviteClicked() {
        if (!processingInvite) {
            if (!cbTerms.isChecked()) {
                tvTermsRequired.setVisibility(View.VISIBLE);
            }
            else if (!TextUtils.isEmpty(etEmail.getText())) {
                new SendInviteTask(this, etEmail.getText(), etFullName.getText()).execute();
                processingInvite = true;
                showProcessing();
            }
            else {
                etEmail.setError(getString(R.string.refer_email_required));
            }
        }
    }
}
