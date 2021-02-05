package com.privateinternetaccess.android.handlers;

import android.app.Activity;


import com.privateinternetaccess.android.pia.interfaces.IPurchasing;
import com.privateinternetaccess.android.pia.model.PurchaseObj;
import com.privateinternetaccess.android.pia.model.SkuDetailsObj;
import com.privateinternetaccess.android.pia.model.enums.PurchasingType;
import com.privateinternetaccess.android.pia.model.events.SystemPurchaseEvent;
import com.privateinternetaccess.core.utils.IPIACallback;

import java.util.List;

/**
 * Created by hfrede on 11/30/17.
 */
public class PurchasingHandler implements IPurchasing {

    @Override
    public void init(
            Activity activity,
            List<String> purchasingList,
            IPIACallback<SystemPurchaseEvent> systemCallback
    ) { }

    @Override
    public PurchasingType getType() {
        return PurchasingType.SAMSUNG;
    }

    @Override
    public PurchaseObj getPurchase(boolean savePurchase) {
        return null;
    }

    @Override
    public void purchase(String subType) { }

    @Override
    public SkuDetailsObj getSkuDetails(String sku) {
        return null;
    }

    @Override
    public void dispose() { }
}