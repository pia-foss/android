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

package com.privateinternetaccess.android.pia.impl;

import android.content.Context;
import android.os.AsyncTask;

import com.privateinternetaccess.android.pia.handlers.PiaPrefHandler;
import com.privateinternetaccess.android.pia.interfaces.IAccount;
import com.privateinternetaccess.android.pia.model.LoginInfo;
import com.privateinternetaccess.android.pia.model.PIAAccountData;
import com.privateinternetaccess.android.pia.model.PurchaseData;
import com.privateinternetaccess.android.pia.model.TrialData;
import com.privateinternetaccess.android.pia.model.UpdateAccountInfo;
import com.privateinternetaccess.android.pia.model.response.LoginResponse;
import com.privateinternetaccess.android.pia.model.response.PurchasingResponse;
import com.privateinternetaccess.android.pia.model.response.TokenResponse;
import com.privateinternetaccess.android.pia.model.response.TrialResponse;
import com.privateinternetaccess.android.pia.model.response.UpdateEmailResponse;
import com.privateinternetaccess.android.pia.tasks.ExpireTokenTask;
import com.privateinternetaccess.android.pia.tasks.FetchAccountTask;
import com.privateinternetaccess.android.pia.tasks.PurchasingTask;
import com.privateinternetaccess.android.pia.tasks.RetryPurchasingTask;
import com.privateinternetaccess.android.pia.tasks.TokenTask;
import com.privateinternetaccess.android.pia.tasks.TrialCreationTask;
import com.privateinternetaccess.android.pia.tasks.UpdateEmailTask;
import com.privateinternetaccess.android.pia.utils.DLog;
import com.privateinternetaccess.core.utils.IPIACallback;


/**
 * Helper methods to manage and acquire the PIA Account information grabbed by {@link TokenTask} or the {@link FetchAccountTask}.
 *
 * Helps with Purchasing by using the {@link RetryPurchasingTask} and {@link PurchasingTask} including the temporary storage if purchasing fails.
 *
 * Implementation for {@link IAccount} and can be accessed by {@link com.privateinternetaccess.android.pia.PIAFactory}
 *
 * Created by hfrede on 9/6/17.
 */

public class AccountImpl implements IAccount {

    private final Context context;

    public AccountImpl(Context context) {
        this.context = context;
    }

    @Override
    public void login(LoginInfo info, IPIACallback<TokenResponse> callback) {
        TokenTask checklogin = new TokenTask(context, info);
        checklogin.setCallback(callback);
        checklogin.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public void updateEmail(UpdateAccountInfo info, IPIACallback<UpdateEmailResponse> callback) {
        UpdateEmailTask task = new UpdateEmailTask(context, info);
        task.setCallback(callback);
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public boolean isLoggedIn() {
        return PiaPrefHandler.isUserLoggedIn(context);
    }

    @Override
    public void logout(String token) {
        new ExpireTokenTask(context, null, token).execute();
        PiaPrefHandler.clearAccountInformation(context);
    }

    @Override
    public PIAAccountData getAccountInfo() {
        return PiaPrefHandler.getAccountInformation(context);
    }

    @Override
    public void checkAccountInfo(IPIACallback<LoginResponse> callback) {
        new FetchAccountTask(context, callback).execute();
    }

    @Override
    public RetryPurchasingTask startPurchaseProcess(IPIACallback<PurchasingResponse> callback) {
        return RetryPurchasingTask.startDelayedTasks(context, callback);
    }

    @Override
    public PurchaseData getTempoaryPurchaseData() {
        return PiaPrefHandler.getPurchasingData(context);
    }

    @Override
    public void purchase(PurchaseData data, IPIACallback<PurchasingResponse> callback) {
        PurchasingTask asyncTask = new PurchasingTask(context, data.getEmail(), data.getOrderId(), data.getToken(), data.getProductId());
        asyncTask.setCallback(callback);
        asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public void saveTemporaryPurchaseData(PurchaseData data) {
        PiaPrefHandler.savePurchasingTask(context, data.getEmail(), data.getOrderId(), data.getToken(), data.getProductId());
    }

    @Override
    public void saveTemporaryTrialData(TrialData data) {
        PiaPrefHandler.saveTempTrialData(context, data);
    }

    @Override
    public TrialCreationTask createTrialAccount(IPIACallback<TrialResponse> callback) {
        TrialCreationTask task = new TrialCreationTask(context);
        task.setCallback(callback);
        task.execute();
        return task;
    }
}