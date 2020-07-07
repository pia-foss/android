package com.privateinternetaccess.android.handlers;

import android.app.Activity;

import com.privateinternetaccess.android.pia.interfaces.IPurchasing;
import com.privateinternetaccess.android.pia.model.PurchaseObj;
import com.privateinternetaccess.android.pia.model.SkuDetailsObj;
import com.privateinternetaccess.android.pia.model.enums.PurchasingType;
import com.privateinternetaccess.android.pia.model.events.PurchasingInfoEvent;
import com.privateinternetaccess.android.pia.model.events.SystemPurchaseEvent;
import com.privateinternetaccess.android.pia.model.response.PurchasingResponse;
import com.privateinternetaccess.core.utils.IPIACallback;

import java.util.List;

/**
 * Created by hfrede on 11/30/17.
 */

public class PurchasingHandler implements IPurchasing {


    @Override
    public void init(Activity activity, List<String> purchasingList, IPIACallback<PurchasingResponse> callback, IPIACallback<PurchasingInfoEvent> infoCallback, IPIACallback<SystemPurchaseEvent> systemCallback) {
    }

    @Override
    public PurchasingType getType() {
        return PurchasingType.SAMSUNG;
    }

    @Override
    public PurchaseObj getPurchase() {
        return null;
    }

    @Override
    public void purchase(String email, String subType) {

    }

    @Override
    public SkuDetailsObj getSkuDetails(String sku) {
        return null;
    }

    @Override
    public void dispose() {
    }

    @Override
    public void setCallback(IPIACallback<PurchasingResponse> callback) {
    }

    @Override
    public void setInfoCallback(IPIACallback<PurchasingInfoEvent> infoCallback) {
    }

    @Override
    public void setSystemCallback(IPIACallback<SystemPurchaseEvent> systemCallback) {
    }

    @Override
    public void setPurchaseList(List<String> purchaseList) {
    }
    @Override
    public void setEmail(String email) {
    }
}