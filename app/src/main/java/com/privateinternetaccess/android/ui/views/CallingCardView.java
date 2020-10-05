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
import android.content.Intent;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.privateinternetaccess.android.R;
import com.privateinternetaccess.android.pia.utils.DLog;
import com.privateinternetaccess.android.ui.drawer.SettingsActivity;
import com.privateinternetaccess.android.ui.features.WebviewActivity;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Optional;

import static com.privateinternetaccess.android.ui.connection.MainActivity.THEME_CHANGED;

public class CallingCardView extends FrameLayout {

    @BindView(R.id.update_cta1_button) Button bCta1;
    @BindView(R.id.update_cta2_button) Button bCta2;

    @BindView(R.id.update_description_text) TextView tvDescription;
    @BindView(R.id.update_header_text) TextView tvHeader;

    public CallingCardView(Context context) {
        super(context);
        init(context);
    }

    public CallingCardView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public CallingCardView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        inflate(context, R.layout.view_calling_card, this);

        FrameLayout contentLayout = findViewById(R.id.update_content_layout);
        inflate(context, R.layout.update_3_7_1_layout, contentLayout);

        ButterKnife.bind(this, this);
    }

    @OnClick(R.id.update_cta1_button)
    public void onCta1Clicked() {
        DLog.d("CallingCard", "CTA1");
        Intent i = new Intent(getContext(), SettingsActivity.class);
        getContext().startActivity(i);
    }

    @OnClick(R.id.update_cta2_button)
    public void onCta2Clicked() {
        DLog.d("CallingCard", "CTA2");
        Intent i = new Intent(getContext(), WebviewActivity.class);
        i.putExtra(WebviewActivity.EXTRA_URL, "https://www.privateinternetaccess.com/blog/wireguide-all-about-the-wireguard-vpn-protocol/");
        getContext().startActivity(i);
    }
}
