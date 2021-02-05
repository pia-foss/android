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
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.privateinternetaccess.android.R;
import com.privateinternetaccess.android.pia.PIAFactory;
import com.privateinternetaccess.android.pia.interfaces.IAccount;
import com.privateinternetaccess.android.pia.model.enums.RequestResponseStatus;
import com.privateinternetaccess.android.pia.utils.AppUtilities;
import com.privateinternetaccess.android.pia.utils.Toaster;
import com.privateinternetaccess.android.ui.views.PiaxEditText;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MagicLoginFragment extends Fragment {

    @BindView(R.id.fragment_login_user_magic) PiaxEditText etLogin;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_magic_login, container,false);
        ButterKnife.bind(this, view);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        initView();
    }

    private void initView() {
        etLogin.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String result = s.toString().replaceAll(" ", "");
                if (!s.toString().equals(result)) {
                    etLogin.setText(result);
                    etLogin.setSelection(result.length());
                }
            }
        });

        etLogin.etMain.setInputType(InputType.TYPE_CLASS_TEXT);
    }

    @OnClick(R.id.fragment_send_button)
    public void onSendClicked() {
        String email = etLogin.getText();

        if (TextUtils.isEmpty(email) || !AppUtilities.isValidEmail(email)) {
            etLogin.setError(getString(R.string.invalid_email_signup));
        }
        else {
            final IAccount account = PIAFactory.getInstance().getAccount(getContext());
            account.loginLink(email, (status) -> {
                if (status == RequestResponseStatus.SUCCEEDED) {
                    Toaster.l(getContext(), R.string.purchasing_magic_success);
                    getActivity().onBackPressed();
                }
                else {
                    Toaster.l(getContext(), R.string.api_check_failure_title);
                }
                return null;
            });
        }
    }
}
