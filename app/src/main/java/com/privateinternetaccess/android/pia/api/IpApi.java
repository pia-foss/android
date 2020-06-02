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

package com.privateinternetaccess.android.pia.api;

import android.content.Context;
import androidx.core.util.Pair;

import com.privateinternetaccess.android.pia.model.response.IPResponse;
import com.privateinternetaccess.android.pia.utils.DLog;

import org.json.JSONObject;

import java.util.Locale;

import okhttp3.Request;
import okhttp3.Response;

/**
 * Get the IP from the server
 *
 * Created by hfrede on 6/13/17.
 */

public class IpApi extends PiaApi {

    private Context context;

    public IpApi(Context context) {
        super();
        this.context = context;
    }

    /**
     * Gets the current IP of the client from our servers
     *
     * @return {@link IPResponse}
     */
    public IPResponse getIPAddress() {
        try {
            Request request = new Request.Builder()
                    .url(getClientURL(context,"status"))
                    .addHeader("User-Agent", PiaApi.ANDROID_HTTP_CLIENT)
                    .build();

            Response res = getOkHttpClient().newCall(request).execute();

            int status = res.code();
            if (status != 200)
                return null;

            String body = res.body().string();
            JSONObject json = new JSONObject(body);

            boolean connected = json.optBoolean("connected");
            String ip = json.optString("ip");

            DLog.d("PIA", String.format(Locale.US, "IP result from API: connected %s, ip %s", connected, ip));
            return new IPResponse(Pair.create(connected, ip));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}