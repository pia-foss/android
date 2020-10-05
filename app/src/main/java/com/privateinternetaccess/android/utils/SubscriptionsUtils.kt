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

package com.privateinternetaccess.android.utils

import android.content.Context
import com.privateinternetaccess.android.pia.handlers.PiaPrefHandler

object SubscriptionsUtils {

    fun getYearlySubscriptionId(context: Context): String? {
        val subscriptions = PiaPrefHandler.availableSubscriptions(context)
        var subscriptionId: String? = null
        for (product in subscriptions.availableProducts) {
            if (!product.legacy && product.plan == "yearly") {
                subscriptionId = product.id
                break
            }
        }
        return subscriptionId
    }

    fun getMonthlySubscriptionId(context: Context): String? {
        val subscriptions = PiaPrefHandler.availableSubscriptions(context)
        var subscriptionId: String? = null
        for (product in subscriptions.availableProducts) {
            if (!product.legacy && product.plan == "monthly") {
                subscriptionId = product.id
                break
            }
        }
        return subscriptionId
    }
}