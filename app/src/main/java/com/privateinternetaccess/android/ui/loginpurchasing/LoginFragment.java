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

import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.privateinternetaccess.android.BuildConfig;
import com.privateinternetaccess.android.PIAApplication;
import com.privateinternetaccess.android.R;
import com.privateinternetaccess.android.pia.PIAFactory;
import com.privateinternetaccess.android.pia.handlers.PIAServerHandler;
import com.privateinternetaccess.android.pia.handlers.PiaPrefHandler;
import com.privateinternetaccess.android.pia.interfaces.IAccount;
import com.privateinternetaccess.android.pia.model.enums.RequestResponseStatus;
import com.privateinternetaccess.android.pia.utils.DLog;
import com.privateinternetaccess.android.pia.utils.Toaster;
import com.privateinternetaccess.android.ui.views.PiaxEditText;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Optional;

/**
 * Created by half47 on 3/8/17.
 */

public class LoginFragment extends Fragment {

    @Nullable
    @BindView(R.id.fragment_login_user) PiaxEditText etLogin;
    @Nullable
    @BindView(R.id.fragment_login_password) PiaxEditText etPassword;

    @BindView(R.id.fragment_login_button) Button bLogin;
    @BindView(R.id.fragment_login_progress) View progress;

    @Nullable
    @BindView(R.id.fragment_login_receipt) TextView bLoginReceipt;

    @Nullable
    @BindView(R.id.fragment_tv_login_user) EditText tvLogin;
    @Nullable
    @BindView(R.id.fragment_tv_login_password) EditText tvPassword;

    private static final String TAG = "LoginFragment";

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view;
        if(!PIAApplication.isAndroidTV(getContext()))
            view = inflater.inflate(R.layout.fragment_login, container, false);
        else {
            view = inflater.inflate(R.layout.activity_tv_login_purchasing, container, false);
        }

        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initView();
    }

    private void initView() {
        loadUserPW();

        if (!PIAApplication.isAndroidTV(getContext())) {
            // prevent white space in the text.
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
                        // alert the user
                    }
                }
            });

            etPassword.setOnEditorActionListener((v, actionId, event) -> {
                boolean handled = false;
                DLog.d("Login Activity Action", "actionid = " + actionId + " event = " + event);
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    bLogin.callOnClick();
                    handled = true;
                }
                return handled;
            });

            etPassword.setOnKeyListener((view, i, keyEvent) -> {
                boolean handled = false;
                if((keyEvent.getAction() == KeyEvent.ACTION_DOWN) && i == KeyEvent.KEYCODE_ENTER){
                    bLogin.callOnClick();
                    handled = true;
                }
                return handled;
            });

            etPassword.etMain.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            etPassword.etMain.setTypeface(Typeface.SANS_SERIF);
            etLogin.etMain.setInputType(InputType.TYPE_CLASS_TEXT);

            if (BuildConfig.FLAVOR_store.equals("amazonstore")) {
                bLoginReceipt.setVisibility(View.GONE);
            }
        }

        bLogin.setOnClickListener(v -> onLoginClick());
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    public void loadUserPW() {
        if (PIAApplication.isAndroidTV(getContext())) {
            return;
        }

        boolean defaultLoad = true;
        Context context = getContext();
        if (getActivity().getIntent() != null) {
            Bundle extras = getActivity().getIntent().getExtras();
            if (extras != null && extras.getString("login") != null && extras.getString("password") != null) {
                defaultLoad = false;
                etLogin.setText(extras.getString("login"));
                etPassword.setText(extras.getString("password"));
            }
        }

        if (defaultLoad) {
            String login = PiaPrefHandler.getLogin(context);
            if (login.startsWith("x")) {
                login = "";
            }

            etLogin.setText(login);
            etLogin.setSelection(etLogin.getText().length());
        }
    }

    private String getPassword() {
        if (!PIAApplication.isAndroidTV(getContext()))
            return etPassword.getText().toString();
        else
            return tvPassword.getText().toString();
    }

    private String getUsername() {
        if (!PIAApplication.isAndroidTV(getContext()))
            return etLogin.getText().toString().trim();
        else
            return tvLogin.getText().toString().trim();
    }

    public void onLoginClick() {
        Context context = getContext();

        boolean connected = PIAApplication.isNetworkAvailable(context);

        if (!PIAApplication.isAndroidTV(getContext())) {
            if (!TextUtils.isEmpty(etLogin.getText().toString()) && !TextUtils.isEmpty(etPassword.getText().toString()) && connected) {
                startLogin();
            } else if (!connected) {
                Toaster.s(context, R.string.no_internet_connection_available);
            } else {
                etPassword.setError(getContext().getResources().getString(R.string.no_username_or_password));
            }
        }
        else {
            if (!TextUtils.isEmpty(tvLogin.getText().toString()) && !TextUtils.isEmpty(tvPassword.getText().toString()) && connected) {
                startLogin();
            } else if (!connected) {
                Toaster.s(context, R.string.no_internet_connection_available);
            } else {
                tvPassword.setError(getContext().getResources().getString(R.string.no_username_or_password));
            }
        }
    }

    public void startLogin() {
        bLogin.setEnabled(false);
        bLogin.setVisibility(View.INVISIBLE);
        progress.setVisibility(View.VISIBLE);
        Context context = getContext();

        final IAccount account = PIAFactory.getInstance().getAccount(progress.getContext());
        account.loginWithCredentials(getUsername(), getPassword(), (token, requestResponseStatus) -> {
            boolean loginSuccessful = false;
            switch (requestResponseStatus) {
                case SUCCEEDED:
                    loginSuccessful = !TextUtils.isEmpty(token);
                    break;
                case AUTH_FAILED:
                case THROTTLED:
                case OP_FAILED:
                    break;
            }

            if (!loginSuccessful) {
                DLog.d(TAG, "Login unsuccessful " + requestResponseStatus);
                handleLoginResponseStatus(context, requestResponseStatus);
                return null;
            }

            account.accountInformation(token, (accountInformation, accountResponseStatus) -> {
                boolean accountSuccessful = false;
                switch (accountResponseStatus) {
                    case SUCCEEDED:
                        accountSuccessful = accountInformation != null;
                        break;
                    case AUTH_FAILED:
                    case THROTTLED:
                    case OP_FAILED:
                        break;
                }

                if (!accountSuccessful) {
                    DLog.d(TAG, "Check account information unsuccessful " + accountResponseStatus);
                    handleLoginResponseStatus(context, accountResponseStatus);
                    return null;
                }

                PiaPrefHandler.setLogin(context, getUsername());
                PiaPrefHandler.saveAuthToken(context, token);
                PiaPrefHandler.setUserIsLoggedIn(context, true);
                PiaPrefHandler.saveAccountInformation(context, accountInformation);
                PiaPrefHandler.clearPurchasingInfo(context);
                handleLoginResponseStatus(context, accountResponseStatus);
                return null;
            });
            return null;
        });
    }

    public void handleLoginResponseStatus(Context context, RequestResponseStatus loginResponseStatus) {
        if (!PIAApplication.isAndroidTV(context)) {
            if (loginResponseStatus == RequestResponseStatus.SUCCEEDED) {
                ((LoginPurchaseActivity) getActivity()).goToMainActivity();

                // Update the list of server upon successful login.
                // GEN4 setting is reset on logout and we need to make sure we show the proper list.
                PIAServerHandler.getInstance(context).fetchServers(context, true);
            } else if (loginResponseStatus == RequestResponseStatus.AUTH_FAILED) {
                // auth failed
                String username = getUsername();
                if (username.matches("\\A\\s*x\\d+\\s*\\z")) {
                    etLogin.setError(getString(R.string.user_x_vs_p_error));
                    etPassword.setText("");
                } else {
                    etLogin.setError("");
                    etPassword.setError(getString(R.string.user_pw_invalid));
                }
                etLogin.setSelection(etLogin.getText().toString().length());
                etLogin.requestFocus();
                progress.setVisibility(View.INVISIBLE);
                bLogin.setVisibility(View.VISIBLE);
                bLogin.setEnabled(true);
            } else if (loginResponseStatus == RequestResponseStatus.THROTTLED){
                etPassword.setError(getContext().getResources().getString(R.string.login_throttled_text));
                progress.setVisibility(View.INVISIBLE);
                bLogin.setVisibility(View.VISIBLE);
                bLogin.setEnabled(true);
            } else {
                // operation failed
                //Display SOMETHING HERE, should be more limited now.
                etPassword.setError(getContext().getResources().getString(R.string.login_operation_failed));
                progress.setVisibility(View.INVISIBLE);
                bLogin.setVisibility(View.VISIBLE);
                bLogin.setEnabled(true);
            }
        } else {
            if (loginResponseStatus == RequestResponseStatus.SUCCEEDED) {
                ((LoginPurchaseActivity) getActivity()).goToMainActivity();
            } else if (loginResponseStatus == RequestResponseStatus.AUTH_FAILED) {
                // auth failed
                String username = getUsername();
                if (username.matches("\\A\\s*x\\d+\\s*\\z")) {
                    tvLogin.setError(getString(R.string.user_x_vs_p_error));
                } else {
                    tvPassword.setError(getString(R.string.user_pw_invalid));
                }
                tvLogin.setSelection(tvLogin.getText().toString().length());
                tvLogin.requestFocus();
                progress.setVisibility(View.INVISIBLE);
                bLogin.setVisibility(View.VISIBLE);
                bLogin.setEnabled(true);
            } else if (loginResponseStatus == RequestResponseStatus.THROTTLED){
                tvPassword.setError(getContext().getResources().getString(R.string.login_throttled_text));
                progress.setVisibility(View.INVISIBLE);
                bLogin.setVisibility(View.VISIBLE);
                bLogin.setEnabled(true);
            } else {
                // operation failed
                //Display SOMETHING HERE, should be more limited now.
                tvPassword.setError(getContext().getResources().getString(R.string.login_operation_failed));
                progress.setVisibility(View.INVISIBLE);
                bLogin.setVisibility(View.VISIBLE);
                bLogin.setEnabled(true);
            }
        }
    }

    @Optional
    @OnClick(R.id.fragment_login_receipt)
    public void onReceiptClicked() {
        DLog.d("PIAAPI", "Has set email: " + PiaPrefHandler.hasSetEmail(getContext()));
        if (((LoginPurchaseActivity) getActivity()).getActiveSubscription() == null) {
            Toaster.l(getContext(), R.string.purchasing_no_subscription);
        }
        else if (PiaPrefHandler.hasSetEmail(getContext())) {
            Toaster.l(getContext(), R.string.error_active_subscription);
        }
        else {
            ((LoginPurchaseActivity) getActivity()).switchToPurchasingProcess(true, false);
        }
    }

    @Optional
    @OnClick(R.id.fragment_login_magic_link)
    public void onMagicLinkClicked() {
        ((LoginPurchaseActivity) getActivity()).switchToMagicLogin();
    }
}