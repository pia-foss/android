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

package com.privateinternetaccess.android.ui.connection;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AlertDialog;
import androidx.core.view.GravityCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.widget.RelativeLayout;

import com.mikepenz.materialdrawer.Drawer;
import com.privateinternetaccess.account.model.response.DedicatedIPInformationResponse.DedicatedIPInformation;
import com.privateinternetaccess.android.BuildConfig;
import com.privateinternetaccess.android.PIAApplication;
import com.privateinternetaccess.android.R;
import com.privateinternetaccess.android.handlers.UpdateHandler;
import com.privateinternetaccess.android.model.states.VPNProtocol;
import com.privateinternetaccess.android.pia.PIAFactory;
import com.privateinternetaccess.android.pia.api.PIAAuthenticator;
import com.privateinternetaccess.android.pia.handlers.LogoutHandler;
import com.privateinternetaccess.android.pia.handlers.PiaPrefHandler;
import com.privateinternetaccess.android.pia.interfaces.IAccount;
import com.privateinternetaccess.android.pia.interfaces.IVPN;
import com.privateinternetaccess.android.pia.model.events.ConnectionAttemptsExhaustedEvent;
import com.privateinternetaccess.android.pia.utils.DLog;
import com.privateinternetaccess.android.pia.utils.Prefs;
import com.privateinternetaccess.android.pia.utils.Toaster;
import com.privateinternetaccess.android.ui.LauncherActivity;
import com.privateinternetaccess.android.ui.WidgetManager;
import com.privateinternetaccess.android.ui.adapters.WidgetsAdapter;
import com.privateinternetaccess.android.ui.drawer.AccountActivity;
import com.privateinternetaccess.android.ui.drawer.AllowedAppsActivity;
import com.privateinternetaccess.android.ui.drawer.DedicatedIPActivity;
import com.privateinternetaccess.android.ui.drawer.SettingsActivity;
import com.privateinternetaccess.android.ui.drawer.settings.AboutActivity;
import com.privateinternetaccess.android.ui.features.WebviewActivity;
import com.privateinternetaccess.android.ui.loginpurchasing.LoginPurchaseActivity;
import com.privateinternetaccess.android.ui.drawer.ServerListActivity;
import com.privateinternetaccess.android.ui.rating.Rating;
import com.privateinternetaccess.android.ui.superclasses.BaseActivity;
import com.privateinternetaccess.android.utils.drag.OnStartDragListener;
import com.privateinternetaccess.android.utils.drag.SimpleItemTouchHelperCallback;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.blinkt.openvpn.core.LogItem;

public class MainActivity extends BaseActivity {

    private static final String TAG = "MainActivity";

    public static final String CHANGE_VPN_SERVER = "com.privateinternetaccess.android.CHANGE_VPN_SERVER";
    public static final String START_VPN_SHORTCUT = "com.privateinternetaccess.android.START_VPN_SHORTCUT";
    public static boolean CHANGE_VPN_SERVER_CLOSE = false;

    public static final String SHORTCUT_SETTINGS = "com.privateinternetaccess.android.SETTINGS";

    public static final int START_SERVER_LIST = 40;
    public static final int START_VPN_PROFILE = 41;
    public static final int THEME_CHANGED = 4747;

    public static String LAST_ACTION;

    private Drawer mDrawer;

    private LogoutHandler mLogoutAssitance;

    private boolean drawerItemOpened;

    @BindView(R.id.activity_main_list) RecyclerView widgetList;
    @BindView(R.id.activity_main_root) RelativeLayout rootLayout;
    private WidgetManager widgetManager;
    private WidgetsAdapter widgetsAdapter;

    private List<WidgetManager.WidgetItem> widgetsList;

    private boolean isOrganizing = false;

    private ItemTouchHelper mItemTouchHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DLog.i("MainActivity", "onCreate");

        IAccount account = PIAFactory.getInstance().getAccount(this);
        if (!account.loggedIn()) {
            switchToLoginActivity();
            return;
        }

        checkKillswitch();

        Rating.Companion.start(this);
        setContentView(R.layout.activity_connect);
        ButterKnife.bind(this);
        setSwipeBackEnable(false);
        initHeader(false, false);
        setIconButton(R.drawable.ic_reorder, R.drawable.ic_reorder_disconnected);
        bindView();

        showUpdateCard();

        if(BuildConfig.FLAVOR_store.equals("noinapp")) {
            UpdateHandler.checkUpdates(this, UpdateHandler.UpdateDisplayType.SHOW_DIALOG);
        }

        widgetManager = new WidgetManager(this);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (PIAApplication.isAndroidTV(getApplicationContext())) {
            if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
                if (!PIAFactory.getInstance().getVPN(this).isVPNActive()) {
                    PIAFactory.getInstance().getVPN(getApplicationContext()).start();
                } else {
                    PIAFactory.getInstance().getVPN(getApplicationContext()).stop();
                }
            } else if (keyCode == KeyEvent.KEYCODE_MENU) {
                if (mDrawer.isDrawerOpen())
                    mDrawer.openDrawer();
                else
                    mDrawer.closeDrawer();
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT && mDrawer.isDrawerOpen()) {
                mDrawer.closeDrawer();
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    private void bindView() {
        initDrawer();
        Context context = getApplicationContext();
        String token = PiaPrefHandler.getAuthToken(context);
        PIAFactory.getInstance()
                .getAccount(context)
                .accountInformation(token, (accountInformation, requestResponseStatus) -> {
                    handleAccountInformation(accountInformation, requestResponseStatus);
                    initDrawer();
                    return null;
                });
    }

    @Override
    public void onIconButtonClicked(View view) {
        isOrganizing = !isOrganizing;
        organizeWidgets();
    }

    @Override
    protected void onResume() {
        super.onResume();
        drawerItemOpened = false;
        initView();
        initDrawer();
        Intent i = getIntent();
        checkForStartIntents(i);
        setIntent(null);

        if (isOrganizing) {
            isOrganizing = false;
            organizeWidgets();
        }

        renewDedicatedIP();
    }

    private void initView() {
        if (mDrawer != null) {
            mDrawer.deselect();
        }

        organizeWidgets();
    }

    private void initDrawer() {
        mDrawer = MainActivityHandler.createDrawer(this, toolbar, (view, position, drawerItem) -> {
            if(drawerItemOpened){
                return true;
            }

            // do something with the clicked item :D
            long iden = drawerItem.getIdentifier();
            mDrawer.deselect();
            mDrawer.deselect(MainActivityHandler.IDEN_FOOTER);
            boolean finishing = true;
            if (iden == MainActivityHandler.IDEN_REGION_SELECTION) {
                Intent i = new Intent(getApplicationContext(), ServerListActivity.class);
                startActivityForResult(i, MainActivity.START_SERVER_LIST);
            } else if (iden == MainActivityHandler.IDEN_ACCOUNT) {
                Intent i = new Intent(getApplicationContext(), AccountActivity.class);
                startActivity(i);
            } else if (iden == MainActivityHandler.IDEN_SETTINGS) {
                Intent i = new Intent(getApplicationContext(), SettingsActivity.class);
                startActivityForResult(i, THEME_CHANGED);
            } else if (iden == MainActivityHandler.IDEN_ABOUT) {
                Intent i = new Intent(getApplicationContext(), AboutActivity.class);
                startActivity(i);
            } else if (iden == MainActivityHandler.IDEN_HOME_PAGE) {
                Intent i = new Intent(getApplicationContext(), WebviewActivity.class);
                i.putExtra(WebviewActivity.EXTRA_URL, "https://www.privateinternetaccess.com");
                startActivity(i);
            } else if (iden == MainActivityHandler.IDEN_HELP) {
                Intent i = new Intent(getApplicationContext(), WebviewActivity.class);
                i.putExtra(WebviewActivity.EXTRA_URL, "https://www.privateinternetaccess.com/helpdesk/");
                startActivity(i);
            } else if (iden == MainActivityHandler.IDEN_PER_APP) {
                Intent i = new Intent(getApplicationContext(), AllowedAppsActivity.class);
                startActivity(i);
            } else if (iden == MainActivityHandler.IDEN_LOGOUT) {
                mLogoutAssitance = new LogoutHandler(MainActivity.this, getLogoutCallback());
                mLogoutAssitance.logout();
                finishing = false;
            } else if (iden == MainActivityHandler.IDEN_RENEW) {
                onRenewClicked();
            } else if (iden == MainActivityHandler.IDEN_PRIVACY) {
                Intent i = new Intent(MainActivity.this, WebviewActivity.class);
                i.putExtra(WebviewActivity.EXTRA_URL, "https://www.privateinternetaccess.com/pages/privacy-policy/");
                startActivity(i);
            }
            else if (iden == MainActivityHandler.IDEN_DIP) {
                Intent i = new Intent(getApplicationContext(), DedicatedIPActivity.class);
                startActivity(i);
            }

            if (finishing) {
                overridePendingTransition(R.anim.left_to_right, R.anim.right_to_left);
                drawerItemOpened = true;
            }
            return true;
        });
    }

    @Override
    public void onBackPressed() {
        if (isOrganizing) {
            onIconButtonClicked(null);
        }
        else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        DLog.d("MainActivity", "onNewIntent");
        LAST_ACTION = null;
        checkForStartIntents(intent);
        setIntent(null);
    }

    @Override
    protected void onDestroy() {
        Rating.Companion.stop();
        if (mLogoutAssitance != null) {
            mLogoutAssitance.onDestroy();
        }
        DLog.d("MainActivity", "onDestroy");
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        DLog.d("PIA", "onActivityResult(" + requestCode + "," + resultCode + "," + data);
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == START_SERVER_LIST) {
            if (resultCode == ServerListActivity.RESULT_SERVER_CHANGED) {
                startVPN(true);
            }
            if (CHANGE_VPN_SERVER_CLOSE) {
                CHANGE_VPN_SERVER_CLOSE = false;
                finish();
            }
        } else if (requestCode == START_VPN_PROFILE && resultCode == RESULT_OK) {
            IVPN vpn = PIAFactory.getInstance().getVPN(this.getBaseContext());
            vpn.start();
        } else if (requestCode == START_VPN_PROFILE && resultCode == RESULT_CANCELED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && System.currentTimeMillis() < 500) {
                AlertDialog.Builder ab = new AlertDialog.Builder(this);
                ab.setPositiveButton(android.R.string.ok, null);
                ab.setMessage(R.string.nought_always_on_warning);
                ab.setNeutralButton(R.string.open_settings, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                    }
                });
                ab.show();
            }
        } else if (resultCode == THEME_CHANGED) {
            // delay this so the activity resumes than recreates so we don't have a crash on pause.
            Handler h = new Handler();
            h.postDelayed(new Runnable() {
                @Override
                public void run() {
                    recreate();
                }
            }, 1); //All we need to do is to delay it on the UI thread so its done once resume is done.
        }
    }

    private void organizeWidgets() {
        if (isOrganizing) {
            initHeader(true, false);
            hideIconButton();
        }
        else {
            initHeader(false, false);
            initDrawer();
            showIconButton();
        }

        setBackground();

        widgetsList = widgetManager.getWidgets(isOrganizing);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        widgetList.setLayoutManager(layoutManager);
        widgetsAdapter = new WidgetsAdapter(this, widgetsList, new OnStartDragListener() {
            @Override
            public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
                mItemTouchHelper.startDrag(viewHolder);
            }
        });
        widgetsAdapter.widgetManager = widgetManager;
        widgetList.setAdapter(widgetsAdapter);
        widgetsAdapter.isReordering = isOrganizing;

        if (mItemTouchHelper != null)
            mItemTouchHelper.attachToRecyclerView(null);

        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(widgetsAdapter, isOrganizing);
        mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(widgetList);
    }

    private void switchToLoginActivity() {
        Intent i = new Intent(this, LoginPurchaseActivity.class);
        startActivity(i);
        overridePendingTransition(R.anim.right_to_left, R.anim.left_to_right);
        finish();
    }

    private void checkForStartIntents(Intent i) {
        boolean autoConnect = PiaPrefHandler.doAutoConnect(getApplicationContext())
                && !Prefs.with(getApplicationContext()).getBoolean(LauncherActivity.HAS_AUTO_STARTED);
        DLog.d("MainActivity", "checkForStartIntents");
        if (i != null) {
            boolean performAction = true;
            DLog.d("MainActivity", "action = " + i.getAction() + " last = " + LAST_ACTION);
            if (!TextUtils.isEmpty(i.getAction()) && i.getAction().equals(LAST_ACTION))
                performAction = false;

            LAST_ACTION = i.getAction();
            if (performAction) {
                if (CHANGE_VPN_SERVER.equals(i.getAction())) {
                    CHANGE_VPN_SERVER_CLOSE = true;
                    Intent server = new Intent(this, ServerListActivity.class);
                    startActivityForResult(server, START_SERVER_LIST);
                } else if (SHORTCUT_SETTINGS.equals(i.getAction())) {
                    Intent apps = new Intent(getApplicationContext(), SettingsActivity.class);
                    startActivity(apps);
                } else if ((autoConnect || START_VPN_SHORTCUT.equals(i.getAction())) && !PIAFactory.getInstance().getVPN(this).isVPNActive()) {
                    autoStartVPN();
                }
            } else {
                if (autoConnect) {
                    autoStartVPN();
                }
            }
        } else {
            if (autoConnect) {
                autoStartVPN();
            }
        }
    }

    private void showUpdateCard() {
        if (!PiaPrefHandler.getLastVersion(this).equals(BuildConfig.VERSION_NAME) && CallingCardActivity.hasCallingCard(BuildConfig.VERSION_NAME)) {
            Intent i = new Intent(this, CallingCardActivity.class);
            startActivity(i);

            PiaPrefHandler.setLastVersion(this, BuildConfig.VERSION_NAME);
        }
    }

    private void autoStartVPN() {
        Prefs.with(getApplicationContext()).set(LauncherActivity.HAS_AUTO_STARTED, true);
        startVPN(false);
    }

    @SuppressLint("NewApi")
    public void startVPN(boolean reconnect) {
        if (PIAFactory.getInstance().getVPN(this).isVPNActive() && !reconnect)
            return;

        if (VPNProtocol.activeProtocol(this) == VPNProtocol.Protocol.OpenVPN) {
            PiaPrefHandler.clearLastIPVPN(getBaseContext());
            Intent intent = VpnService.prepare(getApplicationContext());
            if (intent != null) {
                Intent i = new Intent(getApplicationContext(), VPNPermissionActivity.class);
                i.putExtra(MainActivity.START_VPN_SHORTCUT, true);
                overridePendingTransition(R.anim.launcher_enter, R.anim.launcher_exit);
                startActivityForResult(i, START_VPN_PROFILE);
            } else {
                onActivityResult(START_VPN_PROFILE, RESULT_OK, null);
            }
        }
        else {
            PIAFactory.getInstance().getVPN(getApplicationContext()).start();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mDrawer != null)
            mDrawer.getDrawerLayout().closeDrawer(GravityCompat.START, false);
    }

    private void renewDedicatedIP() {
        IAccount account = PIAFactory.getInstance().getAccount(this);
        String token = PiaPrefHandler.getAuthToken(this);
        List<DedicatedIPInformation> dedicatedIps = PiaPrefHandler.getDedicatedIps(this);
        for (DedicatedIPInformation dedicatedIP : dedicatedIps) {
            account.renewDedicatedIP(token, dedicatedIP.getDipToken(), requestResponseStatus -> {
                DLog.d(TAG, "Dedicated IP renewal response " + requestResponseStatus);
                return null;
            });
        }
    }

    private void onAuthFailureLogout() {
        IAccount account = PIAFactory.getInstance().getAccount(getBaseContext());
        if (!account.loggedIn()) {
            return;
        }

        DLog.d("MainActivity", "Logging out the user due to an authentication failure.");
        Toaster.l(getBaseContext(), R.string.error_invalid_auth);
        PIAFactory.getInstance().getVPN(getBaseContext()).stop();
        mLogoutAssitance = new LogoutHandler(MainActivity.this, getLogoutCallback());
        mLogoutAssitance.logoutLogic(false);
    }

    @Subscribe
    public void newLogRecieved(LogItem logItem) {
        DLog.d("PIA", logItem.getString(this));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onConnectionAttemptsExhausted(ConnectionAttemptsExhaustedEvent event) {
        new AlertDialog.Builder(this).setTitle(R.string.connection_couldnt_establish_title)
                .setMessage(R.string.connection_couldnt_establish_message)
                .setPositiveButton(R.string.send_feedback, (dialog, which) -> {
                    Intent i = new Intent(this, WebviewActivity.class);
                    i.putExtra(WebviewActivity.EXTRA_URL, "https://www.privateinternetaccess.com/helpdesk/new-ticket/");
                    startActivity(i);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAuthFailureEvent(PIAAuthenticator.PIAAuthenticatorFailureEvent event) {
        onAuthFailureLogout();
    }

    private void checkKillswitch() {
        if (PiaPrefHandler.isKillswitchEnabled(this)) {
            Prefs.with(this).set(PiaPrefHandler.KILLSWITCH, false);

            IVPN vpn = PIAFactory.getInstance().getVPN(this);
            if(vpn.isKillswitchActive()){
                vpn.stopKillswitch();
            }

            AlertDialog.Builder ab = new AlertDialog.Builder(this);
            ab.setTitle(R.string.menu_update);
            ab.setMessage(R.string.ks_removal_warning);
            ab.setPositiveButton(R.string.ok, (dialogInterface, i) -> dialogInterface.dismiss());
            ab.show();
        }
    }
}
