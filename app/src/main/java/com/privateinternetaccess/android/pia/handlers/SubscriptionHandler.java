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

package com.privateinternetaccess.android.pia.handlers;

import android.content.Context;

import com.privateinternetaccess.android.pia.model.response.SubscriptionAvailableResponse;
import com.privateinternetaccess.android.pia.tasks.FetchSubscriptionsTask;
import com.privateinternetaccess.core.utils.IPIACallback;

import org.greenrobot.eventbus.EventBus;

public class SubscriptionHandler {

    private static SubscriptionHandler instance;
    private static IPIACallback<SubscriptionAvailableResponse> CALLBACK;

    private Context context;
    public static SubscriptionAvailableResponse subscriptionResponse;

    public static SubscriptionHandler getInstance(Context context){
        if(instance == null){
            startup(context);
        }
        return instance;
    }

    public static void startup(Context context) {
        instance = new SubscriptionHandler();
        instance.context = context;
        instance.fetchSubscriptions(context);
    }

    public void fetchSubscriptions(Context context){
        FetchSubscriptionsTask task = new FetchSubscriptionsTask(context, new IPIACallback<SubscriptionAvailableResponse>() {
            @Override
            public void apiReturn(SubscriptionAvailableResponse subscriptionResponse) {
                SubscriptionHandler.this.subscriptionResponse = subscriptionResponse;
            }
        });

        task.execute("");
    }
}
