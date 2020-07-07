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
import com.privateinternetaccess.android.pia.handlers.PiaPrefHandler;
import com.privateinternetaccess.android.pia.model.PurchasingTestingData;
import com.privateinternetaccess.android.pia.model.events.PurchasingEvent;
import com.privateinternetaccess.android.pia.model.response.PurchasingResponse;
import com.privateinternetaccess.android.pia.utils.DLog;
import com.privateinternetaccess.core.utils.IPIACallback;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;

/**
 * Quick task to handle sign up for users.
 *
 * Returns {@link PurchasingEvent} via otto, {@link PurchasingResponse} via callback
 *
 * Created by half47 on 4/27/17.
 */

public class PurchasingTask extends AsyncTask<String, Void, PurchasingResponse> {

    public static final String TAG = "Purchasing";

    private Context context;

    private String fEmail;
    private String orderId;
    private String token;
    private String productId;

    private IPIACallback<PurchasingResponse> callback;

    public PurchasingTask(Context context, String fEmail, String orderId, String token, String productId) {
        this.context = context;
        this.fEmail = fEmail;
        this.orderId = orderId;
        this.token = token;
        this.productId = productId;
    }

    @Override
    protected PurchasingResponse doInBackground(String... arg0) {
        PurchasingResponse response;
        PurchasingTestingData data = new PurchasingTestingData(PiaPrefHandler.isPurchasingTesting(context),
                PiaPrefHandler.getPurchaseTestingStatus(context),
                PiaPrefHandler.getPurchaseTestingUsername(context),
                PiaPrefHandler.getPurchaseTestingPassword(context),
                PiaPrefHandler.getPurchaseTestingException(context)
                );
        try {
            PurchasingApi api = new PurchasingApi(context);
            response = api.signUp(fEmail, orderId, token, productId, null, data);
        } catch (IOException e) {
            response = new PurchasingResponse();
            response.setException(e);
            e.printStackTrace();
        }

        if (response.getException() == null) {
            DLog.i(TAG, "Subscription notified to backend");
        } else {
            DLog.i(TAG, "Error notifying backend");
        }
        return response;
    }

    @Override
    protected void onPostExecute(PurchasingResponse response) {
        EventBus.getDefault().post(new PurchasingEvent(response));
        if(callback != null) {
            callback.apiReturn(response);
        }
    }

    public void setCallback(IPIACallback<PurchasingResponse> callback) {
        this.callback = callback;
    }
}
