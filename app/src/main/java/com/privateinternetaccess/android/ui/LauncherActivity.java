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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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
import com.privateinternetaccess.android.pia.model.LoginInfo;
import com.privateinternetaccess.android.pia.model.response.TokenResponse;
import com.privateinternetaccess.android.pia.tasks.TokenTask;
import com.privateinternetaccess.android.pia.utils.DLog;
import com.privateinternetaccess.android.pia.utils.Prefs;
import com.privateinternetaccess.android.pia.utils.Toaster;
import com.privateinternetaccess.android.ui.connection.MainActivity;
import com.privateinternetaccess.android.ui.connection.VPNPermissionActivity;
import com.privateinternetaccess.android.ui.loginpurchasing.LoginPurchaseActivity;
import com.privateinternetaccess.android.ui.startup.StartupActivity;
import com.privateinternetaccess.android.ui.tv.DashboardActivity;
import com.privateinternetaccess.core.utils.IPIACallback;

public class LauncherActivity extends AppCompatActivity {

    public static final String LOGIN = "login";
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
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
    }

    private void checkAuthentication() {
        String user = PiaPrefHandler.getLogin(this);
        String password = PiaPrefHandler.getSavedPassword(this);
        String token = PiaPrefHandler.getAuthToken(this);

        if (TextUtils.isEmpty(token) && (!TextUtils.isEmpty(password) && !TextUtils.isEmpty(user))) {
            LoginInfo loginInfo = new LoginInfo(user, password);
            TokenTask task = new TokenTask(this, loginInfo);
            task.setCallback(new IPIACallback<TokenResponse>() {
                @Override
                public void apiReturn(TokenResponse tokenResponse) {
                    nextActivityLogic();
                }
            });
            task.execute();
        }
        else {
            nextActivityLogic();
        }
    }

    private void nextActivityLogic() {
        Handler h = new Handler();
        h.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(!Prefs.with(getApplicationContext()).getBoolean(StartupActivity.HAS_SEEN_STARTUP) && !PIAApplication.isAndroidTV(getApplicationContext())){
                    Intent i = new Intent(getApplicationContext(), StartupActivity.class);
                    startActivity(i);
                    finish();
                    return;
                }

                Intent intent = getIntent();
                if (intent != null && intent.getData() != null) {
                    DLog.i("Launcher Activity", "data not null");
                    Uri openUri = intent.getData();
                    setIntent(null);
//                    boolean isLogin = openUri.getPath().equals(LOGIN);
                    final String username = openUri.getQueryParameter(USERNAME);
                    final String password = openUri.getQueryParameter(PASSWORD);
                    if (username != null && password != null) {
                        // Check if user/pw are equal to what we already have stored
                        DLog.d("Launcher", "Stored = " + PiaPrefHandler.getLogin(getApplicationContext()) + " open = " + username);
                        if (PiaPrefHandler.getLogin(getApplicationContext()).equals(username) &&
                                PiaPrefHandler.getSavedPassword(getApplicationContext()).equals(password)) {
                            Toaster.l(getApplicationContext(), getString(R.string.username_password_already_saved));
                            launchVPN(getApplicationContext());
                        } else {
                            //Username and pw do not match and no account information present
                            if (TextUtils.isEmpty(PiaPrefHandler.getSavedPassword(getApplicationContext()))) {
                                PiaPrefHandler.setUserIsLoggedIn(getApplicationContext(), false);
                                launchVPN(getApplicationContext());
                            } else {
                                Activity act = LauncherActivity.this;
                                AlertDialog.Builder ab = new AlertDialog.Builder(act);
                                ab.setTitle(R.string.replace_login_title);
                                ab.setMessage(getString(R.string.replace_login_msg, username, PiaPrefHandler.getLogin(getApplicationContext())));
                                ab.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        PiaPrefHandler.setUserIsLoggedIn(LauncherActivity.this, false);
                                        launchVPN(LauncherActivity.this);
                                    }
                                });

                                ab.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        launchVPN(LauncherActivity.this);
                                    }
                                });
                                ab.create().show();
                            }
                        }
                    } else {
                        launchVPN(getApplicationContext());
                    }
                } else {
                    launchVPN(getApplicationContext());
                }
            }
        }, DELAY_MILLIS);
    }

    void launchVPN(Context context) {
        DLog.i("Launcher", "launchVPN");
        Intent intent;
        IAccount account = PIAFactory.getInstance().getAccount(this);
        if (account.isLoggedIn()) {
            Intent vpnIntent = VpnService.prepare(getApplicationContext());
            if (vpnIntent == null) {
                DLog.i("Launcher", "Logged In");

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
}