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
import android.os.Handler;


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
 * A task for sending multiple sign in calls.
 * <p>
 * Start via {@link #startDelayedTasks(Context, IPIACallback)}. Runs {@link #MAX_ATTEMPTS} until stops.
 * <p>
 * Stops calling the server if {@link #setFinished(boolean)} true is given.
 * <p>
 * Sends {@link PurchasingEvent} via otto and {@link PurchasingResponse} via callback.
 * <p>
 * Created by hfrede on 8/18/17.
 */

public class RetryPurchasingTask implements Runnable {

    public static final int MAX_ATTEMPTS = 5;
    private static final String TAG = "RetryPurchasingTask";

    Context context;
    IPIACallback<PurchasingResponse> callback;

    private boolean finished;

    public RetryPurchasingTask(Context context, IPIACallback<PurchasingResponse> callback) {
        this.context = context;
        this.callback = callback;
    }

    @Override
    public void run() {
        if (!isFinished()) {
            DLog.d(TAG, "doInBackground");
            PurchasingResponse response = new PurchasingResponse();

            String email = PiaPrefHandler.getPurchasingEmail(context);
            String orderId = PiaPrefHandler.getPurchasingOrderId(context);
            String token = PiaPrefHandler.getPurchasingToken(context);
            String sku = PiaPrefHandler.getPurchasingSku(context);

            PurchasingTestingData data = new PurchasingTestingData(PiaPrefHandler.isPurchasingTesting(context),
                    PiaPrefHandler.getPurchaseTestingStatus(context),
                    PiaPrefHandler.getPurchaseTestingUsername(context),
                    PiaPrefHandler.getPurchaseTestingPassword(context),
                    PiaPrefHandler.getPurchaseTestingException(context)
            );
            PurchasingApi api = new PurchasingApi(context);
            for (int i = 0; i < MAX_ATTEMPTS; i++) {
                // Exponential back off, => 5s, 10s, 20s, 40s
                int timeout = (1 << i) * 5;

                try {
                    response = api.signUp( email, orderId, token, sku, null, data, timeout);
                } catch (IOException e) {
                    response.setException(e);
                    e.printStackTrace();
                }

                response.setAttempt(i);
                if (response.getException() == null) {
                    DLog.d(TAG, "Subscription notified to backend");
                } else {
                    DLog.d(TAG, "Error notifying backend (timeout "  + timeout + ")");
                }
                onPostExecute(response);
            }
        }
    }

    private void onPostExecute(final PurchasingResponse response) {

        if (!isFinished()) {
            DLog.d(TAG, "response = " + response.toString());
            if (response.getResponseNumber() == 200) {
                PiaPrefHandler.clearPurchasingInfo(context);
                setFinished(true);
            }
            // only execute what really needs to be on ui thread there so setFinished is executed on the calling thread
            new Handler(context.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    EventBus.getDefault().postSticky(new PurchasingEvent(response));
                    if (callback != null)
                        callback.apiReturn(response);

                }
            });
        }

    }

    public static RetryPurchasingTask startDelayedTasks(Context context, IPIACallback<PurchasingResponse> callback) {
        DLog.d(TAG, "StartDelayedTasks");
        final RetryPurchasingTask retryTask = new RetryPurchasingTask(context, callback);
        new Thread(retryTask).start();
        return retryTask;
    }

    public synchronized boolean isFinished() {
        return finished;
    }

    public synchronized void setFinished(boolean finished) {
        this.finished = finished;
    }
}