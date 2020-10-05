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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import com.privateinternetaccess.android.BuildConfig;
import com.privateinternetaccess.android.R;
import com.privateinternetaccess.android.ui.adapters.pager.StartupPager;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class StartupContainerFragment extends Fragment {

    @BindView(R.id.startup_dots) public LinearLayout llDots;
    private int dotsCount;
    private ImageView[] dots;

    @BindView(R.id.startup_pager) public ViewPager pager;
    private StartupPager viewPager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.activity_startup, container, false);
        ButterKnife.bind(this, view);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initView();
    }

    public void initView() {
        setupPager();
        setupPagerDots();
    }

    private void setupPager() {
        List<StartupFragment> list = new ArrayList<>();

        list.add(StartupFragment.newInstance(getString(R.string.free_trial_devices_header),
                getString(R.string.free_trial_devices_text),
                R.drawable.ic_walkthrough_devices,
                ContextCompat.getColor(getContext(), R.color.transparent)));

        list.add(StartupFragment.newInstance(getString(R.string.startup_region_title),
                getString(R.string.startup_region_message),
                R.drawable.ic_walkthrough_world,
                ContextCompat.getColor(getContext(), R.color.transparent)));

        if(BuildConfig.FLAVOR_store.equals("noinapp")) {
            list.add(StartupFragment.newInstance(getString(R.string.startup_ads_title),
                    getString(R.string.startup_ads_message),
                    R.drawable.ic_walkthrough_protect,
                    ContextCompat.getColor(getContext(), R.color.transparent)));
        }

        list.add(StartupFragment.newInstance(getString(R.string.startup_connection_title),
                getString(R.string.startup_connection_message),
                R.drawable.ic_walkthrough_per_app,
                ContextCompat.getColor(getContext(), R.color.transparent)));

        viewPager = new StartupPager(getChildFragmentManager(), list);
        pager.setAdapter(viewPager);
    }

    private void setupPagerDots() {
        dotsCount = viewPager.getCount();
        dots = new ImageView[dotsCount];

        for (int i = 0; i < dotsCount; i++) {
            dots[i] = new ImageView(getContext());

            dots[i].setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.shape_unselected_startup));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );

            params.setMargins(4, 0, 10, 0);

            llDots.addView(dots[i], params);
        }

        dots[0].setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.shape_selected_startup));

        pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                for (int i = 0; i < dotsCount; i++) {
                    dots[i].setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.shape_unselected_startup));
                }
                dots[position].setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.shape_selected_startup));
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
    }
}
