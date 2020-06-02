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

import com.privateinternetaccess.android.pia.IPIACallback;
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
import com.privateinternetaccess.android.pia.tasks.RetryPurchasingTask;
import com.privateinternetaccess.android.pia.tasks.TrialCreationTask;

/**
 * This is how you interact with the PIA account information and purchasing and login backend services.
 *
 * Created by hfrede on 9/6/17.
 */

public interface IAccount {

    /**
     * Fires off {@link com.privateinternetaccess.android.pia.tasks.LoginTask}
     *
     * Responses:
     * Otto - {@link com.privateinternetaccess.android.pia.model.events.LoginEvent}
     * Callback - {@link LoginResponse}
     *
     * @param info
     * @param callback
     */
    void login(LoginInfo info, IPIACallback<TokenResponse> callback);

    /**
     * Fires off {@link com.privateinternetaccess.android.pia.tasks.UpdateEmailTask}
     *
     * Responses:
     * Otto - {@link com.privateinternetaccess.android.pia.model.events.UpdateEmailEvent}
     * Callback = {@link UpdateEmailResponse}
     *
     * @param email
     * @param callback
     */
    void updateEmail(UpdateAccountInfo email, IPIACallback<UpdateEmailResponse> callback);

    /**
     * Grabs the boolean using {@link com.privateinternetaccess.android.pia.handlers.PiaPrefHandler}
     *
     * @return
     */
    boolean isLoggedIn();

    /**
     * deletes the user data
     */
    void logout(String token);

    /**
     * Grabs the last login response information
     *
     * @return {@link LoginResponse}
     */
    PIAAccountData getAccountInfo();

    /**
     * Fires off {@link com.privateinternetaccess.android.pia.tasks.FetchAccountTask}
     *
     * This is a nice quick way that grabs the username and password for you.
     * Use this to quickly call and check the validity of an account.
     *
     * Responses:
     * Otto - {@link com.privateinternetaccess.android.pia.model.events.LoginEvent}
     * Callback - {@link LoginResponse}
     *
     * @param callback
     */
    void checkAccountInfo(IPIACallback<LoginResponse> callback);

    /**
     * Executes the {@link com.privateinternetaccess.android.pia.tasks.PurchasingTask}
     *
     * @param data
     */
    void purchase(PurchaseData data, IPIACallback<PurchasingResponse> callback);

    /**
     * Starts up a process that will try multiple times hitting the purchase api in case of an issue occuring.
     *
     * Use {@link #saveTemporaryPurchaseData(PurchaseData)} to store data that this will use.
     *
     * Responses:
     * Otto - {@link com.privateinternetaccess.android.pia.model.events.PurchasingEvent}
     * Callback - {@link PurchasingResponse}
     *
     * @param callback
     * @return - {@link RetryPurchasingTask}
     */
    RetryPurchasingTask startPurchaseProcess(IPIACallback<PurchasingResponse> callback);

    /**
     *
     *
     * @return PurchaseData if productId is not null
     */
    PurchaseData getTempoaryPurchaseData();

    /**
     * Stores the purchase data for the {@link RetryPurchasingTask}
     *
     * @param data
     */
    void saveTemporaryPurchaseData(PurchaseData data);

    void saveTemporaryTrialData(TrialData data);

    TrialCreationTask createTrialAccount(IPIACallback<TrialResponse> callback);
}