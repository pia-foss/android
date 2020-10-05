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
import androidx.appcompat.widget.AppCompatImageView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.privateinternetaccess.android.PIAApplication;
import com.privateinternetaccess.android.R;
import com.privateinternetaccess.android.model.events.PricingLoadedEvent;
import com.privateinternetaccess.android.pia.handlers.ThemeHandler;
import com.privateinternetaccess.android.pia.utils.DLog;
import com.privateinternetaccess.android.ui.features.WebviewActivity;
import com.privateinternetaccess.android.utils.SubscriptionsUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.text.DecimalFormat;
import java.util.Currency;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by half47 on 3/8/17.
 */

public class PurchasingFragment extends Fragment {

    private static final String TAG = "Purchasing";

    @BindView(R.id.fragment_purchasing_monthly) RelativeLayout aMonthly;
    @BindView(R.id.fragment_purchasing_yearly) RelativeLayout aYearly;

    @BindView(R.id.fragment_purchasing_monthly_cost) TextView tvMonthlyText;
    @BindView(R.id.fragment_purchasing_yearly_cost) TextView tvYearlyText;
    @BindView(R.id.fragment_purchasing_yearly_total) TextView tvYearlyTotalText;

    @BindView(R.id.fragment_purchasing_button) Button bSubmit;

    @BindView(R.id.fragment_purchasing_TOS) TextView tvToS;

    @BindView(R.id.fragment_purchasing_progress) View progress;

    @BindView(R.id.fragment_purchasing_yearly_icon) AppCompatImageView ivYearlyIcon;
    @BindView(R.id.fragment_purchasing_monthly_icon) AppCompatImageView ivMonthlyIcon;

    public static String PRODUCT_ID_SELECTED;

    private boolean pricesLoaded = false;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_purchasing, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
        initView();
    }

    private void initView() {
        if (PRODUCT_ID_SELECTED == null) {
            PRODUCT_ID_SELECTED =
                    SubscriptionsUtils.INSTANCE.getYearlySubscriptionId(getContext());
        }

        changeBackgrounds();
        aMonthly.setOnClickListener(v -> {
            PRODUCT_ID_SELECTED =
                    SubscriptionsUtils.INSTANCE.getMonthlySubscriptionId(getContext());
            changeBackgrounds();
        });

        aYearly.setOnClickListener(v -> {
            PRODUCT_ID_SELECTED =
                    SubscriptionsUtils.INSTANCE.getYearlySubscriptionId(getContext());
            changeBackgrounds();
        });

        bSubmit.setOnClickListener(v -> {
            if (pricesLoaded) {
                onSignUpClicked();
            }
        });

        if(!PIAApplication.isRelease())
            bSubmit.setOnLongClickListener(view -> {
                ((LoginPurchaseActivity) getActivity()).switchToPurchasingProcess(true, false, false);
                return true;
            });

        try {
            ((LoginPurchaseActivity) getActivity()).refreshCurrencyTexts();
        } catch (Exception e) { }

        LoginPurchaseActivity.setupToSPPText(getActivity(), tvToS);
    }

    @Override
    public void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
    }

    @Subscribe(sticky = true)
    public void loadPricing(PricingLoadedEvent event) {
        setUpCosts(event.monthlyCost, event.yearlyCost);
    }

    public void onSignUpClicked() {
        if (PIAApplication.isPlayStoreSupported(getContext().getPackageManager())) {
            ((LoginPurchaseActivity) getActivity()).onSubscribeClicked(PRODUCT_ID_SELECTED);
        } else {
            Intent i = new Intent(getContext(), WebviewActivity.class);
            i.putExtra(WebviewActivity.EXTRA_URL, Uri.parse(getString(R.string.buyvpn_url_localized)));
            startActivity(i);
        }
    }

    void changeBackgrounds(){
        ThemeHandler.Theme theme = ThemeHandler.getCurrentTheme(aYearly.getContext());
        String productId = SubscriptionsUtils.INSTANCE.getMonthlySubscriptionId(getContext());

        if (productId != null && PRODUCT_ID_SELECTED.equals(productId)){
            aYearly.setSelected(false);
            aMonthly.setSelected(true);

            ivYearlyIcon.setImageResource(theme == ThemeHandler.Theme.DAY ? R.drawable.ic_selection : R.drawable.ic_selection_dark);
            ivMonthlyIcon.setImageResource(theme == ThemeHandler.Theme.DAY ? R.drawable.ic_selection_checked : R.drawable.ic_selection_checked_dark);
        } else {
            aYearly.setSelected(true);
            aMonthly.setSelected(false);

            ivYearlyIcon.setImageResource(theme == ThemeHandler.Theme.DAY ? R.drawable.ic_selection_checked : R.drawable.ic_selection_checked_dark);
            ivMonthlyIcon.setImageResource(theme == ThemeHandler.Theme.DAY ? R.drawable.ic_selection : R.drawable.ic_selection_dark);
        }
    }

    public void setUpCosts(String monthly, String yearly){
        DLog.d("PurchasingFragment", "monthly = " + monthly + " yearly = " + yearly);
        tvMonthlyText.setText(String.format(getString(R.string.purchasing_monthly_ending), monthly));
        tvYearlyText.setText(getString(R.string.yearly_sub_text, yearly));
        if(!TextUtils.isEmpty(yearly)){
            pricesLoaded = true;

            StringBuilder sb = new StringBuilder();
            sb.append("#");
            Currency c = Currency.getInstance(Locale.getDefault());
            int fractionNumber = c.getDefaultFractionDigits();
            if(fractionNumber > 0) {
                sb.append(".");
                for (int i = 0; i < fractionNumber; i++) {
                    sb.append("#");
                }
            }
            DLog.d("PurchasingFragment", "formatting = " + sb.toString());
            DecimalFormat format = new DecimalFormat(sb.toString());
            String cleaned = yearly.replaceAll("\\D+","");
            String currency = yearly.replaceAll("[0-9.,]","");

            try {
                Float year = Float.parseFloat(cleaned);
                DLog.d("Purchasing", "year = " + year + " cleaned = " + cleaned);
                year = (year / 100) / 12;
                DLog.d("PurchasingFragment", "mYearlyCost = " + format.format(year));
                tvYearlyTotalText.setText(String.format(getString(R.string.purchasing_yearly_month_ending), format.format(year), currency));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}