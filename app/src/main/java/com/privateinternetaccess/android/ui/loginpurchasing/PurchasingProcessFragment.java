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
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.privateinternetaccess.android.PIAApplication;
import com.privateinternetaccess.android.R;
import com.privateinternetaccess.android.pia.PIAFactory;
import com.privateinternetaccess.android.pia.handlers.PiaPrefHandler;
import com.privateinternetaccess.android.pia.interfaces.IAccount;
import com.privateinternetaccess.android.pia.model.LoginInfo;
import com.privateinternetaccess.android.pia.model.enums.LoginResponseStatus;
import com.privateinternetaccess.android.pia.model.events.LoginEvent;
import com.privateinternetaccess.android.pia.model.events.PurchasingEvent;
import com.privateinternetaccess.android.pia.model.events.TokenEvent;
import com.privateinternetaccess.android.pia.model.events.TrialEvent;
import com.privateinternetaccess.android.pia.model.response.LoginResponse;
import com.privateinternetaccess.android.pia.model.response.PurchasingResponse;
import com.privateinternetaccess.android.pia.model.response.TokenResponse;
import com.privateinternetaccess.android.pia.model.response.TrialResponse;
import com.privateinternetaccess.android.pia.utils.DLog;
import com.privateinternetaccess.android.ui.superclasses.BaseActivity;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by hfrede on 8/24/17.
 */

public class PurchasingProcessFragment extends Fragment {

    private static final String TAG = "PurchaseProcess";

    @BindView(R.id.fragment_purchase_process_progress_area) LinearLayout aProgress;
    @BindView(R.id.fragment_purchase_process_success_area) View aSuccess;
    @BindView(R.id.fragment_purchase_process_failure_area) View aFailure;

    @BindView(R.id.fragment_purchase_process_button) Button button;
    @BindView(R.id.fragment_purchase_process_button_progress) View progress;

    @BindView(R.id.fragment_success_redeemed_username) TextView tvUsername;
    @BindView(R.id.fragment_success_redeemed_password) TextView tvPassword;

    @BindView(R.id.fragment_purchase_process_failure_title) TextView tvFailureTitle;
    @BindView(R.id.fragment_purchase_process_failure_text) TextView tvFailureMessage;

    private boolean firePurchasing;
    private boolean isTrial;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_purchase_process, container, false);
        ButterKnife.bind(this, view);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
        initView();
    }

    @Override
    public void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
    }

    private void initView() {
        PurchasingEvent purchasingEvent = EventBus.getDefault().getStickyEvent(PurchasingEvent.class);
        TrialEvent trialEvent = EventBus.getDefault().getStickyEvent(TrialEvent.class);
        if (purchasingEvent != null || trialEvent != null) {
            int status = 0;
            if(!isTrial) {
                PurchasingResponse purchasingResponse = purchasingEvent.getResponse();
                status = purchasingResponse.getResponseNumber();
            } else {
                TrialResponse trialResponse = trialEvent.getResponse();
                status = trialResponse.getStatus();
            }
            if (status == 200) {
                onSuccess();
            } else if ((purchasingEvent != null && purchasingEvent.getResponse() != null && purchasingEvent.getResponse().wasLastAttempt())
                    || (trialEvent != null)) {
                onFailure();
            } else {
                showProgress();
            }
        } else {
            showProgress();
            if (firePurchasing) {
                firePurchasing = false;
                notifySubscriptionToBackend();
            }
        }
    }

    private void showProgress() {
        aProgress.setVisibility(View.VISIBLE);
        aSuccess.setVisibility(View.GONE);
        aFailure.setVisibility(View.GONE);
        button.setVisibility(View.GONE);

        PurchasingEvent purchasingEvent = EventBus.getDefault().getStickyEvent(PurchasingEvent.class);
        if (PIAApplication.isQA() && purchasingEvent != null && purchasingEvent.getResponse() != null && purchasingEvent.getResponse().getAttempt() > 0) {
            PurchasingResponse response = purchasingEvent.getResponse();
        }
    }

    private void onSuccess() {
        TrialEvent trialEvent = EventBus.getDefault().getStickyEvent(TrialEvent.class);
        PurchasingEvent purchasingEvent = EventBus.getDefault().getStickyEvent(PurchasingEvent.class);
        if(!isTrial) {
            PurchasingResponse response = purchasingEvent.getResponse();
            String username = response.getUsername();
            String password = response.getPassword();
            String email = PiaPrefHandler.getEmail(aSuccess.getContext());

            tvUsername.setText(username);
            tvPassword.setText(password);

            button.setText(R.string.get_started);
            button.setVisibility(View.VISIBLE);

            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    PurchasingEvent event = EventBus.getDefault().getStickyEvent(PurchasingEvent.class);
                    PurchasingResponse response = event.getResponse();
                    if (!TextUtils.isEmpty(response.getPassword())) {
                        IAccount account = PIAFactory.getInstance().getAccount(view.getContext());
                        LoginInfo info = new LoginInfo(response.getUsername(), response.getPassword());
                        account.login(info, null);
                        button.setVisibility(View.INVISIBLE);
                        progress.setVisibility(View.VISIBLE);
                    } else {
                        LoginResponse loginResponse = new LoginResponse();
                        loginResponse.setLrStatus(LoginResponseStatus.OP_FAILED);
                        EventBus.getDefault().postSticky(new LoginEvent(loginResponse));
                    }
                }
            });
        } else {
            TrialResponse trialResp = trialEvent.getResponse();
            tvUsername.setText(trialResp.getUsername());
            tvPassword.setText(trialResp.getPassword());

            button.setVisibility(View.VISIBLE);
            button.setText(R.string.get_started);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    TrialEvent event = EventBus.getDefault().getStickyEvent(TrialEvent.class);
                    TrialResponse response = event.getResponse();
                    if (!TextUtils.isEmpty(response.getPassword())) {
                        IAccount account = PIAFactory.getInstance().getAccount(v.getContext());
                        LoginInfo info = new LoginInfo(response.getUsername(), response.getPassword());
                        account.login(info, null);
                        button.setVisibility(View.INVISIBLE);
                        progress.setVisibility(View.VISIBLE);
                    } else {
                        LoginResponse loginResponse = new LoginResponse();
                        loginResponse.setLrStatus(LoginResponseStatus.OP_FAILED);
                        EventBus.getDefault().postSticky(new LoginEvent(loginResponse));
                    }
                }
            });
        }

        aProgress.setVisibility(View.GONE);
        aSuccess.setVisibility(View.VISIBLE);
        aFailure.setVisibility(View.GONE);
    }

    private void onFailure() {
        boolean connected = PIAApplication.isNetworkAvailable(aProgress.getContext());
        if(!isTrial) {
            if (!connected) {
                tvFailureTitle.setText(R.string.no_internet_title);
                tvFailureMessage.setText(R.string.no_internet_message);

                button.setText(R.string.try_again);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        aFailure.setVisibility(View.GONE);
                        aSuccess.setVisibility(View.GONE);
                        aProgress.setVisibility(View.VISIBLE);
                        button.setVisibility(View.INVISIBLE);
                        notifySubscriptionToBackend();
                    }
                });
            } else {
                tvFailureTitle.setText(R.string.account_creation_failure);
                tvFailureMessage.setText(getString(R.string.account_creation_failure_message_no_ticket_id));

                button.setText(R.string.go_to_login);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        LoginResponse response = new LoginResponse();
                        response.setLrStatus(LoginResponseStatus.OP_FAILED);
                        EventBus.getDefault().postSticky(new LoginEvent(response));
                    }
                });

            }
        } else {
            TrialEvent event = EventBus.getDefault().getStickyEvent(TrialEvent.class);
            String message = event.getResponse().getResponse();
            if(message != null){
                int imageResId = R.drawable.ic_account_creation_failed;
                int titleResId = R.string.account_creation_failure;
                int messageResId = R.string.account_creation_failure_message;
                if(message.equals("bad_email")) { //Invalid email address
                    titleResId = R.string.trial_failure_email_title;
                    messageResId = R.string.trial_failure_email_message;
                } else if(message.equals("redeemed")) { // Pin code already redeemed
                    titleResId = R.string.trial_failure_redeemed_title;
                    messageResId = R.string.trial_failure_redeemed_message;
                    imageResId = R.drawable.ic_trial_account_claimed;
                } else if(message.equals("not_found") || message.equals("canceled")) { // Pin code not found or pin code canceled
                    titleResId = R.string.trial_failure_invalid_title;
                    messageResId = R.string.trial_failure_invalid_message;
                    imageResId = R.drawable.ic_invalid_card;
                } else { // throttled
                    titleResId = R.string.trial_failure_throttled_title;
                    messageResId = R.string.trial_failure_throttled_message;
                }

                tvFailureTitle.setText(titleResId);
                tvFailureMessage.setText(messageResId);
            }

            button.setText(R.string.try_again);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    EventBus.getDefault().removeStickyEvent(TrialEvent.class);
                    getActivity().onBackPressed();
                }
            });
        }
        button.setVisibility(View.VISIBLE);
        aProgress.setVisibility(View.GONE);
        aSuccess.setVisibility(View.GONE);
        aFailure.setVisibility(View.VISIBLE);
    }

    /**
     * notifies the backend. It will check if the email is valid.
     */
    public void notifySubscriptionToBackend() {
        if(!isTrial)
            PIAFactory.getInstance().getAccount(aProgress.getContext()).startPurchaseProcess(null);
        else
            PIAFactory.getInstance().getAccount(aProgress.getContext()).createTrialAccount(null);
    }

    @Subscribe
    public void onLogin(TokenEvent event) {
        TokenResponse response = event.getResponse();
        // this will handle signing in. Might have to go back to the login page.
        if (response.getStatus() == LoginResponseStatus.CONNECTED) {
            ((BaseActivity) getActivity()).goToMainActivity();
            if(isTrial)
                EventBus.getDefault().removeStickyEvent(TrialEvent.class);

            EventBus.getDefault().removeStickyEvent(TokenEvent.class);
        } else {
            ((LoginPurchaseActivity) getActivity()).switchToLogin();
        }
        EventBus.getDefault().removeStickyEvent(PurchasingEvent.class);
    }


    @Subscribe
    public void onSignUp(PurchasingEvent event) {
        DLog.d(TAG, "event = " + event.getResponse().toString());
        initView();
    }

    @Subscribe
    public void onTrialCreation(TrialEvent response){
        initView();
    }

    public void setFirePurchasing(boolean firePurchasing) {
        this.firePurchasing = firePurchasing;
    }

    public void setTrial(boolean trial) {
        isTrial = trial;
    }
}