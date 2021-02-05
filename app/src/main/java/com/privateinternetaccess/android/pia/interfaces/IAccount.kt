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

package com.privateinternetaccess.android.pia.interfaces

import com.privateinternetaccess.account.model.response.*
import com.privateinternetaccess.account.model.response.DedicatedIPInformationResponse.DedicatedIPInformation
import com.privateinternetaccess.android.pia.model.AccountInformation
import com.privateinternetaccess.android.pia.model.PurchaseData
import com.privateinternetaccess.android.pia.model.enums.RequestResponseStatus

/**
 * This is how you interact with the PIA account information and purchasing and login backend services.
 */
interface IAccount {

    /**
     * @param orderId
     * @param token
     * @param sku
     * @param callback
     */
    fun signUp(
            orderId: String,
            token: String,
            sku: String,
            callback: (information: SignUpInformation?, status: RequestResponseStatus) -> Unit
    )

    /**
     * @param email
     * @param callback
     */
    fun loginLink(email: String, callback: (status: RequestResponseStatus) -> Unit)

    /**
     * @param username
     * @param password
     * @param callback
     */
    fun loginWithCredentials(
            username: String,
            password: String,
            callback: (token: String?, status: RequestResponseStatus) -> Unit
    )

    /**
     * @param token
     * @param productId
     * @param callback
     */
    fun loginWithReceipt(
            token: String,
            productId: String,
            callback: (token: String?, status: RequestResponseStatus) -> Unit
    )

    /**
     * @return `boolean`
     */
    fun loggedIn(): Boolean

    /**
     * @param token
     * @param callback
     */
    fun accountInformation(
            token: String,
            callback: (accountInformation: AccountInformation?, status: RequestResponseStatus) -> Unit
    )

    /**
     * @param token
     * @param ipTokens
     * @param callback
     */
    fun dedicatedIPs(
            token: String,
            ipTokens: List<String>,
            callback: (details: List<DedicatedIPInformation>, status: RequestResponseStatus) -> Unit
    )

    /**
     * @return `AccountInformation`
     */
    fun persistedAccountInformation(): AccountInformation?

    /**
     * @param token
     * @param email
     * @param callback
     */
    fun updateEmail(
            token: String,
            email: String,
            resetPassword: Boolean,
            callback: (temporaryPassword: String?, status: RequestResponseStatus) -> Unit
    )

    /**
     * @param email
     * @param code
     * @param callback
     */
    fun createTrialAccount(
            email: String,
            code: String,
            callback: (
                    username: String?,
                    password: String?,
                    message: String?,
                    status: RequestResponseStatus
            ) -> Unit
    )

    /**
     * @param token
     * @param recipientEmail
     * @param recipientName
     * @param callback
     */
    fun sendInvite(
            token: String,
            recipientEmail: String,
            recipientName: String,
            callback: (status: RequestResponseStatus) -> Unit
    )

    /**
     * @param token
     * @param callback
     */
    fun invites(
            token: String,
            callback: (details: InvitesDetailsInformation?, status: RequestResponseStatus) -> Unit
    )

    /**
     * @param token
     */
    fun logout(token: String)

    /**
     * @param callback
     */
    fun clientStatus(
            callback: (details: ClientStatusInformation?, status: RequestResponseStatus) -> Unit
    )

    /**
     * @param callback
     */
    fun availableSubscriptions(
            callback: (
                    subscriptions: AndroidSubscriptionsInformation?,
                    status: RequestResponseStatus
            ) -> Unit
    )

    /**
     * @param token
     * @param callback
     */
    fun message(
            token: String,
            callback: (
                    message: MessageInformation?,
                    status: RequestResponseStatus
            ) -> Unit
    )

    /**
     * @param callback
    */
    fun featureFlags(
            callback: (
                    featureFlags: FeatureFlagsInformation?,
                    status: RequestResponseStatus
            ) -> Unit
    )

    /**
     * @param callback
     */
    fun sendDebugReport(
            callback: (reportIdentifier: String?, status: RequestResponseStatus) -> Unit
    )

    /**
     * @return `PurchaseData`
     */
    fun temporaryPurchaseData(): PurchaseData?

    /**
     * @param data
     */
    fun saveTemporaryPurchaseData(data: PurchaseData)
}