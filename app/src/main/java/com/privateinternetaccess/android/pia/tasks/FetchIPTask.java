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
import android.os.Handler;


import com.privateinternetaccess.android.pia.api.IpApi;
import com.privateinternetaccess.android.pia.api.PiaApi;
import com.privateinternetaccess.android.pia.handlers.PiaPrefHandler;
import com.privateinternetaccess.android.pia.model.events.FetchIPEvent;
import com.privateinternetaccess.android.pia.model.response.IPResponse;
import com.privateinternetaccess.android.pia.utils.DLog;
import com.privateinternetaccess.core.utils.IPIACallback;

import org.greenrobot.eventbus.EventBus;

import java.util.Calendar;
import java.util.List;
import java.util.Vector;


/**
 * Quick task to grab the IP for you. Depending on connection, it will send you back either the connected IP or the users current IP.
 *
 * IP is saved in LastIP and a timestamp is also saved. This helps alliviate the number of calls.
 *
 * UPDATE IP TIMING is 120s.
 *
 * returns {@link FetchIPEvent} via Otto and {@link #callback}
 *
 * Created by half47 on 2/17/17.
 */

public class FetchIPTask extends AsyncTask<Integer, Void, List<IPResponse>> {

    private static final int UPDATE_IP_TIMING = 120;
    private static final String TAG = "FetchIPInformation";
    private static final int TOTAL_IP_ATTEMPTS = 4;

    public static FetchIPTask instance = null;

    private static int IP_ATTEMPTS;

    private Context context;

    private IPIACallback<FetchIPEvent> callback;

    public FetchIPTask(Context context, IPIACallback<FetchIPEvent> callback) {
        this.context = context;
        this.callback = callback;
    }

    @Override
    protected void onPostExecute(List<IPResponse> pairs) {
        DLog.d(TAG, "OnPostExecute " + IP_ATTEMPTS + " " + pairs);
        if(!isCancelled()) {
            if (pairs != null) {
                IP_ATTEMPTS = 0;
                IPResponse ipconn = pairs.get(0);
                DLog.d(TAG, "ipConn = " + ipconn);
                if (ipconn == null) {
                    sendBackEvent("", false);
                    return;
                }

                for (int i = 0; i < pairs.size(); i++) {
                    DLog.d(TAG, "ipConnFirst = " + ipconn.getPair().first + " Index: " + i);
                    DLog.d(TAG, "ipConnSecond = " + ipconn.getPair().second + " Index: " + i);
                }

                if (ipconn.getPair().first) {
                    long time = Calendar.getInstance().getTimeInMillis();
                    PiaPrefHandler.saveLastIPTimestamp(context, time);
                    DLog.d(TAG, "timestamp = " + PiaPrefHandler.getLastIPTimestamp(context));
                    PiaPrefHandler.saveLastIPVPN(context, ipconn.getPair().second);
                    sendBackEvent(ipconn.getPair().second, false);
                } else {
                    PiaPrefHandler.saveLastIP(context, ipconn.getPair().second);
                    sendBackEvent(ipconn.getPair().second, false);
                    resetValues(context);
                }
            } else {
                if (IP_ATTEMPTS <= TOTAL_IP_ATTEMPTS) {
                    Handler h = new Handler();
                    h.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            instance = new FetchIPTask(context, callback);
                            instance.execute();
                            IP_ATTEMPTS++;
                            DLog.d(TAG, "ip redo " + IP_ATTEMPTS);
                        }
                    }, 750);
                } else {
                    resetValues(context);
                    sendBackEvent("", false);
                }
            }
        }
    }

    @Override
    protected List<IPResponse> doInBackground(Integer... params) {
        if(!isCancelled()) {
            DLog.d(TAG, "grabbing ip address");

            // Even we are already connected to the VPN, directly querying the server results in
            // a race condition and gets us the non VPN IP, wait 1,234s
            try {
                if (IP_ATTEMPTS == 0)
                    Thread.sleep(PiaApi.VPN_DELAY_TIME);
            } catch (InterruptedException ignored) {
            }

            // First try without service to get VPN IP
            IpApi api = new IpApi(context);
            IPResponse ipConnected = api.getIPAddress();
            IPResponse ipNormalNet = null;
            if (ipConnected != null) {
                if (ipConnected.getPair().first) {
                    ipNormalNet = api.getIPAddress();
                }
            }
            DLog.d(TAG, "grabbed new ip " + ipNormalNet);

            Vector<IPResponse> ret = new Vector<>();
            ret.add(ipConnected);
            ret.add(ipNormalNet);
            return ret;
        } else {
            return null;
        }
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        sendBackEvent("", true);
    }

    public static boolean updateIPInformation(Context context){
        boolean update = true;
        long savedIPTimestamp = PiaPrefHandler.getLastIPTimestamp(context);
        DLog.d(TAG, "savedTimestamp = " + savedIPTimestamp);
        if(savedIPTimestamp > 0L) {
            long now = Calendar.getInstance().getTimeInMillis();
            long diff = now - savedIPTimestamp;
            diff = diff / 1000; // switch to seconds
            DLog.d(TAG, "saveddiff = " + diff);
            if(diff < UPDATE_IP_TIMING) { // less than 120s
                update = false;
            } else {
                instance = null;
            }
        }
        return update;
    }

    public static void resetValues(Context context){
        PiaPrefHandler.saveLastIPVPN(context, "");
        PiaPrefHandler.saveLastIPTimestamp(context, 0L);
        IP_ATTEMPTS = 0;
        instance = null;
    }

    public static void execute(Context context, IPIACallback<FetchIPEvent> callback) {
        DLog.d(TAG, "execute = " + instance);
        if(instance == null){
            instance = new FetchIPTask(context, callback);
            instance.execute();
        }
    }

    private void sendBackEvent(String text, boolean searching){
        if(callback != null) {
            callback.apiReturn(new FetchIPEvent(text, searching));
        }
        EventBus.getDefault().post(new FetchIPEvent(text, searching));
    }

    public void setCallback(IPIACallback<FetchIPEvent> callback) {
        this.callback = callback;
    }
}