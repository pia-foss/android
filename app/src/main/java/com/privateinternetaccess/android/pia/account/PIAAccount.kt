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
import com.privateinternetaccess.account.AccountBuilder
import com.privateinternetaccess.account.AndroidAccountAPI
import com.privateinternetaccess.account.Platform
import com.privateinternetaccess.android.BuildConfig
import com.privateinternetaccess.android.pia.api.PiaApi.ANDROID_HTTP_CLIENT
import com.privateinternetaccess.android.pia.handlers.PiaPrefHandler

class PIAAccount {

    companion object {
        private var api: AndroidAccountAPI? = null

        fun getApi(context: Context): AndroidAccountAPI {
            val staging = PiaPrefHandler.useStaging(context)
            if (api == null || api?.isStaging() != staging) {
                val builder = AccountBuilder<AndroidAccountAPI>()
                        .setPlatform(Platform.ANDROID)
                        .setUserAgentValue(ANDROID_HTTP_CLIENT)
                if (staging) {
                    builder.setStagingEndpoint(BuildConfig.STAGEINGHOST)
                }
                api = builder.build()
            }
            return api!!
        }
    }
}