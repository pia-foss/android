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

package com.privateinternetaccess.android.ui;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.VpnService;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.text.TextUtils;

import com.privateinternetaccess.android.PIAApplication;
import com.privateinternetaccess.android.R;
import com.privateinternetaccess.android.pia.PIAFactory;
import com.privateinternetaccess.android.pia.handlers.PiaPrefHandler;
import com.privateinternetaccess.android.pia.interfaces.IAccount;
import com.privateinternetaccess.android.pia.model.enums.RequestResponseStatus;
import com.privateinternetaccess.android.pia.utils.DLog;
import com.privateinternetaccess.android.pia.utils.Prefs;
import com.privateinternetaccess.android.ui.connection.MainActivity;
import com.privateinternetaccess.android.ui.connection.VPNPermissionActivity;
import com.privateinternetaccess.android.ui.loginpurchasing.LoginPurchaseActivity;
import com.privateinternetaccess.android.ui.tv.DashboardActivity;
import com.privateinternetaccess.android.utils.DedicatedIpUtils;
import com.privateinternetaccess.android.utils.InAppMessageManager;

import static com.privateinternetaccess.android.pia.handlers.PiaPrefHandler.TOKEN;
import static com.privateinternetaccess.android.pia.vpn.PiaOvpnConfig.DEFAULT_AUTH;
import static com.privateinternetaccess.android.pia.vpn.PiaOvpnConfig.DEFAULT_HANDSHAKE;

public class LauncherActivity extends AppCompatActivity {

    public static final String USERNAME = "username";
    public static final int DELAY_MILLIS = 1500;
    public static final String HAS_AUTO_STARTED = "hasAutoStarted";

    @Override
    protected void onNewIntent(Intent intent) {
        this.setIntent(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        DLog.i("MainActivity", "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);
        Prefs.with(getApplicationContext()).set(HAS_AUTO_STARTED, false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkAuthentication();
        loadFlags(this);
    }

    private void checkAuthentication() {
        String token = PiaPrefHandler.getAuthToken(this);
        if (TextUtils.isEmpty(token)) {
            PiaPrefHandler.setUserIsLoggedIn(this, false);
        }
        nextActivityLogic();
    }

    private void nextActivityLogic() {
        Handler h = new Handler();
        h.postDelayed(() -> {
            Intent intent = getIntent();
            DLog.i("Launcher Activity", "Starting app");
            if (intent != null && intent.getData() != null) {
                Uri openUri = intent.getData();
                setIntent(null);

                if (openUri.toString().contains("login")) {
                    String url = openUri.toString();
                    url = url.replace("piavpn:login?", "piavpn://login/?");
                    DLog.i("Launcher Activity", "URL: " + url);
                    openUri = Uri.parse(url);
                }

                final String username = openUri.getQueryParameter(USERNAME);
                final String token = openUri.getQueryParameter(TOKEN);
                if (token != null) {
                    IAccount account = PIAFactory.getInstance().getAccount(LauncherActivity.this);

                    if (!account.loggedIn()) {
                        PiaPrefHandler.saveAuthToken(LauncherActivity.this, token);
                        PiaPrefHandler.setUserIsLoggedIn(LauncherActivity.this, true);
                        launchVPN(LauncherActivity.this);
                    }
                }

                if (username != null) {
                    DLog.d("Launcher", "Stored = " + PiaPrefHandler.getLogin(getApplicationContext()) + " open = " + username);
                    if (!PiaPrefHandler.getLogin(getApplicationContext()).equals(username)) {
                        PiaPrefHandler.setUserIsLoggedIn(getApplicationContext(), false);
                    }
                }
            }
            launchVPN(getApplicationContext());
        }, DELAY_MILLIS);
    }

    void launchVPN(Context context) {
        DLog.i("Launcher", "launchVPN");
        Intent intent;
        IAccount account = PIAFactory.getInstance().getAccount(this);
        if (account.loggedIn()) {
            Intent vpnIntent = VpnService.prepare(getApplicationContext());
            if (vpnIntent == null) {
                DLog.i("Launcher", "Logged In");

                loadOnLaunch(context);

                if (PIAApplication.isAndroidTV(getApplicationContext())) {
                    intent = new Intent(this, DashboardActivity.class);
                }
                else {
                    intent = new Intent(this, MainActivity.class);
                }

                if(getIntent() != null) {
                    String shortcut = getIntent().getAction();
                    DLog.d("LauncherActivity", "shortcut = " + shortcut);
                    if (!TextUtils.isEmpty(shortcut)) {
                        intent.setAction(shortcut);
                    }
                }
            } else {
                intent = new Intent(getApplicationContext(), VPNPermissionActivity.class);
            }
        } else {
            DLog.i("Launcher", "Logged Out");
            intent = new Intent(context, LoginPurchaseActivity.class);
        }
        startActivity(intent);
        overridePendingTransition(R.anim.launcher_enter, R.anim.launcher_exit);
        finish();
    }

    void loadOnLaunch(Context context) {
        DedicatedIpUtils.refreshTokens(context);
        validatePreferences();

        if (!Prefs.with(this).get(PiaPrefHandler.HIDE_INAPP_MESSAGES, false)) {
            PIAFactory.getInstance().getAccount(context).message(PiaPrefHandler.getAuthToken(context), (message, response) -> {
                if (response == RequestResponseStatus.SUCCEEDED) {
                    InAppMessageManager.setActiveMessage(message);
                }

                return null;
            });
        }
    }

    void loadFlags(Context context) {
        PIAFactory.getInstance().getAccount(context).featureFlags((flags, response) -> {
            if (flags != null)
                PiaPrefHandler.saveFeatureFlags(context, flags.getFlags());

            return null;
        });
    }

    private void validatePreferences() {
        boolean authExists = false;
        boolean handshakeExists = false;

        String currentAuth = Prefs.with(this).get("auth", DEFAULT_AUTH);
        String currentHandshake = Prefs.with(this).get("tlscipher", DEFAULT_HANDSHAKE);

        String[] authOptions = getResources().getStringArray(R.array.auth_values);
        String[] handshakeOptions = getResources().getStringArray(R.array.tls_values);

        for (String auth : authOptions) {
            if (auth.equals(currentAuth)) {
                authExists = true;
                break;
            }
        }

        for (String handshake : handshakeOptions) {
            if (handshake.equals(currentHandshake)) {
                handshakeExists = true;
                break;
            }
        }

        if (!authExists) {
            Prefs.with(this).set("auth", DEFAULT_AUTH);
        }

        if (!handshakeExists) {
            Prefs.with(this).set("tlscipher", DEFAULT_HANDSHAKE);
        }
    }
}