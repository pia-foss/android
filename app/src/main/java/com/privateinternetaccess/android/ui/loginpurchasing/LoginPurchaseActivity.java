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
import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.TextView;

import com.google.zxing.client.android.Intents;
import com.privateinternetaccess.android.BuildConfig;
import com.privateinternetaccess.android.PIAApplication;
import com.privateinternetaccess.android.R;
import com.privateinternetaccess.android.handlers.PurchasingHandler;
import com.privateinternetaccess.android.handlers.UpdateHandler;
import com.privateinternetaccess.android.model.events.PricingLoadedEvent;
import com.privateinternetaccess.android.pia.IPIACallback;
import com.privateinternetaccess.android.pia.PIAFactory;
import com.privateinternetaccess.android.pia.handlers.PiaPrefHandler;
import com.privateinternetaccess.android.pia.interfaces.IAccount;
import com.privateinternetaccess.android.pia.interfaces.IPurchasing;
import com.privateinternetaccess.android.pia.model.PurchaseData;
import com.privateinternetaccess.android.pia.model.PurchaseObj;
import com.privateinternetaccess.android.pia.model.events.LoginEvent;
import com.privateinternetaccess.android.pia.model.events.PurchasingInfoEvent;
import com.privateinternetaccess.android.pia.model.events.SystemPurchaseEvent;
import com.privateinternetaccess.android.pia.model.response.PurchasingResponse;
import com.privateinternetaccess.android.pia.model.response.SubscriptionAvailableResponse;
import com.privateinternetaccess.android.pia.subscription.InAppPurchasesHelper;
import com.privateinternetaccess.android.pia.utils.DLog;
import com.privateinternetaccess.android.pia.utils.Toaster;
import com.privateinternetaccess.android.ui.features.WebviewActivity;
import com.privateinternetaccess.android.ui.superclasses.BaseActivity;
import com.privateinternetaccess.android.pia.model.SkuDetailsObj;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;

/**
 * Created by half47 on 3/13/17.
 */

public class LoginPurchaseActivity extends BaseActivity {

    public static final String EXTRA_GOTO_PURCHASING = "gotoPurchasing";

    private static final int RC_REQUEST = 13;
    public static final String TAG = "Purchasing";

    private final String EXTRA_MONTHLY_COST = "mMonthlyCost";
    private final String EXTRA_YEARLY_COST = "mYearlyCost";
    private final String EXTRA_YEARLY_TOTAL_COST = "yearlyTotalCost";

    public String mMonthlyCost;
    public String mYearlyCost;

    private boolean showPurchasing;

    IPurchasing purchasingHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login_purchasing);

        ButterKnife.bind(this);

        if(savedInstanceState != null){
            mMonthlyCost = savedInstanceState.getString(EXTRA_MONTHLY_COST);
            mYearlyCost = savedInstanceState.getString(EXTRA_YEARLY_COST);
        } else {
            showPurchasing = getIntent().hasExtra(EXTRA_GOTO_PURCHASING);
        }
        setSwipeBackEnable(false);

        if(BuildConfig.FLAVOR_store.equals("noinapp")) {
            UpdateHandler.checkUpdates(this, UpdateHandler.UpdateDisplayType.SHOW_DIALOG);
        }
    }

    private void createIabHelper() {
        if(purchasingHandler == null) {
            purchasingHandler = new PurchasingHandler();
            List<String> purchases = new ArrayList<String>();
            purchases.add(InAppPurchasesHelper.getMontlySubscriptionId());
            purchases.add(InAppPurchasesHelper.getYearlySubscriptionId());
            purchasingHandler.init(this, purchases, null, null, new IPIACallback<SystemPurchaseEvent>() {
                @Override
                public void apiReturn(SystemPurchaseEvent systemPurchaseEvent) {
                    onSystemPurchaseEvent(systemPurchaseEvent);
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        initView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (purchasingHandler != null)
            purchasingHandler.dispose();
    }

    private void initView() {
        Fragment frag = getSupportFragmentManager().findFragmentById(R.id.container);
        if(frag == null){
            if (!PIAApplication.isAndroidTV(getApplicationContext())) {
                frag = new GetStartedFragment();
                FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
                trans.add(R.id.container, frag);
                trans.commit();
                if(!PiaPrefHandler.isPurchasingProcessDone(getApplicationContext())){
                    switchToPurchasingProcess(true, false);
                } else if(showPurchasing)
                    switchToPurchasing();
            }
            else {
                frag = new LoginFragment();
                FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
                trans.add(R.id.container, frag);
                trans.commit();
            }
        }

        processEvent();
    }

    private void processEvent() {
//        LoginEvent event = EventBus.getDefault().getStickyEvent(LoginEvent.class);
//        if(event != null){
//            Fragment frag = getSupportFragmentManager().findFragmentById(R.id.container);
//            if(!(frag instanceof LoginFragment)){
//                // pop backstack
//                FragmentManager manager = getSupportFragmentManager();
//                int backstack = manager.getBackStackEntryCount();
//                DLog.d(TAG, "backstack = " + backstack);
//                if(backstack > 0) {
//                    FragmentManager.BackStackEntry entry = manager.getBackStackEntryAt(0);
//                    manager.popBackStack(entry.getName(), FragmentManager.POP_BACK_STACK_INCLUSIVE);
//                }
//            }
//        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(EXTRA_MONTHLY_COST, mMonthlyCost);
        outState.putString(EXTRA_YEARLY_COST, mYearlyCost);
    }

    public void switchToFreeTrial() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Fragment frag = new FreeTrialFragment();
                FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
                trans.setCustomAnimations(R.anim.left_to_right, R.anim.right_to_left, R.anim.right_to_left_exit, R.anim.left_to_right_exit);
                trans.replace(R.id.container, frag);
                trans.addToBackStack("free_trial");
                trans.commit();
            }
        });
    }

    public void switchToPurchasing(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Fragment frag = new PurchasingFragment();
                FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
                trans.setCustomAnimations(R.anim.left_to_right, R.anim.right_to_left, R.anim.right_to_left_exit, R.anim.left_to_right_exit);
                trans.replace(R.id.container, frag);
                trans.addToBackStack("purchasing");
                trans.commit();
            }
        });
    }

    public void switchToPurchasingProcess(final boolean fireOffPurchasing, final boolean isTrial){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                PurchasingProcessFragment frag = new PurchasingProcessFragment();
                frag.setFirePurchasing(fireOffPurchasing);
                frag.setTrial(isTrial);
                EventBus.getDefault().removeStickyEvent(PurchasingResponse.class);
                getSupportFragmentManager().beginTransaction().add(R.id.container, frag).commit();
            }
        });
    }

    public void switchToTrialAccount(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Fragment frag = new TrialFragment();
                FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
                trans.setCustomAnimations(R.anim.left_to_right, R.anim.right_to_left, R.anim.right_to_left_exit, R.anim.left_to_right_exit);
                trans.replace(R.id.container, frag);
                trans.addToBackStack("trial");
                trans.commit();

                DLog.d("LoginPurchaseActivity", "Moving to redemption");
            }
        });
    }

    public void switchToLogin() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                LoginFragment frag = new LoginFragment();
                FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
                trans.setCustomAnimations(R.anim.left_to_right, R.anim.right_to_left, R.anim.right_to_left_exit, R.anim.left_to_right_exit);
                trans.replace(R.id.container, frag);
                trans.addToBackStack("login");
                trans.commit();

                DLog.d("LoginPurchaseActivity", "Moving to login");
            }
        });
    }

    public void onSubscribeClicked(String email, String subscriptionType) {
        PiaPrefHandler.saveEmail(getApplicationContext(), email);
        showPurchasing = false;
        PurchaseObj mActiveSubscription = purchasingHandler.getPurchase();
        if(!PIAApplication.isQA())
            if (mActiveSubscription != null) {
                Toaster.l(getApplicationContext(), R.string.error_active_subscription);
                return;
            }
        String payload = "";

        DLog.i("Purchasing", "Launching purchase flow for subscription.");
        purchasingHandler.purchase(email, subscriptionType);
    }

    public void onContinuePurchasingClicked(String subscriptionType) {
        PurchasingEmailFragment emailFragment = new PurchasingEmailFragment();
        PurchasingEmailFragment.PRODUCT_ID_SELECTED = subscriptionType;

        FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
        trans.setCustomAnimations(R.anim.left_to_right, R.anim.right_to_left, R.anim.right_to_left_exit, R.anim.left_to_right_exit);
        trans.replace(R.id.container, emailFragment);
        trans.addToBackStack("email");
        trans.commit();
    }

    public void onConfirmEmailClicked(String email, String subscriptionType) {
        PurchasingFinalizeFragment finalizeFragment = new PurchasingFinalizeFragment();
        finalizeFragment.email = email;
        PurchasingFinalizeFragment.PRODUCT_ID_SELECTED = subscriptionType;

        FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
        trans.setCustomAnimations(R.anim.left_to_right, R.anim.right_to_left, R.anim.right_to_left_exit, R.anim.left_to_right_exit);
        trans.replace(R.id.container, finalizeFragment);
        trans.addToBackStack("finalize");
        trans.commit();
    }

    public void onFinalizePurchasingClicked(String email, String subscriptionType) {

    }

    public void goToMainActivity(){
        EventBus.getDefault().removeStickyEvent(LoginEvent.class);
        super.goToMainActivity();
    }

    public void onSystemPurchaseEvent(SystemPurchaseEvent event){
        if(event.isSuccess()){
            switchToPurchasingProcess(true, false);
        } else {
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(data != null){
            if(data.getExtras() != null && data.getExtras().containsKey(Intents.Scan.RESULT)){
                String giftCode = data.getExtras().getString(Intents.Scan.RESULT);
                // Gots it.
                TrialFragment fragment = getGiftCardFragment();
                if(fragment != null)
                    fragment.onGiftCardQRCodeReceived(giftCode);
                else {
                    //Save?
                }
            } else {
                // Error message? Or should Trial Fragment handle the error
            }
        } else {
            // probably not from parsing the QR reader
        }
    }

    @Subscribe(sticky = true)
    public void onReceivedSubscriptions(SubscriptionAvailableResponse response) {
        DLog.d(TAG, "received subscription information");
        createIabHelper();

        String email = PiaPrefHandler.getLoginEmail(getApplicationContext());
        if(!TextUtils.isEmpty(email))
            purchasingHandler.setEmail(email);
    }

    @Subscribe
    public void onQueryInventoryFinished(PurchasingInfoEvent event){
        SkuDetailsObj monthlySub = purchasingHandler.getSkuDetails(InAppPurchasesHelper.getMontlySubscriptionId());
        SkuDetailsObj yearlySub = purchasingHandler.getSkuDetails(InAppPurchasesHelper.getYearlySubscriptionId());
        if (monthlySub != null && yearlySub != null) {
            mMonthlyCost = monthlySub.getPrice();
            mYearlyCost = yearlySub.getPrice();

            EventBus.getDefault().postSticky(new PricingLoadedEvent(mMonthlyCost, mYearlyCost));

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    PurchasingFragment fragment = getPurchasingFragment();
                    if(fragment != null){
                        fragment.setUpCosts(mMonthlyCost, mYearlyCost);
                    }
                }
            });
            IAccount account = PIAFactory.getInstance().getAccount(getApplicationContext());
            PurchaseData data = account.getTempoaryPurchaseData();
            // If purchasing has failed, retry
            if(data != null){
                switchToPurchasingProcess(true, false);
                //Toast to let them know what is going on?
            }
        }
    }

    public PurchasingFragment getPurchasingFragment(){
        PurchasingFragment login = null;
        Fragment frag = getSupportFragmentManager().findFragmentById(R.id.container);
        if(frag instanceof PurchasingFragment){
            login = (PurchasingFragment) frag;
        }
        return login;
    }

    public TrialFragment getGiftCardFragment(){
        TrialFragment giftCard = null;
        Fragment frag = getSupportFragmentManager().findFragmentById(R.id.container);
        if(frag instanceof TrialFragment){
            giftCard = (TrialFragment) frag;
        }
        return giftCard;
    }

    public void refreshCurrencyTexts(){
        PurchasingFragment frag = getPurchasingFragment();
        if(frag != null)
            if(!TextUtils.isEmpty(mMonthlyCost))
                frag.setUpCosts(mMonthlyCost, mYearlyCost);
    }

    public void noInternetReInit(){
        createIabHelper();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        showPurchasing = false;

        Fragment frag = getSupportFragmentManager().findFragmentById(R.id.container);
    }

    public static void setupTypeText(final Context context, TextView tv, String key) {
        InAppPurchasesHelper.SubscriptionType type = InAppPurchasesHelper.getType(key);

        if (type != null) {
            if (type == InAppPurchasesHelper.SubscriptionType.YEARLY) {
                tv.setText(String.format(context.getString(R.string.you_are_purchasing),
                        context.getString(R.string.yearly_only)));
            }
            else if (type == InAppPurchasesHelper.SubscriptionType.MONTHLY) {
                tv.setText(String.format(context.getString(R.string.you_are_purchasing),
                        context.getString(R.string.monthly_only)));
            }
        }
    }

    public static void setupToSPPText(final Context context, TextView tv){
        String ppText = context.getString(R.string.pp_text);
        String tosText = context.getString(R.string.tos_text);

        String tosPPText = String.format(context.getString(R.string.tos_pos_text), tosText, ppText);
        String[] splitTos = tosPPText.split(tosText);
        String[] splitPP = tosPPText.split(ppText);
        int tosStart = splitTos[0].length();
        int ppStart = splitPP[0].length();
        SpannableStringBuilder spanTxt = new SpannableStringBuilder(
                tosPPText);
        spanTxt.setSpan(new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                // tos selected
                DLog.d("PurchasingFragment","tos");
                Intent i = new Intent(context, WebviewActivity.class);
                i.putExtra(WebviewActivity.EXTRA_URL, "https://www.privateinternetaccess.com/pages/terms-of-service/");
                context.startActivity(i);
            }
        }, tosStart, tosStart + tosText.length(), 0);
        spanTxt.setSpan(new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                // pp selected
                DLog.d("PurchasingFragment","pp");
                Intent i = new Intent(context, WebviewActivity.class);
                i.putExtra(WebviewActivity.EXTRA_URL, "https://www.privateinternetaccess.com/pages/privacy-policy/");
                context.startActivity(i);
            }
        }, ppStart, ppStart + ppText.length(), 0);
        tv.setMovementMethod(LinkMovementMethod.getInstance());
        tv.setText(spanTxt, TextView.BufferType.SPANNABLE);
    }

}