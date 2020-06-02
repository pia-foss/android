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

import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import com.privateinternetaccess.android.R;
import com.privateinternetaccess.android.pia.handlers.ThemeHandler;
import com.privateinternetaccess.android.ui.superclasses.BaseActivity;

public class PrivacyPolicyActivity extends BaseActivity {

    private WebView webview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_secondary);

        initHeader(true, true);
        setTitle(getString(R.string.about_privacy_policy));
        setGreenBackground();
        setSecondaryGreenBackground();
        addSnippetToView();
        initView();
    }

    private void addSnippetToView() {
        FrameLayout container = findViewById(R.id.activity_secondary_container);
        webview = new WebView(this);
        container.addView(webview);
    }

    private void initView() {
        webview.getSettings().setJavaScriptEnabled(true);
        webview.loadUrl("file:///android_asset/privacy_policy.html");

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
    }
}