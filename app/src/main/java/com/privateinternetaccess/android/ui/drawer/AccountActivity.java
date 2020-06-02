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
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.privateinternetaccess.android.R;
import com.privateinternetaccess.android.pia.PIAFactory;
import com.privateinternetaccess.android.pia.handlers.PiaPrefHandler;
import com.privateinternetaccess.android.pia.interfaces.IAccount;
import com.privateinternetaccess.android.pia.model.PIAAccountData;
import com.privateinternetaccess.android.pia.model.UpdateAccountInfo;
import com.privateinternetaccess.android.pia.utils.AppUtilities;
import com.privateinternetaccess.android.pia.utils.DLog;
import com.privateinternetaccess.android.pia.utils.Toaster;
import com.privateinternetaccess.android.ui.superclasses.BaseActivity;
import com.privateinternetaccess.android.ui.views.PiaxEditText;

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

    @BindView (R.id.settings_account_email_edit) PiaxEditText etEmail;
    @BindView (R.id.settings_account_username) TextView tvUsername;

    @BindView (R.id.settings_account_refer_layout) RelativeLayout lReferralBanner;

    private MenuItem updateItem;

    private boolean tapped;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_secondary);

        initHeader(true, true);
        setTitle(getString(R.string.drawer_account));
        setRightButton(getString(R.string.menu_update));
        DLog.d("AccountActivity", getString(R.string.menu_update));
        setGreenBackground();
        setSecondaryGreenBackground();

        addSnippetToView();

        ButterKnife.bind(this);

        setUpEmail();
    }

    private void addSnippetToView() {
        FrameLayout container = findViewById(R.id.activity_secondary_container);
        View view = getLayoutInflater().inflate(R.layout.snippet_account_details, container, false);
        container.addView(view);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(EMAIL, etEmail.getText().toString());
    }

    @Override
    public void onRightButtonClicked(View view) {
        if(!tapped) {
            final String email = etEmail.getText().toString();
            if (!AppUtilities.isValidEmail(email))
                etEmail.setError(getString(R.string.invalid_email_signup));
            else {
                final EditText input = new EditText(AccountActivity.this);
                final LinearLayout layout = new LinearLayout(AccountActivity.this);
                layout.setOrientation(LinearLayout.VERTICAL);

                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT);
                layout.setLayoutParams(lp);
                layout.setPadding(40, 0, 40,0);
                layout.addView(input);

                input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

                AlertDialog.Builder alert = new AlertDialog.Builder(AccountActivity.this)
                        .setTitle(R.string.password_required)
                        .setMessage(R.string.password_required_body)
                        .setView(layout)
                        .setPositiveButton(R.string.submit, null)
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        });

                final AlertDialog dialog = alert.show();
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v)
                    {
                        String password = input.getText().toString();

                        if (!TextUtils.isEmpty(password)) {
                            saveEmailInformation(email, password);
                            dialog.dismiss();
                        }
                        else {
                            input.setError(getString(R.string.password_empty));
                        }
                    }
                });
            }
        }
    }

    private void saveEmailInformation(String email, String password) {
        tapped = true;
        IAccount account = PIAFactory.getInstance().getAccount(getApplicationContext());
        UpdateAccountInfo info = new UpdateAccountInfo(email, password);
        account.updateEmail(info, null);
    }

    @Override
    protected void onResume() {
        super.onResume();

        setUpUserNamePassword();

        setupExpirationDate();
    }

    private void setupExpirationDate() {
        IAccount account = PIAFactory.getInstance().getAccount(getApplicationContext());
        PIAAccountData data = account.getAccountInfo();
        Date date = new Date();
        date.setTime(data.getExpiration_time());
        DLog.d("AccountActivity","" + data.toString());
        Locale current = getResources().getConfiguration().locale;
        DateFormat sdf = SimpleDateFormat.getDateInstance(DateFormat.MEDIUM, current);
        if(!data.isExpired())
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

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null) {
            etEmail.setText(savedInstanceState.getString(EMAIL, PiaPrefHandler.getEmail(this)));
        }
    }

    private void setUpUserNamePassword() {
        tvUsername.setText(PiaPrefHandler.getLogin(getApplicationContext()));

        View copyObj = tvUsername;

        copyObj.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copyTextToClipboard(tvUsername.getText().toString());
            }
        });
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

    private void setUpEmail() {
        etEmail.setText(PiaPrefHandler.getEmail(getApplicationContext()));
        int initialSize = etEmail.getText().toString().length();
        etEmail.setSelection(initialSize);
        etEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    @OnClick (R.id.snippet_refer_button)
    public void onReferClicked() {
        Intent i = new Intent(this, ReferralActivity.class);
        startActivity(i);
        overridePendingTransition(R.anim.left_to_right, R.anim.right_to_left);
    }
}