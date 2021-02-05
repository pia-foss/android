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

package com.privateinternetaccess.android.ui.views;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import androidx.core.view.ViewCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.appcompat.widget.AppCompatImageView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.privateinternetaccess.android.R;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class PiaxEditText extends FrameLayout {

    @BindView(R.id.piaxEditText) public EditText etMain;
    @BindView(R.id.piaxEditTextButtonsLayout) LinearLayout llButtons;
    @BindView(R.id.piaxEditTextHint)TextView tvHint;
    @BindView(R.id.piaxEditTextIcon) AppCompatImageView ivIcon;
    @BindView(R.id.piaxEditTextLeftIcon) AppCompatImageView ivLeftIcon;
    @BindView(R.id.piaxEditTextLayout) LinearLayout llBody;
    @BindView(R.id.piaxEditTextError) TextView tvError;
    @BindView(R.id.piaxEditTextUnderline) View vUnderline;

    private List<View> buttonList;

    private String hint;

    private int errorColor;
    private int hintColor;
    private int textColor;
    private int maxLines;

    private boolean showLeftIcon;
    private boolean hideHint;

    private int underlineColor;

    public PiaxEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        inflate(context, R.layout.view_piax_edit_text, this);
        ButterKnife.bind(this);

        buttonList = new ArrayList<>();

        if (attrs != null) {
            TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.PiaxEditText);

            hint = typedArray.getString(R.styleable.PiaxEditText_hint);
            hintColor = typedArray.getColor(R.styleable.PiaxEditText_hintColor, context.getResources().getColor(R.color.grey55));
            errorColor = typedArray.getColor(R.styleable.PiaxEditText_errorColor, context.getResources().getColor(R.color.red));
            textColor = typedArray.getColor(R.styleable.PiaxEditText_textColorMain, context.getResources().getColor(R.color.grey20));
            underlineColor = typedArray.getColor(R.styleable.PiaxEditText_underlineColor, context.getResources().getColor(R.color.grey85));
            maxLines = typedArray.getInteger(R.styleable.PiaxEditText_maxLines, 0);

            hideHint = typedArray.getBoolean(R.styleable.PiaxEditText_hideHint, false);
            showLeftIcon = typedArray.getBoolean(R.styleable.PiaxEditText_showLeftIcon, false);

            tvHint.setText(hint);
            tvHint.setTextColor(hintColor);

            etMain.setTextColor(textColor);
            etMain.setHintTextColor(hintColor);
            etMain.setHint(hint);

            if (maxLines != 0) {
                etMain.setMaxLines(maxLines);
            }

            ivLeftIcon.setVisibility(showLeftIcon ? View.VISIBLE : View.GONE);

            vUnderline.setBackgroundColor(underlineColor);

            typedArray.recycle();
        }

        etMain.addTextChangedListener(baseTextWatcher);
        etMain.setOnFocusChangeListener((view, b) -> showHint());

        ivIcon.setVisibility(View.GONE);
    }

    public String getText() {
        return etMain.getText().toString();
    }

    public void setText(String text) {
        etMain.setText(text);
    }

    public void addButton(View button) {
        if (button != null) {
            buttonList.add(button);
            llButtons.addView(button);
        }

        showButtons();
    }

    public void addTextChangedListener(TextWatcher watcher) {
        etMain.addTextChangedListener(watcher);
    }

    public void setOnEditorActionListener(TextView.OnEditorActionListener listener) {
        etMain.setOnEditorActionListener(listener);
    }

    public void setSelection(int index) {
        etMain.setSelection(index);
    }

    public void removeButton(Button button) {
        if (button != null) {
            buttonList.remove(button);
            llButtons.removeView(button);
        }

        showButtons();
    }

    private void showButtons() {
        if (buttonList.size() > 0) {
            llButtons.setVisibility(View.VISIBLE);
        }
        else {
            llButtons.setVisibility(View.GONE);
        }
    }

    private void removeError() {
        tvError.setText("");
        tvError.setVisibility(View.GONE);
        ivIcon.setVisibility(View.GONE);

        tvHint.setTextColor(hintColor);
        etMain.setTextColor(textColor);
        etMain.setHintTextColor(hintColor);
        vUnderline.setBackgroundColor(underlineColor);

        for (int i = 0; i < llButtons.getChildCount(); i++) {
            View v = llButtons.getChildAt(i);

            if (v instanceof ImageView) {
                ImageViewCompat.setImageTintList((ImageView)v, null);
            }

            ViewCompat.setBackgroundTintList(v, null);
        }
    }

    public void setError(String error) {
        if (error == null) {
            removeError();
            return;
        }

        if (error.length() > 0) {
            tvError.setText(error);
            tvError.setVisibility(View.VISIBLE);
        }

        ivIcon.setVisibility(View.VISIBLE);

        tvHint.setTextColor(errorColor);
        etMain.setTextColor(errorColor);
        etMain.setHintTextColor(errorColor);
        vUnderline.setBackgroundColor(errorColor);

        for (int i = 0; i < llButtons.getChildCount(); i++) {
            View v = llButtons.getChildAt(i);

            if (v instanceof ImageView) {
                ImageViewCompat.setImageTintList((ImageView)v, ColorStateList.valueOf(errorColor));
            }

            ViewCompat.setBackgroundTintList(v, ColorStateList.valueOf(errorColor));
        }
    }

    private TextWatcher baseTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
            showHint();

            if (before != count) {
                removeError();
            }
        }

        @Override
        public void afterTextChanged(Editable editable) {

        }
    };

    private void showHint() {
        if (hideHint) {
            tvHint.setVisibility(View.GONE);
        }
        else if (etMain.getText().length() > 0 || etMain.isFocused()) {
            tvHint.setVisibility(View.VISIBLE);
            etMain.setHint("");
        }
        else {
            tvHint.setVisibility(View.GONE);
            etMain.setHint(hint);
        }
    }

}
