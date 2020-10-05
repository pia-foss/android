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
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.privateinternetaccess.android.R;
import com.privateinternetaccess.android.model.listModel.NetworkItem;
import com.privateinternetaccess.android.pia.handlers.PiaPrefHandler;
import com.privateinternetaccess.android.ui.DialogFactory;
import com.privateinternetaccess.android.ui.adapters.TrustedWifiAdapter;
import com.privateinternetaccess.android.ui.superclasses.BaseActivity;
import com.privateinternetaccess.android.ui.tv.views.ServerSelectionItemDecoration;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class TrustedWifiActivity extends BaseActivity {

    @BindView(R.id.trusted_wifi_permission_layout) View permissionsLayout;
    @BindView(R.id.trusted_wifi_list_layout) View listLayout;

    @BindView(R.id.network_recyclerview) RecyclerView rulesList;

    @BindView(R.id.network_add_rule_button) LinearLayout lAddRule;

    @BindView(R.id.trusted_wifi_permissions_button) Button permissionsButton;

    @BindView(R.id.trusted_wifi_title) TextView tvTitle;
    @BindView(R.id.trusted_wifi_description) TextView tvDescription;

    private WifiManager wifiManager;
    private List<ScanResult> wifiScanList;
    private List<NetworkItem> networkList;

    private RecyclerView.LayoutManager gridLayoutManager;
    private RecyclerView.LayoutManager linearLayoutManager;

    private ServerSelectionItemDecoration rulesDecoration;
    private TrustedWifiAdapter wifiAdapter;

    private List<ScanResult> scanList;

    private static int REMOVE_ID = 123456;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trusted_wifi);
        ButterKnife.bind(this);

        initHeader(true, true);
        setTitle(getString(R.string.trusted_wifi_plural));
        setBackground();
        setSecondaryGreenBackground();

        wifiScanList = new ArrayList<>();
        networkList = new ArrayList<>();
        scanList = new ArrayList<>();

        wifiAdapter = new TrustedWifiAdapter(this, wifiScanList, networkList);
        wifiAdapter.isLoading = true;

        gridLayoutManager = new GridLayoutManager(this, 2);
        linearLayoutManager = new LinearLayoutManager(this);
        rulesDecoration = new ServerSelectionItemDecoration(2, 32, 0);
        rulesList.setLayoutManager(gridLayoutManager);
        rulesList.addItemDecoration(rulesDecoration);
        rulesList.setAdapter(wifiAdapter);

        permissionsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityCompat.requestPermissions(TrustedWifiActivity.this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        0);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        setupRules();
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
    public void onBackPressed() {
        if (wifiAdapter.isAddingRule) {
            toggleRules();
        }
        else {
            super.onBackPressed();
        }
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

        if (!checkPermissions()) {
            rulesList.setVisibility(View.GONE);

            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) || !PiaPrefHandler.getLocationRequest(this)) {
                permissionsLayout.setVisibility(View.VISIBLE);
                listLayout.setVisibility(View.GONE);
            }
        }
        else if (!checkWifi()) {
            rulesList.setVisibility(View.GONE);
        }
        else {
            rulesList.setVisibility(View.VISIBLE);
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

    public void setupRules() {
        List<String> serializedRules = PiaPrefHandler.getNetworkRules(this);
        networkList.clear();

        if (serializedRules.size() == 0) {
            networkList.addAll(NetworkItem.defaultList(this));
            saveDefaults();
        }
        else {
            for (int i = 0; i < serializedRules.size(); i++) {
                networkList.add(NetworkItem.fromString(serializedRules.get(i)));
            }
        }

        wifiAdapter.notifyDataSetChanged();
    }

    public void setupLists() {
        List<ScanResult> availableNetworks = new ArrayList<>();
        for(ScanResult item : scanList) {
            if (item.SSID.length() > 0 && !availableNetworks.contains(item) &&
                networkItemForSsid(item.SSID) == null && !containsSsid(availableNetworks, item.SSID)) {
                availableNetworks.add(item);
            }
        }

        wifiScanList.clear();
        wifiScanList.addAll(availableNetworks);

        wifiAdapter.isLoading = false;
        wifiAdapter.notifyDataSetChanged();
    }

    public void addRuleForNetwork(ScanResult network) {
        final DialogFactory factory = new DialogFactory(this);
        final Dialog dialog = factory.buildDialog();
        factory.setHeader(getResources().getString(R.string.nmt_rule_for, network.SSID));

        factory.setPositiveButton(getString(R.string.ok), view -> {
            NetworkItem newItem = new NetworkItem();
            newItem.type = NetworkItem.NetworkType.WIFI_CUSTOM;
            newItem.behavior = NetworkItem.getBehaviorFromId(factory.getSelectedItem());
            newItem.networkName = network.SSID;

            PiaPrefHandler.addNetworkRule(TrustedWifiActivity.this, newItem);

            setupLists();
            setupRules();

            toggleRules();

            dialog.dismiss();
        });

        factory.setNegativeButton(getString(R.string.cancel), view -> dialog.dismiss());

        List<Pair<Integer, String>> options = new ArrayList();
        options.add(new Pair(NetworkItem.NetworkBehavior.ALWAYS_CONNECT.name().hashCode(), getString(R.string.nmt_connect)));
        options.add(new Pair(NetworkItem.NetworkBehavior.ALWAYS_DISCONNECT.name().hashCode(), getString(R.string.nmt_disconnect)));
        options.add(new Pair(NetworkItem.NetworkBehavior.RETAIN_STATE.name().hashCode(), getString(R.string.nmt_retain)));

        NetworkItem.NetworkBehavior selectedBehavior = NetworkItem.NetworkBehavior.ALWAYS_CONNECT;

        factory.addRadioGroup(options, selectedBehavior.name().hashCode());
        dialog.show();
    }

    public void updateNetworkRule(NetworkItem rule) {
        if (rule == null)
            return;

        final DialogFactory factory = new DialogFactory(this);
        final Dialog dialog = factory.buildDialog();
        factory.setHeader(getResources().getString(R.string.nmt_rule_for, rule.networkName));

        factory.setPositiveButton(getString(R.string.ok), view -> {
            if (factory.getSelectedItem() == REMOVE_ID) {
                PiaPrefHandler.removeNetworkRule(TrustedWifiActivity.this, rule);
                setupLists();
            }
            else {
                rule.behavior = NetworkItem.getBehaviorFromId(factory.getSelectedItem());
                PiaPrefHandler.addNetworkRule(TrustedWifiActivity.this, rule);
            }

            setupRules();
            dialog.dismiss();
        });

        factory.setNegativeButton(getString(R.string.cancel), view -> dialog.dismiss());

        List<Pair<Integer, String>> options = new ArrayList();
        options.add(new Pair(NetworkItem.NetworkBehavior.ALWAYS_CONNECT.name().hashCode(), getString(R.string.nmt_connect)));
        options.add(new Pair(NetworkItem.NetworkBehavior.ALWAYS_DISCONNECT.name().hashCode(), getString(R.string.nmt_disconnect)));
        options.add(new Pair(NetworkItem.NetworkBehavior.RETAIN_STATE.name().hashCode(), getString(R.string.nmt_retain)));

        if (!rule.isDefault())
            options.add(new Pair(REMOVE_ID, getString(R.string.nmt_remove_rule)));

        NetworkItem.NetworkBehavior selectedBehavior = rule.behavior;
        factory.addRadioGroup(options, selectedBehavior.name().hashCode());
        dialog.show();
    }

    private void saveDefaults() {
        List<NetworkItem> rules = NetworkItem.defaultList(this);
        List<String> serializedRules = new ArrayList<>();

        for (int i = 0; i < rules.size(); i++) {
            serializedRules.add(rules.get(i).toString());
        }

        PiaPrefHandler.updateNetworkRules(this, serializedRules);
    }

    private boolean containsSsid(List<ScanResult> results, String ssid) {
        for (ScanResult result : results) {
            if (result.SSID.equals(ssid))
                return true;
        }

        return false;
    }

    @OnClick(R.id.network_add_rule_button)
    public void onAddRuleClicked() {
        toggleRules();
    }

    @Nullable
    private NetworkItem networkItemForSsid(String ssid) {
        List<String> serializedResults = PiaPrefHandler.getNetworkRules(this);

        for (String item : serializedResults) {
            NetworkItem network = NetworkItem.fromString(item);

            if (network.networkName.equals(ssid)) {
                return network;
            }
        }

        return null;
    }

    private void toggleRules() {
        if (wifiAdapter.isAddingRule) {
            rulesDecoration.disable = false;
            rulesList.setLayoutManager(gridLayoutManager);
            lAddRule.setVisibility(View.VISIBLE);
            tvTitle.setVisibility(View.VISIBLE);

            tvDescription.setText(R.string.nmt_manage_description);
        }
        else {
            rulesDecoration.disable = true;
            rulesList.setLayoutManager(linearLayoutManager);
            lAddRule.setVisibility(View.GONE);
            tvTitle.setVisibility(View.GONE);

            tvDescription.setText(R.string.nmt_add_description);
        }

        wifiAdapter.isAddingRule = !wifiAdapter.isAddingRule;
        wifiAdapter.notifyDataSetChanged();
    }
}
