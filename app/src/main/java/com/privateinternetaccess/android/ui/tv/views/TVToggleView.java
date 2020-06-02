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

package com.privateinternetaccess.android.ui.tv.views;

import android.content.Context;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import android.util.AttributeSet;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.privateinternetaccess.android.R;
import com.privateinternetaccess.android.pia.utils.Prefs;


import butterknife.BindView;
import butterknife.ButterKnife;

public class TVToggleView extends RelativeLayout {

    @BindView(R.id.toggle_switch) SwitchCompat toggleSwitch;
    @BindView(R.id.toggle_title) TextView tvTitle;

    private String prefKey;

    public TVToggleView(Context context) {
        super(context);
        init(context);
    }

    public TVToggleView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public TVToggleView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public void init(final Context context) {
        inflate(context, R.layout.view_toggle, this);
        ButterKnife.bind(this, this);

        toggleSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                Prefs.with(context).set(prefKey, b);
            }
        });
    }

    public void toggle() {
        toggleSwitch.toggle();
    }

    public void setTitle(String title) {
        tvTitle.setText(title);
    }

    public void setPrefKey(String key) {
        prefKey = key;
        toggleSwitch.setChecked(Prefs.with(getContext()).getBoolean(key));
    }

}
