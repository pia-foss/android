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

package com.privateinternetaccess.android.ui.drawer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;
import android.text.TextUtils;
import android.widget.Toast;

import com.privateinternetaccess.android.BuildConfig;
import com.privateinternetaccess.android.PIAApplication;
import com.privateinternetaccess.android.R;
import com.privateinternetaccess.android.pia.PIAFactory;
import com.privateinternetaccess.android.pia.handlers.PIAServerHandler;
import com.privateinternetaccess.android.pia.handlers.PiaPrefHandler;
import com.privateinternetaccess.android.pia.handlers.ThemeHandler;
import com.privateinternetaccess.android.pia.interfaces.IVPN;
import com.privateinternetaccess.android.pia.utils.DLog;
import com.privateinternetaccess.android.pia.utils.Prefs;
import com.privateinternetaccess.android.pia.utils.Toaster;
import com.privateinternetaccess.android.ui.LauncherActivity;
import com.privateinternetaccess.android.ui.connection.CallingCardActivity;
import com.privateinternetaccess.android.ui.drawer.settings.DeveloperActivity;
import com.privateinternetaccess.android.ui.features.WebviewActivity;
import com.privateinternetaccess.android.ui.widgets.WidgetBaseProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import me.philio.preferencecompatextended.PreferenceFragmentCompat;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static com.privateinternetaccess.android.pia.handlers.PiaPrefHandler.GEO_SERVERS_ACTIVE;
import static com.privateinternetaccess.android.pia.model.enums.RequestResponseStatus.SUCCEEDED;
import static com.privateinternetaccess.android.pia.vpn.PiaOvpnConfig.DEFAULT_AUTH;
import static com.privateinternetaccess.android.pia.vpn.PiaOvpnConfig.DEFAULT_CIPHER;
import static com.privateinternetaccess.android.pia.vpn.PiaOvpnConfig.DEFAULT_HANDSHAKE;

/**
 * Created by half47 on 8/3/16.
 */
public class SettingsFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceChangeListener,
        SharedPreferences.OnSharedPreferenceChangeListener
{

    private SwitchPreferenceCompat pTCP;
    private PreferenceScreen pRemotePort;

    private String[] ovpnKeys = {
            "useTCP", "rport", "lport", "mssfix", "proxy_settings", "ovpn_cipher_warning",
            "useproxy", "blockipv6", "encryption", "cipher", "auth", "killswitch", "tlscipher"};
    private String[] wgKeys = {};

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootkey) {
        setPreferencesFromResource(R.xml.fragment_preference, rootkey);
        Prefs prefs = new Prefs(getContext());

        for (String prefname : new String[]{"rport", "lport", "proxyport"}) {
            Preference pref = findPreference(prefname);
            pref.setOnPreferenceChangeListener(this);
        }

        Preference cipher = findPreference("cipher");
        cipher.setSummary(SettingsFragmentHandler.getSummaryItem(getActivity(), "cipher",
                DEFAULT_CIPHER, getResources().getStringArray(R.array.cipher_list),
                getResources().getStringArray(R.array.ciphers_values)));

        Preference auth = findPreference("auth");
        auth.setSummary(SettingsFragmentHandler.getSummaryItem(getActivity(), "auth",
                DEFAULT_AUTH, getResources().getStringArray(R.array.auth_list),
                getResources().getStringArray(R.array.auth_values)));

        Preference tlsCipher = findPreference("tlscipher");
        tlsCipher.setSummary(SettingsFragmentHandler.getSummaryItem(getActivity(), "tlscipher",
                DEFAULT_HANDSHAKE, getResources().getStringArray(R.array.tls_cipher),
                getResources().getStringArray(R.array.tls_values)));

        setLportSummary(prefs.get("lport", "auto"));
        findPreference(PiaPrefHandler.PROXY_PORT).setSummary(
                prefs.get(PiaPrefHandler.PROXY_PORT, "8080")
        );

        pTCP = (SwitchPreferenceCompat) findPreference("useTCP");
        pTCP.setOnPreferenceChangeListener(this);

        pRemotePort = (PreferenceScreen) findPreference("rport");
        setRportSummary(prefs.get(PiaPrefHandler.RPORT, "auto"));

        // Always On VPN is only available after Oreo
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            PreferenceCategory category = (PreferenceCategory) findPreference("blocking");
            Preference alwaysOn = findPreference("oreoalwayson");
            if(alwaysOn != null)
                category.removePreference(alwaysOn);
        } else {
            setAlwaysOnClickListener(findPreference("oreoalwayson"));
        }

        updateSettingsUiWithKnownPersistedData();
    }

    private void setAlwaysOnClickListener(Preference preference) {
        preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new AlertDialog.Builder(getContext()).setTitle(R.string.block_connection_wo_vpn)
                        .setMessage(R.string.always_oreo_message)
                        .setPositiveButton(R.string.open_android_settings, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, null).show();
                return true;
            }
        });
    }


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setDividerHeight(0);
    }

    @Override
    public void onResume() {
        super.onResume();

        ViewCompat.setNestedScrollingEnabled(getListView(), false);

        PreferenceManager.getDefaultSharedPreferences(pRemotePort.getContext()).registerOnSharedPreferenceChangeListener(this);

        setUpVersionCode();

        setupOnClicks();

        setupPortDialogs();

        setupDialogs();

        setupBlockLan();

        setupProxyArea();

        setDNSSummary();

        setProtocolSummary();

        setAutomation();

        handleAndroidTVRemovals();

        handleDevMode();
    }

    private void setupProxyArea() {
        Context context = getActivity();
        Prefs prefs = new Prefs(context);
        final SwitchPreferenceCompat proxyChoice = (SwitchPreferenceCompat) findPreference(PiaPrefHandler.PROXY_ENABLED);
        proxyChoice.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                boolean proxy = (boolean) newValue;
                if(proxy){
                    Prefs prefs = new Prefs(preference.getContext());
                    String app = prefs.get(PiaPrefHandler.PROXY_APP, "");
                    if(!TextUtils.isEmpty(app)){
                        Set<String> excludedApps = prefs.getStringSet(PiaPrefHandler.VPN_PER_APP_PACKAGES);
                        excludedApps.add(app);
                        prefs.set(PiaPrefHandler.VPN_PER_APP_PACKAGES, excludedApps);
                        toggleProxyArea(proxy);
                    } else {
                        Activity act = getActivity();
                        AlertDialog.Builder builder = new AlertDialog.Builder(act);
                        builder.setTitle(R.string.enable_proxy_dialog_title);
                        builder.setMessage(R.string.enable_proxy_dialog_message);
                        builder.setCancelable(false);
                        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                // take the user to the app selection area
                                Intent i = new Intent(getActivity(), AllowedAppsActivity.class);
                                i.putExtra(AllowedAppsActivity.EXTRA_SELECT_APP, true);
                                startActivity(i);
                            }
                        });
                        builder.setNegativeButton(R.string.dismiss, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ((SwitchPreferenceCompat) findPreference(PiaPrefHandler.PROXY_ENABLED)).setChecked(false);
                                dialog.dismiss();
                            }
                        });
                        builder.show();
                    }
                } else {
                    resetProxyArea(preference, proxy);
                }
                return true;
            }
        });

        boolean useProxy = prefs.get(PiaPrefHandler.PROXY_ENABLED, false);
        Preference proxyApp = findPreference(PiaPrefHandler.PROXY_APP);
        String app = prefs.get(PiaPrefHandler.PROXY_APP, "");
        if(!TextUtils.isEmpty(app) && useProxy){
            proxyApp.setSummary(app);
            if(app.equals(AllowedAppsActivity.ORBOT)){
                boolean useUDP = prefs.get(PiaPrefHandler.USE_TCP, false);
                if(!useUDP){
                    showOrbotDialog(context);
                }
            }
        } else {
            useProxy = false;
            ((SwitchPreferenceCompat) findPreference(PiaPrefHandler.PROXY_ENABLED)).setChecked(false);
        }

        Preference proxyPort = findPreference(PiaPrefHandler.PROXY_PORT);
        SettingsFragmentHandler.setupProxyPortDialog(getActivity(), proxyPort);

        toggleProxyArea(useProxy);

        proxyApp.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent i = new Intent(preference.getContext(), AllowedAppsActivity.class);
                i.putExtra(AllowedAppsActivity.EXTRA_SELECT_APP, true);
                startActivity(i);
                return false;
            }
        });
    }

    private void showOrbotDialog(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.settings_orbot_udp_problem_title);
        builder.setMessage(R.string.settings_orbot_udp_problem_message);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ((SwitchPreferenceCompat) findPreference(PiaPrefHandler.USE_TCP)).setChecked(true);
                dialog.dismiss();
            }
        });
        builder.setNegativeButton(R.string.dismiss, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.show();
    }

    public void resetProxyArea(Preference preference, boolean proxy) {
        Prefs prefs = new Prefs(preference.getContext());
        String app = prefs.get(PiaPrefHandler.PROXY_APP, "");
        if(!TextUtils.isEmpty(app)){
            Set<String> excludedApps = prefs.getStringSet(PiaPrefHandler.VPN_PER_APP_PACKAGES);
            excludedApps.remove(app);
            prefs.set(PiaPrefHandler.VPN_PER_APP_PACKAGES, excludedApps);
        }

        toggleProxyArea(proxy);
    }

    private void toggleProxyArea(boolean useProxy){
        Preference proxyApp = findPreference(PiaPrefHandler.PROXY_APP);
        Preference proxyPort = findPreference(PiaPrefHandler.PROXY_PORT);
        if(useProxy){
            proxyApp.setVisible(true);
            proxyPort.setVisible(true);
        } else {
            proxyApp.setVisible(false);
            proxyPort.setVisible(false);
        }
    }

    private void handleDevMode() {
        if(PIAApplication.isRelease()){
            PreferenceCategory mAppSettingsPref = (PreferenceCategory) findPreference("app_settings");
            PreferenceScreen mCategory = (PreferenceScreen) findPreference("developer_mode");
            if(mAppSettingsPref != null && mCategory != null)
                mAppSettingsPref.removePreference(mCategory);
        }
    }

    private void handleAndroidTVRemovals() {
        Preference pref = findPreference("widgetConfiguration");
        Preference viewVPN = findPreference("vpn_log");
        Preference secureWifi = findPreference("networkManagement");
        Preference alwaysOn = findPreference("oreoalwayson");
        Preference viewUpdates = findPreference("updates");

        if(!PIAApplication.isAndroidTV(pTCP.getContext())){
            pref.setOnPreferenceClickListener(preference -> {
                getActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.activity_secondary_container, new WidgetSettingsFragment())
                        .addToBackStack("WIDGETSSSSS")
                        .commit();
                return true;
            });
            viewVPN.setOnPreferenceClickListener(preference -> {
                Intent i = new Intent(getActivity(), VpnLogActivity.class);
                startActivity(i);
                return false;
            });
            viewUpdates.setOnPreferenceClickListener(preference -> {
                Intent i = new Intent(getActivity(), CallingCardActivity.class);
                i.putExtra(CallingCardActivity.SHOW_CTA1, false);
                startActivity(i);
                return false;
            });
        } else {
            PreferenceCategory category = findPreference("app_settings");
            Preference haptic = findPreference(PiaPrefHandler.HAPTIC_FEEDBACK);
            Preference theme = findPreference("darktheme");
            Preference inapp = findPreference(PiaPrefHandler.HIDE_INAPP_MESSAGES);

            if(pref != null && category != null && haptic != null && theme != null) {
                category.removePreference(pref);
                category.removePreference(haptic);
                category.removePreference(theme);

                if (viewUpdates != null) {
                    category.removePreference(viewUpdates);
                }

                if (viewVPN != null) {
                    category.removePreference(viewVPN);
                }

                if (inapp != null) {
                    category.removePreference(inapp);
                }
            }
            PreferenceCategory info = findPreference("app_info_cat");
            Preference recentChanges = findPreference("update_and_patch_notes");
            if(recentChanges != null){
                info.removePreference(recentChanges);
            }

            PreferenceCategory blocking = findPreference("blocking");
            if (alwaysOn != null && blocking != null) {
                blocking.removePreference(alwaysOn);
            }

            PreferenceScreen screen = getPreferenceScreen();
            PreferenceCategory proxy = findPreference("proxy_settings");

            if (proxy != null) {
                screen.removePreference(proxy);
            }

            Preference mace = findPreference("pia_mace");
            Preference killswitch = findPreference("killswitch");

            if (mace != null) {
                blocking.removePreference(mace);
            }

            if (killswitch != null) {
                blocking.removePreference(killswitch);
            }

            PreferenceCategory connectionCat = findPreference("connection_setting");
            if (secureWifi != null)
                category.removePreference(secureWifi);
        }

        if(BuildConfig.FLAVOR_store.equals("playstore")){
            PreferenceCategory blocking = (PreferenceCategory)findPreference("blocking");
            Preference mace = findPreference("pia_mace");
            if(blocking != null && mace != null)
                blocking.removePreference(mace);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        PreferenceManager.getDefaultSharedPreferences(pRemotePort.getContext()).unregisterOnSharedPreferenceChangeListener(this);
    }

    private void setupBlockLan() {
        Preference pref = findPreference(PiaPrefHandler.BLOCK_LOCAL_LAN);
        pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                                              @Override
                                              public boolean onPreferenceClick(Preference preference) {
                                                  Prefs prefs = Prefs.with(preference.getContext());
                                                  boolean blockLan = prefs.get(PiaPrefHandler.BLOCK_LOCAL_LAN, true);
                                                  if(!blockLan) {
                                                      Context context = getActivity();
                                                      AlertDialog.Builder builder = new AlertDialog.Builder(context);
                                                      builder.setTitle(R.string.pref_block_dialog_title);
                                                      builder.setMessage(R.string.pref_block_dialog_message);
                                                      builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                                          @Override
                                                          public void onClick(DialogInterface dialog, int which) {
                                                              dialog.dismiss();
                                                          }
                                                      });
                                                      builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                                          @Override
                                                          public void onClick(DialogInterface dialog, int which) {
                                                              revert(dialog);
                                                          }
                                                      });
                                                      builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                                                          @Override
                                                          public void onCancel(DialogInterface dialog) {
                                                              revert(dialog);
                                                          }
                                                      });
                                                      builder.show();
                                                  }
                                                  return true;
                                              }
                                              private void revert(DialogInterface dialog) {
                                                  boolean blockLan = PiaPrefHandler.getBlockLocal(getActivity());
                                                  blockLan = !blockLan;
                                                  Prefs.with(getActivity()).set(PiaPrefHandler.BLOCK_LOCAL_LAN, blockLan);
                                                  PiaPrefHandler.setBlockLocal(getActivity(), blockLan);
                                                  ((SwitchPreferenceCompat) findPreference(PiaPrefHandler.BLOCK_LOCAL_LAN)).setChecked(blockLan);
                                                  dialog.dismiss();
                                              }
                                          }
        );
    }

    private void setupDialogs() {
        SettingsFragmentHandler.setupOtherDialogs(getActivity(),
                findPreference("cipher"), findPreference("auth"), findPreference("tlscipher"));
    }

    private void setupPortDialogs() {
        SettingsFragmentHandler.setupPortDialogs(getActivity(), this,
                findPreference("lport"), findPreference("rport"),
                pTCP);
    }

    private void setupKillswitch() {
        Preference p = findPreference("killswitch");
        Preference pConnectBoot = findPreference("autostart");

        if (pConnectBoot != null && !PIAApplication.isAndroidTV(pTCP.getContext())) {
            pConnectBoot.setSummary(R.string.preference_on_boot_always_on);
        }
    }

    // did this to fix up reusability of the xml file so you don't have to change it so much.
    private void setupOnClicks() {
        Preference p = findPreference("developer_mode");
        if(p != null)
            p.setOnPreferenceClickListener(preference -> {
                Intent i = new Intent(getActivity(), DeveloperActivity.class);
                startActivity(i);
                getActivity().overridePendingTransition(R.anim.left_to_right, R.anim.right_to_left);
                return true;
            });

        Preference ovpnWarning = findPreference("ovpn_cipher_warning");

        if (ovpnWarning != null) {
            ovpnWarning.setOnPreferenceClickListener(preference -> {
                Intent i = new Intent(getContext(), WebviewActivity.class);
                i.putExtra(WebviewActivity.EXTRA_URL, "https://www.privateinternetaccess.com/helpdesk/kb/articles/removing-openvpn-handshake-and-authentication-settings/");
                getContext().startActivity(i);

                return true;
            });
        }

        findPreference("send_log").setOnPreferenceClickListener(preference -> {
            Toaster.s(getActivity(), R.string.sending_debug_log);
            preference.setEnabled(false);
            sendDebugLog();
            return true;
        });

        findPreference("resetToDefault").setOnPreferenceClickListener(preference -> {
            Context ctx = getActivity();
            AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
            builder.setTitle(R.string.pref_reset_settings);
            builder.setMessage(R.string.pref_reset_settings_message);
            builder.setPositiveButton(R.string.reset, (dialogInterface, i) -> {
                SettingsFragmentHandler.resetToDefault(getActivity());
                updateSettingsUiWithKnownPersistedData();
                if (!PIAFactory.getInstance().getVPN(ctx).isVPNActive()) {
                    Toaster.s(ctx, ctx.getString(R.string.settings_reset));
                }
                WidgetBaseProvider.updateWidget(ctx, false);
                dialogInterface.dismiss();
            });
            builder.setNegativeButton(R.string.dismiss, (dialogInterface, i) -> dialogInterface.dismiss());
            builder.show();
            return true;
        });
    }

    private void setAutomation() {
        Preference automation = findPreference("networkAutomation");
        if (Prefs.with(getContext()).getBoolean("networkManagement")) {
            automation.setVisible(true);
            automation.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent i = new Intent(getActivity(), TrustedWifiActivity.class);
                    startActivity(i);
                    return false;
                }
            });
        }
        else {
            automation.setVisible(false);
        }
    }

    private void setUpVersionCode() {
        String version = BuildConfig.VERSION_NAME;
        int versionCode = BuildConfig.VERSION_CODE;

        findPreference("version_info").setSummary("v" + version + " (" + versionCode + ")");
        findPreference("version_info").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if(BuildConfig.FLAVOR_store.equals("playstore") && !PIAApplication.isAndroidTV(getContext())) {
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse("market://details?id=com.privateinternetaccess.android"));
                    startActivity(i);
                }
                return false;
            }
        });
    }

    public void setProtocolSummary() {
        SettingsFragmentHandler.setupProtocolDialog(
                getContext(),
                findPreference(PiaPrefHandler.VPN_PROTOCOL),
                this
        );

        String[] protocols = getContext().getResources().getStringArray(R.array.protocol_options);
        String activeProtocol = Prefs.with(getContext()).get(
                PiaPrefHandler.VPN_PROTOCOL,
                protocols[0]);

        findPreference(PiaPrefHandler.VPN_PROTOCOL).setSummary(activeProtocol);

        boolean showOvpn = activeProtocol.equals(protocols[0]);

        for (String key : ovpnKeys) {
            Preference pref = findPreference(key);

            if (pref != null) {
                pref.setVisible(showOvpn);
            }
        }

        for (String key : wgKeys) {
            Preference pref = findPreference(key);

            if (pref != null) {
                pref.setVisible(!showOvpn);
            }
        }

        findPreference(PiaPrefHandler.VPN_PROTOCOL).setVisible(true);

        setupKillswitch();
    }

    public void setDNSSummary(){
        SettingsFragmentHandler.setupDNSDialog(
                getContext(),
                findPreference(PiaPrefHandler.DNS_PREF),
                this
        );

        String dnsSummary = getActivity().getString(R.string.auto_dns_setting_summary);
        String dns = Prefs.with(getActivity()).get(PiaPrefHandler.DNS, "");
        String secondaryDns = Prefs.with(getActivity()).get(PiaPrefHandler.DNS_SECONDARY, "");

        if (secondaryDns.length() > 0 && dns.length() > 0) {
            dnsSummary = getResources().getString(R.string.custom_dns) +
                    String.format(" (%s / %s)", dns, secondaryDns);
        }
        else if (dns.length() > 0) {
            dnsSummary = getResources().getString(R.string.custom_dns) +
                    String.format(" (%s)", dns);
        }

        String[] baseDns = getContext().getResources().getStringArray(R.array.dns_options);
        String[] dnsNames = getContext().getResources().getStringArray(R.array.dns_names);

        for (int i = 0; i < baseDns.length; i++) {
            if (dns.equals(baseDns[i])) {
                dnsSummary = dnsNames[i];
                break;
            }
        }

        findPreference(PiaPrefHandler.DNS_PREF).setSummary(dnsSummary);
    }


    @SuppressLint("StringFormatInvalid")
    public void setRportSummary(String newvalue) {
        if (newvalue.equals("") || newvalue.equals("auto"))
            pRemotePort.setSummary(R.string.auto_rport);
        else
            pRemotePort.setSummary(getString(R.string.rportsummary, newvalue));
    }

    @SuppressLint("StringFormatInvalid")
    public void setLportSummary(String newvalue) {
        PreferenceScreen lport = (PreferenceScreen) findPreference("lport");
        if (newvalue.equals("") || newvalue.equals("auto"))
            lport.setSummary(R.string.lportrandom);
        else
            lport.setSummary(getString(R.string.lportsummary, newvalue));
    }


    private boolean setLPort(String newvalue) {
        if (newvalue.equals(""))
            return true;
        try {
            int lport = Integer.parseInt(newvalue);
            if (lport < 1024 || lport > 65335) {
                Toaster.s(getActivity(), R.string.localport_warning);
                return false;
            } else {
                return true;
            }
        } catch (NumberFormatException nfe) {
            Toaster.s(getActivity(), R.string.notanumber_localport);
        }
        return false;
    }


    private boolean checkProxyPort(String newvalue) {
        try {
            int pport = Integer.parseInt(newvalue);
            if (pport < 1 || pport > 65335) {
                Toaster.s(getActivity(), R.string.notanumber_proxyport);
                return false;
            } else {
                return true;
            }
        } catch (NumberFormatException nfe) {
            Toaster.s(getActivity(), R.string.notanumber_localport);
        }
        return false;
    }

    @Override
    public boolean onPreferenceChange(Preference pref, Object arg1) {
        if (pref.getKey().equals(PiaPrefHandler.RPORT)) {
            setRportSummary((String) arg1);
        } else if (pref.getKey().equals(PiaPrefHandler.LPORT)) {
            if (!setLPort((String) arg1))
                return false;
            setLportSummary((String) arg1);
        } else if (pref.getKey().equals(PiaPrefHandler.USE_TCP)) {
            Prefs.with(getActivity()).set(PiaPrefHandler.RPORT, "auto");
            setRportSummary("auto");
        } else if (pref.getKey().equals(PiaPrefHandler.PROXY_PORT)) {
            if (!checkProxyPort((String) arg1))
                return false;
            pref.setSummary((String) arg1);
        }
        return true;
    }

    private void sendDebugLog() {
        Context context = getContext();
        PIAFactory.getInstance().getAccount(context).sendDebugReport((reportIdentifier, requestResponseStatus) -> {

            if (context == null) {
                DLog.d("PIASettings", "Invalid context on sendDebugReport response");
                return null;
            }

            if (reportIdentifier == null && requestResponseStatus != SUCCEEDED) {
                Toast.makeText(context, getString(R.string.failure_sending_log, requestResponseStatus.toString()), Toast.LENGTH_LONG).show();
                return null;
            }

            androidx.appcompat.app.AlertDialog.Builder ab = new androidx.appcompat.app.AlertDialog.Builder(context);
            ab.setTitle(R.string.log_send_done_title);
            ab.setMessage(getString(R.string.log_send_done_msg, reportIdentifier));
            ab.setPositiveButton(getString(android.R.string.ok), null);
            ab.create().show();
            findPreference("send_log").setEnabled(true);
            return null;
        });
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        DLog.d("PIASettings", "key = " + key);
        Activity context = getActivity();
        if (key.equals(ThemeHandler.PREF_THEME)) {
            ((SettingsActivity) context).setChangedTheme(true);
            boolean isAmazon = PIAApplication.isAmazon();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && !isAmazon) {
                ThemeHandler.setAppTheme(context.getApplication());
                context.onBackPressed();
            } else {
                triggerRebirth(getActivity());
            }
            return;
        } else if(key.equals(PiaPrefHandler.KILLSWITCH)){
            boolean killswitch = PiaPrefHandler.isKillswitchEnabled(context);
            IVPN vpn = PIAFactory.getInstance().getVPN(context);
            if(!killswitch && vpn.isKillswitchActive()){
                vpn.stopKillswitch();
            }
        } else if (key.equals(PiaPrefHandler.PIA_MACE)) {
            if (isUsingCustomDns() && sharedPreferences.getBoolean(key, false)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle(R.string.custom_dns_warning_title);
                builder.setMessage(R.string.custom_dns_mace_warning);
                builder.setCancelable(false);

                builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Prefs.with(getContext()).remove(PiaPrefHandler.DNS);
                        Prefs.with(getContext()).remove(PiaPrefHandler.DNS_SECONDARY);
                        setDNSSummary();
                    }
                });

                builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        toggleMace(false);
                    }
                });

                builder.show();
            }
        }

        setAutomation();

        boolean showMessage = true;
        List<String> dontShowList = new ArrayList<>();
        dontShowList.add(PiaPrefHandler.AUTOCONNECT);
        dontShowList.add(PiaPrefHandler.AUTOSTART);
        dontShowList.add(PiaPrefHandler.KILLSWITCH);
        dontShowList.add(PiaPrefHandler.LAST_IP);
        dontShowList.add(PiaPrefHandler.CONNECT_ON_APP_UPDATED);
        dontShowList.add(PiaPrefHandler.GEO_SERVERS_ACTIVE);
        if (dontShowList.contains(key)) {
            showMessage = false;
        } else if(key.equals(PiaPrefHandler.USE_TCP)){
            PIAServerHandler.getInstance(context).triggerLatenciesUpdate();
            Prefs prefs = Prefs.with(context);
            boolean useTCP = prefs.get(PiaPrefHandler.USE_TCP, false);
            boolean proxyEnabled = prefs.get(PiaPrefHandler.PROXY_ENABLED, false);
            String proxyApp = prefs.get(PiaPrefHandler.PROXY_APP, "");
            if(!useTCP && proxyEnabled && proxyApp.equals(AllowedAppsActivity.ORBOT)){
                showOrbotDialog(context);
            }
        }
        if (showMessage && PIAFactory.getInstance().getVPN(context).isVPNActive()) {
            Toaster.l(getActivity().getApplicationContext(), R.string.reconnect_vpn);
        }
    }

    public void toggleMace(boolean state) {
        SwitchPreferenceCompat mace = (SwitchPreferenceCompat) findPreference(PiaPrefHandler.PIA_MACE);
        Prefs.with(getContext()).set(PiaPrefHandler.PIA_MACE, state);
        mace.setChecked(Prefs.with(getContext()).get(PiaPrefHandler.PIA_MACE, false));
    }

    public static void triggerRebirth(Context context) {
        Intent intent = new Intent(context, LauncherActivity.class);
        intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
        if (context instanceof Activity) {
            ((Activity) context).finish();
        }
        Runtime.getRuntime().exit(0);
    }

    private boolean isUsingCustomDns() {
        String dns = Prefs.with(getContext()).getString(PiaPrefHandler.DNS);

        if (dns == null || dns.length() < 1) {
            return false;
        }

        String[] dnsArray = getContext().getResources().getStringArray(R.array.dns_options);

        for (String def : dnsArray) {
            if (def.equals(dns))
                return false;
        }

        return true;
    }

    private void updateSettingsUiWithKnownPersistedData() {
        Prefs prefs = new Prefs(getContext());

        // Connection Area
        SwitchPreferenceCompat useTcpPreference = findPreference(PiaPrefHandler.USE_TCP);
        if (useTcpPreference != null) {
            useTcpPreference.setChecked(prefs.get(PiaPrefHandler.USE_TCP, false));
        }

        SwitchPreferenceCompat portForwardingPreference =
                findPreference(PiaPrefHandler.PORTFORWARDING);
        if (portForwardingPreference != null) {
            portForwardingPreference.setChecked(prefs.get(PiaPrefHandler.PORTFORWARDING, false));
        }

        setRportSummary(prefs.get(PiaPrefHandler.RPORT, ""));
        setLportSummary(prefs.get(PiaPrefHandler.LPORT, ""));

        SwitchPreferenceCompat packetSizePreference = findPreference(PiaPrefHandler.PACKET_SIZE);
        if (packetSizePreference != null) {
            packetSizePreference.setChecked(
                    prefs.get(PiaPrefHandler.PACKET_SIZE, getResources().getBoolean(R.bool.usemssfix))
            );
        }

        SwitchPreferenceCompat geoServersActivePreference =
                findPreference(PiaPrefHandler.GEO_SERVERS_ACTIVE);
        if (geoServersActivePreference != null) {
            geoServersActivePreference.setChecked(prefs.get(GEO_SERVERS_ACTIVE, true));
        }

        if (!PIAApplication.isAndroidTV(getContext())) {
            Preference proxyPortPreference = findPreference(PiaPrefHandler.PROXY_PORT);
            if (proxyPortPreference != null) {
                proxyPortPreference.setSummary(prefs.get(PiaPrefHandler.PROXY_PORT, "8080"));
            }

            Preference proxyAppPreference = findPreference(PiaPrefHandler.PROXY_APP);
            if (proxyAppPreference != null) {
                proxyAppPreference.setSummary(prefs.get(PiaPrefHandler.PROXY_APP, ""));
            }

            SwitchPreferenceCompat proxyEnabledPreference =
                    findPreference(PiaPrefHandler.PROXY_ENABLED);
            if (proxyEnabledPreference != null) {
                proxyEnabledPreference.setChecked(prefs.get(PiaPrefHandler.PROXY_ENABLED, false));
                resetProxyArea(proxyEnabledPreference, false);
            }

            SwitchPreferenceCompat piaMacePreference = findPreference(PiaPrefHandler.PIA_MACE);
            if (piaMacePreference != null) {
                piaMacePreference.setChecked(prefs.get(PiaPrefHandler.PIA_MACE, false));
            }

            SwitchPreferenceCompat killSwitchPreference = findPreference(PiaPrefHandler.KILLSWITCH);
            if (killSwitchPreference != null) {
                killSwitchPreference.setChecked(prefs.get(PiaPrefHandler.KILLSWITCH, false));
            }
        }

        //Blocking
        SwitchPreferenceCompat ipv6Preference = findPreference(PiaPrefHandler.IPV6);
        if (ipv6Preference != null) {
            ipv6Preference.setChecked(
                    prefs.get(PiaPrefHandler.IPV6, getResources().getBoolean(R.bool.useblockipv6))
            );
        }

        SwitchPreferenceCompat blockLocalLanPreference =
                findPreference(PiaPrefHandler.BLOCK_LOCAL_LAN);
        if (blockLocalLanPreference != null) {
            blockLocalLanPreference.setChecked(prefs.get(PiaPrefHandler.BLOCK_LOCAL_LAN, true));
        }

        // Encryption
        Preference cipherPreference = findPreference(PiaPrefHandler.CIPHER);
        if (cipherPreference != null) {
            cipherPreference.setSummary(
                    prefs.get(
                            PiaPrefHandler.CIPHER,
                            getResources().getStringArray(R.array.ciphers_values)[0]
                    )
            );
        }

        Preference authPreference = findPreference(PiaPrefHandler.AUTH);
        if (authPreference != null) {
            authPreference.setSummary(
                    prefs.get(
                            PiaPrefHandler.AUTH,
                            getResources().getStringArray(R.array.auth_values)[0]
                    )
            );
        }

        Preference tlsCipherPreference = findPreference(PiaPrefHandler.TLSCIPHER);
        if (tlsCipherPreference != null) {
            tlsCipherPreference.setSummary(
                    prefs.get(
                            PiaPrefHandler.TLSCIPHER,
                            getResources().getStringArray(R.array.tls_values)[0]
                    )
            );
        }

        // Application Settings
        SwitchPreferenceCompat autoConnectPreference = findPreference(PiaPrefHandler.AUTOCONNECT);
        if (autoConnectPreference != null) {
            autoConnectPreference.setChecked(
                    prefs.get(PiaPrefHandler.AUTOCONNECT, false)
            );
        }

        SwitchPreferenceCompat autoStartPreference = findPreference(PiaPrefHandler.AUTOSTART);
        if (autoStartPreference != null) {
            autoStartPreference.setChecked(
                    prefs.get(PiaPrefHandler.AUTOSTART, false)
            );
        }

        SwitchPreferenceCompat hapticFeedbackPreference =
                findPreference(PiaPrefHandler.HAPTIC_FEEDBACK);
        if (hapticFeedbackPreference != null) {
            hapticFeedbackPreference.setChecked(
                    prefs.get(PiaPrefHandler.HAPTIC_FEEDBACK, true)
            );
        }

        SwitchPreferenceCompat connectOnAppUpdatedPreference =
                findPreference(PiaPrefHandler.CONNECT_ON_APP_UPDATED);
        if (connectOnAppUpdatedPreference != null) {
            connectOnAppUpdatedPreference.setChecked(
                    prefs.get(PiaPrefHandler.CONNECT_ON_APP_UPDATED, false)
            );
        }

        setDNSSummary();
        setProtocolSummary();
    }
}
