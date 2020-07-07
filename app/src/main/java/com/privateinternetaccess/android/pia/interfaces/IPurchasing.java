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

package com.privateinternetaccess.android.pia.interfaces;

import android.app.Activity;

import com.privateinternetaccess.android.pia.model.PurchaseObj;
import com.privateinternetaccess.android.pia.model.SkuDetailsObj;
import com.privateinternetaccess.android.pia.model.enums.PurchasingType;
import com.privateinternetaccess.android.pia.model.events.PurchasingInfoEvent;
import com.privateinternetaccess.android.pia.model.events.SystemPurchaseEvent;
import com.privateinternetaccess.android.pia.model.response.PurchasingResponse;
import com.privateinternetaccess.core.utils.IPIACallback;

import java.util.List;

/**
 * Handles all methods for purchasing needed across the main platforms we have or will support.
 *
 * Created by hfrede on 1/5/18.
 */

public interface IPurchasing {

    /**
     * Setup for the PurchasingHandler class. This will need all of the call backs to work so you can listen when an event happens.
     *
     *
     * @param activity
     * @param purchasingList - List of objects that we are selling, may or may not be needed depending on platform.
     * @param callback - called when a purchasing event has occurred and returns the user data from the server. Eventbus can be used for this instead.
     * @param infoCallback - called when the information about a purchasable product has returned.
     * @param systemCallback - called when a purchasing event has occurred
     */
    void init(Activity activity, List<String> purchasingList,
              IPIACallback<PurchasingResponse> callback,
              IPIACallback<PurchasingInfoEvent> infoCallback,
              IPIACallback<SystemPurchaseEvent> systemCallback);

    /**
     * Sends back what Purchasing handler you are using so you can decide how to act.
     *
     * @return {@link PurchasingType}
     */
    PurchasingType getType();

    /**
     * Get the current purchased object if there is one.
     *
     * @return Can return null.
     */
    PurchaseObj getPurchase();

    /**
     * Calls the backend with the email for the account and subscription type it is or product ID in most cases.
     *
     * @param email
     * @param subType
     */
    void purchase(String email, String subType);

    /**
     * Grabs the {@link SkuDetailsObj} for a certain subtype.
     *
     * @param sku
     * @return {@link SkuDetailsObj} or null if not found.
     */
    SkuDetailsObj getSkuDetails(String sku);

    /**
     * Call this when you want to close and end the connection to the purchasing system.
     *
     */
    void dispose();

    /**
     * Set the callbacks for a purchased event completion and an account is created.
     *
     * @param callback
     */
    void setCallback(IPIACallback<PurchasingResponse> callback);

    /**
     * Sets the callback for information about the purchasable items are returned.
     *
     * @param infoCallback
     */
    void setInfoCallback(IPIACallback<PurchasingInfoEvent> infoCallback);

    /**
     * Sets the callback for when a item is purchased in the system.
     *
     * @param systemCallback
     */
    void setSystemCallback(IPIACallback<SystemPurchaseEvent> systemCallback);

    /**
     * Sets the list of items the handler needs to grab {@link SkuDetailsObj} or purchased items.
     *
     * @param purchaseList
     */
    void setPurchaseList(List<String> purchaseList);

    /**
     * Sets the email for sending to the server
     *
     * @param email
     */
    void setEmail(String email);
}
