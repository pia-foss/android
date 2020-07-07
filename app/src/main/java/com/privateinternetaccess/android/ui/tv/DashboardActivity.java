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

package com.privateinternetaccess.android.ui.tv;

import android.content.Intent;
import android.net.VpnService;
import android.animation.ValueAnimator;
import android.os.Build;
import android.os.Bundle;
import android.transition.Fade;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;

import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.Guideline;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.privateinternetaccess.android.R;
import com.privateinternetaccess.android.pia.PIAFactory;
import com.privateinternetaccess.android.pia.handlers.LogoutHandler;
import com.privateinternetaccess.android.pia.handlers.PiaPrefHandler;
import com.privateinternetaccess.android.pia.interfaces.IVPN;
import com.privateinternetaccess.android.pia.utils.Prefs;
import com.privateinternetaccess.android.ui.LauncherActivity;
import com.privateinternetaccess.android.ui.connection.ConnectFragment;
import com.privateinternetaccess.android.ui.connection.MainActivity;
import com.privateinternetaccess.android.ui.connection.VPNPermissionActivity;
import com.privateinternetaccess.android.ui.drawer.ServerListFragment;
import com.privateinternetaccess.android.ui.loginpurchasing.LoginPurchaseActivity;
import com.privateinternetaccess.android.ui.superclasses.BaseActivity;
import com.privateinternetaccess.android.ui.tv.fragments.FavoritesFragment;
import com.privateinternetaccess.android.ui.tv.fragments.PanelFragment;
import com.privateinternetaccess.android.ui.tv.fragments.PerAppFragment;
import com.privateinternetaccess.android.ui.tv.fragments.SearchFragment;
import com.privateinternetaccess.core.utils.IPIACallback;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.privateinternetaccess.android.pia.handlers.PiaPrefHandler.KILLSWITCH;

public class DashboardActivity extends BaseActivity {

    private static final int PANEL_ANIMATION_LENGTH = 200;
    private static final float PANEL_OPEN_PERCENT = 0.75f;
    private static final float PANEL_CLOSED_PERCENT = 1.0f;

    private ConnectFragment connectFragment;
    private ServerListFragment serverFragment;
    private PanelFragment panelFragment;

    @BindView(R.id.dashboard_guideline) Guideline fragmentGuideline;

    @BindView(R.id.fragment_dashboard_panel) FrameLayout panelFrame;

    private LogoutHandler mLogoutAssitance;

    private boolean isPanelOpen = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        ButterKnife.bind(this);

        init();
    }

    @Override
    protected void onDestroy() {
        if (mLogoutAssitance != null) {
            mLogoutAssitance.onDestroy();
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (!isPanelOpen) {
            openWidgetPanel();
        }

        super.onBackPressed();
    }

    private void init() {
        connectFragment = new ConnectFragment();
        serverFragment = new ServerListFragment();
        panelFragment = new PanelFragment();

        FragmentTransaction cTransaction = getSupportFragmentManager().beginTransaction();
        cTransaction.replace(R.id.fragment_dashboard_connect, connectFragment);
        cTransaction.commit();

        FragmentTransaction sTransaction = getSupportFragmentManager().beginTransaction();
        sTransaction.replace(R.id.fragment_server_list, serverFragment);
        sTransaction.commit();

        FragmentTransaction pTransaction = getSupportFragmentManager().beginTransaction();
        pTransaction.replace(R.id.fragment_dashboard_panel, panelFragment);
        pTransaction.commit();
    }

    public void showAllowedAppsFragment() {
        Fragment frag = new PerAppFragment();
        FragmentTransaction trans = getSupportFragmentManager().beginTransaction();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Fade exitFade = new Fade();
            exitFade.setDuration(PANEL_ANIMATION_LENGTH);
            serverFragment.setExitTransition(exitFade);

            Fade enterFade = new Fade();
            enterFade.setDuration(PANEL_ANIMATION_LENGTH);
            frag.setEnterTransition(enterFade);
        }

        trans.replace(R.id.fragment_server_list, frag);
        trans.addToBackStack(null);
        trans.commit();

        closeWidgetPanel();
    }

    public void showFavoritesFragment() {
        Fragment frag = new FavoritesFragment();
        FragmentTransaction trans = getSupportFragmentManager().beginTransaction();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Fade exitFade = new Fade();
            exitFade.setDuration(PANEL_ANIMATION_LENGTH);
            serverFragment.setExitTransition(exitFade);

            Fade enterFade = new Fade();
            enterFade.setDuration(PANEL_ANIMATION_LENGTH);
            frag.setEnterTransition(enterFade);
        }

        trans.replace(R.id.fragment_server_list, frag);
        trans.addToBackStack(null);
        trans.commit();

        closeWidgetPanel();
    }

    public void showSearchFragment() {
        Fragment frag = new SearchFragment();
        FragmentTransaction trans = getSupportFragmentManager().beginTransaction();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Fade exitFade = new Fade();
            exitFade.setDuration(PANEL_ANIMATION_LENGTH);
            serverFragment.setExitTransition(exitFade);

            Fade enterFade = new Fade();
            enterFade.setDuration(PANEL_ANIMATION_LENGTH);
            frag.setEnterTransition(enterFade);
        }

        trans.replace(R.id.fragment_server_list, frag);
        trans.addToBackStack(null);
        trans.commit();

        closeWidgetPanel();
    }

    public void showKillSwitchWarning() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && PiaPrefHandler.isKillswitchEnabled(this)) {
//            Prefs prefs = new Prefs(this);
//            prefs.set(KILLSWITCH, false);
//
//            AlertDialog.Builder builder = new AlertDialog.Builder(this);
//            builder.setTitle(R.string.launcher_name);
//            builder.setCancelable(false);
//            builder.setMessage(R.string.ks_removal_warning);
//            builder.setPositiveButton(android.R.string.ok, null);
//
//            builder.show();
//        }
    }

    public void logout() {
        mLogoutAssitance = new LogoutHandler(this, getLogoutCallback());
        mLogoutAssitance.logout();
    }


    public void openWidgetPanel() {
        ConstraintLayout.LayoutParams lp = (ConstraintLayout.LayoutParams)fragmentGuideline.getLayoutParams();
        animatePanel(lp.guidePercent, PANEL_OPEN_PERCENT, lp);
        isPanelOpen = true;
        panelFrame.setVisibility(View.VISIBLE);
    }


    public void closeWidgetPanel() {
        ConstraintLayout.LayoutParams lp = (ConstraintLayout.LayoutParams)fragmentGuideline.getLayoutParams();
        animatePanel(lp.guidePercent, PANEL_CLOSED_PERCENT, lp);
        isPanelOpen = false;
        panelFrame.setVisibility(View.GONE);
    }

    private void animatePanel(float start, float end, final ConstraintLayout.LayoutParams params) {
        ValueAnimator valueAnimator = ValueAnimator.ofFloat(start, end);
        valueAnimator.setDuration(PANEL_ANIMATION_LENGTH);
        valueAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                params.guidePercent = (float)valueAnimator.getAnimatedValue();
                fragmentGuideline.setLayoutParams(params);
            }
        });

        valueAnimator.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkAutoConnect();
        showKillSwitchWarning();
    }

    private void checkAutoConnect() {
        boolean autoConnect = PiaPrefHandler.doAutoConnect(getApplicationContext())
                && !Prefs.with(getApplicationContext()).getBoolean(LauncherActivity.HAS_AUTO_STARTED);
        IVPN vpn = PIAFactory.getInstance().getVPN(getApplicationContext());
        if(autoConnect && !vpn.isVPNActive()){
            Prefs.with(getApplicationContext()).set(LauncherActivity.HAS_AUTO_STARTED, true);

            Intent intent = VpnService.prepare(getApplicationContext());
            if (intent != null) {
                Intent i = new Intent(getApplicationContext(), VPNPermissionActivity.class);
                i.putExtra(MainActivity.START_VPN_SHORTCUT, true);
                overridePendingTransition(R.anim.launcher_enter, R.anim.launcher_exit);
                startActivityForResult(i, MainActivity.START_VPN_PROFILE);
            } else {
                onActivityResult(MainActivity.START_VPN_PROFILE, RESULT_OK, null);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == MainActivity.START_VPN_PROFILE && resultCode == RESULT_OK){
            IVPN vpn = PIAFactory.getInstance().getVPN(getApplicationContext());
            vpn.start();
        }
    }

    protected IPIACallback<Boolean> getLogoutCallback() {
        return new IPIACallback<Boolean>() {
            @Override
            public void apiReturn(Boolean gotoPurchasing) {
                Intent intent = new Intent(DashboardActivity.this, LoginPurchaseActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
                overridePendingTransition(R.anim.right_to_left_exit, R.anim.left_to_right_exit);
            }
        };
    }
}
