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

package com.privateinternetaccess.android.pia.subscription;

import com.privateinternetaccess.android.pia.handlers.SubscriptionHandler;
import com.privateinternetaccess.android.pia.model.response.SubscriptionAvailableResponse;
import com.privateinternetaccess.android.pia.utils.DLog;

/**
 * Created by arne on 16.05.2015.
 */
public class InAppPurchasesHelper
{

    public enum SubscriptionType {
        MONTHLY,
        YEARLY
    }

    public static String getMontlySubscriptionId() {
        if (SubscriptionHandler.subscriptionResponse != null) {
            return SubscriptionHandler.subscriptionResponse.getActiveMonthlySubscription();
        }

        return null;
    }

    public static String getYearlySubscriptionId() {
        if (SubscriptionHandler.subscriptionResponse != null) {
            return SubscriptionHandler.subscriptionResponse.getActiveYearlySubscription();
        }

        return null;
    }

    public static SubscriptionType getType(String key) {
        if (key == null){
            return null;
        }

        SubscriptionAvailableResponse response = SubscriptionHandler.subscriptionResponse;
        if (response != null) {
            if (response.getActiveMonthlySubscription() != null &&
                    key.equals(response.getActiveMonthlySubscription())) {
                return SubscriptionType.MONTHLY;
            }
            else if (response.getActiveYearlySubscription() != null &&
                    key.equals(SubscriptionHandler.subscriptionResponse.getActiveYearlySubscription())) {
                return SubscriptionType.YEARLY;
            }
        }

        return null;
    }

}
