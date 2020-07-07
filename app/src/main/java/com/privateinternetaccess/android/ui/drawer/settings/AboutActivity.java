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

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.privateinternetaccess.android.BuildConfig;
import com.privateinternetaccess.android.R;
import com.privateinternetaccess.android.pia.handlers.ThemeHandler;
import com.privateinternetaccess.android.ui.superclasses.BaseActivity;

import butterknife.BindView;
import butterknife.ButterKnife;

public class AboutActivity extends BaseActivity {

    @BindView(R.id.about_version) TextView tvVersionInfo;
    @BindView(R.id.about_webView) WebView webview;
    @BindView(R.id.about_gpl) TextView tvGpl;

    @BindView(R.id.about_full_collapse) ImageView ivFull;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_secondary);

        initHeader(true, true);
        setTitle(getString(R.string.menu_about));
        setBackground();
        setSecondaryGreenBackground();
        addSnippetToView();
        ButterKnife.bind(this);
        initView();
    }

    private void addSnippetToView() {
        FrameLayout container = findViewById(R.id.activity_secondary_container);
        View view = getLayoutInflater().inflate(R.layout.snippet_about, container, false);
        container.addView(view);
    }

    private void initView() {
        String version;
        String name = "Openvpn";
        int versionCode = BuildConfig.VERSION_CODE;
        try {
            PackageInfo packageinfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            version = packageinfo.versionName;
            versionCode = packageinfo.versionCode;
            name = getString(R.string.app_name);
        } catch (NameNotFoundException e) {
            version = "error fetching version";
        }

        tvVersionInfo.setText(getString(R.string.version_info, name, version, versionCode, BuildConfig.FLAVOR_store));

        tvGpl.setText(String.format(getString(R.string.license_gpl), "https://s3.amazonaws.com/privateinternetaccess/sources/android-v" + versionCode + ".zip"));

        webview.getSettings().setJavaScriptEnabled(true);
        webview.loadUrl("file:///android_asset/full_licenses.html");

        if(ThemeHandler.getCurrentTheme(this) == ThemeHandler.Theme.NIGHT){
            webview.setWebViewClient(new WebViewClient() {
                public void onPageFinished(WebView view, String url) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                        webview.evaluateJavascript("changeColors();", null);
                    } else {
                        webview.loadUrl("javascript:changeColors();");
                    }
                }
            });
        }

        ivFull.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hideViews(webview, ivFull);
            }
        });
    }

    private void hideViews(View content, ImageView button) {
        boolean visible = content.getVisibility() == View.VISIBLE;
        content.setVisibility(visible ? View.GONE : View.VISIBLE);
        button.setImageResource(visible ? R.drawable.ic_plus : R.drawable.ic_minus);
    }
}