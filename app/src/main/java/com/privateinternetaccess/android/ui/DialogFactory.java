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

package com.privateinternetaccess.android.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;

import androidx.core.view.ViewCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.privateinternetaccess.android.R;

import butterknife.BindView;
import butterknife.ButterKnife;

public class DialogFactory {

    private Context mContext;

    @BindView(R.id.dialog_body) FrameLayout bodyView;
    @BindView(R.id.dialog_button_layout) RelativeLayout buttonLayout;
    @BindView(R.id.dialog_left_buttons) LinearLayout leftLayout;
    @BindView(R.id.dialog_right_buttons) LinearLayout rightLayout;

    @BindView(R.id.dialog_positive_button) TextView positiveButton;
    @BindView(R.id.dialog_negative_button) TextView negativeButton;
    @BindView(R.id.dialog_neutral_button) TextView neutralButton;

    @BindView(R.id.dialog_title) TextView titleText;

    private RadioGroup radioGroup;

    public DialogFactory(Context context) {
        mContext = context;
    }

    public Dialog buildDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        View body = LayoutInflater.from(mContext).inflate(R.layout.view_base_dialog, null);

        ButterKnife.bind(this, body);

        builder.setView(body);

        return builder.create();
    }

    public void addRadioGroup(String[] options, String selected) {
        View radioLayout = LayoutInflater.from(mContext).inflate(R.layout.snippet_radio_group, null);
        RadioGroup group = radioLayout.findViewById(R.id.dialog_radio_group);

        for (String option : options) {
            int buttonId = ViewCompat.generateViewId();
            RadioButton button = (RadioButton) LayoutInflater.from(mContext).inflate(R.layout.snippet_dialog_radio_button, null);

            RadioGroup.LayoutParams params = new RadioGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 20, 0, 0);
            button.setLayoutParams(params);

            button.setText(option);
            button.setId(buttonId);
            group.addView(button);

            if (option.equals(selected)) {
                group.check(buttonId);
            }
        }

        bodyView.addView(radioLayout);

        radioGroup = group;
    }

    public String getSelectedItem() {
        if (radioGroup == null) {
            return "";
        }

        RadioButton button  = radioGroup.findViewById(radioGroup.getCheckedRadioButtonId());
        return button.getText().toString();
    }

    public void setBody(View view) {
        bodyView.addView(view);
    }

    public void setMessage(String message) {
        TextView textView = new TextView(mContext);
        textView.setText(message);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(24, 20, 24, 0);
        textView.setLayoutParams(params);

        setBody(textView);
    }

    public void setHeader(String header) {
        titleText.setText(header);
    }

    public void setNeutralButton(String neutral) {
        neutralButton.setText(neutral);
        neutralButton.setVisibility(View.VISIBLE);
        leftLayout.setVisibility(View.VISIBLE);
    }

    public void setNeutralButton(String neutral, View.OnClickListener listener) {
        setNeutralButton(neutral);
        neutralButton.setOnClickListener(listener);
    }

    public void setPositiveButton(String positive) {
        positiveButton.setText(positive.toUpperCase());
        positiveButton.setVisibility(View.VISIBLE);
        buttonLayout.setVisibility(View.VISIBLE);
        rightLayout.setVisibility(View.VISIBLE);
    }

    public void setPositiveButton(String positive, View.OnClickListener listener) {
        setPositiveButton(positive);
        positiveButton.setOnClickListener(listener);
    }

    public void setNegativeButton(String negative) {
        negativeButton.setText(negative.toUpperCase());
        negativeButton.setVisibility(View.VISIBLE);
        buttonLayout.setVisibility(View.VISIBLE);
        rightLayout.setVisibility(View.VISIBLE);
    }

    public void setNegativeButton(String negative, View.OnClickListener listener) {
        setNegativeButton(negative);
        negativeButton.setOnClickListener(listener);
    }
}
