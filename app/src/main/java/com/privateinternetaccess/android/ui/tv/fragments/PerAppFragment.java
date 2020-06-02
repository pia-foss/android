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

package com.privateinternetaccess.android.ui.tv.fragments;

import android.Manifest;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.privateinternetaccess.android.R;
import com.privateinternetaccess.android.model.interfaces.IAllowedApps;
import com.privateinternetaccess.android.pia.handlers.PiaPrefHandler;
import com.privateinternetaccess.android.pia.utils.DLog;
import com.privateinternetaccess.android.pia.utils.Prefs;
import com.privateinternetaccess.android.pia.utils.Toaster;
import com.privateinternetaccess.android.ui.adapters.AllowedAppsAdapter;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import butterknife.BindView;
import butterknife.ButterKnife;

public class PerAppFragment extends Fragment implements IAllowedApps {

    @BindView(R.id.favorites_list) RecyclerView perAppList;
    @BindView(R.id.favorites_search) EditText searchView;
    @BindView(R.id.favorites_search_icon) ImageView searchIcon;

    @BindView(R.id.favorites_title) TextView titleTextView;

    private LinearLayoutManager layoutManager;

    private AllowedAppsAdapter mAdapter;
    private HashSet<String> mExcludedApps;
    private Vector<ApplicationInfo> mPackages = new Vector<>();
    private PackageManager mPm;

    private Map<String, String> mProblemApps;

    private Toast toastApp;

    private boolean selectApp;
    private String selectedApp;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_favorites, container, false);
        ButterKnife.bind(this, view);

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        layoutManager = new LinearLayoutManager(perAppList.getContext());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);

        perAppList.setHasFixedSize(true);
        perAppList.setLayoutManager(layoutManager);

        mAdapter = new AllowedAppsAdapter(getContext(), this, false);
        perAppList.setAdapter(mAdapter);

        titleTextView.setText(R.string.per_app_settings);

        searchView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    if (mAdapter != null) {
                        mAdapter.filter(searchView.getText().toString());
                    }
                } catch (Exception e) {
                    if(mAdapter == null){
                        perAppList.setAdapter(null);
                        mAdapter = new AllowedAppsAdapter(getContext(), PerAppFragment.this, selectApp);
                    }
                    if(mPackages != null) {
                        mAdapter.setmPackages(mPackages);
                        mAdapter.filter(searchView.getText().toString());
                    } else {
                        genInternetPackagesList();
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        saveSelectedApps();
        if(toastApp != null){
            toastApp.cancel();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        genInternetPackagesList();
        Set<String> prefPackages = Prefs.with(getContext()).getStringSet(PiaPrefHandler.VPN_PER_APP_PACKAGES);
        mExcludedApps = new HashSet<>();
        mExcludedApps.addAll(prefPackages);
        if(mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }
    }

    private void genInternetPackagesList() {
       // pbLoad.setVisibility(View.VISIBLE);
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                mPm = getContext().getPackageManager();
                List<ApplicationInfo> installedPackages = mPm.getInstalledApplications(PackageManager.GET_META_DATA);

                int androidSystemUid = 0;
                ApplicationInfo system;
                Vector<ApplicationInfo> apps = new Vector<>();

                try {
                    system = mPm.getApplicationInfo("android", PackageManager.GET_META_DATA);
                    androidSystemUid = system.uid;
                    apps.add(system);
                } catch (PackageManager.NameNotFoundException e) {
                }

                for (ApplicationInfo app : installedPackages) {
                    if (mPm.checkPermission(Manifest.permission.INTERNET, app.packageName) == PackageManager.PERMISSION_GRANTED &&
                            app.uid != androidSystemUid && !app.packageName.equals("com.privateinternetaccess.android")) {
                        apps.add(app);
                    }
                }

                Collections.sort(apps, new ApplicationInfo.DisplayNameComparator(mPm));

                mPackages = apps;

                perAppList.post(new Runnable() {
                    @Override
                    public void run() {
                        //pbLoad.setVisibility(View.GONE);
                        if (mAdapter != null) {
                            mAdapter.setmPackages(mPackages);
                            if(!TextUtils.isEmpty(searchView.getText().toString())){
                                mAdapter.filter(searchView.getText().toString());
                            }

                        }
                    }
                });
            }
        });
        t.start();
    }

    private void saveSelectedApps() {
        DLog.d("SaveSelectedApps", "excludedSet = " + mExcludedApps);
        Prefs.with(getContext()).set(PiaPrefHandler.VPN_PER_APP_PACKAGES, mExcludedApps);
    }

    /**
     * returns if the app is not in the excluded list.
     *
     */
    @Override
    public boolean contains(String name) {
        return mExcludedApps.contains(name);
    }

    @Override
    public void toggleApp(String packageName, String name) {
        if(mExcludedApps != null){
            int resId;
            if (mExcludedApps.contains(packageName)) {
                if(!isSelectedApp(packageName)) {
                    DLog.d("openvpn", "removing from excluded apps " + packageName);
                    mExcludedApps.remove(packageName);
                    resId = R.string.allowed_apps_toast_removed;
                } else {
                    DLog.d("openvpn", "not removing app because its the proxy app");
                    resId = R.string.app_is_proxy;
                }
            } else {
                DLog.d("openvpn", "adding to excluded apps " + packageName);
                mExcludedApps.add(packageName);
                resId = R.string.allowed_apps_toast_added;
            }
            if(toastApp != null)
                toastApp.cancel();

            toastApp = Toaster.s(getContext(), getString(resId, name));
        }
    }

    @Override
    public boolean isProblem(String name) {
        if(mProblemApps == null) {
            mProblemApps = new HashMap<>();
            mProblemApps.put("com.estrongs.android.pop", "ES File explorer");
            mProblemApps.put("com.ape.apps.networkbrowser", "Network Browser");
        }
        return mProblemApps.containsKey(name);
    }

    @Override
    public void appSelected(String name) {
        selectedApp = name;
    }

    @Override
    public boolean isSelectedApp(String name) {
        return name.equals(selectedApp);
    }
}
