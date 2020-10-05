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

package com.privateinternetaccess.android.ui.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.net.VpnService;
import androidx.core.content.ContextCompat;
import android.view.View;
import android.widget.RemoteViews;

import com.privateinternetaccess.android.R;
import com.privateinternetaccess.android.pia.PIAFactory;
import com.privateinternetaccess.android.pia.handlers.PIAServerHandler;
import com.privateinternetaccess.android.pia.handlers.PiaPrefHandler;
import com.privateinternetaccess.android.pia.handlers.PingDelayStartHandler;
import com.privateinternetaccess.android.pia.interfaces.IAccount;
import com.privateinternetaccess.android.pia.interfaces.IVPN;
import com.privateinternetaccess.android.pia.model.events.VpnStateEvent;
import com.privateinternetaccess.android.pia.utils.AppUtilities;
import com.privateinternetaccess.android.pia.utils.Prefs;
import com.privateinternetaccess.android.tunnel.PIAVpnStatus;
import com.privateinternetaccess.android.ui.LauncherActivity;
import com.privateinternetaccess.android.ui.connection.MainActivity;
import com.privateinternetaccess.android.ui.features.LaunchVPNForService;
import com.privateinternetaccess.core.model.PIAServer;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

import de.blinkt.openvpn.core.ConnectionStatus;
import de.blinkt.openvpn.core.OpenVPNManagement;
import de.blinkt.openvpn.core.OpenVPNService;
import de.blinkt.openvpn.core.VpnStatus;

/**
 * Created by half47 on 9/22/16.
 */

public class WidgetBaseProvider extends AppWidgetProvider  {

    protected static final String TAG = "WidgetLongProvider";
    protected static final String WIDGET_ID_KEY = "key_ids";

    protected static final String LAUNCH = "Launch";
    public static final String ATTACH_LISTENER = "AttachListener";
    public static final String WIDGET_TEXT_COLOR = "widgetTextColor";
    public static final String WIDGET_BACKGROUND_COLOR = "widgetBackgroundColor";

    protected Context context;

    protected static String lastUpSpeed;
    protected static String lastDownSpeed;

    protected void setupView(RemoteViews views, WidgetProvider listener, int layoutResId){

        Intent intent = new Intent(context, WidgetProvider.class);
        intent.setAction(LAUNCH);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        int clickId = R.id.widget_area;
        views.setOnClickPendingIntent(clickId, pendingIntent);

        setColors(context, views, layoutResId);
        if(layoutResId == R.layout.widget_main_cc || layoutResId == R.layout.widget_main_text){
            setUpCC(context, views);
        }
        boolean isOnlyText = layoutResId == R.layout.widget_main_text;

        VpnStateEvent event = EventBus.getDefault().getStickyEvent(VpnStateEvent.class);

        // if connecting, show progress
        if(event != null){
            if(event.getLevel() == ConnectionStatus.LEVEL_CONNECTED){
                setRegionDisplay(context, views, true);
                views.setViewVisibility(R.id.widget_progress, View.GONE);
                views.setViewVisibility(R.id.widget_image, View.VISIBLE);
            } else if(event.getLevel() == ConnectionStatus.LEVEL_NOTCONNECTED) {
                setRegionDisplay(context, views, false);
                if(event.getLocalizedResId() == R.string.status_server_ping)
                    views.setTextViewText(R.id.widget_top_text, context.getText(R.string.status_server_ping));
                else
                    views.setTextViewText(R.id.widget_top_text, context.getString(R.string.click_to_connect));
                views.setViewVisibility(R.id.widget_image, View.VISIBLE);
                views.setViewVisibility(R.id.widget_progress, View.GONE);
                if(event.getLevel() != null) {
                    EventBus.getDefault().unregister(listener);
                }
            } else {
                setRegionDisplay(context, views, false);
                if(event.getLocalizedResId() > 0)
                    views.setTextViewText(R.id.widget_top_text, context.getString(event.getLocalizedResId()));
                views.setViewVisibility(R.id.widget_progress, View.VISIBLE);
                views.setViewVisibility(R.id.widget_image, View.INVISIBLE);
            }
        } else {
            views.setTextViewText(R.id.widget_top_text, context.getString(R.string.click_to_connect));
//            setImage(views, R.drawable.flag_world);
            setRegionDisplay(context, views, false);
            views.setViewVisibility(R.id.widget_progress, View.GONE);
        }
    }

    private void setUpCC(Context context, RemoteViews views) {
        Intent i = new Intent(context, WidgetProvider.class);
        i.setAction(MainActivity.CHANGE_VPN_SERVER);
        PendingIntent intent = PendingIntent.getBroadcast(context, 0, i, 0);
        views.setOnClickPendingIntent(R.id.widget_image, intent);

//        DLog.d("WidgetProvider", lastSpeed + "");
        VpnStateEvent event = EventBus.getDefault().getStickyEvent(VpnStateEvent.class);
        if((lastUpSpeed != null || lastDownSpeed != null) && event != null && event.getLevel() == ConnectionStatus.LEVEL_CONNECTED) {
            views.setTextViewText(R.id.widget_speed_up, lastUpSpeed);
            views.setTextViewText(R.id.widget_speed_down, lastDownSpeed);
        } else {
            Resources res= context.getResources();

            views.setTextViewText(R.id.widget_speed_up, String.format(context.getString(R.string.shorthand_bytecount), OpenVPNService.humanReadableByteCount(0, false, res),
                    OpenVPNService.humanReadableByteCount(0 / OpenVPNManagement.mBytecountInterval, true, res)));
            views.setTextViewText(R.id.widget_speed_down, String.format(context.getString(R.string.shorthand_bytecount),
                    OpenVPNService.humanReadableByteCount(0, false, res),
                    OpenVPNService.humanReadableByteCount(0 / OpenVPNManagement.mBytecountInterval, true, res)));
        }
    }

    protected void onReceiveBroadcast(Intent intent, Context context, WidgetProvider listener){
        if(intent != null && intent.getAction() != null && intent.getAction().equals(AppWidgetManager.ACTION_APPWIDGET_UPDATE)){
            AppWidgetManager man = AppWidgetManager.getInstance(context);
            List<Integer> combination = getAllWidgetIds(context, man);
            int[] ids = new int[combination.size()];
            for(int i = 0; i < combination.size(); i++) {
                ids[i] = combination.get(i);
            }
            if(intent.getBooleanExtra(ATTACH_LISTENER, false)) {
                if(!EventBus.getDefault().isRegistered(listener))
                    EventBus.getDefault().register(listener);
            }
            onUpdate(context, man, ids);
            return;
        }

        if(intent != null && intent.getAction() != null) {
            if (intent.getAction().equals(LAUNCH)) {
                IAccount account = PIAFactory.getInstance().getAccount(context);
                if(!account.loggedIn()){
                    Intent i = new Intent(context.getApplicationContext(), LauncherActivity.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    i.setAction(MainActivity.START_VPN_SHORTCUT);
                    context.startActivity(i);
                    return;
                }

                boolean connecting = false;

                VpnStateEvent event = EventBus.getDefault().getStickyEvent(VpnStateEvent.class);
                if (event != null)
                    connecting = event.getLevel() != ConnectionStatus.LEVEL_NOTCONNECTED;

                // make sure that if the user isn't logged in go to the login screen.
                if (!connecting) {
                    EventBus.getDefault().register(listener);

                    VpnStatus.updateStateString("GEN_CONFIG", "", R.string.state_gen_config,
                            ConnectionStatus.LEVEL_START);

                    launchVPN(context);
                } else {
                    IVPN vpn = PIAFactory.getInstance().getVPN(context.getApplicationContext());
                    vpn.stop();
                    lastDownSpeed = null;
                    lastUpSpeed = null;
                    EventBus.getDefault().unregister(listener);
                }
            } else if (intent.getAction().equals(MainActivity.CHANGE_VPN_SERVER)){
                Intent i = new Intent(context, LauncherActivity.class);
                i.setAction(MainActivity.CHANGE_VPN_SERVER);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                MainActivity.LAST_ACTION = "";
                context.startActivity(i);
            }
        }
    }

    void launchVPN(Context context) {
        Intent intent = VpnService.prepare(context);
        if (intent == null) {
            PingDelayStartHandler handler = new PingDelayStartHandler();
            handler.setTimeDiff(0);
            handler.startPings(context);
        } else {
            Intent startVpnIntent = new Intent(Intent.ACTION_MAIN);
            startVpnIntent.setClass(context, LaunchVPNForService.class);
            startVpnIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(startVpnIntent);
        }
    }

    private void setImage(RemoteViews views, int id){
        Bitmap src = BitmapFactory.decodeResource(context.getResources(), id);

        int imageAlpha = Prefs.with(context).get(PiaPrefHandler.WIDGET_ALPHA, 100);
        int alpha = (int) (255f * (imageAlpha / 100f));

        Bitmap image = getCroppedBitmap(src, alpha, 8, 8);

        Bitmap small = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_robot_default);

        views.setImageViewBitmap(R.id.widget_pia_logo, getCroppedBitmap(small, alpha, 0, 0));
        views.setImageViewBitmap(R.id.widget_image, image);
    }

    public Bitmap getCroppedBitmap(Bitmap bitmap, int alpha, int radii1, int radii2) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
                bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());

        canvas.drawARGB(0, 0, 0, 0);

        RectF rectF = new RectF(0, 0, bitmap.getWidth(), bitmap.getHeight());
        paint.setAntiAlias(true);
        paint.setAlpha(alpha);
        canvas.drawRoundRect(rectF, radii1, radii2, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
        return output;
    }

    /**
     * This is something I want to build out into settings for customization
     *
     */
    private void setColors(Context context, RemoteViews views, int resId) {
        Prefs prefs = new Prefs(context);

        int textColor = prefs.get(WIDGET_TEXT_COLOR, ContextCompat.getColor(context, R.color.widget_text_default));
        int backgroundColor = prefs.get(WIDGET_BACKGROUND_COLOR, ContextCompat.getColor(context, R.color.widget_background_default));
        int uploadColor = prefs.get("widgetUploadColor", ContextCompat.getColor(context, R.color.graph_color_up));
        int downloadColor = prefs.get("widgetDownloadColor", ContextCompat.getColor(context, R.color.graph_color_down));


        try {
            int text = Color.argb(Color.alpha(textColor), Color.red(textColor), Color.green(textColor), Color.blue(textColor));
            views.setTextColor(R.id.widget_top_text, text);
            if(resId == R.layout.widget_main_cc || resId == R.layout.widget_main_text) {
                views.setTextColor(R.id.widget_speed_down, text);
                views.setTextColor(R.id.widget_speed_up, text);
            }

            setBackgroundDrawable(context, views, resId, backgroundColor);

            int uploadAlpha = Color.alpha(uploadColor);
            int uploadBack = Color.argb(Color.alpha(uploadColor), Color.red(uploadColor), Color.green(uploadColor), Color.blue(uploadColor));

            Bitmap uploadSource = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_upload_white);
            Bitmap uploadResult = changeBitmapColor(uploadSource, uploadBack, uploadAlpha);
            views.setImageViewBitmap(R.id.widget_speed_up_icon, uploadResult);

            int downloadAlpha = Color.alpha(downloadColor);
            int downloadBack = Color.argb(Color.alpha(downloadColor), Color.red(downloadColor), Color.green(downloadColor), Color.blue(downloadColor));

            Bitmap downloadSource = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_download_white);
            Bitmap downloadResult = changeBitmapColor(downloadSource, downloadBack, downloadAlpha);
            views.setImageViewBitmap(R.id.widget_speed_down_icon, downloadResult);

            uploadSource.recycle();
            downloadSource.recycle();

        } catch(Exception e){
            e.printStackTrace();
        }
    }

    private void setBackgroundDrawable(Context context, RemoteViews views, int resId, int backgroundColor) {
        GradientDrawable drawable = (GradientDrawable) ContextCompat.getDrawable(context, R.drawable.shape_widget_rect);

        int widgetRadius = Prefs.with(context).get("widgetRadius", 8);
        boolean rect = resId == R.layout.widget_main_cc || resId == R.layout.widget_main_long || resId == R.layout.widget_main_text;
        int height = rect ? 250 : 500;

        int width = 500;
        switch (resId){
            case R.layout.widget_main_long:
            case R.layout.widget_main_text:
                width = 750;
                break;
            case R.layout.widget_main_cc:
                width = 1000;
                break;
        }

        drawable.setSize(width, height);
        drawable.setCornerRadius(widgetRadius);
        drawable.setColor(backgroundColor);
        drawable.setDither(true);


        views.setImageViewBitmap(R.id.widget_area_background, AppUtilities.drawableToBitmap(drawable));
    }

    private Bitmap changeBitmapColor(Bitmap sourceBitmap, int color, int alpha) {
        int width = sourceBitmap.getWidth() - 1;
        int height = sourceBitmap.getHeight() - 1;
        Bitmap resultBitmap = Bitmap.createBitmap(sourceBitmap, 0, 0,
                width,
                height);
        resultBitmap.setHasAlpha(true);

        Paint p = new Paint();
        ColorFilter filter = new PorterDuffColorFilter(color, PorterDuff.Mode.MULTIPLY); //Multiple worked but doesn't set alpha.
        p.setColorFilter(filter);
        p.setAlpha(alpha);
        p.setAntiAlias(true);
        p.setFlags(Paint.ANTI_ALIAS_FLAG);

        Canvas canvas = new Canvas(resultBitmap);

        canvas.drawBitmap(resultBitmap, 0, 0, p);

        int colour = (alpha & 0xFF) << 24; //rendering alpha.
        canvas.drawColor(colour, PorterDuff.Mode.DST_IN);

        return resultBitmap;
    }

    private void setRegionDisplay(Context context, RemoteViews views, boolean setText){
        VpnStateEvent event = EventBus.getDefault().getStickyEvent(VpnStateEvent.class);
        PIAServerHandler handler = PIAServerHandler.getInstance(context);
        if (handler.isSelectedRegionAuto(context) && event != null && event.getLevel() == ConnectionStatus.LEVEL_CONNECTED) {
            PIAServer currentServer = PIAVpnStatus.getLastConnectedRegion();
            if (!(event.getLevel() == ConnectionStatus.LEVEL_NOTCONNECTED ||
                    event.getLevel() == ConnectionStatus.LEVEL_AUTH_FAILED) && currentServer != null) {
                int flag = handler.getFlagResource(currentServer);
                if(setText) {
                    String name = currentServer.getName();
                    views.setTextViewText(R.id.widget_top_text, name);
                }
                setImage(views, flag);
            } else {
                setServerName(context, views, setText);
            }
        } else {
            setServerName(context, views, setText);
        }
    }

    private void setServerName(Context context, RemoteViews views, boolean setText) {
        int flag = R.drawable.flag_world;
        String name = context.getString(R.string.automatic_server_selection_main);
        PIAServerHandler handler = PIAServerHandler.getInstance(context);
        PIAServer selectedServer = handler.getSelectedRegion(context, true);
        if (selectedServer != null) {
            flag = handler.getFlagResource(selectedServer);
            name = selectedServer.getName();
        }
        if(setText)
            views.setTextViewText(R.id.widget_top_text, name);

        setImage(views, flag);
    }

    public static void updateWidget(Context context, boolean attachListener){
        AppWidgetManager man = AppWidgetManager.getInstance(context);
        List<Integer> combination = getAllWidgetIds(context, man);

        if(combination.size() > 0) {
            Intent updateIntent = new Intent();
            updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            updateIntent.putExtra(WidgetProvider.WIDGET_ID_KEY, combination.toArray());
            updateIntent.putExtra(ATTACH_LISTENER, attachListener);
            context.sendBroadcast(updateIntent);
        }
    }

    public static void updateCCWidget(Context context, boolean attachListener){
        AppWidgetManager man = AppWidgetManager.getInstance(context);
        List<Integer> combination = getCCWidgetIds(context, man);

        if(combination.size() > 0) {
            Intent updateIntent = new Intent();
            updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            updateIntent.putExtra(WidgetProvider.WIDGET_ID_KEY, combination.toArray());
            updateIntent.putExtra(ATTACH_LISTENER, attachListener);
            context.sendBroadcast(updateIntent);
        }
    }

    public static List<Integer> getAllWidgetIds(Context context, AppWidgetManager awm){
        int[] ids = awm.getAppWidgetIds(new ComponentName(context, WidgetProvider.class));
        int[] longIds = awm.getAppWidgetIds(new ComponentName(context, WidgetLongProvider.class));
        int[] smallIds = awm.getAppWidgetIds(new ComponentName(context, WidgetSmallProvider.class));
        int[] ccIds = awm.getAppWidgetIds(new ComponentName(context, WidgetCCProvider.class));
        int[] textIds = awm.getAppWidgetIds(new ComponentName(context, WidgetTextProvider.class));
        List<Integer> combination = new ArrayList<>();
        for(int i = 0; i < ids.length;i++){
            combination.add(ids[i]);
        }
        for(int i = 0; i < longIds.length;i++){
            combination.add(longIds[i]);
        }
        for(int i =0; i < smallIds.length; i++){
            combination.add(smallIds[i]);
        }
        for(int i =0; i < ccIds.length; i++){
            combination.add(ccIds[i]);
        }
        for(int i =0; i < textIds.length; i++){
            combination.add(textIds[i]);
        }
        return combination;
    }

    public static List<Integer> getCCWidgetIds(Context context, AppWidgetManager awm){
        int[] ccIds = awm.getAppWidgetIds(new ComponentName(context, WidgetCCProvider.class));
        int[] textIds = awm.getAppWidgetIds(new ComponentName(context, WidgetTextProvider.class));
        List<Integer> combination = new ArrayList<>();
        for(int i =0; i < ccIds.length; i++){
            combination.add(ccIds[i]);
        }
        for(int i =0; i < textIds.length; i++){
            combination.add(textIds[i]);
        }
        return combination;
    }

    public boolean areThereCCWidgets(Context context, AppWidgetManager awm){
        int[] ccIds = awm.getAppWidgetIds(new ComponentName(context, WidgetCCProvider.class));
        int[] textIds = awm.getAppWidgetIds(new ComponentName(context, WidgetTextProvider.class));
        return ccIds.length > 0 || textIds.length > 0;
    }
}