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