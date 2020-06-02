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

package com.privateinternetaccess.android.ui.loginpurchasing;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.zxing.integration.android.IntentIntegrator;
import com.privateinternetaccess.android.R;
import com.privateinternetaccess.android.pia.handlers.PiaPrefHandler;
import com.privateinternetaccess.android.pia.model.TrialData;
import com.privateinternetaccess.android.pia.utils.AppUtilities;
import com.privateinternetaccess.android.ui.views.PiaxEditText;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 *
 * Used to collect the trial account information.
 *
 * Created by HALF 5/7/18
 */
public class TrialFragment extends Fragment{

    public static final int MAX_LENGTH_CARD_PIN = 16;

    @BindView(R.id.fragment_trial_email) PiaxEditText etEmail;
    @BindView(R.id.fragment_trial_card_pin) PiaxEditText etPin;

    @BindView(R.id.fragment_trial_submit) Button bSubmit;
    @BindView(R.id.fragment_trial_TOS) TextView tvTosPP;

    private ViewTreeObserver.OnGlobalLayoutListener listener;

    @BindView(R.id.fragment_trial_qr) ImageView qrCode;

    //@BindView(R.id.fragment_trial_warning_layout) RelativeLayout rlWarning;
    //@BindView(R.id.fragment_trial_warning_text) TextView tvWarning;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_trial_account, container, false);
        ButterKnife.bind(this, view);

        ((ViewGroup)qrCode.getParent()).removeView(qrCode);

        etPin.addButton(qrCode);
        qrCode.setVisibility(View.VISIBLE);

        etEmail.etMain.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        etPin.etMain.setInputType(InputType.TYPE_CLASS_NUMBER);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        initView();
    }

    private void initView() {
        etPin.etMain.addTextChangedListener(new TextWatcher() {
            int count = 0;
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                this.count = s.length();
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if(count < s.toString().length()){
                    etPin.etMain.removeTextChangedListener(this);
                    StringBuilder sb = new StringBuilder(s.toString());
                    for (int i = 4; i < 15 && i < sb.length(); i = i + 5) {
                        if (sb.charAt(i) != '-')
                            sb.insert(i, "-");
                    }
                    etPin.etMain.setText(sb.toString());
                    etPin.etMain.setSelection(sb.length());
                    etPin.etMain.addTextChangedListener(this);
                }
            }
        });

        LoginPurchaseActivity.setupToSPPText(getActivity(), tvTosPP);

        bSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = etEmail.getText().toString();
                String cardPin = etPin.getText().toString();
                etEmail.setError(null);
                etPin.setError(null);
                boolean validEmail = AppUtilities.isValidEmail(email);
                String cleanedPin = cardPin.replaceAll("-", "");
                boolean validPin = !TextUtils.isEmpty(cleanedPin) && cleanedPin.length() == 16;
                if(validEmail && validPin) {
                    //send off
                    PiaPrefHandler.saveTempTrialData(etEmail.getContext(), new TrialData(email, cleanedPin));
                    ((LoginPurchaseActivity) getActivity()).switchToPurchasingProcess(true, true);
                    //((LoginPurchaseActivity) getActivity()).switchToProgress();
                }
                if(!validEmail) {
                    etEmail.setError(getContext().getResources().getString(R.string.invalid_email_signup));
                }
                if (!validPin) {
                    etPin.setError(getContext().getResources().getString(R.string.card_pin_invalid));
                }
            }
        });

        qrCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getContext(), QRScanActivity.class);
                startActivityForResult(i, IntentIntegrator.REQUEST_CODE);
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        String email = etEmail.getText().toString();
        if (!TextUtils.isEmpty(email) && AppUtilities.isValidEmail(email)) {
            PiaPrefHandler.saveTrialEmail(etEmail.getContext(), email);
        }
    }

    public void onGiftCardQRCodeReceived(String contents){
        if(!TextUtils.isEmpty(contents)){
            if(contents.matches("\\d{16}")) {
                StringBuilder sb = new StringBuilder();
                sb.append(contents.substring(0,4));
                sb.append("-");
                sb.append(contents.substring(4,8));
                sb.append("-");
                sb.append(contents.substring(8, 12));
                sb.append("-");
                sb.append(contents.substring(12));
                etPin.etMain.setText(sb.toString());
            } else if (contents.matches("\\b\\d{4}(| |-)\\d{4}\\1\\d{4}\\1\\d{4}\\b")) {
                etPin.etMain.setText(contents);
            } else {
                // invalid sequence
                etPin.setError(getString(R.string.invalid_qr_code));
            }
        } else {
            // Handle the error here
            etPin.setError(getString(R.string.empty_qr_code));
        }
    }
}