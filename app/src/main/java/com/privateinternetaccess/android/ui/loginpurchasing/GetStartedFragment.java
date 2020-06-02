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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.privateinternetaccess.android.BuildConfig;
import com.privateinternetaccess.android.PIAApplication;
import com.privateinternetaccess.android.R;
import com.privateinternetaccess.android.ui.drawer.settings.DeveloperActivity;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class GetStartedFragment extends Fragment {

    @BindView(R.id.activity_login_purchasing_dev_button) View bDev;
    @BindView(R.id.activity_login_purchasing_version_text) TextView tvVersion;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_get_started, container, false);
        ButterKnife.bind(this, view);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        initView();
    }

    private void initView() {
        if(!PIAApplication.isRelease()){
            bDev.setVisibility(View.VISIBLE);
            bDev.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent i = new Intent(getContext(), DeveloperActivity.class);
                    startActivity(i);
                }
            });
        } else {
            bDev.setVisibility(View.GONE);
        }

        String version = BuildConfig.VERSION_NAME;
        int versionCode = BuildConfig.VERSION_CODE;
        tvVersion.setText(String.format(getString(R.string.drawer_version), version, versionCode + ""));
    }

    @OnClick(R.id.activity_login_purchasing_redeem_button)
    public void trialPressed() {
        if (getActivity() instanceof LoginPurchaseActivity) {
            ((LoginPurchaseActivity) getActivity()).switchToTrialAccount();
        }
    }

    @OnClick(R.id.activity_login_purchasing_buy_button)
    public void buyPressed() {
        if (getActivity() instanceof LoginPurchaseActivity) {
            ((LoginPurchaseActivity) getActivity()).switchToFreeTrial();
        }
    }

    @OnClick(R.id.activity_login_purchasing_login_button)
    public void loginPressed() {
        if (getActivity() instanceof LoginPurchaseActivity) {
            ((LoginPurchaseActivity) getActivity()).switchToLogin();
        }
    }
}
