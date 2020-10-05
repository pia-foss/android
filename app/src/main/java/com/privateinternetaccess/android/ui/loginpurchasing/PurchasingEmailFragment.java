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

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.privateinternetaccess.android.R;
import com.privateinternetaccess.android.pia.handlers.PiaPrefHandler;
import com.privateinternetaccess.android.pia.utils.AppUtilities;
import com.privateinternetaccess.android.ui.views.PiaxEditText;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class PurchasingEmailFragment extends Fragment {

    public static String PRODUCT_ID_SELECTED;

    @BindView(R.id.fragment_purchasing_email) PiaxEditText etEmail;
    @BindView(R.id.fragment_purchasing_you_are_purchasing) TextView tvPurchasing;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_purchasing_email, container, false);
        ButterKnife.bind(this, view);

        etEmail.etMain.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        LoginPurchaseActivity.setupTypeText(getContext(), tvPurchasing, PRODUCT_ID_SELECTED);
    }

    @OnClick(R.id.fragment_purchasing_email_submit)
    public void submitEmail() {
        String email = etEmail.getText();
        if (!AppUtilities.isValidEmail(email)) {
            etEmail.setError(getString(R.string.invalid_email_signup));
            return;
        } else {
            etEmail.setError(null);
        }

        PiaPrefHandler.saveLoginEmail(etEmail.getContext(), email);
    }
}
