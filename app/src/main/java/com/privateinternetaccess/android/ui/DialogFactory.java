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

import android.util.Pair;
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
import com.privateinternetaccess.android.ui.views.PiaxEditText;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.ktor.http.cio.websocket.Frame;

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
    private PiaxEditText editText;

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

    public void addTextBox() {
        View editLayout = LayoutInflater.from(mContext).inflate(R.layout.snippet_dialog_edit_text, null);
        PiaxEditText editText = editLayout.findViewById(R.id.snippet_dialog_edit_text);

        this.editText = editText;

        bodyView.addView(editLayout);
    }

    /**
     *
     * @param options List<Pair<Integer, String>>. List of pair objects where the first parameter is
     *               the identifier and the second one the text to be used on the widget.
     * @param selectedId Integer. The identifier to be selected
     */
    public void addRadioGroup(
            List<Pair<Integer, String>> options,
            Integer selectedId
    ) {
        View radioLayout = LayoutInflater.from(mContext).inflate(R.layout.snippet_radio_group, null);
        RadioGroup group = radioLayout.findViewById(R.id.dialog_radio_group);

        for (Pair<Integer, String> option : options) {
            RadioButton button = new RadioButton(mContext);
            RadioGroup.LayoutParams params = new RadioGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 20, 0, 0);
            button.setLayoutParams(params);

            button.setText(option.second);
            button.setId(option.first.hashCode());
            group.addView(button);
        }
        group.check(selectedId);

        bodyView.addView(radioLayout);
        radioGroup = group;
    }

    /**
     *
     * @return Integer. The identifier for the selected item
     */
    public Integer getSelectedItem() {
        return radioGroup.getCheckedRadioButtonId();
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

    public String getEditText() {
        if (editText == null) {
            return "";
        }

        return editText.getText();
    }

    public void setEditHint(String hint) {
        if (editText == null) {
            return;
        }

        editText.setHint(hint);
    }

    public void setEditText(String text) {
        if (editText == null) {
            return;
        }

        editText.setText(text);
    }
}
