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

package com.privateinternetaccess.android.pia.tasks;

import android.content.Context;
import android.os.AsyncTask;

import com.privateinternetaccess.android.pia.api.PurchasingApi;
import com.privateinternetaccess.android.pia.model.response.SubscriptionAvailableResponse;
import com.privateinternetaccess.android.pia.utils.DLog;
import com.privateinternetaccess.core.utils.IPIACallback;

import org.greenrobot.eventbus.EventBus;

public class FetchSubscriptionsTask extends AsyncTask<String, Void, SubscriptionAvailableResponse> {

    private IPIACallback<SubscriptionAvailableResponse> callback;

    private Context context;

    public FetchSubscriptionsTask(Context context, IPIACallback<SubscriptionAvailableResponse> callback) {
        this.callback = callback;
        this.context = context;
    }

    @Override
    protected SubscriptionAvailableResponse doInBackground(String... strings) {
        PurchasingApi api = new PurchasingApi(context);
        return api.findSubscriptions();
    }

    @Override
    protected void onPostExecute(SubscriptionAvailableResponse subscriptionResponse) {
        super.onPostExecute(subscriptionResponse);
        if(callback != null){
            callback.apiReturn(subscriptionResponse);
        }
        EventBus.getDefault().postSticky(subscriptionResponse);
        DLog.d("SubscriptionTask", "Posted sticky response");
    }

    public void setCallback(IPIACallback<SubscriptionAvailableResponse> callback) {
        this.callback = callback;
    }
}
