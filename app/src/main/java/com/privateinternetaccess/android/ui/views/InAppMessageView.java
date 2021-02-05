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
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.privateinternetaccess.account.model.response.MessageInformation;
import com.privateinternetaccess.android.R;
import com.privateinternetaccess.android.pia.utils.DLog;
import com.privateinternetaccess.android.utils.InAppMessageManager;

import java.util.Locale;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class InAppMessageView extends FrameLayout {

    @BindView(R.id.view_inapp_text) TextView tvMessage;

    public InAppMessageView(Context context) {
        super(context);
        init(context);
    }

    public InAppMessageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public InAppMessageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public void init(Context context) {
        inflate(context, R.layout.view_inapp_message, this);
        ButterKnife.bind(this, this);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        showMessage();
    }

    private void showMessage() {
        MessageInformation inappMessage = InAppMessageManager.getActiveMessage(getContext());

        if (inappMessage != null) {
            SpannableStringBuilder localizedMessage = InAppMessageManager.showMessage(getContext(), new ClickableSpan() {
                @Override
                public void onClick(@NonNull View view) {
                    InAppMessageManager.handleLink(getContext(), inappMessage);
                    showMessage();
                }
            });

            this.setVisibility(View.VISIBLE);

            tvMessage.setMovementMethod(LinkMovementMethod.getInstance());
            tvMessage.setText(localizedMessage, TextView.BufferType.SPANNABLE);
        }
        else {
            this.setVisibility(View.GONE);
        }
    }

    @OnClick(R.id.view_inapp_close)
    public void onDismissClicked() {
        InAppMessageManager.dismissMessage(getContext());
        showMessage();
    }
}
