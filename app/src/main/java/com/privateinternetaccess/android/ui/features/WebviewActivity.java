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

package com.privateinternetaccess.android.ui.features;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.privateinternetaccess.android.R;
import com.privateinternetaccess.android.pia.handlers.PiaPrefHandler;
import com.privateinternetaccess.android.pia.utils.DLog;
import com.privateinternetaccess.android.pia.utils.Toaster;
import com.privateinternetaccess.android.ui.superclasses.BaseActivity;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by half47 on 7/20/16.
 */
public class WebviewActivity extends BaseActivity {

    public static final String EXTRA_URL = "url";

    private static final String[] WHITELIST_URLS = {
            "https://www.privateinternetaccess.com",
            "https://www.privateinternetaccess.com/helpdesk/",
            "https://www.privateinternetaccess.com/pages/privacy-policy/",
            "https://www.privateinternetaccess.com/pages/terms-of-service/",
            "https://www.privateinternetaccess.com/pages/buy-vpn/",
            "https://bra.privateinternetaccess.com/pages/buy-vpn/",
            "https://www.privateinternetaccess.com/helpdesk/new-ticket/",
            "https://www.privateinternetaccess.com/blog/wireguide-all-about-the-wireguard-vpn-protocol/",
            "https://www.privateinternetaccess.com/helpdesk/kb/articles/removing-openvpn-handshake-and-authentication-settings/"
            };

    private WebView mWebView;
    private SwipeRefreshLayout swipeRefreshLayout;

    private String mURL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);
        if(savedInstanceState != null){
            mURL = savedInstanceState.getString(EXTRA_URL);
        } else {
            mURL = getIntent().getStringExtra(EXTRA_URL);
        }

        initHeader(true, true);
        bindView();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(EXTRA_URL, mURL);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void bindView() {
        mWebView = findViewById(R.id.webview_webview);
        swipeRefreshLayout = findViewById(R.id.swipe_to_refresh);

        mWebView.setWebChromeClient(new WebChromeClient(){
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                if(newProgress < 95){
                    swipeRefreshLayout.setRefreshing(true);
                } else {
                    swipeRefreshLayout.setRefreshing(false);
                }
            }

        });

        swipeRefreshLayout.setColorSchemeColors(ContextCompat.getColor(getApplicationContext(), R.color.pia_gen_green));
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mWebView.loadUrl(mWebView.getUrl());
            }
        });
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    mURL = request.getUrl().toString();
                } else {
                    mURL = mWebView.getUrl();
                }

                if(PiaPrefHandler.getWebviewTesting(getApplicationContext()))
                    mURL = PiaPrefHandler.getWebviewTestingSite(getApplicationContext());

                DLog.d("WebViewActivity", "url = " + mURL);
                try {
                    URL url  = new URL(mURL);
                    DLog.d("WebViewActivity", "url = " + url.getProtocol());
                    if(url.getProtocol().equals("https")) {
                        view.loadUrl(mURL);
                        return super.shouldOverrideUrlLoading(view, request);
                    } else {
                        Toaster.s(getApplicationContext(), getString(R.string.website_isnt_protected));
                        return true;
                    }
                } catch (MalformedURLException e) {
                    Toaster.s(getApplicationContext(), getString(R.string.website_isnt_protected));
                    return true;
                }
            }
        });

        mWebView.getSettings().setJavaScriptEnabled(true);
        setBackground();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.website, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.website_open:
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(mWebView.getUrl()));
                startActivity(i);
                break;
            case R.id.website_link:
                copyTextToClipboard(mWebView.getUrl());
                break;
            case R.id.website_refresh:
                mWebView.loadUrl(mWebView.getUrl());
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(TextUtils.isEmpty(mURL))
            mURL = "https://www.privateinternetaccess.com/helpdesk/";

        if(mWebView.getUrl() == null) {
            if (isWhitelisted(mURL))
                mWebView.loadUrl(mURL);
        }
    }

    @Override
    public void onBackPressed() {
        if(mWebView.canGoBack()){
            mWebView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    private void copyTextToClipboard(String copyText) {
        boolean copied = false;
        try {
            if(!TextUtils.isEmpty(copyText)) {
                android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                android.content.ClipData clip = android.content.ClipData.newPlainText("pia_clip", copyText);
                clipboard.setPrimaryClip(clip);
                Toaster.s(getApplicationContext(), R.string.link_copied_to_clipboard);
                copied = true;
            }
        } catch (Resources.NotFoundException e) {
            copied = false;
        }

        if(!copied)
            Toaster.s(getApplicationContext(), R.string.text_failed_to_copy);
    }

    private boolean isWhitelisted(String url) {
        for (String whitelistUrl : WHITELIST_URLS) {
            if (url.equals(whitelistUrl))
                return true;
        }

        return false;
    }
}