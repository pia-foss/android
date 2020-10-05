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
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.privateinternetaccess.android.BuildConfig;
import com.privateinternetaccess.android.R;
import com.privateinternetaccess.android.model.events.PricingLoadedEvent;
import com.privateinternetaccess.android.utils.SubscriptionsUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.regex.Pattern;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class FreeTrialFragment extends Fragment {

    @BindView(R.id.fragment_trial_cost) TextView tvCost;
    @BindView(R.id.fragment_trial_terms) TextView tvToS;

    private boolean pricesLoaded = false;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_free_trial, container, false);
        ButterKnife.bind(this, view);

        LoginPurchaseActivity.setupToSPPText(getActivity(), tvToS);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
    }

    @OnClick(R.id.fragment_trial_start)
    public void startSubscriptionClicked() {
        LoginPurchaseActivity loginPurchaseActivity = (LoginPurchaseActivity) getActivity();
        if (!BuildConfig.FLAVOR_store.equals("playstore")) {
            loginPurchaseActivity.navigateToBuyVpnSite();
            return;
        }

        if (pricesLoaded) {
            ((LoginPurchaseActivity) getActivity()).onContinuePurchasingClicked(
                    SubscriptionsUtils.INSTANCE.getYearlySubscriptionId(getContext())
            );
        }
        else {
            ((LoginPurchaseActivity) getActivity()).showConnectionError();
        }
    }

    @OnClick(R.id.fragment_trial_see_all)
    public void seeAllClicked() {
        LoginPurchaseActivity loginPurchaseActivity = (LoginPurchaseActivity) getActivity();
        if (!BuildConfig.FLAVOR_store.equals("playstore")) {
            loginPurchaseActivity.navigateToBuyVpnSite();
            return;
        }

        if (pricesLoaded) {
            ((LoginPurchaseActivity) getActivity()).switchToPurchasing();
        } else {
            ((LoginPurchaseActivity) getActivity()).showConnectionError();
        }
    }

    @Subscribe(sticky = true)
    public void loadPricing(PricingLoadedEvent event) {
        setUpCosts(event.yearlyCost);
    }

    private void setUpCosts(String yearly){
        String costText = String.format(getString(R.string.free_trial_cost), yearly);
        String[] splitCost = costText.split(Pattern.quote(yearly));

        int costStart = splitCost[0].length();
        Spannable spanText = new SpannableString(costText);

        spanText.setSpan(new ForegroundColorSpan(ContextCompat.getColor(getContext(), R.color.trial_bold)),
                costStart, costStart + yearly.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        tvCost.setText(spanText);

        if(!TextUtils.isEmpty(yearly)){
            pricesLoaded = true;
        }
        else {
            pricesLoaded = false;
        }
    }
}
