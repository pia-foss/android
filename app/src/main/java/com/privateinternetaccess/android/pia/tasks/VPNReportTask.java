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
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.text.TextUtils;
import android.text.format.DateFormat;

import com.privateinternetaccess.android.BuildConfig;
import com.privateinternetaccess.android.R;
import com.privateinternetaccess.android.model.exceptions.CustomExceptionHandler;
import com.privateinternetaccess.android.model.states.VPNProtocol;
import com.privateinternetaccess.android.pia.IPIACallback;
import com.privateinternetaccess.android.pia.api.ReportingApi;
import com.privateinternetaccess.android.pia.handlers.PiaPrefHandler;
import com.privateinternetaccess.android.pia.handlers.ThemeHandler;
import com.privateinternetaccess.android.pia.model.events.ReportEvent;
import com.privateinternetaccess.android.pia.model.response.ReportResponse;
import com.privateinternetaccess.android.pia.utils.DLog;
import com.privateinternetaccess.android.pia.utils.Prefs;
import com.privateinternetaccess.android.pia.vpn.PiaOvpnConfig;

import org.greenrobot.eventbus.EventBus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import de.blinkt.openvpn.core.LogItem;
import de.blinkt.openvpn.core.NativeUtils;
import de.blinkt.openvpn.core.VpnStatus;

/**
 * Returns {@link ReportEvent} via otto, {@link ReportResponse} via callback
 *
 * Created by hfrede on 8/18/17.
 */

public class VPNReportTask extends AsyncTask<String, Void, ReportResponse> {

    private Context context;

    private IPIACallback<ReportResponse> callback;

    public VPNReportTask(Context context) {
        this.context = context;
    }

    @Override
    protected ReportResponse doInBackground(String... strings) {
        try {
            VpnStatus.logDebug("Requested support log");
            ReportingApi api = new ReportingApi(context);

            if (VPNProtocol.activeProtocol(context) == VPNProtocol.Protocol.OpenVPN) {
                return api.sendReport(new String[] {
                    getVersionString(context),
                    getConfigStr(context),
                    getUserSettings(context),
                    getLogStr(context),
                    CustomExceptionHandler.getStackTrace(context)});
            }
            else  {
                return api.sendReport(new String[] {
                        getVersionString(context),
                        getConfigStr(context),
                        getUserSettings(context),
                        getWireguardReport(),
                        CustomExceptionHandler.getStackTrace(context)});
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return new ReportResponse(ioe);
        } catch (Exception ex){
            ex.printStackTrace();
            return new ReportResponse(ex);
        }
    }

    @Override
    protected void onPostExecute(ReportResponse reportResponse) {
        super.onPostExecute(reportResponse);
        EventBus.getDefault().post(new ReportEvent(reportResponse));
        if(callback != null) {
            callback.apiReturn(reportResponse);
        }
    }

    String getWireguardReport() throws Exception {
        StringBuilder builder = new StringBuilder();
        try {
            final Process process = Runtime.getRuntime().exec(new String[]{
                    "logcat", "-b", "all", "-d", "-v", "threadtime", "*:V"});
            try (final BufferedReader stdout = new BufferedReader(new InputStreamReader(process.getInputStream()));
                 final BufferedReader stderr = new BufferedReader(new InputStreamReader(process.getErrorStream())))
            {
                String line;
                while ((line = stdout.readLine()) != null) {
                    builder.append(line);
                    builder.append(line + "\n");
                }

                stdout.close();
                if (process.waitFor() != 0) {
                    final StringBuilder errors = new StringBuilder();
                    errors.append("Unable to run logcat:");
                    while ((line = stderr.readLine()) != null)
                        errors.append(line);
                    throw new Exception(errors.toString());
                }
            }
        } catch (final Exception e) {
            throw e;
        }

        return builder.toString();
    }

    String getConfigStr(Context context) throws IOException {
//        return "Config if the user would connect NOW. (The log included in the report might have used a different config if the user changed settings before sending the report)" +
//                PiaOvpnConfig.genConfig(context, PiaPrefHandler.getLogin(context), "NOT_IN_DEBUG_LOG");
        return "\nopenvpn_config\n" +
                PiaOvpnConfig.genConfig(context, PiaPrefHandler.getLogin(context), "NOT_IN_DEBUG_LOG") + "\n";
    }

    String getVersionString(Context context) {
        LogItem info = new LogItem(VpnStatus.LogLevel.INFO, R.string.mobile_info_report, Build.MODEL, Build.BOARD, Build.BRAND, Build.VERSION.SDK_INT,
                NativeUtils.getNativeAPI(), Build.VERSION.RELEASE, Build.ID, Build.FINGERPRINT, BuildConfig.VERSION_CODE, BuildConfig.VERSION_NAME, BuildConfig.FLAVOR_store);
        return info.getString(context);
    }

    String getLogStr(Context context) {
        StringBuilder sb = new StringBuilder();
        sb.append("\npia_log\n");
        for (LogItem entry : VpnStatus.getlogbuffer()) {
            sb.append(getTime(entry, TIME_FORMAT_ISO) + entry.getString(context) + '\n');
        }
        return sb.toString();
    }

    String getUserSettings(Context context){
        Prefs prefs = Prefs.with(context);
        StringBuilder sb = new StringBuilder();
        sb.append("\n\n~~~~~ User Settings ~~~~~\n\n");
        sb.append("~~ Connection Settings ~~").append("\n");
        sb.append("Connection Type: " + (prefs.getBoolean(PiaPrefHandler.USE_TCP) ? "TCP": "UDP")).append("\n");
        sb.append("Port Forwarding: " + prefs.getBoolean(PiaPrefHandler.PORTFORWARDING)).append("\n");
        sb.append("Remote Port: " + prefs.get(PiaPrefHandler.RPORT, "auto")).append("\n");
        sb.append("Local Port: " + prefs.get(PiaPrefHandler.LPORT, "auto")).append("\n");
        sb.append("Use Small Packets: " + prefs.get(PiaPrefHandler.PACKET_SIZE, context.getResources().getBoolean(R.bool.usemssfix))).append("\n").append("\n");
        sb.append("Protocol: " + (VPNProtocol.activeProtocol(context) == VPNProtocol.Protocol.OpenVPN ? "OpenVPN" : "WireGuard")).append("\n");
        sb.append("~~ Proxy Settings ~~").append("\n");
        sb.append("Proxy Enabled: " + prefs.get(PiaPrefHandler.PROXY_ENABLED, false)).append("\n");
        sb.append("Proxy App: " + prefs.get(PiaPrefHandler.PROXY_APP, "")).append("\n");
        sb.append("Proxy Port: " + prefs.get(PiaPrefHandler.PROXY_PORT, "")).append("\n").append("\n");
        sb.append("~~ Blocking Settings ~~").append("\n");
        sb.append("MACE: " + prefs.get(PiaPrefHandler.PIA_MACE, false)).append("\n");
        sb.append("Killswitch: " + prefs.get(PiaPrefHandler.KILLSWITCH, false)).append("\n");
        sb.append("Ipv6 Blocking: " + prefs.get(PiaPrefHandler.IPV6, context.getResources().getBoolean(R.bool.useblockipv6))).append("\n");
        sb.append("Block Local Network: " + prefs.get(PiaPrefHandler.BLOCK_LOCAL_LAN, true)).append("\n").append("\n");
        sb.append("~~ Encryption Settings ~~").append("\n");
        String cipher = prefs.get(PiaPrefHandler.CIPHER, "AES-128-CBC");
        sb.append("Data Encryption: " + cipher).append("\n");
        if(!TextUtils.isEmpty(cipher))
            sb.append("Data Authentication: " + (cipher.toLowerCase(Locale.ENGLISH).contains("gcm") ? prefs.get(PiaPrefHandler.AUTH, "HMAC-SHA1") : "")).append("\n");
        sb.append("Handshake: " + prefs.get(PiaPrefHandler.TLSCIPHER, "RSA-2048")).append("\n").append("\n");
        sb.append("~~ App Settings ~~").append("\n");
        sb.append("1 click connect: " + prefs.get(PiaPrefHandler.AUTOCONNECT, false)).append("\n");
        sb.append("Connect on Boot: " + prefs.get(PiaPrefHandler.AUTOSTART, false)).append("\n");
        sb.append("Connect on App Updated: " + prefs.get(PiaPrefHandler.CONNECT_ON_APP_UPDATED, false)).append("\n");
        sb.append("Haptic Feedback: " + prefs.get(PiaPrefHandler.HAPTIC_FEEDBACK, true)).append("\n");
        sb.append("Dark theme: " + prefs.get(ThemeHandler.PREF_THEME, false)).append("\n");
        sb.append("\n~~~~~ End User Settings ~~~~~\n\n");
        return sb.toString();
    }


    public static final int TIME_FORMAT_NONE = 0;
    public static final int TIME_FORMAT_SHORT = 1;
    public static final int TIME_FORMAT_ISO = 2;

    private String getTime(LogItem le, int time) {
        if (time != TIME_FORMAT_NONE) {
            Date d = new Date(le.getLogtime());
            java.text.DateFormat timeformat;
            if (time == TIME_FORMAT_ISO)
                timeformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            else
                timeformat = DateFormat.getTimeFormat(context);

            return timeformat.format(d) + " ";

        } else {
            return "";
        }
    }

    public void setCallback(IPIACallback<ReportResponse> callback) {
        this.callback = callback;
    }
}
