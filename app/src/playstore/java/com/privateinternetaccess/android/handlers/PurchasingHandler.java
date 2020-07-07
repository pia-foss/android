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

package com.privateinternetaccess.android.handlers;

import android.app.Activity;
import androidx.annotation.Nullable;
import android.text.TextUtils;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;
import com.privateinternetaccess.android.pia.PIAFactory;
import com.privateinternetaccess.android.pia.interfaces.IAccount;
import com.privateinternetaccess.android.pia.interfaces.IPurchasing;
import com.privateinternetaccess.android.pia.model.PurchaseData;
import com.privateinternetaccess.android.pia.model.PurchaseObj;
import com.privateinternetaccess.android.pia.model.enums.PurchasingType;
import com.privateinternetaccess.android.pia.model.events.PurchasingEvent;
import com.privateinternetaccess.android.pia.model.events.PurchasingInfoEvent;
import com.privateinternetaccess.android.pia.model.events.SystemPurchaseEvent;
import com.privateinternetaccess.android.pia.model.response.PurchasingResponse;
import com.privateinternetaccess.android.pia.model.SkuDetailsObj;
import com.privateinternetaccess.android.pia.utils.AppUtilities;
import com.privateinternetaccess.android.pia.utils.DLog;
import com.privateinternetaccess.core.utils.IPIACallback;

import org.greenrobot.eventbus.EventBus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by hfrede on 11/30/17.
 */

public class PurchasingHandler implements PurchasesUpdatedListener, SkuDetailsResponseListener, IPurchasing {

    public static final String TAG = "PurchasingHandler";
    private Activity activity;
    private List<String> purchaseList;
    private Map<String, SkuDetailsObj> skuDetailsList;

    private BillingClient mBillingClient;

    private IPIACallback<PurchasingResponse> callback;
    private IPIACallback<PurchasingInfoEvent> infoCallback;
    private IPIACallback<SystemPurchaseEvent> systemCallback;

    private String email;
    private boolean mIsServiceConnected;
    private int mBillingClientResponseCode;

    public PurchasingHandler(){
        // This is for other versions of this
    }

    public void init(Activity activity, List<String> purchasingList, IPIACallback<PurchasingResponse> callback,
                     IPIACallback<PurchasingInfoEvent> infoCallback,
                     IPIACallback<SystemPurchaseEvent> systemCallback){
        this.activity = activity;
        setCallback(callback);
        setPurchaseList(purchasingList);
        setInfoCallback(infoCallback);
        setSystemCallback(systemCallback);

        mBillingClient = BillingClient.newBuilder(activity)
                .setListener(this).build();
        startServiceConnection(new Runnable() {
            @Override
            public void run() {
                if (mBillingClientResponseCode == BillingClient.BillingResponse.OK) {
                    // The billing client is ready. You can query purchases here.
                    DLog.d(TAG, "Billing setup succeed");
                    grabPurchases();
                } else {
                    DLog.d(TAG, "Billing setup failed " + mBillingClientResponseCode);
                }
            }
        });
    }

    private void startServiceConnection(final Runnable executable){
        mBillingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(int billingResponseCode) {
                DLog.d(TAG, "Setup finished. Response code: " + billingResponseCode);

                if (billingResponseCode == BillingClient.BillingResponse.OK) {
                    mIsServiceConnected = true;
                    if (executable != null) {
                        executable.run();
                    }
                }

                mBillingClientResponseCode = billingResponseCode;
            }

            @Override
            public void onBillingServiceDisconnected() {
                mIsServiceConnected = false;
            }
        });
    }

    private void executeServiceRequest(Runnable runnable) {
        if (mIsServiceConnected) {
            runnable.run();
        } else {
            // If billing service was disconnected, we try to reconnect 1 time.
            // (feel free to introduce your retry policy here).
            startServiceConnection(runnable);
        }
    }

    @Override
    public void onSkuDetailsResponse(int responseCode, List<SkuDetails> skuDetailsList) {
        if(responseCode == BillingClient.BillingResponse.OK) {
            DLog.d(TAG,"SkuDetails have arrived");
            this.skuDetailsList = new HashMap<String, SkuDetailsObj>();
            for(SkuDetails details: skuDetailsList){
                this.skuDetailsList.put(details.getSku(), convertToSkuDetailsObj(details));
            }
            PurchasingInfoEvent event = new PurchasingInfoEvent(this.skuDetailsList);
            EventBus.getDefault().post(event);
            if(infoCallback != null){
                infoCallback.apiReturn(event);
            }
        } else {
            DLog.d(TAG,"SkuDetails just aren't happening");
        }
    }

    @Override
    public void onPurchasesUpdated(int responseCode, @Nullable List<Purchase> purchases) {
        boolean success = false;
        String returnString = "";

        if (responseCode == BillingClient.BillingResponse.OK && purchases != null) {
            Purchase purchase = null;
            for(Purchase p : purchases){
                if(p != null){
                    purchase = p;
                    break;
                }
            }
            if(purchase != null) {
                DLog.d(TAG, "Purchases call completed");
                DLog.d(TAG, "purchases = " + purchases.toString());
                DLog.i(TAG, "Purchase successful.");
                DLog.d(TAG, "Purchase token: '" + purchase.getPurchaseToken() + "'.");
                DLog.d(TAG, "Purchase order id: '" + purchase.getOrderId() + "'.");
                savePurchaseForProcess(purchase);

                success = true;
                returnString = purchase.getSku();
            }
        } else if (responseCode == BillingClient.BillingResponse.USER_CANCELED) {
            // Handle an error caused by a user cancelling the purchase flow.
            DLog.d(TAG,"User Canceled Purchasing");
        } else {
            // Handle any other error codes.
            DLog.d(TAG,"Purchasing failed with this code(" + responseCode+ ")");
        }

        sendBackSystemPurchase(success, returnString);
    }

    private void sendBackSystemPurchase(boolean b, String sku) {
        SystemPurchaseEvent event = new SystemPurchaseEvent(b, sku);
        if (systemCallback != null)
            systemCallback.apiReturn(event);
    }

    private void grabPurchases(){
        Runnable runnable = new Runnable() {
            public void run() {
                if(purchaseList != null) {
                    SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
                    params.setSkusList(purchaseList).setType(BillingClient.SkuType.SUBS);
                    mBillingClient.querySkuDetailsAsync(params.build(), PurchasingHandler.this);
                } else {
                    DLog.d(TAG,"You must enter a purchaseList for PurchasingHandler to work.");
                }
            }
        };

        executeServiceRequest(runnable);
    }

    private void savePurchaseForProcess(Purchase purchase){
        PurchaseData data = new PurchaseData(
                email,
                purchase.getPurchaseToken(),
                purchase.getSku(),
                purchase.getOrderId()
        );
        IAccount account = PIAFactory.getInstance().getAccount(activity);
        account.saveTemporaryPurchaseData(data);
    }

    public void purchase(final String email, final String skuId){
        Runnable runnable = new Runnable() {
            public void run() {
                setEmail(email);
                if(mBillingClient != null) {
                    BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                            .setSku(skuId)
                            .setType(BillingClient.SkuType.SUBS)
                            .build();
                    int responseCode = mBillingClient.launchBillingFlow(activity, flowParams);
                    // Maybe do something with the code?
                    DLog.d(TAG,"responseCode = " + responseCode);
                } else {
                    handlerInvalid();
                }
            }
        };

        executeServiceRequest(runnable);
    }

    public Purchase getPurchase(String sku){
        if(mBillingClient != null) {
            Purchase.PurchasesResult purchasesResult = mBillingClient.queryPurchases(BillingClient.SkuType.SUBS);
            List<Purchase> list = purchasesResult.getPurchasesList();
            Purchase purchase = null;
            for(Purchase p : list){
                if(sku.equals(p.getSku())){
                    purchase = p;
                    break;
                }
            }
            return purchase;
        } else {
            handlerInvalid();
            return null;
        }
    }

    public PurchaseObj getPurchase(){
        if(mBillingClient != null){
            Purchase.PurchasesResult purchasesResult = mBillingClient.queryPurchases(BillingClient.SkuType.SUBS);
            List<Purchase> list = purchasesResult.getPurchasesList();
            Purchase purchase = null;
            if(list != null)
                for(Purchase p : list){
                    if(purchaseList.contains(p.getSku())){
                        purchase = p;
                        break;
                    }
                }
            if(purchase != null)
                return convertToPurchaseObject(purchase);
            else
                return null;
        } else {
            handlerInvalid();
            return null;
        }
    }

    private PurchaseObj convertToPurchaseObject(Purchase purchase){
        return new PurchaseObj(purchase.getOrderId(), purchase.getPackageName(), purchase.getSku(), purchase.getPurchaseTime(), purchase.getPurchaseToken());
    }

    private SkuDetailsObj convertToSkuDetailsObj(SkuDetails details){
        return new SkuDetailsObj(details.getSku(), details.getType(), details.getPrice(), details.getTitle(), details.getDescription());
    }

    public SkuDetailsObj getSkuDetails(String sku){
        if(this.skuDetailsList != null){
            SkuDetailsObj d = this.skuDetailsList.get(sku);
            return d;
        } else {
            return null;
        }
    }

    public void dispose(){
        DLog.d(TAG,"disposed");
        try {
            if(mBillingClient != null)
                mBillingClient.endConnection();
            mBillingClient = null;
            activity = null;
        } catch (Exception e) {
        }

    }

    public void handlerInvalid(){
        DLog.d(TAG,"Purchasing Handler is no longer valid at this time.");
    }

    public Map<String, SkuDetailsObj> getSkuDetailsList() {
        return skuDetailsList;
    }

    public void setPurchaseList(List<String> purchaseList) {
        this.purchaseList = purchaseList;
    }

    @Override
    public void setEmail(String email) {
        this.email = email;
    }

    public void setCallback(IPIACallback<PurchasingResponse> callback) {
        this.callback = callback;
    }

    public void setInfoCallback(IPIACallback<PurchasingInfoEvent> infoCallback) {
        this.infoCallback = infoCallback;
    }

    public void setSystemCallback(IPIACallback<SystemPurchaseEvent> systemCallback) {
        this.systemCallback = systemCallback;
    }

    @Override
    public PurchasingType getType() {
        return PurchasingType.GOOGLE;
    }
}