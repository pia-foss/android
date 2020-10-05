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


import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.privateinternetaccess.android.PIAApplication;
import com.privateinternetaccess.android.R;
import com.privateinternetaccess.android.pia.PIAFactory;
import com.privateinternetaccess.android.pia.handlers.PIAServerHandler;
import com.privateinternetaccess.android.pia.handlers.PiaPrefHandler;
import com.privateinternetaccess.android.pia.interfaces.IVPN;
import com.privateinternetaccess.android.pia.utils.DLog;
import com.privateinternetaccess.android.pia.utils.Prefs;
import com.privateinternetaccess.android.pia.utils.Toaster;
import com.privateinternetaccess.android.pia.vpn.PiaOvpnConfig;
import com.privateinternetaccess.android.ui.DialogFactory;
import com.privateinternetaccess.android.ui.adapters.SettingsAdapter;
import com.privateinternetaccess.android.wireguard.backend.GoBackend;

import java.util.Locale;
import java.util.Vector;

import static com.privateinternetaccess.android.pia.handlers.PiaPrefHandler.GEN4_ACTIVE;
import static com.privateinternetaccess.android.pia.handlers.PiaPrefHandler.GEO_SERVERS_ACTIVE;
import static com.privateinternetaccess.android.pia.handlers.PiaPrefHandler.KILLSWITCH;
import static com.privateinternetaccess.android.pia.vpn.PiaOvpnConfig.DEFAULT_AUTH;
import static com.privateinternetaccess.android.pia.vpn.PiaOvpnConfig.DEFAULT_CIPHER;

/**
 * Created by half47 on 4/27/17.
 *
 * Code overflow area for SettingsFragment. Shorting our files will make the code somewhat easier to maintain.
 */

public class SettingsFragmentHandler {

    public static void setupProtocolDialog(final Context context, final Preference screen, final SettingsFragment fragment) {
        screen.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle(R.string.settings_protocol);
                builder.setCancelable(true);

                final String[] protocolArray = context.getResources().getStringArray(R.array.protocol_options);

                final SettingsAdapter adapter = new SettingsAdapter(context);
                adapter.setOptions(protocolArray);
                adapter.setSelected(Prefs.with(context).get(PiaPrefHandler.VPN_PROTOCOL, protocolArray[0]));
                adapter.setDisplayNames(protocolArray);

                builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if(PIAApplication.isAndroidTV(context)) {
                            String selectedProtocol = protocolArray[i];
                            String previousProtocol = Prefs.with(context).get(PiaPrefHandler.VPN_PROTOCOL, protocolArray[0]);

                            String warningMessage = "";

                            if (!selectedProtocol.equals(previousProtocol)) {
                                if(PIAFactory.getInstance().getVPN(context).isVPNActive()) {
                                    Toaster.l(fragment.getActivity().getApplicationContext(), R.string.reconnect_vpn);
                                }

                                if (previousProtocol.equals(protocolArray[1])) {
                                    if (GoBackend.VpnService.backend != null) {
                                        GoBackend.VpnService.backend.stopVpn();
                                    }
                                }
                                else {
                                    IVPN vpn = PIAFactory.getInstance().getVPN(context);

                                    if(vpn.isVPNActive()) {
                                        vpn.stop();

                                        Handler handler = new Handler(Looper.getMainLooper());
                                        handler.postDelayed(() -> {
                                            if(vpn.isKillswitchActive()){
                                                vpn.stopKillswitch();
                                            }
                                        }, 1000);
                                    }

                                    if (PiaPrefHandler.isKillswitchEnabled(context)) {
                                        warningMessage += context.getResources().getString(R.string.killswitch) + "\n";
                                    }

                                    if (PiaPrefHandler.getVPNPerAppPackages(context).size() > 0) {
                                        warningMessage += context.getResources().getString(R.string.per_app_settings);
                                    }
                                }

                                if (warningMessage.length() > 0) {
                                    showPerAppWarning(context, warningMessage);
                                }

                                Prefs.with(context).set(PiaPrefHandler.VPN_PROTOCOL, selectedProtocol);
                                fragment.setProtocolSummary();
                            }
                            DLog.d("Wireguard", "Changing protocol TV: " + selectedProtocol);
                            dialogInterface.dismiss();
                        }
                    }
                });

                builder.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        int index = adapter.getSelectedIndex();
                        String selectedProtocol = protocolArray[index];
                        String previousProtocol = Prefs.with(context).get(PiaPrefHandler.VPN_PROTOCOL, protocolArray[0]);

                        String warningMessage = "";

                        if (!selectedProtocol.equals(previousProtocol)) {
                            if(PIAFactory.getInstance().getVPN(context).isVPNActive()) {
                                Toaster.l(fragment.getActivity().getApplicationContext(), R.string.reconnect_vpn);
                            }

                            if (previousProtocol.equals(protocolArray[1])) {
                                if (GoBackend.VpnService.backend != null) {
                                    GoBackend.VpnService.backend.stopVpn();
                                }
                            }
                            else {
                                IVPN vpn = PIAFactory.getInstance().getVPN(context);

                                if(vpn.isVPNActive()) {
                                    vpn.stop();

                                    Handler handler = new Handler(Looper.getMainLooper());
                                    handler.postDelayed(() -> {
                                        if(vpn.isKillswitchActive()){
                                            vpn.stopKillswitch();
                                        }
                                    }, 1000);
                                }

                                if (PiaPrefHandler.isKillswitchEnabled(context)) {
                                    warningMessage += context.getResources().getString(R.string.killswitch) + "\n";
                                }

                                if (PiaPrefHandler.getVPNPerAppPackages(context).size() > 0) {
                                    warningMessage += context.getResources().getString(R.string.per_app_settings);
                                }
                            }

                            if (warningMessage.length() > 0) {
                                showPerAppWarning(context, warningMessage);
                            }

                            Prefs.with(context).set(PiaPrefHandler.VPN_PROTOCOL, selectedProtocol);
                            fragment.setProtocolSummary();
                            PIAServerHandler.getInstance(context).triggerLatenciesUpdate();
                        }

                        dialogInterface.dismiss();
                    }
                });

                builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

                builder.show();

                return false;
            }
        });
    }

    public static void setupDNSDialog(final Context context, final Preference screen, final SettingsFragment fragment){
        screen.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                String customDNS = Prefs.with(context).getString(PiaPrefHandler.CUSTOM_DNS);
                String customSecondaryDNS = Prefs.with(context).getString(PiaPrefHandler.CUSTOM_SECONDARY_DNS);
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle(context.getString(R.string.dns_pref_header));

                builder.setCancelable(false);

                final String[] array = context.getResources().getStringArray(R.array.dns_names);
                String[] dnsArray = context.getResources().getStringArray(R.array.dns_options);

                if (customDNS != null && customDNS.length() > 0) {
                    String customDnsHeader = array[array.length - 1];

                    if (customSecondaryDNS != null && customSecondaryDNS.length() > 0) {
                        customDnsHeader += String.format(" (%s / %s)", customDNS, customSecondaryDNS);
                    }
                    else {
                        customDnsHeader += String.format(" (%s)", customDNS);
                    }

                    String[] customDnsArray = new String[dnsArray.length + 1];
                    for (int i = 0; i < dnsArray.length; i++)
                        customDnsArray[i] = dnsArray[i];

                    customDnsArray[dnsArray.length] = customDNS;
                    array[array.length - 1] = customDnsHeader;

                    dnsArray = customDnsArray;
                }

                String dns = array[0];
                try {
                    String prefDns = Prefs.with(context).get(PiaPrefHandler.DNS, dnsArray[0]);

                    if (prefDns != null && prefDns.length() > 0) {
                        dns = prefDns;
                    }
                } catch (Exception e) {
                }

                final SettingsAdapter adapter = new SettingsAdapter(context);
                adapter.setOptions(dnsArray);

                if (customDNS != null && Prefs.with(context).get(PiaPrefHandler.CUSTOM_DNS_SELECTED, false)) {
                    adapter.setLastItemSelected();
                }
                else {
                    adapter.setSelected(dns);
                }

                adapter.setDisplayNames(array);

                final String[] dnsCustomArray = dnsArray;

                builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if(PIAApplication.isAndroidTV(context)) {
                            Prefs.with(context).set(PiaPrefHandler.DNS, dnsCustomArray[i]);

                            if (i == dnsCustomArray.length- 1) {
                                Prefs.with(context).set(PiaPrefHandler.DNS_SECONDARY, Prefs.with(context).get(PiaPrefHandler.CUSTOM_SECONDARY_DNS, ""));
                                Prefs.with(context).set(PiaPrefHandler.CUSTOM_DNS_SELECTED, true);
                                showMaceWarning(context, fragment);
                            }
                            else {
                                Prefs.with(context).remove(PiaPrefHandler.DNS_SECONDARY);
                                Prefs.with(context).remove(PiaPrefHandler.CUSTOM_DNS_SELECTED);
                            }

                            fragment.setDNSSummary();
                            dialogInterface.dismiss();
                        }
                    }
                });

                builder.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int index = adapter.getSelectedIndex();

                        if (index < dnsCustomArray.length && index >= 0) {
                            PiaPrefHandler.setDnsChanged(context,
                                    !Prefs.with(context).get(PiaPrefHandler.DNS, "").equals(dnsCustomArray[index]));
                            Prefs.with(context).set(PiaPrefHandler.DNS, dnsCustomArray[index]);

                            DLog.d("SettingsFragment", "DNS Changed: " + PiaPrefHandler.hasDnsChanged(context));

                            if (index == dnsCustomArray.length- 1) {
                                Prefs.with(context).set(PiaPrefHandler.DNS_SECONDARY, Prefs.with(context).get(PiaPrefHandler.CUSTOM_SECONDARY_DNS, ""));
                                Prefs.with(context).set(PiaPrefHandler.CUSTOM_DNS_SELECTED, true);
                                showMaceWarning(context, fragment);
                            }
                            else {
                                Prefs.with(context).remove(PiaPrefHandler.DNS_SECONDARY);
                                Prefs.with(context).remove(PiaPrefHandler.CUSTOM_DNS_SELECTED);
                            }

                            fragment.setDNSSummary();
                            dialog.dismiss();
                        }
                    }
                });

                if (customDNS != null) {
                    builder.setNeutralButton(R.string.edit_custom_dns, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            showCustomDNSDialog(context, fragment);
                            dialog.dismiss();
                        }
                    });
                }
                else {
                    builder.setNeutralButton(R.string.custom_dns, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            showDNSWarning(context, fragment);
                        }
                    });
                }

                builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

                builder.show();

                return false;
            }
        });
    }

    private static void showDNSWarning(final Context context, final SettingsFragment fragment) {
        DialogFactory factory = new DialogFactory(context);
        final Dialog dialog = factory.buildDialog();
        factory.setHeader(context.getString(R.string.custom_dns_warning_title));
        factory.setMessage(context.getString(R.string.custom_dns_warning_body));

        factory.setPositiveButton(context.getString(R.string.ok), new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showCustomDNSDialog(context, fragment);
                dialog.dismiss();
            }
        });

        factory.setNegativeButton(context.getString(R.string.cancel), new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private static void showCustomDNSDialog(final Context context, final SettingsFragment fragment) {
        final TextInputLayout textLayoutPrimary = new TextInputLayout(context);
        final TextInputLayout textLayoutSecondary = new TextInputLayout(context);
        final TextInputEditText primaryDns = new TextInputEditText(context);
        final TextInputEditText secondaryDns = new TextInputEditText(context);

        LinearLayout textLayout = new LinearLayout(context);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        textLayout.setOrientation(LinearLayout.VERTICAL);
        textLayout.setPadding(40,20,40,20);
        textLayout.setLayoutParams(lp);

        LinearLayout.LayoutParams editLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        editLp.setMargins(15, 0, 15, 0);
        primaryDns.setLayoutParams(editLp);
        secondaryDns.setLayoutParams(editLp);

        textLayout.addView(textLayoutPrimary);
        textLayout.addView(textLayoutSecondary);

        if (Prefs.with(context).get(PiaPrefHandler.PIA_MACE, false)) {
            final TextView maceWarningText = new TextView(context);
            maceWarningText.setText(R.string.custom_dns_disabling_mace);
            maceWarningText.setLayoutParams(editLp);

            textLayout.addView(maceWarningText);
        }

        String customDNS = Prefs.with(context).getString(PiaPrefHandler.CUSTOM_DNS);
        final String secondaryDNS = Prefs.with(context).getString(PiaPrefHandler.CUSTOM_SECONDARY_DNS);

        textLayoutPrimary.addView(primaryDns);
        textLayoutSecondary.addView(secondaryDns);

        primaryDns.setHint(R.string.custom_primary_dns);
        secondaryDns.setHint(R.string.custom_secondary_dns);

        DialogFactory factory = new DialogFactory(context);
        final Dialog dialog = factory.buildDialog();
        factory.setHeader(context.getString(R.string.custom_dns));
        factory.setBody(textLayout);

        if (customDNS != null && customDNS.length() > 0) {
            primaryDns.setText(customDNS);
        }

        if (secondaryDNS != null && secondaryDNS.length() > 0) {
            secondaryDns.setText(secondaryDNS);
        }

        factory.setPositiveButton(context.getString(R.string.save), new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isDnsValid(primaryDns, context.getString(R.string.custom_primary_dns_invalid)) &&
                        isSecondaryValid(secondaryDns, context.getString(R.string.custom_secondary_dns_invalid))) {
                    Prefs.with(context).set(PiaPrefHandler.CUSTOM_DNS, primaryDns.getText().toString());
                    Prefs.with(context).set(PiaPrefHandler.CUSTOM_SECONDARY_DNS, secondaryDns.getText().toString());
                    Prefs.with(context).set(PiaPrefHandler.DNS, primaryDns.getText().toString());
                    Prefs.with(context).set(PiaPrefHandler.DNS_SECONDARY, secondaryDns.getText().toString());
                    Prefs.with(context).set(PiaPrefHandler.CUSTOM_DNS_SELECTED, true);
                    fragment.setDNSSummary();

                    if (Prefs.with(context).get(PiaPrefHandler.PIA_MACE, false)) {
                        fragment.toggleMace(false);
                    }

                    dialog.dismiss();
                }
            }
        });

        factory.setNegativeButton(context.getString(R.string.cancel), new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

        if (customDNS != null && customDNS.length() > 0) {
            factory.setNeutralButton(context.getString(R.string.clear), new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Prefs.with(context).remove(PiaPrefHandler.CUSTOM_DNS);
                    Prefs.with(context).remove(PiaPrefHandler.CUSTOM_SECONDARY_DNS);
                    Prefs.with(context).remove(PiaPrefHandler.DNS);
                    Prefs.with(context).remove(PiaPrefHandler.DNS_SECONDARY);
                    Prefs.with(context).remove(PiaPrefHandler.CUSTOM_DNS_SELECTED);
                    fragment.setDNSSummary();
                    dialog.dismiss();
                }
            });
        }

        dialog.show();
    }

    private static void showMaceWarning(Context context, SettingsFragment fragment) {
        if (!Prefs.with(context).get(PiaPrefHandler.PIA_MACE, false)) {
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.custom_dns_warning_title);
        builder.setMessage(R.string.custom_dns_disabling_mace);

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });

        builder.show();

        fragment.toggleMace(false);
    }

    private static void showPerAppWarning(Context context, String message) {
        String fullMessage = context.getResources().getString(R.string.wg_protocol_warning) + "\n\n" + message;

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.settings_protocol);
        builder.setMessage(fullMessage);

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });

        builder.show();
    }

    private static boolean isDnsValid(TextInputEditText text, String error) {
        String address = text.getText().toString();
        if (address == null || address.length() == 0 || !isValidIp(address)) {
            text.setError(error);
            return false;
        }

        return true;
    }

    private static boolean isSecondaryValid(TextInputEditText text, String error) {
        String address = text.getText().toString();
        if (address != null && address.length() > 0 && !isValidIp(address)) {
            text.setError(error);
            return false;
        }

        return true;
    }

    private static boolean isValidIp(String address) {
        return address.matches("^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$");
    }

    public static void setupPortDialogs(final Context context, final SettingsFragment fragment,
                                        final Preference lPort, Preference rPort,
                                        final SwitchPreferenceCompat mTCP){
        final PIAServerHandler serverHandler = PIAServerHandler.getInstance(context);
        lPort.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle(R.string.local_port_titla);

                final NumberView view = new NumberView(context);

                view.getText().setHint(R.string.settings_lport_hint);
                builder.setView(view.getView());

                String lport = "";
                try { //doing this since we save as an int. I will look into this as it should save as a string so we don't need this.
                    lport = Prefs.with(context).get(PiaPrefHandler.LPORT, "auto");
                } catch (Exception e) {
                    int port = Prefs.with(context).get(PiaPrefHandler.LPORT, 0);
                    if(port != 0)
                        lport = port + "";
                }

                if(!TextUtils.isEmpty(lport) && !lport.equals("auto")) {
                    view.getText().setText(lport);
                    view.getText().setSelection(lport.length());
                }

                builder.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String input = view.getText().getText().toString();
                        if(!TextUtils.isEmpty(input)) {
                            try {
                                Integer port = Integer.parseInt(input);
                                if (port <= 65535 && port >= 1024) {
                                    fragment.setLportSummary(port + "");
                                    Prefs.with(view.getText().getContext()).set(PiaPrefHandler.LPORT, port + "");
                                } else {
                                    Toaster.s(view.getText().getContext(), context.getString(R.string.settings_port_number_restriction));
                                }
                            } catch (Exception e) {
                                view.getText().setText("");
                                Toaster.s(view.getText().getContext(), context.getString(R.string.settings_port_number_restriction));
                            }
                        }
                    }
                });

                builder.setNeutralButton(R.string.default_base, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Prefs.with(view.getText().getContext()).remove(PiaPrefHandler.LPORT);
                        fragment.setLportSummary("auto");
                        dialog.dismiss();
                    }
                });

                builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

                builder.show();
                return false;
            }
        });

        rPort.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                mTCP.setEnabled(false);
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle(R.string.remote_port);

                boolean tcp = mTCP.isChecked();

                Vector<Integer> ports;
                if (tcp)
                    ports = serverHandler.getInfo().getTcpPorts();
                else
                    ports = serverHandler.getInfo().getUdpPorts();
                DLog.d("Settings", "ports " + ports);
                String[] strPorts = new String[ports.size() + 1];
                int i = 0;
                strPorts[i++] = "auto";
                for (int p : ports) {
                    strPorts[i++] = "" + p;
                }

                final SettingsAdapter adapter = new SettingsAdapter(context);
                adapter.setOptions(strPorts);
                adapter.setSelected(Prefs.with(context).get("rport", "auto"));

                builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(PIAApplication.isAndroidTV(context)) {
                            DLog.d("onClickAdapter", "adapter = " + which);
                            Vector<Integer> ports;
                            boolean tcp = mTCP.isChecked();
                            if (tcp)
                                ports = serverHandler.getInfo().getTcpPorts();
                            else
                                ports = serverHandler.getInfo().getUdpPorts();
                            String selected = "auto";
                            if(which >= 1){
                                selected = ports.get(--which) + "";
                            }
                            Prefs.with(context).set(PiaPrefHandler.RPORT, selected);
                            fragment.setRportSummary(selected);
                            dialog.dismiss();
                        }
                    }
                });

                if(!PIAApplication.isAndroidTV(context))
                    builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String selected = adapter.getSelected();
                            Prefs.with(context).set(PiaPrefHandler.RPORT, selected);
                            fragment.setRportSummary(selected);
                            dialog.dismiss();
                        }
                    });
                builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        mTCP.setEnabled(true);
                    }
                });
                builder.show();
                return false;
            }
        });


    }

    public static void setupOtherDialogs(Context context, Preference cipher, Preference auth, Preference tlsCipher){
        createListDialog(context, cipher,
                "cipher", context.getResources().getStringArray(R.array.cipher_list), context.getResources().getStringArray(R.array.ciphers_values)
                , context.getString(R.string.data_encyrption), DEFAULT_CIPHER, auth);

        createListDialog(context, auth,
                "auth", context.getResources().getStringArray(R.array.auth_list), context.getResources().getStringArray(R.array.auth_values)
                , context.getString(R.string.data_auth), PiaOvpnConfig.DEFAULT_AUTH, null);

        createListDialog(context, tlsCipher,
                "tlscipher", context.getResources().getStringArray(R.array.tls_cipher), context.getResources().getStringArray(R.array.tls_values)
                , context.getString(R.string.handshake), "rsa2048", null);

        setAuthEnabledFromCipher(context, Prefs.with(context).get("cipher", DEFAULT_CIPHER), auth);


    }

    public static void createListDialog(final Context context, Preference perf,
                                        final String prefName, final String[] list, final String[] valuesList,
                                        final String title, final String defaultValue, final Preference auth){
        perf.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(final Preference preference) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle(title);

                final SettingsAdapter adapter = new SettingsAdapter(context);
                adapter.setOptions(list);
                adapter.setSelected(getSummaryItem(context, prefName, defaultValue, list, valuesList));

                builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(PIAApplication.isAndroidTV(context)) {
                            DLog.d("onClickAdapter", "adapter = " + which);
                            String value = valuesList[which];
                            Prefs.with(context).set(prefName, value);
                            preference.setSummary(list[which]);
                            dialog.dismiss();
                            if (auth != null) {
                                // Auth is not null, so we are the cipher dialog and need to disable/enable auth depending on our result
                                setAuthEnabledFromCipher(context, value, auth);
                            }
                        }
                    }
                });

                if(!PIAApplication.isAndroidTV(context))
                    builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String selected = adapter.getSelected();
                            int selectedPos = 0;
                            for(String name : list){
                                if(name.equals(selected)){
                                    break;
                                }
                                selectedPos++;
                            }
                            String value = valuesList[selectedPos];
                            DLog.d("SettingsFragment", "selected = " + selected + " value = " + value);
                            Prefs.with(context).set(prefName, value);
                            preference.setSummary(adapter.getSelected());
                            dialog.dismiss();
                            if (auth != null) {
                                // Auth is not null, so we are the cipher dialog and need to disable/enable auth depending on our result
                                setAuthEnabledFromCipher(context, value, auth);
                            }
                        }
                    });

                builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

                builder.show();
                return false;
            }
        });
    }

    private static void setAuthEnabledFromCipher(Context context, String value, Preference auth) {
        if (value.toLowerCase(Locale.ENGLISH).contains("gcm")) {
            auth.setEnabled(false);
            auth.setSummary(R.string.auth_setby_gcm);
        } else {
            auth.setEnabled(true);
            auth.setSummary(SettingsFragmentHandler.getSummaryItem(context, "auth",
                    DEFAULT_AUTH, context.getResources().getStringArray(R.array.auth_list),
                    context.getResources().getStringArray(R.array.auth_values)));
        }
    }

    public static String getSummaryItem(Context context, String prefname, String defaultName, String[] names, String[] values){
        String value = Prefs.with(context).get(prefname, defaultName);
        int pos = 0;
        for(String v : values){
            if(v.equals(value)){
                break;
            }
            pos++;
        }
        return names[pos];
    }

    public static void setupProxyPortDialog(final Context context, final Preference pref){
        pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(final Preference preference) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle(R.string.preference_proxy_port);

                final NumberView view = new NumberView(context);

                builder.setView(view.getView());

                String port = Prefs.with(context).get(PiaPrefHandler.PROXY_PORT, "8080");
                view.getText().setText(port);
                view.getText().setSelection(port.length());

                builder.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String text = view.getText().getText().toString();
                        pref.setSummary(text);
                        Prefs.with(context).set(PiaPrefHandler.PROXY_PORT, text);
                        dialog.dismiss();
                    }
                });

                builder.setNegativeButton(R.string.dismiss, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

                builder.setNeutralButton(R.string.default_base, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Prefs.with(view.getText().getContext()).remove(PiaPrefHandler.PROXY_PORT);
                        pref.setSummary("8080");
                        dialog.dismiss();
                    }
                });

                builder.show();


                return false;
            }
        });

    }

    static class NumberView {

        private View view;
        private EditText text;

        public NumberView(Context context){
            LinearLayout linearLayout = new LinearLayout(context);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT);
            linearLayout.setOrientation(LinearLayout.VERTICAL);
            linearLayout.setPadding(40,20,40,20);
            linearLayout.setLayoutParams(lp);

            EditText editText = new EditText(context);
            LinearLayout.LayoutParams lp3 = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT);
            lp3.setMargins(15, 0, 0, 0);
            editText.setLayoutParams(lp3);
            editText.setTextSize(15);
            editText.setInputType(InputType.TYPE_CLASS_NUMBER);
            linearLayout.addView(editText);

            view = linearLayout;
            text = editText;
        }

        public View getView() {
            return view;
        }

        public void setView(View view) {
            this.view = view;
        }

        public EditText getText() {
            return text;
        }

        public void setText(EditText text) {
            this.text = text;
        }
    }

    public static void resetToDefault(Context ctx){
        Prefs prefs = new Prefs(ctx);

        // Connection Area
        prefs.set(PiaPrefHandler.USE_TCP, false);
        prefs.set(PiaPrefHandler.PORTFORWARDING, false);
        prefs.set(PiaPrefHandler.RPORT, "");
        prefs.set(PiaPrefHandler.LPORT, "");
        prefs.set(PiaPrefHandler.PACKET_SIZE, ctx.getResources().getBoolean(R.bool.usemssfix));
        prefs.set(GEN4_ACTIVE, true);
        prefs.set(GEO_SERVERS_ACTIVE, true);

        if (!PIAApplication.isAndroidTV(ctx)) {
            prefs.set(PiaPrefHandler.PROXY_PORT, "8080");
            prefs.set(PiaPrefHandler.PROXY_APP, "");
            prefs.set(PiaPrefHandler.PIA_MACE, false);
            prefs.set(KILLSWITCH, false);
        }

        //Blocking
        prefs.set(PiaPrefHandler.IPV6, ctx.getResources().getBoolean(R.bool.useblockipv6));
        prefs.set(PiaPrefHandler.BLOCK_LOCAL_LAN, true);

        // Encryption
        String cipher = ctx.getResources().getStringArray(R.array.ciphers_values)[0];
        String auth = ctx.getResources().getStringArray(R.array.auth_values)[0];
        String tls = ctx.getResources().getStringArray(R.array.tls_values)[0];

        prefs.set(PiaPrefHandler.CIPHER, cipher);
        prefs.set(PiaPrefHandler.AUTH, auth);
        prefs.set(PiaPrefHandler.TLSCIPHER, tls);

        // Protocol
        prefs.set(PiaPrefHandler.VPN_PROTOCOL, ctx.getResources().getStringArray(R.array.protocol_options)[0]);

        // Application Settings
        prefs.set(PiaPrefHandler.AUTOCONNECT, false);
        prefs.set(PiaPrefHandler.AUTOSTART, false);
        prefs.set(PiaPrefHandler.HAPTIC_FEEDBACK, true);
        prefs.set(PiaPrefHandler.CONNECT_ON_APP_UPDATED, false);
        prefs.set(PiaPrefHandler.WIDGET_BACKGROUND_COLOR, ContextCompat.getColor(ctx, R.color.widget_background_default));
        prefs.set(PiaPrefHandler.WIDGET_TEXT_COLOR, ContextCompat.getColor(ctx, R.color.widget_text_default));
        prefs.set(PiaPrefHandler.WIDGET_UPLOAD_COLOR, ContextCompat.getColor(ctx, R.color.widget_upload_default));
        prefs.set(PiaPrefHandler.WIDGET_DOWNLOAD_COLOR, ContextCompat.getColor(ctx, R.color.widget_download_default));
        prefs.set(PiaPrefHandler.WIDGET_RADIUS, 8);
        prefs.set(PiaPrefHandler.WIDGET_ALPHA, 100);
        prefs.remove(PiaPrefHandler.DNS);
        prefs.remove(PiaPrefHandler.DNS_SECONDARY);
        prefs.remove(PiaPrefHandler.CUSTOM_SECONDARY_DNS);
        prefs.remove(PiaPrefHandler.CUSTOM_DNS);

        //PiaPrefHandler.clearTrustWifi(ctx);
        PiaPrefHandler.clearTrustedNetworks(ctx);
    }
}
