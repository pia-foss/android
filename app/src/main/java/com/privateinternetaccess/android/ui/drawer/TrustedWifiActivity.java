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

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.SwitchCompat;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.privateinternetaccess.android.R;
import com.privateinternetaccess.android.pia.handlers.PiaPrefHandler;
import com.privateinternetaccess.android.pia.utils.Prefs;
import com.privateinternetaccess.android.ui.adapters.TrustedWifiAdapter;
import com.privateinternetaccess.android.ui.superclasses.BaseActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;

public class TrustedWifiActivity extends BaseActivity {

    @BindView(R.id.trusted_wifi_switch) SwitchCompat autoConnectToggle;
    @BindView(R.id.trusted_wifi_list) RecyclerView trustedList;

    @BindView(R.id.trusted_wifi_permission_layout) View permissionsLayout;
    @BindView(R.id.trusted_wifi_list_layout) View listLayout;

    @BindView(R.id.trusted_wifi_warning_message) TextView warningText;

    @BindView(R.id.trusted_wifi_permissions_button) Button permissionsButton;

    private WifiManager wifiManager;
    private List<String> wifiScanList;
    private List<String> trustedSsidList;

    private RecyclerView.LayoutManager layoutManager;

    private TrustedWifiAdapter wifiAdapter;

    private List<ScanResult> scanList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trusted_wifi);
        ButterKnife.bind(this);

        initHeader(true, true);
        setTitle(getString(R.string.trusted_wifi_header));
        setGreenBackground();
        setSecondaryGreenBackground();

        wifiScanList = new ArrayList<>();
        trustedSsidList = new ArrayList<>();
        scanList = new ArrayList<>();

        wifiAdapter = new TrustedWifiAdapter(this, wifiScanList, trustedSsidList);
        wifiAdapter.isLoading = true;

        layoutManager = new LinearLayoutManager(this);
        trustedList.setLayoutManager(layoutManager);
        trustedList.setAdapter(wifiAdapter);

        permissionsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityCompat.requestPermissions(TrustedWifiActivity.this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        0);
            }
        });

        autoConnectToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b && !checkPermissions()) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(TrustedWifiActivity.this);
                    builder.setTitle(R.string.no_permission_title);
                    builder.setMessage(R.string.trusted_wifi_no_permission);
                    builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    builder.show();

                    autoConnectToggle.setChecked(false);
                }
                else {
                    Prefs.with(TrustedWifiActivity.this).set(PiaPrefHandler.TRUST_WIFI, b);
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        autoConnectToggle.setChecked(Prefs.with(this).get(PiaPrefHandler.TRUST_WIFI, false) &&
                checkPermissions());

        updateUi();

        registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        registerReceiver(networkChangeReceiver, new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));
        wifiManager.startScan();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(wifiReceiver);
        unregisterReceiver(networkChangeReceiver);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        updateUi();
        PiaPrefHandler.setLocationRequest(this, true);
    }

    private BroadcastReceiver wifiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            scanList = wifiManager.getScanResults();
            setupLists();
        }
    };

    private BroadcastReceiver networkChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateUi();
        }
    };

    private void updateUi() {
        listLayout.setVisibility(View.VISIBLE);
        permissionsLayout.setVisibility(View.GONE);
        autoConnectToggle.setVisibility(View.VISIBLE);

        if (!checkPermissions()) {
            warningText.setVisibility(View.VISIBLE);
            warningText.setText(R.string.trusted_wifi_no_permission);
            trustedList.setVisibility(View.GONE);
            autoConnectToggle.setVisibility(View.GONE);

            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) || !PiaPrefHandler.getLocationRequest(this)) {
                permissionsLayout.setVisibility(View.VISIBLE);
                listLayout.setVisibility(View.GONE);
            }
        }
        else if (!checkWifi()) {
            warningText.setVisibility(View.VISIBLE);
            warningText.setText(R.string.trusted_wifi_no_wifi);
            trustedList.setVisibility(View.GONE);
        }
        else {
            warningText.setVisibility(View.GONE);
            trustedList.setVisibility(View.VISIBLE);
        }
    }

    private boolean checkWifi() {
        ConnectivityManager cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

        return activeNetwork != null && activeNetwork.isConnectedOrConnecting() && activeNetwork.getType() == ConnectivityManager.TYPE_WIFI;
    }

    private boolean checkPermissions() {
        return ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    public void setupLists() {
        Set<String> trustedSsids = PiaPrefHandler.getTrustedNetworks(this);

        List<String> availableNetworks = new ArrayList<>();
        List<String> trustedNetworks = new ArrayList<>();
        trustedNetworks.addAll(trustedSsids);

        for(ScanResult item : scanList) {
            if (!trustedSsids.contains(item.SSID) && item.SSID.length() > 0 &&
                    !availableNetworks.contains(item.SSID)) {
                availableNetworks.add(item.SSID);
            }
        }

        wifiScanList.clear();
        wifiScanList.addAll(availableNetworks);

        trustedSsidList.clear();
        trustedSsidList.addAll(trustedNetworks);

        wifiAdapter.isLoading = false;
        wifiAdapter.notifyDataSetChanged();
    }
}
