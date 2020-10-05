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
import android.content.res.Resources;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.privateinternetaccess.android.R;
import com.privateinternetaccess.android.pia.PIAFactory;
import com.privateinternetaccess.android.pia.handlers.PiaPrefHandler;
import com.privateinternetaccess.android.pia.interfaces.IAccount;
import com.privateinternetaccess.android.pia.model.AccountInformation;
import com.privateinternetaccess.android.pia.utils.DLog;
import com.privateinternetaccess.android.pia.utils.Toaster;
import com.privateinternetaccess.android.ui.superclasses.BaseActivity;

import org.greenrobot.eventbus.EventBus;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by half47 on 2/23/16.
 */
public class AccountActivity extends BaseActivity {

    private static final String EMAIL = "email";
    private static final int PASSWORD_REQUEST_CODE = 21;
    private static final String UNLOCKED = "unlocked";
    private static final int SAVE_REQUEST_CODE = 22;

    @BindView (R.id.settings_account_expiration_date) TextView tvExpirationDate;
    @BindView (R.id.settings_account_expiration_text) TextView tvExpirationText;

    @BindView (R.id.settings_account_username) TextView tvUsername;

    @BindView (R.id.settings_account_refer_layout) RelativeLayout lReferralBanner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_secondary);

        initHeader(true, true);
        setTitle(getString(R.string.drawer_account));
        setBackground();
        setSecondaryGreenBackground();

        addSnippetToView();

        ButterKnife.bind(this);
    }

    private void addSnippetToView() {
        FrameLayout container = findViewById(R.id.activity_secondary_container);
        View view = getLayoutInflater().inflate(R.layout.snippet_account_details, container, false);
        container.addView(view);
    }

    @Override
    protected void onResume() {
        super.onResume();

        setUpUserNamePassword();

        setupExpirationDate();
    }

    private void setupExpirationDate() {
        IAccount account = PIAFactory.getInstance().getAccount(getApplicationContext());
        AccountInformation accountInformation = account.persistedAccountInformation();
        Date date = new Date();
        date.setTime(accountInformation.getExpirationTime());
        DLog.d("AccountActivity", accountInformation.toString());
        Locale current = getResources().getConfiguration().locale;
        DateFormat sdf = SimpleDateFormat.getDateInstance(DateFormat.MEDIUM, current);
        if (!accountInformation.getExpired())
            tvExpirationDate.setText(sdf.format(date));
        else {
            tvExpirationDate.setText(R.string.timeleft_expired);
            tvExpirationText.setText(R.string.subscription_expired);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
    }

    private void setUpUserNamePassword() {
        tvUsername.setText(PiaPrefHandler.getLogin(getApplicationContext()));

        View copyObj = tvUsername;
        copyObj.setOnClickListener(v -> copyTextToClipboard(tvUsername.getText().toString()));
    }

    private void copyTextToClipboard(String copyText) {
        boolean copied = false;
        try {
            if(!TextUtils.isEmpty(copyText)) {
                android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                android.content.ClipData clip = android.content.ClipData.newPlainText("pia_clip", copyText);
                clipboard.setPrimaryClip(clip);
                Toaster.s(getApplicationContext(), R.string.copy_toast_text);
                copied = true;
            }
        } catch (Resources.NotFoundException e) {
            copied = false;
        }

        if(!copied)
        Toaster.s(getApplicationContext(), R.string.text_failed_to_copy);
    }

    @OnClick (R.id.snippet_refer_button)
    public void onReferClicked() {
        Intent i = new Intent(this, ReferralActivity.class);
        startActivity(i);
        overridePendingTransition(R.anim.left_to_right, R.anim.right_to_left);
    }
}