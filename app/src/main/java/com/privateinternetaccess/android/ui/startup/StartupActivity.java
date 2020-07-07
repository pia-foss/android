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

package com.privateinternetaccess.android.ui.startup;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.privateinternetaccess.android.PIAApplication;
import com.privateinternetaccess.android.R;
import com.privateinternetaccess.android.pia.PIAFactory;
import com.privateinternetaccess.android.pia.interfaces.IAccount;
import com.privateinternetaccess.android.pia.utils.Prefs;
import com.privateinternetaccess.android.ui.adapters.pager.StartupPager;
import com.privateinternetaccess.android.ui.connection.MainActivity;
import com.privateinternetaccess.android.ui.loginpurchasing.LoginPurchaseActivity;
import com.privateinternetaccess.android.ui.tv.DashboardActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by half47 on 4/25/17.
 */

public class StartupActivity extends AppCompatActivity {

    public static final String HAS_SEEN_STARTUP = "hasSeenStartup";

    private LinearLayout llDots;
    private int dotsCount;
    private ImageView[] dots;


    private View bSkip;
    private TextView tvNext;

    private ViewPager pager;
    private StartupPager viewPager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_startup);

        Prefs.with(this).set(HAS_SEEN_STARTUP, true);

        bindView();
        setupPager();
        setupPagerDots();
        initViews();
    }

    private void bindView() {
        llDots = findViewById(R.id.startup_dots);

        tvNext = findViewById(R.id.startup_next);
        bSkip = findViewById(R.id.startup_skip);

        pager = findViewById(R.id.startup_pager);
    }

    private void initViews() {
        if(PIAApplication.isAndroidTV(getApplicationContext())){
            bSkip.setVisibility(View.INVISIBLE);
            tvNext.setVisibility(View.VISIBLE);
            tvNext.setText(R.string.tv_startup_press_right);
        }

        bSkip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToNextActivity();
            }
        });
        tvNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int selected = pager.getCurrentItem();
                if(selected + 1 <= 3) {
                    pager.setCurrentItem(++selected, true);
                } else {
                    goToNextActivity();
                }
            }
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(PIAApplication.isAndroidTV(getApplicationContext())){
            if(keyCode == KeyEvent.KEYCODE_DPAD_RIGHT || keyCode == KeyEvent.KEYCODE_DPAD_CENTER){
                int selected = pager.getCurrentItem();
                if(selected + 1 <= 3) {
                    pager.setCurrentItem(++selected, true);
                } else {
                    goToNextActivity();
                }
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    private void setupPager() {
        List<StartupFragment> list = new ArrayList<>();



        list.add(StartupFragment.newInstance(getString(R.string.free_trial_devices_header),
                getString(R.string.free_trial_devices_text),
                R.drawable.ic_walkthrough_devices,
                ContextCompat.getColor(getApplicationContext(), R.color.transparent),
                ContextCompat.getColor(getApplicationContext(), R.color.pia_text_dark_primary),
                ContextCompat.getColor(getApplicationContext(), R.color.pia_text_dark_secondary)));

        list.add(StartupFragment.newInstance(getString(R.string.startup_region_title),
                getString(R.string.startup_region_message),
                R.drawable.ic_walkthrough_world,
                ContextCompat.getColor(getApplicationContext(), R.color.transparent),
                ContextCompat.getColor(getApplicationContext(), R.color.pia_text_dark_primary),
                ContextCompat.getColor(getApplicationContext(), R.color.pia_text_dark_secondary)));

        list.add(StartupFragment.newInstance(getString(R.string.startup_ads_title),
                getString(R.string.startup_ads_message),
                R.drawable.ic_walkthrough_protect,
                ContextCompat.getColor(getApplicationContext(), R.color.transparent),
                ContextCompat.getColor(getApplicationContext(), R.color.pia_text_dark_primary),
                ContextCompat.getColor(getApplicationContext(), R.color.pia_text_dark_secondary)));

        list.add(StartupFragment.newInstance(getString(R.string.startup_connection_title),
                getString(R.string.startup_connection_message),
                R.drawable.ic_walkthrough_per_app,
                ContextCompat.getColor(getApplicationContext(), R.color.transparent),
                ContextCompat.getColor(getApplicationContext(), R.color.pia_text_dark_primary),
                ContextCompat.getColor(getApplicationContext(), R.color.pia_text_dark_secondary)));

        viewPager = new StartupPager(getSupportFragmentManager(), list);
        pager.setAdapter(viewPager);
    }

    private void goToNextActivity() {
        IAccount account = PIAFactory.getInstance().getAccount(this);
        if(!account.isLoggedIn()) {
            Intent i = new Intent(getApplicationContext(), LoginPurchaseActivity.class);
            startActivity(i);
            overridePendingTransition(R.anim.left_to_right, R.anim.right_to_left);
            finish();
        }
        else if (PIAApplication.isAndroidTV(getApplicationContext())) {
            Intent i = new Intent(getApplicationContext(), DashboardActivity.class);
            startActivity(i);
            overridePendingTransition(R.anim.left_to_right, R.anim.right_to_left_exit);
            finish();
        }
        else {
            Intent i = new Intent(getApplicationContext(), MainActivity.class);
            overridePendingTransition(R.anim.left_to_right, R.anim.right_to_left);
            startActivity(i);
            finish();
        }
    }


    private void setupPagerDots() {
        dotsCount = viewPager.getCount();
        dots = new ImageView[dotsCount];

        for (int i = 0; i < dotsCount; i++) {
            dots[i] = new ImageView(this);

            dots[i].setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.shape_unselected_startup));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );

            params.setMargins(4, 0, 10, 0);

            llDots.addView(dots[i], params);
        }

        dots[0].setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.shape_selected_startup));

        pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                for (int i = 0; i < dotsCount; i++) {
                    dots[i].setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.shape_unselected_startup));
                }
                dots[position].setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.shape_selected_startup));
                boolean isTV = PIAApplication.isAndroidTV(bSkip.getContext());
                if(position >= 3){
                    tvNext.setText(!isTV ? R.string.done : R.string.tv_startup_press_right);
                    bSkip.setVisibility(View.INVISIBLE);
                } else {
                    tvNext.setText(!isTV ? R.string.next : R.string.tv_startup_press_right);
                    if(!PIAApplication.isAndroidTV(bSkip.getContext()))
                        bSkip.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
    }
}