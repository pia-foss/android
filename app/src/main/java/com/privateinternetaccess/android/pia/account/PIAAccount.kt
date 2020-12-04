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

package com.privateinternetaccess.android.pia.account

import android.content.Context
import com.privateinternetaccess.account.*
import com.privateinternetaccess.android.pia.api.PiaApi.ANDROID_HTTP_CLIENT
import com.privateinternetaccess.android.pia.providers.ModuleClientStateProvider

class PIAAccount {

    companion object {
        private var api: AndroidAccountAPI? = null

        fun getApi(context: Context): AndroidAccountAPI {
            if (api == null) {
                val builder = AccountBuilder<AndroidAccountAPI>()
                        .setClientStateProvider(ModuleClientStateProvider(context))
                        .setPlatform(Platform.ANDROID)
                        .setUserAgentValue(ANDROID_HTTP_CLIENT)
                api = builder.build()
            }
            return api!!
        }
    }
}