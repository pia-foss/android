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

package com.privateinternetaccess.android.ui.drawer.settings;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.privateinternetaccess.android.R;
import com.privateinternetaccess.android.pia.PIAFactory;
import com.privateinternetaccess.android.pia.handlers.PIAServerHandler;
import com.privateinternetaccess.android.pia.handlers.PiaPrefHandler;
import com.privateinternetaccess.android.pia.interfaces.IVPN;
import com.privateinternetaccess.android.pia.model.events.VpnStateEvent;
import com.privateinternetaccess.android.pia.utils.DLog;
import com.privateinternetaccess.android.pia.utils.Prefs;
import com.privateinternetaccess.android.pia.utils.Toaster;
import com.privateinternetaccess.android.ui.superclasses.BaseActivity;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import de.blinkt.openvpn.core.OpenVPNService;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.privateinternetaccess.android.pia.model.enums.RequestResponseStatus.SUCCEEDED;

/**
 * Created by half47 on 11/22/16.
 */

public class DeveloperActivity extends BaseActivity {

    public static final String PREF_DEBUG_MODE = "developer_mode3";
    public static final String PREF_DEVELOPER_CONFIGURATION = "developer_configuration";
    public static final String PREF_DEBUG_LEVEL = "debug_level";

    @BindView(R.id.developer_clear) Button bClear;
    @BindView(R.id.developer_save) Button bSave;

    @BindView(R.id.developer_text) EditText tvText;

    @BindView(R.id.developer_debug_mode_switch) Switch sDebugMode;
    @BindView(R.id.developer_debug_mode) View aDebug;

    @BindView(R.id.developer_vpn_start) Button bStart;
    @BindView(R.id.developer_vpn_status)  TextView tvConnectionStatus;
    @BindView(R.id.developer_vpn_pause)  Button bPause;
    @BindView(R.id.developer_vpn_resume)  Button bResume;
    @BindView(R.id.developer_vpn_stop)  Button bStop;
    @BindView(R.id.developer_vpn_stop_without_user)  Button bBomb;
    @BindView(R.id.developer_vpn_stop_killswitch)  Button bKillswitch;

    @BindView(R.id.developer_testing_purchasing_switch) Switch sPurchasingTesting;
    @BindView(R.id.developer_testing_purchasing) View aPurchaseTesting;

    @BindView(R.id.developer_testing_purchasing_area) View aPurchasing;
    @BindView(R.id.developer_testing_purchasing_et_status) EditText etStatus;
    @BindView(R.id.developer_testing_purchasing_et_username) EditText etUsername;
    @BindView(R.id.developer_testing_purchasing_et_password) EditText etPassword;
    @BindView(R.id.developer_testing_purchasing_et_exception) EditText etException;

    @BindView(R.id.developer_testing_trial_switch) Switch sTrialTesting;
    @BindView(R.id.developer_testing_trial) View aTrialTesting;

    @BindView(R.id.developer_testing_trial_area) View aTrial;
    @BindView(R.id.developer_testing_trial_et_status) EditText etTrialStatus;
    @BindView(R.id.developer_testing_trial_et_message) EditText etTrialMessage;
    @BindView(R.id.developer_testing_trial_et_username) EditText etTrialUsername;
    @BindView(R.id.developer_testing_trial_et_password) EditText etTrialPassword;

    @BindView(R.id.developer_testing_webview_area) View aWebviewTesting;
    @BindView(R.id.developer_testing_webview_input_area) View aWebviewTestingInput;
    @BindView(R.id.developer_testing_webview_switch) Switch sWebViewTesting;

    @BindView(R.id.developer_testing_webview_site) EditText etWebviewUrl;

    @BindView(R.id.developer_theme_toggle) Switch sTheme;

    @BindView(R.id.developer_testing_updater_area) View aUpdater;
    @BindView(R.id.developer_testing_updater_input_area) View aUpdaterTestingInput;
    @BindView(R.id.developer_testing_updater_switch) Switch sUpdaterSwitch;
    @BindView(R.id.developer_testing_region_offline_random_switch) Switch sRegionOfflineSwitch;
    @BindView(R.id.developer_testing_region_initial_conn_success_random_switch) Switch sRegionInitialConnSuccessSwitch;
    @BindView(R.id.developer_testing_update_show_dialog) EditText etUpdaterShowDialog;
    @BindView(R.id.developer_testing_update_show_notification) EditText etUpdaterShowNotification;
    @BindView(R.id.developer_testing_update_interval) EditText etUpdaterInterval;
    @BindView(R.id.developer_testing_update_build_version) EditText etUpdaterBuildVersion;
    @BindView(R.id.developer_testing_updater_reset_button) TextView tvUpdaterReset;

    @BindView(R.id.developer_testing_flags_area) View aFlagTesting;
    @BindView(R.id.developer_testing_flags_input_area) View aFlagTestingInput;
    @BindView(R.id.developer_testing_flags_input) EditText etFlagTesting;
    @BindView(R.id.developer_testing_flags_icon) ImageView ivFlagTesting;

    @BindView(R.id.developer_delete_log_file) Button bDeleteFile;
    @BindView(R.id.developer_print_file) Button bPrintFile;

    @BindView(R.id.developer_clear_cache) View aClearCache;

    @BindView(R.id.developer_text_file) TextView tvLog;

    @BindView(R.id.developer_print_file_progress) View progressPrint;
    @BindView(R.id.developer_delete_log_progress) View progressDelete;

    @BindView(R.id.developer_log_level) TextView tvLogLevel;

    @BindView(R.id.developer_debug_log_button) TextView tvSendDebugLog;

    @BindView(R.id.developer_clear_app_timers) View aClearTimers;

    @BindView(R.id.developer_token) TextView tvToken;

    private final String[] levels = new String[]{"Info","Debug","Warning","Exception"};
    @BindView(R.id.developer_use_staging) Switch sStaging;
    @BindView(R.id.staging_servers) View aStaging;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_developer);
        ButterKnife.bind(this);
        initHeader(true, true);
        setBackground();
        bindView(savedInstanceState);
    }

    private void bindView(Bundle savedInstanceState) {
        if(savedInstanceState != null) {
            String saved = savedInstanceState.getString(PREF_DEVELOPER_CONFIGURATION, null);
            if (TextUtils.isEmpty(saved))
                tvText.setText(Prefs.with(getApplicationContext()).getString(PREF_DEVELOPER_CONFIGURATION));
            else
                tvText.setText(saved);
        } else {
            tvText.setText(Prefs.with(getApplicationContext()).getString(PREF_DEVELOPER_CONFIGURATION));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        initView();
    }

    private void initView() {
        setLogLevel();

        setupDebug();

        setupPurchaseTesting();

        setupStagingViews();

        setupOnClick();

        setupWebViewTesting();

        setupTrialTesting();

        setupThemeTesting();

        setupSendDebugLog();

        setupRegionOfflineRandomTesting();

        setupRegionInitialConnectionSuccessRandomTesting();

        setupUpdaterTesting();

        setupVPNControls();

        tvToken.setText(PiaPrefHandler.getAuthToken(this));

        VpnStateEvent event = EventBus.getDefault().getStickyEvent(VpnStateEvent.class);
        tvConnectionStatus.setText(event.getLocalizedResId());

        setupFlagTesting();

        aClearCache.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    // clearing app data
                    if (Build.VERSION_CODES.KITKAT <= Build.VERSION.SDK_INT) {
                        ((ActivityManager)getSystemService(ACTIVITY_SERVICE)).clearApplicationUserData(); // note: it has a return value!
                    } else {
                        String packageName = getApplicationContext().getPackageName();
                        Runtime runtime = Runtime.getRuntime();
                        runtime.exec("pm clear "+packageName);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        aClearTimers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Prefs prefs = Prefs.with(getApplicationContext());
                prefs.remove(PIAServerHandler.LAST_SERVER_GRAB);
                prefs.remove(PIAServerHandler.GEN4_LAST_SERVER_BODY);
            }
        });

        bSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!TextUtils.isEmpty(tvText.getText().toString())) {
                    Prefs.with(view.getContext()).set(PREF_DEVELOPER_CONFIGURATION, tvText.getText().toString().trim());
                    Toaster.s(view.getContext(), R.string.saved);
                    onBackPressed();
                }
            }
        });
        bClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Prefs.with(view.getContext()).remove(PREF_DEVELOPER_CONFIGURATION);
                tvText.setText("");
            }
        });
    }

    private void setupRegionOfflineRandomTesting() {
        sRegionOfflineSwitch.setChecked(PiaPrefHandler.getRegionOfflineRandomizerTesting(getApplicationContext()));
        sRegionOfflineSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                PiaPrefHandler.setRegionOfflineRandomizerTesting(getApplicationContext(), b);
            }
        });
    }

    private void setupRegionInitialConnectionSuccessRandomTesting() {
        sRegionInitialConnSuccessSwitch.setChecked(PiaPrefHandler.getRegionInitialConnectionRandomizerTesting(getApplicationContext()));
        sRegionInitialConnSuccessSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                PiaPrefHandler.setRegionInitialConnectionRandomizerTesting(getApplicationContext(), b);
            }
        });
    }

    private void setupUpdaterTesting() {
        final Prefs prefs = Prefs.with(getApplicationContext());
        boolean updaterTesting = PiaPrefHandler.getUpdaterTesting(getApplicationContext());
        aUpdaterTestingInput.setVisibility(updaterTesting ? View.VISIBLE : View.GONE);
        sUpdaterSwitch.setChecked(updaterTesting);

        aUpdater.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean updaterTesting = PiaPrefHandler.getUpdaterTesting(getApplicationContext());
                updaterTesting = !updaterTesting;
                sUpdaterSwitch.setChecked(updaterTesting);
                aUpdaterTestingInput.setVisibility(updaterTesting ? View.VISIBLE : View.GONE);
                PiaPrefHandler.setUpdaterTesting(getApplicationContext(), updaterTesting);
            }
        });

        tvUpdaterReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                prefs.remove("last_update_version");
            }
        });

        etUpdaterInterval.setText(Long.toString(prefs.get(PiaPrefHandler.TESTING_UPDATER_INTERVAL, 0L)));
        etUpdaterBuildVersion.setText(Integer.toString(prefs.get(PiaPrefHandler.TESTING_UPDATER_BUILD, 0)));
        boolean showDialog = prefs.get(PiaPrefHandler.TESTING_UPDATER_SHOW_DIALOG, false);
        etUpdaterShowDialog.setText(showDialog ? "1" : "0");
        boolean showNotification = prefs.get(PiaPrefHandler.TESTING_UPDATER_SHOW_NOTIFICATION, false);
        etUpdaterShowNotification.setText(showNotification ? "1" : "0");
    }

    private void setupVPNControls() {
        bStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IVPN vpn = PIAFactory.getInstance().getVPN(getApplicationContext());
                vpn.stop();
            }
        });
        bPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IVPN vpn = PIAFactory.getInstance().getVPN(getApplicationContext());
                vpn.pause();
            }
        });
        bResume.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IVPN vpn = PIAFactory.getInstance().getVPN(getApplicationContext());
                vpn.resume();
            }
        });
        bStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IVPN vpn = PIAFactory.getInstance().getVPN(getApplicationContext());
                vpn.start();
            }
        });
        bBomb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getApplicationContext(), OpenVPNService.class);
                i.setAction(OpenVPNService.DISCONNECT_VPN);
                startService(i);
            }
        });
        bKillswitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IVPN vpn = PIAFactory.getInstance().getVPN(getApplicationContext());
                vpn.stopKillswitch();
            }
        });
    }

    private void setupThemeTesting() {
        final Prefs prefs = Prefs.with(getApplicationContext());
        boolean themeTesting = prefs.get("darktheme", false);

        sTheme.setChecked(themeTesting);

        sTheme.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                prefs.set("darktheme", sTheme.isChecked());
            }
        });
    }

    private void setupFlagTesting() {
        aFlagTesting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int visibility = aFlagTestingInput.getVisibility() == View.GONE ? View.VISIBLE : View.GONE;
                aFlagTestingInput.setVisibility(visibility);
            }
        });
        etFlagTesting.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Runnable runnable = new Runnable() {
                    public void run() {
                        int flagResource = PIAServerHandler.getInstance(getApplicationContext()).getFlagResource(etFlagTesting.getText().toString());
                        ivFlagTesting.setImageResource(flagResource);
                    }
                };
                runOnUiThread(runnable);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    private void setupSendDebugLog() {
        Context context = this;
        tvSendDebugLog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PIAFactory.getInstance().getAccount(context).sendDebugReport((reportIdentifier, requestResponseStatus) -> {

                    if (reportIdentifier == null && requestResponseStatus != SUCCEEDED) {
                        Toast.makeText(context, getString(R.string.failure_sending_log, requestResponseStatus.toString()), Toast.LENGTH_LONG).show();
                        return null;
                    }

                    androidx.appcompat.app.AlertDialog.Builder ab = new androidx.appcompat.app.AlertDialog.Builder(context);
                    ab.setTitle(R.string.log_send_done_title);
                    ab.setMessage(getString(R.string.log_send_done_msg, reportIdentifier));
                    ab.setPositiveButton(getString(android.R.string.ok), null);
                    ab.create().show();
                    return null;
                });
            }
        });
    }

    @Subscribe
    public void onVpnStateReceived(VpnStateEvent event){
        tvConnectionStatus.setText(event.getLocalizedResId());
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(PREF_DEVELOPER_CONFIGURATION, tvText.getText().toString());
    }

    private void setupWebViewTesting() {
        aWebviewTesting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean webviewTesting = PiaPrefHandler.getWebviewTesting(getApplicationContext());
                webviewTesting = !webviewTesting;
                sWebViewTesting.setChecked(webviewTesting);
                aWebviewTestingInput.setVisibility(webviewTesting ? View.VISIBLE : View.GONE);
                PiaPrefHandler.setWebviewTesting(getApplicationContext(), webviewTesting);
            }
        });
        boolean webviewTesting = PiaPrefHandler.getWebviewTesting(getApplicationContext());
        sWebViewTesting.setChecked(webviewTesting);
        aWebviewTestingInput.setVisibility(webviewTesting ? View.VISIBLE : View.GONE);
        etWebviewUrl.setText(PiaPrefHandler.getWebviewTestingSite(getApplicationContext()));
    }

    private void setupOnClick() {
        bDeleteFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bDeleteFile.setVisibility(View.INVISIBLE);
                progressDelete.setVisibility(View.VISIBLE);
                Runnable runnable = new Runnable() {
                    public void run() {
                        File debugFile = new File(getFilesDir(), DLog.DEBUG_FILE_TXT);
                        final boolean deleted = debugFile.delete();
                        progressDelete.post(new Runnable() {
                            @Override
                            public void run() {
                                Toaster.s(getApplicationContext(), "File Deleted " + deleted);
                                tvLog.setText("");
                                bDeleteFile.setVisibility(View.VISIBLE);
                                progressDelete.setVisibility(View.GONE);
                            }
                        });
                    }
                };
                new Thread(runnable).start();
            }
        });

        bPrintFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bPrintFile.setVisibility(View.INVISIBLE);
                progressPrint.setVisibility(View.VISIBLE);
                DLog.i("DeveloperActivity", "PRINTING LOG");

                Runnable runnable = new Runnable() {
                    public void run() {
                        File debugFile = new File(getFilesDir(), DLog.DEBUG_FILE_TXT);

                        final String debugLogs = readFile(debugFile) + "END";
                        tvLog.post(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    tvLog.setText(debugLogs.replaceAll(DLog.LINE_SEPERATOR, "\n"));
                                } catch (Exception e) {
                                    tvLog.setText(String.format("Text printing failed %1$s", e.getMessage()));
                                }
                                bPrintFile.setVisibility(View.VISIBLE);
                                progressPrint.setVisibility(View.GONE);
                            }
                        });

                    }
                };
                new Thread(runnable).start();
            }
        });
    }

    private void setupDebug() {
        boolean debug = Prefs.with(getApplicationContext()).getBoolean(PREF_DEBUG_MODE);
        sDebugMode.setChecked(debug);

        aDebug.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean debug = Prefs.with(getApplicationContext()).getBoolean(PREF_DEBUG_MODE);
                DLog.i("DeveloperActivity", DLog.DEBUG_MODE ? "Debug Activated" : "Debug Deactivated");
                DLog.DEBUG_MODE = !debug;
                DLog.i("DeveloperActivity", DLog.DEBUG_MODE ? "Debug Activated" : "Debug Deactivated");
                sDebugMode.setChecked(DLog.DEBUG_MODE);
                Prefs.with(getApplicationContext()).set(PREF_DEBUG_MODE, DLog.DEBUG_MODE);
            }
        });
    }

    private void setupStagingViews()
    {
        boolean staging = PiaPrefHandler.useStaging(getApplicationContext());
        sStaging.setChecked(staging);

        aStaging.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean newStaging = !PiaPrefHandler.useStaging(getApplicationContext());
                sStaging.setChecked(newStaging);
                PiaPrefHandler.setUseStaging(getApplicationContext(), newStaging);
            }
        });

    }

    private void setupPurchaseTesting(){
        boolean testing = PiaPrefHandler.isPurchasingTesting(getApplicationContext());
        sPurchasingTesting.setChecked(testing);

        aPurchaseTesting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean testing = PiaPrefHandler.isPurchasingTesting(getApplicationContext());
                testing = !testing;
                sPurchasingTesting.setChecked(testing);
                PiaPrefHandler.setPurchaseTesting(getApplicationContext(), testing);
                aPurchasing.setVisibility(testing ? View.VISIBLE : View.GONE);
            }
        });

        aPurchasing.setVisibility(testing ? View.VISIBLE : View.GONE);

        etStatus.setText(Prefs.with(getApplicationContext()).getInt(PiaPrefHandler.PURCHASING_TESTING_STATUS) + "");
        etUsername.setText(Prefs.with(getApplicationContext()).getString(PiaPrefHandler.PURCHASING_TESTING_USERNAME));
        etPassword.setText(Prefs.with(getApplicationContext()).getString(PiaPrefHandler.PURCHASING_TESTING_PASSWORD));
        etException.setText(Prefs.with(getApplicationContext()).getString(PiaPrefHandler.PURCHASING_TESTING_EXCEPTION));
    }

    private void setupTrialTesting(){
        boolean testing = PiaPrefHandler.isTrialTesting(getApplicationContext());
        sTrialTesting.setChecked(testing);

        aTrialTesting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean testing = PiaPrefHandler.isTrialTesting(getApplicationContext());
                testing = !testing;
                sTrialTesting.setChecked(testing);
                PiaPrefHandler.setTrialTesting(getApplicationContext(), testing);
                aTrial.setVisibility(testing ? View.VISIBLE : View.GONE);
            }
        });

        aTrial.setVisibility(testing ? View.VISIBLE : View.GONE);

        Prefs prefs = Prefs.with(getApplicationContext());
        etTrialStatus.setText(prefs.getInt(PiaPrefHandler.TRIAL_TESTING_STATUS) + "");
        etTrialMessage.setText(prefs.getString(PiaPrefHandler.TRIAL_TESTING_MESSAGE));
        etTrialUsername.setText(prefs.getString(PiaPrefHandler.TRIAL_TESTING_USERNAME));
        etTrialPassword.setText(prefs.getString(PiaPrefHandler.TRIAL_TESTING_PASSWORD));
    }

    @Override
    protected void onPause() {
        super.onPause();
        Prefs prefs = new Prefs(getApplicationContext());
        String status = etStatus.getText().toString();
        int statusNum = 0;
        try {
            statusNum = Integer.parseInt(status);
        } catch (NumberFormatException e) {
        }
        prefs.set(PiaPrefHandler.PURCHASING_TESTING_STATUS, statusNum);
        prefs.set(PiaPrefHandler.PURCHASING_TESTING_USERNAME, etUsername.getText().toString());
        prefs.set(PiaPrefHandler.PURCHASING_TESTING_PASSWORD, etPassword.getText().toString());
        prefs.set(PiaPrefHandler.PURCHASING_TESTING_EXCEPTION, etException.getText().toString());
        PiaPrefHandler.setUseStaging(this, sStaging.isChecked());

        // updater testing
        if (etUpdaterInterval.getText().toString().length() > 0)
            prefs.set(PiaPrefHandler.TESTING_UPDATER_INTERVAL, Long.parseLong(etUpdaterInterval.getText().toString()));
        if (etUpdaterBuildVersion.getText().toString().length() > 0)
            prefs.set(PiaPrefHandler.TESTING_UPDATER_BUILD, Integer.parseInt(etUpdaterBuildVersion.getText().toString()));

        prefs.set(PiaPrefHandler.TESTING_UPDATER_SHOW_NOTIFICATION, etUpdaterShowNotification.getText().toString().equals("1"));
        prefs.set(PiaPrefHandler.TESTING_UPDATER_SHOW_DIALOG, etUpdaterShowDialog.getText().toString().equals("1"));

        // web view testing
        prefs.set(PiaPrefHandler.TESTING_WEBVIEW_SITE, etWebviewUrl.getText().toString());

        String trialStatus = etTrialStatus.getText().toString();
        int trialStatusNum = 0;
        try {
            trialStatusNum = Integer.parseInt(trialStatus);
        } catch (NumberFormatException e) {
        }
        prefs.set(PiaPrefHandler.TRIAL_TESTING_MESSAGE, etTrialMessage.getText().toString());
        prefs.set(PiaPrefHandler.TRIAL_TESTING_STATUS, trialStatusNum);
        prefs.set(PiaPrefHandler.TRIAL_TESTING_USERNAME, etTrialUsername.getText().toString());
        prefs.set(PiaPrefHandler.TRIAL_TESTING_PASSWORD, etTrialPassword.getText().toString());
    }

    private void setLogLevel() {
        final int level = Prefs.with(getApplicationContext()).get(PREF_DEBUG_LEVEL, 1);
        tvLogLevel.setText(String.format("Log Level:\n%1$s", getLogLevel(level)));

        tvLogLevel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Context context = DeveloperActivity.this;
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("Choose Level:");
                DLog.d("Developer", "Levels = " + levels.length);
                ArrayAdapter<String> adapter = new ArrayAdapter<>(context, R.layout.list_textview, R.id.text, levels);
                builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            String choice = levels[which];
                            Prefs.with(getApplicationContext()).set(PREF_DEBUG_LEVEL, which);
                            Toaster.s(getApplicationContext(), "Level switched to " + choice);
                            DLog.i("Debug Level Changed", "Level is now " + which);
                            DLog.DEBUG_LEVEL = which;
                            tvLogLevel.setText(String.format("Log Level:\n%1$s", getLogLevel(which)));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
                builder.setPositiveButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                builder.show();
            }
        });
    }

    private String getLogLevel(int level){
        return levels[level];
    }

    private String readFile(File filename)
    {
        StringBuilder records = new StringBuilder();
        try
        {
            BufferedReader reader = new BufferedReader(new FileReader(filename));
            String line;
            while ((line = reader.readLine()) != null)
            {
                records.append(line);
            }
            reader.close();
            return records.toString();
        }
        catch (Exception e)
        {
            System.err.format("Exception occurred trying to read '%s'.", filename);
            e.printStackTrace();
            return e.getLocalizedMessage();
        }
    }

}