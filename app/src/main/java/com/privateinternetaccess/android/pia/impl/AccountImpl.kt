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

package com.privateinternetaccess.android.pia.impl

import android.content.Context
import androidx.annotation.VisibleForTesting
import com.privateinternetaccess.account.AndroidAccountAPI
import com.privateinternetaccess.account.model.request.AndroidSignupInformation
import com.privateinternetaccess.account.model.response.*
import com.privateinternetaccess.account.model.response.DedicatedIPInformationResponse.DedicatedIPInformation
import com.privateinternetaccess.android.BuildConfig
import com.privateinternetaccess.android.pia.account.PIAAccount
import com.privateinternetaccess.android.pia.handlers.PiaPrefHandler
import com.privateinternetaccess.android.pia.handlers.PiaPrefHandler.PREFNAME
import com.privateinternetaccess.android.pia.interfaces.IAccount
import com.privateinternetaccess.android.pia.model.AccountInformation
import com.privateinternetaccess.android.pia.model.PurchaseData
import com.privateinternetaccess.android.pia.model.enums.RequestResponseStatus
import com.privateinternetaccess.android.pia.providers.ModuleClientStateProvider
import com.privateinternetaccess.android.pia.utils.DLog
import com.privateinternetaccess.android.ui.connection.MainActivityHandler
import com.privateinternetaccess.android.utils.CSIHelper
import com.privateinternetaccess.csi.*


class AccountImpl(private val context: Context) : IAccount, ProtocolInformationProvider, RegionInformationProvider {

    companion object {
        private const val TAG = "AccountImpl"
        private const val STORE = "google_play"
        private lateinit var CSI: CSIAPI
    }

    init {
        CSI = CSIBuilder()
                .setAndroidPreferenceFilename(PREFNAME)
                .setPlatform(Platform.ANDROID)
                .setAppVersion(BuildConfig.VERSION_NAME)
                .setCSIClientStateProvider(ModuleClientStateProvider(context))
                .setProtocolInformationProvider(this)
                .setRegionInformationProvider(this)
                .build()
    }

    override fun protocolInformation(): String {
        return CSIHelper.getProtocol(context)
    }

    override fun regionInformation(): String {
        return CSIHelper.getRegions(context)
    }

    private var androidAccountAPI: AndroidAccountAPI? = null
        get() {
            if (field == null) {
                androidAccountAPI = PIAAccount.getApi(context)
            }
            return field
        }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    fun setAndroidAccountAPI(androidAccountAPI: AndroidAccountAPI) {
        this.androidAccountAPI = androidAccountAPI
    }

    override fun signUp(
            orderId: String,
            token: String,
            sku: String,
            callback: (information: SignUpInformation?, status: RequestResponseStatus) -> Unit
    ) {
        val receipt = AndroidSignupInformation.Receipt(orderId, token, sku)
        androidAccountAPI?.signUp(AndroidSignupInformation(
                store = STORE,
                receipt = receipt)
        ) { details, error ->
            error?.let {
                DLog.w(TAG, "signUp error: $error")
                callback(null, adaptResponseCode(it.code))
                return@signUp
            }

            if (details == null) {
                DLog.w(TAG, "signUp Invalid response")
                callback(null, RequestResponseStatus.OP_FAILED)
                return@signUp
            }

            callback(details, RequestResponseStatus.SUCCEEDED)
        }
    }

    override fun loginLink(email: String, callback: (status: RequestResponseStatus) -> Unit) {
        androidAccountAPI?.loginLink(email) { error ->
            error?.let {
                DLog.w(TAG, "loginLink error: $error")
                callback(adaptResponseCode(it.code))
                return@loginLink
            }

            callback(RequestResponseStatus.SUCCEEDED)
        }
    }

    override fun loginWithCredentials(
            username: String,
            password: String,
            callback: (token: String?, status: RequestResponseStatus) -> Unit
    ) {
        androidAccountAPI?.loginWithCredentials(username, password) { token, error ->
            error?.let {
                DLog.w(TAG, "loginWithCredentials error: $error")
                callback(null, adaptResponseCode(it.code))
                return@loginWithCredentials
            }

            if (token.isNullOrEmpty()) {
                DLog.w(TAG, "loginWithCredentials Invalid response")
                callback(null, RequestResponseStatus.OP_FAILED)
                return@loginWithCredentials
            }

            callback(token, RequestResponseStatus.SUCCEEDED)
        }
    }

    override fun loginWithReceipt(
            token: String,
            productId: String,
            callback: (token: String?, status: RequestResponseStatus) -> Unit
    ) {
        androidAccountAPI?.loginWithReceipt(
                STORE,
                token,
                productId,
                context.packageName
        ) { token, error ->
            error?.let {
                DLog.w(TAG, "loginWithReceipt error: $error")
                callback(null, adaptResponseCode(it.code))
                return@loginWithReceipt
            }

            if (token.isNullOrEmpty()) {
                DLog.w(TAG, "loginWithReceipt Invalid response")
                callback(null, RequestResponseStatus.OP_FAILED)
                return@loginWithReceipt
            }

            callback(token, RequestResponseStatus.SUCCEEDED)
        }
    }

    override fun updateEmail(
            token: String,
            email: String,
            resetPassword: Boolean,
            callback: (temporaryPassword: String?, status: RequestResponseStatus) -> Unit
    ) {
        androidAccountAPI?.setEmail(token, email, false) { temporaryPassword, error ->
            error?.let {
                DLog.w(TAG, "setEmail error: $error")
                callback(temporaryPassword, adaptResponseCode(it.code))
                return@setEmail
            }

            if (resetPassword && temporaryPassword.isNullOrEmpty()) {
                DLog.w(TAG, "setEmail Invalid response")
                callback(temporaryPassword, RequestResponseStatus.OP_FAILED)
                return@setEmail
            }

            callback(temporaryPassword, RequestResponseStatus.SUCCEEDED)
        }
    }

    override fun loggedIn(): Boolean {
        return PiaPrefHandler.isUserLoggedIn(context)
    }

    override fun logout(token: String) {
        androidAccountAPI?.logout(token) { error ->
            error?.let {
                DLog.w(TAG, "logout error: $error")
                return@logout
            }
        }
    }

    override fun persistedAccountInformation(): AccountInformation? {
        return PiaPrefHandler.getAccountInformation(context)
    }

    override fun accountInformation(
            token: String,
            callback: (accountInformation: AccountInformation?, status: RequestResponseStatus) -> Unit
    ) {
        androidAccountAPI?.accountDetails(token) { details, error ->
            error?.let {
                DLog.w(TAG, "accountInformation error: $error")
                callback(null, adaptResponseCode(it.code))
                return@accountDetails
            }

            if (details == null) {
                DLog.w(TAG, "accountInformation Invalid response")
                callback(null, RequestResponseStatus.OP_FAILED)
                return@accountDetails
            }

            val accountInformation = AccountInformation(
                    details.email,
                    details.active,
                    details.expired,
                    details.renewable,
                    details.expireAlert,
                    details.plan,
                    details.expirationTime * 1000L,
                    details.username
            )
            callback(accountInformation, RequestResponseStatus.SUCCEEDED)
        }
    }

    override fun dedicatedIPs(
            token: String,
            ipTokens: List<String>,
            callback: (details: List<DedicatedIPInformation>, status: RequestResponseStatus) -> Unit
    ) {
        androidAccountAPI?.dedicatedIPs(token, ipTokens) { details, error ->
            error?.let {
                callback(emptyList(), adaptResponseCode(it.code))
                return@dedicatedIPs
            }

            callback(details, RequestResponseStatus.SUCCEEDED)
        }
    }

    override fun renewDedicatedIP(
            token: String,
            ipToken: String,
            callback: (status: RequestResponseStatus) -> Unit
    ) {
        if (PiaPrefHandler.isFeatureActive(context, MainActivityHandler.DIP_CHECK_EXPIRATION_REQUEST)) {
            androidAccountAPI?.renewDedicatedIP(token, ipToken) { error ->
                error?.let {
                    DLog.w(TAG, "renewDedicatedIP error: $error")
                    callback(adaptResponseCode(it.code))
                    return@renewDedicatedIP
                }

                callback(RequestResponseStatus.SUCCEEDED)
            }
        } else {
            DLog.w(TAG, "renewDedicatedIP error: Feature flag missing.")
            callback(RequestResponseStatus.OP_FAILED)
        }
    }

    override fun createTrialAccount(
            email: String,
            code: String,
            callback: (
                    username: String?,
                    password: String?,
                    message: String?,
                    status: RequestResponseStatus
            ) -> Unit
    ) {
        androidAccountAPI?.redeem(email, code) { details, error ->
            error?.let {
                DLog.w(TAG, "createTrialAccount error: $error")
                callback(null, null, null, adaptResponseCode(it.code))
                return@redeem
            }

            if (details == null) {
                DLog.w(TAG, "createTrialAccount Invalid response")
                callback(null, null, null, RequestResponseStatus.OP_FAILED)
                return@redeem
            }

            callback(
                    details.username,
                    details.password,
                    details.message,
                    RequestResponseStatus.SUCCEEDED
            )
        }
    }

    override fun sendInvite(
            token: String,
            recipientEmail: String,
            recipientName: String,
            callback: (status: RequestResponseStatus) -> Unit
    ) {
        androidAccountAPI?.sendInvite(token, recipientEmail, recipientName) { error ->
            error?.let {
                DLog.w(TAG, "sendInvite error: $error")
                callback(adaptResponseCode(it.code))
                return@sendInvite
            }

            callback(RequestResponseStatus.SUCCEEDED)
        }
    }

    override fun invites(
            token: String,
            callback: (details: InvitesDetailsInformation?, status: RequestResponseStatus) -> Unit
    ) {
        androidAccountAPI?.invitesDetails(token) { details, error ->
            error?.let {
                DLog.w(TAG, "invites error: $error")
                callback(null, adaptResponseCode(it.code))
                return@invitesDetails
            }

            if (details == null) {
                DLog.w(TAG, "invites Invalid response")
                callback(null, RequestResponseStatus.OP_FAILED)
                return@invitesDetails
            }

            callback(details, RequestResponseStatus.SUCCEEDED)
        }
    }

    override fun clientStatus(
            callback: (details: ClientStatusInformation?, status: RequestResponseStatus) -> Unit
    ) {
        androidAccountAPI?.clientStatus { details, error ->
            error?.let {
                DLog.w(TAG, "clientStatus error: $error")
                callback(null, adaptResponseCode(it.code))
                return@clientStatus
            }

            if (details == null) {
                DLog.w(TAG, "clientStatus Invalid response")
                callback(null, RequestResponseStatus.OP_FAILED)
                return@clientStatus
            }

            callback(details, RequestResponseStatus.SUCCEEDED)
        }
    }

    override fun availableSubscriptions(
            callback: (
                    subscriptions: AndroidSubscriptionsInformation?,
                    status: RequestResponseStatus
            ) -> Unit
    ) {
        androidAccountAPI?.subscriptions { details, error ->
            error?.let {
                DLog.w(TAG, "availableSubscriptions error: $error")
                callback(null, adaptResponseCode(it.code))
                return@subscriptions
            }

            if (details == null) {
                DLog.w(TAG, "availableSubscriptions Invalid response")
                callback(null, RequestResponseStatus.OP_FAILED)
                return@subscriptions
            }

            callback(details, RequestResponseStatus.SUCCEEDED)
        }
    }

    override fun message(
            token: String,
            callback: (
                    message: MessageInformation?,
                    status: RequestResponseStatus
            ) -> Unit
    ) {
        androidAccountAPI?.message(token, BuildConfig.VERSION_NAME) { message, error ->
            error?.let {
                DLog.w(TAG, "messages error: $error")
                callback(null, adaptResponseCode(it.code))
                return@message
            }

            if (message == null) {
                DLog.w(TAG, "message Invalid response")
                callback(null, RequestResponseStatus.OP_FAILED)
                return@message
            }

            callback(message, RequestResponseStatus.SUCCEEDED)
        }
    }

    override fun featureFlags(
            callback: (
                    featureFlags: FeatureFlagsInformation?,
                    status: RequestResponseStatus
            ) -> Unit) {
        androidAccountAPI?.featureFlags { flags, error ->
            error?.let {
                DLog.w(TAG, "feature flags error: $error")
                callback(null, adaptResponseCode(it.code))
                return@featureFlags
            }

            if (flags == null) {
                DLog.w(TAG, "feature flags Invalid response")
                callback(null, RequestResponseStatus.OP_FAILED)
                return@featureFlags
            }

            callback(flags, RequestResponseStatus.SUCCEEDED)
        }
    }

    override fun sendDebugReport(
            callback: (reportIdentifier: String?, status: RequestResponseStatus) -> Unit
    ) {
        CSI.send(true) { reportIdentifier, error ->
            error?.let {
                DLog.w(TAG, "debug report error: $error")
                callback(null, adaptResponseCode(it.code))
                return@send
            }

            if (reportIdentifier == null) {
                DLog.w(TAG, "debug report Invalid response")
                callback(null, RequestResponseStatus.OP_FAILED)
                return@send
            }

            callback(reportIdentifier, RequestResponseStatus.SUCCEEDED)
        }
    }

    override fun temporaryPurchaseData(): PurchaseData? {
        return PiaPrefHandler.getPurchasingData(context)
    }

    override fun saveTemporaryPurchaseData(data: PurchaseData) {
        PiaPrefHandler.savePurchasingTask(context, data.orderId, data.token, data.productId)
    }

    // region private
    private fun adaptResponseCode(responseCode: Int) = when (responseCode) {
        200 -> RequestResponseStatus.SUCCEEDED
        401 -> RequestResponseStatus.AUTH_FAILED
        429 -> RequestResponseStatus.THROTTLED
        else -> RequestResponseStatus.OP_FAILED
    }
    // endregion
}