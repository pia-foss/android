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

package com.privateinternetaccess.android.ui.connection;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import android.view.View;
import android.widget.TextView;

import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.holder.DimenHolder;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.OnPostBindViewListener;
import com.privateinternetaccess.android.BuildConfig;
import com.privateinternetaccess.android.PIAApplication;
import com.privateinternetaccess.android.R;
import com.privateinternetaccess.android.model.draweritems.PIAPrimaryDrawerItem;
import com.privateinternetaccess.android.model.states.VPNProtocol;
import com.privateinternetaccess.android.pia.PIAFactory;
import com.privateinternetaccess.android.pia.handlers.PiaPrefHandler;
import com.privateinternetaccess.android.pia.handlers.ThemeHandler;
import com.privateinternetaccess.android.pia.interfaces.IAccount;
import com.privateinternetaccess.android.pia.model.AccountInformation;

import java.util.LinkedList;

/**
 * Created by half47 on 4/27/17.
 *
 * Code overflow area for MainActivity. Shorting our files will make the code somewhat easier to maintain.
 */

public class MainActivityHandler {

    public static final int IDEN_REGION_SELECTION = 1000;
    public static final int IDEN_ACCOUNT = 2000;
    public static final int IDEN_SETTINGS = 3000;
    public static final int IDEN_LOGOUT = 4000;
    public static final int IDEN_HOME_PAGE = 5000;
    public static final int IDEN_HELP = 6000;
    public static final int IDEN_FOOTER = 7000;
    public static final long IDEN_ABOUT = 8000;
    public static final long IDEN_PER_APP = 9000;
    public static final int IDEN_RENEW = 10000;
    public static final int IDEN_PRIVACY = 11000;

    public static Drawer createDrawer(Activity act, Toolbar toolbar, Drawer.OnDrawerItemClickListener itemClickListener){
        String version = BuildConfig.VERSION_NAME;
        int versionCode = BuildConfig.VERSION_CODE;

        ThemeHandler.Theme theme = ThemeHandler.getCurrentTheme(act);

        boolean isAndroidTv = PIAApplication.isAndroidTV(act);

        int textColor = theme == ThemeHandler.Theme.DAY ? R.color.textColorPrimary : R.color.textColorPrimaryDark;

        // Create the AccountHeader
        AccountHeader headerResult = new AccountHeaderBuilder()
                .withActivity(act)
                .withAccountHeader(R.layout.drawer_header_new)
                .addProfiles(
                        new ProfileDrawerItem()
                                .withName(PiaPrefHandler.getLogin(act).toUpperCase())
                                .withEmail(String.format(act.getString(R.string.drawer_version), version, versionCode + ""))
                                .withIcon(R.drawable.drawer_icon)
                )
                .withDividerBelowHeader(false)
                .withProfileImagesClickable(false)
                .build();
        IAccount account = PIAFactory.getInstance().getAccount(act);
        AccountInformation accountInformation = account.persistedAccountInformation();
        String upper = getExpiresText(act, accountInformation);

        final LinkedList<IDrawerItem> drawerItems = new LinkedList<>();
        if (accountInformation.getShowExpire()) {
            drawerItems.add(new PIAPrimaryDrawerItem()
                    .withIdentifier(IDEN_RENEW)
                    .withName(upper)
                    .withIcon(R.drawable.ic_orange_arrow_circle)
                    .withDescription(R.string.update_account)
                    .withTextColorRes(R.color.textColorPrimary)
                    .withDescriptionTextColorRes(R.color.textColorPrimaryDark)
                    .withPostOnBindViewListener(new OnPostBindViewListener() {
                        @Override
                        public void onBindView(IDrawerItem drawerItem, View view) {
                            view.setBackgroundResource(R.color.connecting_orange);
                            ((TextView) view.findViewById(R.id.material_drawer_name)).setTextSize(12);
                            ((TextView) view.findViewById(R.id.material_drawer_description)).setTextSize(10);
                        }
                    }));
        }

        drawerItems.add(new PIAPrimaryDrawerItem()
                .withIdentifier(IDEN_REGION_SELECTION)
                .withName(R.string.drawer_region_selection)
                .withIcon(R.drawable.ic_drawer_region)
                .withTextColorRes(textColor)
        );
        if(!isAndroidTv)
            drawerItems.add(new PIAPrimaryDrawerItem()
                    .withIdentifier(IDEN_ACCOUNT).withName(R.string.drawer_account)
                    .withIcon(R.drawable.ic_drawer_account)
                            .withTextColorRes(textColor)
                    );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            drawerItems.add(new PIAPrimaryDrawerItem()
                    .withIdentifier(IDEN_PER_APP)
                    .withName(R.string.per_app_settings)
                    .withIcon(R.drawable.ic_drawer_per_app)
                            .withTextColorRes(textColor)
                    );
        }

        drawerItems.add(new PIAPrimaryDrawerItem()
                .withIdentifier(IDEN_SETTINGS).withName(R.string.menu_settings).withIcon(R.drawable.ic_drawer_settings)
                        .withTextColorRes(textColor)
                );
        drawerItems.add(new PIAPrimaryDrawerItem()
                .withIdentifier(IDEN_LOGOUT).withName(R.string.logout).withIcon(R.drawable.ic_drawer_logout)
                        .withTextColorRes(textColor)
                );

        if(!isAndroidTv) {
            drawerItems.add(new DividerDrawerItem());
            drawerItems.add(new PIAPrimaryDrawerItem()
                    .withIdentifier(IDEN_ABOUT).withName(R.string.drawer_about).withIcon(R.drawable.ic_drawer_about)
                    .withTextColorRes(textColor)
            );
            drawerItems.add(new PIAPrimaryDrawerItem()
                    .withIdentifier(IDEN_PRIVACY).withName(R.string.about_privacy_policy).withIcon(R.drawable.ic_privacy_link)
                    .withTextColorRes(textColor)
            );
            drawerItems.add(new PIAPrimaryDrawerItem()
                    .withIdentifier(IDEN_HOME_PAGE).withName(R.string.drawer_home_page).withIcon(R.drawable.ic_drawer_homepage)
                    .withTextColorRes(textColor)
            );
            drawerItems.add(new PIAPrimaryDrawerItem()
                    .withIdentifier(IDEN_HELP).withName(R.string.drawer_contact_support).withIcon(R.drawable.ic_drawer_support)
                    .withTextColorRes(textColor)
            );
        }

        DimenHolder holder = new DimenHolder();
        holder.setDp(160);

        Drawer mDrawer = new DrawerBuilder()
                .withActivity(act)
                .withToolbar(toolbar)
                .withAccountHeader(headerResult)
                .withStickyFooterDivider(false)
                .withStickyFooterShadow(false)
                .withHeaderHeight(holder)
                .withDrawerItems(drawerItems)
                .withOnDrawerItemClickListener(itemClickListener)
                .build();

        mDrawer.deselect();
        // This code closes drawer without animation
        // mDrawer.getDrawerLayout().closeDrawer(mDrawer.getDrawerLayout().getForegroundGravity(), false);

        return mDrawer;
    }

    @NonNull
    private static String getExpiresText(Context context, AccountInformation accountInformation) {

        long time = accountInformation.getTimeLeftMs();
        long seconds = time / 1000;// to seconds
        int minutes = (int) (seconds / 60); // to minutes;
        int hours = minutes / 60; // to hours
        int days = hours / 24; // to days

        String timeRes = getTimeLeft(context, accountInformation);
        int resId = R.string.subscription_expires_in;
        if(days < 0 && hours < 0 && minutes < 0){
            resId = R.string.subscription_expired;
        }
        String finalStr = context.getString(resId) + " " + timeRes;
        return finalStr.toUpperCase();
    }

    private static String getTimeLeft(Context context, AccountInformation accountInformation) {
        long time = accountInformation.getTimeLeftMs();
        long seconds = time / 1000;// to seconds
        int minutes = (int) (seconds / 60); // to minutes;
        int hours = minutes / 60; // to hours
        int days = hours / 24; // to days

        if (days > 0) {
            return context.getResources().getString(days > 1 ? R.string.days_left : R.string.one_day_left, days, days);
        } else if (hours > 0) {
            return context.getResources().getString(hours > 1 ? R.string.hours_left : R.string.one_hour_left , hours, hours);
        } else if (minutes > 0) {
            return context.getResources().getString(minutes > 1 ? R.string.minutes_left : R.string.one_minute_left, minutes, minutes);
        } else {
            return context.getString(R.string.timeleft_expired);
        }
    }

    private String cleanUpPlan(Context context, String plan) {
        String cleanedPlan = plan;
        if (AccountInformation.PLAN_TRIAL.equals(plan)) {
            cleanedPlan = context.getString(R.string.account_trail);
        } else if (AccountInformation.PLAN_YEARLY.equals(plan)) {
            cleanedPlan = context.getString(R.string.account_yearly);
        } else if (AccountInformation.PLAN_MONTHLY.equals(plan)) {
            cleanedPlan = context.getString(R.string.account_montly);
        }
        return cleanedPlan;
    }
}