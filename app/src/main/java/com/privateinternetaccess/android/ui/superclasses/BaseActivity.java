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

package com.privateinternetaccess.android.ui.superclasses;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;

import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.privateinternetaccess.android.PIAApplication;
import com.privateinternetaccess.android.R;
import com.privateinternetaccess.android.handlers.PurchasingHandler;
import com.privateinternetaccess.android.pia.IPIACallback;
import com.privateinternetaccess.android.pia.PIAFactory;
import com.privateinternetaccess.android.pia.handlers.LogoutHandler;
import com.privateinternetaccess.android.pia.handlers.PIAServerHandler;
import com.privateinternetaccess.android.pia.handlers.PiaPrefHandler;
import com.privateinternetaccess.android.pia.handlers.PingHandler;
import com.privateinternetaccess.android.pia.handlers.ThemeHandler;
import com.privateinternetaccess.android.pia.interfaces.IAccount;
import com.privateinternetaccess.android.pia.model.PIAAccountData;
import com.privateinternetaccess.android.pia.model.PIAServer;
import com.privateinternetaccess.android.pia.model.enums.LoginResponseStatus;
import com.privateinternetaccess.android.pia.model.enums.PurchasingType;
import com.privateinternetaccess.android.pia.model.events.VpnStateEvent;
import com.privateinternetaccess.android.pia.model.response.LoginResponse;
import com.privateinternetaccess.android.pia.services.ExpiryNotificationService;
import com.privateinternetaccess.android.pia.utils.DLog;
import com.privateinternetaccess.android.pia.utils.Toaster;
import com.privateinternetaccess.android.ui.connection.MainActivity;
import com.privateinternetaccess.android.ui.connection.VPNPermissionActivity;
import com.privateinternetaccess.android.ui.loginpurchasing.LoginPurchaseActivity;
import com.privateinternetaccess.android.ui.tv.DashboardActivity;
import com.privateinternetaccess.android.ui.views.SwipeBackLayout;
import com.privateinternetaccess.android.utils.SnoozeUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import de.blinkt.openvpn.core.ConnectionStatus;
import de.blinkt.openvpn.core.VpnStatus;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

import static com.privateinternetaccess.android.ui.loginpurchasing.LoginPurchaseActivity.EXTRA_GOTO_PURCHASING;


/**
 * Created by arne on 02.12.2014.
 */
public abstract class BaseActivity extends SwipeBackBaseActivity {

    protected Toolbar toolbar;

    private TextView tvTitle;
    private Button bTextButton;
    private AppCompatImageView ivIconButton;

    private ImageView ivBackground;

    private boolean showTitle = false;

    private int iconResId = -1;
    private int iconResIdDisconnected = -1;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home){
            onBackPressed();
            overridePendingTransition(R.anim.right_to_left_exit, R.anim.left_to_right_exit);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getDelegate().setLocalNightMode(ThemeHandler.getThemeMode(this));
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            // clear FLAG_TRANSLUCENT_STATUS flag:
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            // add FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS flag to the window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            // finally change the color
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.status_bar_color));
        }
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setBackground();
        PingHandler.getInstance(getApplicationContext()).fetchPings();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.right_to_left_exit, R.anim.left_to_right_exit);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(ivBackground != null){
            ivBackground.setImageDrawable(null);
        }
    }

    protected void onRenewClicked() {
        IAccount account = PIAFactory.getInstance().getAccount(this);
        PIAAccountData pai = account.getAccountInfo();
        boolean logout = false;
        if (!pai.isRenewable()) {
            if (PIAAccountData.PLAN_TRIAL.equals(pai.getPlan())) {
                Toaster.l(this.getApplicationContext(), R.string.error_renew_trial);
                logout = true;
            } else if (pai.isExpired()) {
                Toaster.l(this.getApplicationContext(), R.string.error_renew_expired);
                logout = true;
            } else {
                Toaster.l(this.getApplicationContext(), R.string.account_not_renewable);
            }
        }
        if(logout){
            PurchasingHandler handler = new PurchasingHandler();
            Context context = this;
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(R.string.purchase_renew_title);
            builder.setMessage(R.string.purchase_renew_message);
            if(handler.getType() == PurchasingType.GOOGLE)
                builder.setPositiveButton(R.string.purchase, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        new LogoutHandler(BaseActivity.this, getLogoutCallback()).logoutLogic(true);
                    }
                });
            builder.setNegativeButton(R.string.dismiss, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                }
            });
            builder.show();
        }
    }

    protected void initHeader(boolean showBack, boolean enabledSwipe) {
        toolbar = findViewById(R.id.header_toolbar);
        tvTitle = findViewById(R.id.header_text);
        bTextButton = findViewById(R.id.header_text_button);
        ivIconButton = findViewById(R.id.header_icon_button);
        ivBackground = findViewById(R.id.background);

        setSupportActionBar(toolbar);

        if(getSupportActionBar() != null) {
            if (showBack) {
                getSupportActionBar().setHomeButtonEnabled(true);
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                Drawable upArrow = AppCompatResources.getDrawable(getApplicationContext(), R.drawable.ic_back);
                getSupportActionBar().setHomeAsUpIndicator(upArrow);
            }
            toolbar.setOverflowIcon(AppCompatResources.getDrawable(getApplicationContext(), R.drawable.ic_dark_overflow));
            getSupportActionBar().setTitle("");
        }
        setSwipeBackEnable(enabledSwipe);
        if(!PIAApplication.isChromebook(getApplicationContext())) {
            getSwipeBackLayout().setEdgeTrackingEnabled(SwipeBackLayout.EDGE_LEFT);
        }

        if (bTextButton != null) {
            bTextButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onRightButtonClicked(view);
                }
            });
        }

        if (ivIconButton != null) {
            ivIconButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onIconButtonClicked(view);
                }
            });
        }
    }

    public void onRightButtonClicked(View view) {}
    public void onIconButtonClicked(View view) {}

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void setStatusBarColor(int resId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(getResources().getColor(android.R.color.transparent));
            window.setBackgroundDrawableResource(resId);
        }
    }

    private void setHeaderTints() {
        if(ThemeHandler.getCurrentTheme(this) == ThemeHandler.Theme.NIGHT) {
            setHeaderTints(getResources().getColor(R.color.white));
        }
        else {
            setHeaderTints(getResources().getColor(R.color.grey20));
        }
    }

    private void setHeaderTints(int tintColor) {
        Toolbar toolbar = findViewById(R.id.header_toolbar);
        TextView connectionText = findViewById(R.id.header_connection_status);

        toolbar.getNavigationIcon().setColorFilter(tintColor, PorterDuff.Mode.SRC_ATOP);
        toolbar.getOverflowIcon().setColorFilter(tintColor, PorterDuff.Mode.SRC_ATOP);

        if (tvTitle != null) {
            tvTitle.setTextColor(tintColor);
        }

        if (bTextButton != null) {
            bTextButton.setTextColor(tintColor);
        }

        if (connectionText != null) {
            connectionText.setTextColor(tintColor);
        }
    }

    private void showIcon() {
        if (findViewById(R.id.header_logo) != null) {
            findViewById(R.id.header_logo).setVisibility(View.VISIBLE);
        }
    }

    private void removeIcon(){
        if (findViewById(R.id.header_logo) != null)
            findViewById(R.id.header_logo).setVisibility(View.GONE);
    }

    protected void setBackground() {
        DLog.d("BaseActivity", "Setting background");
        VpnStateEvent event = EventBus.getDefault().getStickyEvent(VpnStateEvent.class);
        ConnectionStatus status = event.getLevel();

        Toolbar toolbar = findViewById(R.id.header_toolbar);
        TextView connectionText = findViewById(R.id.header_connection_status);

        if (toolbar == null) {
            return;
        }

        if (iconResIdDisconnected != -1 && iconResId != -1 && ivIconButton != null) {
            ivIconButton.setImageResource(iconResId);
            DLog.d("BaseActivity", "Setting connected res");
        }

        if (status == ConnectionStatus.LEVEL_CONNECTED) {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.greendark20));

            toolbar.setBackground(getResources().getDrawable(R.drawable.actionbar_gradient));
            connectionText.setVisibility(View.VISIBLE);
            removeIcon();
            setHeaderTints(getResources().getColor(R.color.white));
        }
        else if (status == ConnectionStatus.LEVEL_CONNECTING_NO_SERVER_REPLY_YET || status == ConnectionStatus.LEVEL_CONNECTING_SERVER_REPLIED) {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.connecting_yellow));

            toolbar.setBackground(getResources().getDrawable(R.drawable.actionbar_gradient_connecting));
            connectionText.setVisibility(View.VISIBLE);
            removeIcon();
            setHeaderTints(getResources().getColor(R.color.grey15));
        }
        else if (status == ConnectionStatus.LEVEL_AUTH_FAILED || status == ConnectionStatus.LEVEL_NONETWORK ||
                status == null) {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.failed_start));

            toolbar.setBackground(getResources().getDrawable(R.drawable.actionbar_gradient_failed));
            connectionText.setVisibility(View.VISIBLE);
            removeIcon();
            setHeaderTints(getResources().getColor(R.color.white));
        }
        else if (status == ConnectionStatus.LEVEL_NOTCONNECTED && SnoozeUtils.hasActiveAlarm(this)) {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.connecting_yellow));

            toolbar.setBackground(getResources().getDrawable(R.drawable.actionbar_gradient_connecting));
            connectionText.setVisibility(View.VISIBLE);
            removeIcon();
            setHeaderTints(getResources().getColor(R.color.grey15));
        }
        else {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.windowBackground));

            toolbar.setBackgroundColor(getResources().getColor(R.color.transparent));
            connectionText.setVisibility(View.GONE);
            showIcon();
            setHeaderTints();

            if (iconResIdDisconnected != -1 && iconResId != -1 && ivIconButton != null) {
                ivIconButton.setImageResource(iconResIdDisconnected);
                DLog.d("BaseActivity", "Setting disconnected res");
            }
        }

        int lastStateResId = event.getLocalizedResId();

        if (lastStateResId != 0) {
            if (lastStateResId == de.blinkt.openvpn.R.string.state_waitconnectretry)
                connectionText.setText(VpnStatus.getLastCleanLogMessage(this));
            else if (event.getLevel() == ConnectionStatus.LEVEL_NONETWORK) {
                connectionText.setText(getString(R.string.failed_connect_status));
            }
            else if (event.getLevel() == ConnectionStatus.LEVEL_CONNECTED){
                PIAServer server = PIAServerHandler.getInstance(this).getSelectedRegion(this, false);
                StringBuilder sb = new StringBuilder();
                sb.append(this.getString(R.string.state_connected));
                sb.append(": ");
                sb.append(server.getName());
                connectionText.setText(sb.toString());
            }
            else if (SnoozeUtils.hasActiveAlarm(this) && event.getLevel() == ConnectionStatus.LEVEL_NOTCONNECTED) {
                connectionText.setText(this.getString(R.string.snooze_status));
            }
            else
                connectionText.setText(this.getString(lastStateResId));
        }

        if (showTitle) {
            removeIcon();
            connectionText.setVisibility(View.GONE);
        }
    }

    protected void setGreenBackground(){
        setBackground();
    }

    protected void setSecondaryGreenBackground(){
        View view = findViewById(R.id.activity_secondary_background_green_area);
        if(view != null)
            view.setVisibility(View.VISIBLE);
    }

    protected void showTopExtraArea(){
        View extraArea = findViewById(R.id.activity_secondary_top_card_view);
        if(extraArea instanceof CardView) {
            CardView card = (CardView) extraArea;
            card.setCardBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.windowBackground));
        } else {
        }
        extraArea.setVisibility(View.VISIBLE);
    }

    @Override
    public void setTitle(CharSequence title) {
        super.setTitle("");
        removeIcon();
        if (tvTitle != null) {
            tvTitle.setText(title);
            tvTitle.setVisibility(View.VISIBLE);
            showTitle = true;
        }
    }

    public void setRightButton(String text) {
        if (bTextButton != null) {
            bTextButton.setText(text);
            bTextButton.setVisibility(View.VISIBLE);
        }
    }

    public void setIconButton(int resourceId) {
        if (ivIconButton != null) {
            ivIconButton.setImageResource(resourceId);
            ivIconButton.setVisibility(View.VISIBLE);
        }
    }

    public void setIconButton(int resId, int resIdDisconnected) {
        iconResId = resId;
        iconResIdDisconnected = resIdDisconnected;

        ivIconButton.setImageResource(iconResId);
        ivIconButton.setVisibility(View.VISIBLE);
    }

    protected IPIACallback<Boolean> getLogoutCallback() {
        return new IPIACallback<Boolean>() {
            @Override
            public void apiReturn(Boolean gotoPurchasing) {
                Intent intent = new Intent(BaseActivity.this, LoginPurchaseActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                if (gotoPurchasing) {
                    intent.putExtra(EXTRA_GOTO_PURCHASING, true);
                }
                startActivity(intent);
                finish();
                overridePendingTransition(R.anim.right_to_left_exit, R.anim.left_to_right_exit);
            }
        };
    }

    protected IPIACallback<LoginResponse> getLoginCallback() {
        return new IPIACallback<LoginResponse>() {
            @Override
            public void apiReturn(LoginResponse ai) {
                if(ai.getException() == null) {
                    if(ai.getStatus() == LoginResponseStatus.CONNECTED) {
                        PiaPrefHandler.saveAccountInformation(getApplicationContext(), ai);
                        ExpiryNotificationService.armReminders(getApplicationContext());
                    } else if(ai.getStatus() == LoginResponseStatus.AUTH_FAILED){
                        DLog.d("FetchAccountTask", "ai = " + ai.toString());
                        if(!ai.isActive() && ai.getException() == null) {
                            Toaster.s(getApplicationContext(), R.string.account_termination);
                        } else if(ai.isExpired()) {
                            Toaster.s(getApplicationContext(), R.string.subscription_expired + " " + getString(R.string.timeleft_expired));
                        } else {
                            Toaster.s(getApplicationContext(), R.string.login_operation_failed);
                        }
                        new LogoutHandler(BaseActivity.this, getLogoutCallback()).logoutLogic(true);
                        Intent i = new Intent(getApplicationContext(), LoginPurchaseActivity.class);
                        startActivity(i);
                        finish();
                        overridePendingTransition(R.anim.right_to_left_exit, R.anim.left_to_right_exit);
                    }
                }
            }
        };
    }

    /*
        Attaching Calligraphy to base context for font handling.
     */
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    public void goToMainActivity(){
        Intent vpnIntent = VpnService.prepare(getApplicationContext());
        if (PIAApplication.isAndroidTV(getApplicationContext()) && vpnIntent == null) {
            Intent i = new Intent(getApplicationContext(), DashboardActivity.class);
            startActivity(i);
            overridePendingTransition(R.anim.left_to_right, R.anim.right_to_left);
            finishAffinity();
        }
        else if (vpnIntent == null) {
            Intent i = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(i);
            overridePendingTransition(R.anim.left_to_right, R.anim.right_to_left);
            finishAffinity();
        } else {
            Intent i = new Intent(getApplicationContext(), VPNPermissionActivity.class);
            startActivity(i);
            overridePendingTransition(R.anim.left_to_right, R.anim.right_to_left);
            finishAffinity();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void updateState(VpnStateEvent event) {
        setBackground();
    }
}