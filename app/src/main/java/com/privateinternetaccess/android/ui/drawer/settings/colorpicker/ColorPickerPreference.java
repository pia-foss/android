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

package com.privateinternetaccess.android.ui.drawer.settings.colorpicker;


import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;
import android.util.AttributeSet;

import com.jrummyapps.android.colorpicker.ColorPanelView;
import com.jrummyapps.android.colorpicker.ColorPickerDialog.DialogType;
import com.jrummyapps.android.colorpicker.ColorPickerDialogListener;
import com.jrummyapps.android.colorpicker.ColorShape;
import com.privateinternetaccess.android.R;
import com.privateinternetaccess.android.pia.utils.DLog;

/**
 * Created by hfrede on 5/31/17.
 */

public class ColorPickerPreference extends Preference implements ColorPickerDialogListener {

    private static final int SIZE_NORMAL = 0;
    private static final int SIZE_LARGE = 1;

    private OnShowDialogListener onShowDialogListener;
    private int color = Color.BLACK;
    private boolean showDialog;
    @DialogType
    private int dialogType;
    private int colorShape;
    private boolean allowPresets;
    private boolean allowCustom;
    private boolean showAlphaSlider;
    private boolean showColorShades;
    private int previewSize;
    private int[] presets;
    private int dialogTitle;
    private Activity activity;

    public static ColorPickerDialog dialog;
    public static String key;

    public ColorPickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public ColorPickerPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        setPersistent(true);
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.ColorPreference);
        showDialog = a.getBoolean(R.styleable.ColorPreference_cpv_showDialog, true);
        //noinspection WrongConstant
        dialogType = a.getInt(R.styleable.ColorPreference_cpv_dialogType, ColorPickerDialog.TYPE_PRESETS);
        colorShape = a.getInt(R.styleable.ColorPreference_cpv_colorShape, ColorShape.CIRCLE);
        allowPresets = a.getBoolean(R.styleable.ColorPreference_cpv_allowPresets, true);
        allowCustom = a.getBoolean(R.styleable.ColorPreference_cpv_allowCustom, true);
        showAlphaSlider = a.getBoolean(R.styleable.ColorPreference_cpv_showAlphaSlider, false);
        showColorShades = a.getBoolean(R.styleable.ColorPreference_cpv_showColorShades, true);
        previewSize = a.getInt(R.styleable.ColorPreference_cpv_previewSize, SIZE_NORMAL);
        final int presetsResId = a.getResourceId(R.styleable.ColorPreference_cpv_colorPresets, 0);
        dialogTitle = a.getResourceId(R.styleable.ColorPreference_cpv_dialogTitle, R.string.cpv_default_title);

//        int dColor = ContextCompat.getColor(getContext(), com.privateinternetaccess.android.R.color.white);
//        switch (getKey()){
//            case "widgetBackgroundColor":
//                dColor = ContextCompat.getColor(getContext(), com.privateinternetaccess.android.R.color.widget_background_default);
//                break;
//            case "widgetTextColor":
//                dColor = ContextCompat.getColor(getContext(), com.privateinternetaccess.android.R.color.widget_text_default);
//                break;
//            case "widgetUploadColor":
//                dColor = ContextCompat.getColor(getContext(), com.privateinternetaccess.android.R.color.widget_upload_default);
//                break;
//            case "widgetDownloadColor":
//                dColor = ContextCompat.getColor(getContext(), com.privateinternetaccess.android.R.color.widget_download_default);
//                break;
//        }
//        color = dColor;
//        setDefaultValue(color);
        if (presetsResId != 0) {
            presets = getContext().getResources().getIntArray(presetsResId);
        } else {
            presets = ColorPickerDialog.MATERIAL_COLORS;
        }
        if (colorShape == ColorShape.CIRCLE) {
            setWidgetLayoutResource(
                    previewSize == SIZE_LARGE ? R.layout.cpv_preference_circle_large : R.layout.cpv_preference_circle);
        } else {
            setWidgetLayoutResource(
                    previewSize == SIZE_LARGE ? R.layout.cpv_preference_square_large : R.layout.cpv_preference_square
            );
        }
        a.recycle();
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        ColorPanelView preview = holder.itemView.findViewById(R.id.cpv_preference_preview_color_panel);
        if (preview != null) {
            DLog.d("ColorPicker", "onBind = " + color);
            preview.setColor(color);
        }
        if (dialog != null && key != null && key.equals(getKey())) {
            dialog.setColorPickerDialogListener(this);
        }
    }

    @Override protected void onClick() {
        super.onClick();
        if (onShowDialogListener != null) {
            onShowDialogListener.onShowColorPickerDialog((String) getTitle(), color);
        } else if (showDialog) {
            ColorPickerDialog dialog = ColorPickerDialog.newBuilder()
                    .setDialogType(dialogType)
                    .setDialogTitle(dialogTitle)
                    .setColorShape(colorShape)
                    .setPresets(presets)
                    .setAllowPresets(allowPresets)
                    .setAllowCustom(allowCustom)
                    .setShowAlphaSlider(showAlphaSlider)
                    .setShowColorShades(showColorShades)
                    .setColor(color)
                    .create();
            key = getKey();
            dialog.setColorPickerDialogListener(ColorPickerPreference.this);
            dialog.show(activity.getFragmentManager(), getFragmentTag());
        }
    }

    @Override protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        if (restorePersistedValue) {
            color = getPersistedInt(0xFF000000);
        } else {
            color = (Integer) defaultValue;
            DLog.d("ColorPicker", "color = " + color);
            persistInt(color);
        }
    }

    @Override protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInteger(index, Color.BLACK);
    }

    @Override public void onColorSelected(int dialogId, @ColorInt int color) {
        saveValue(color);
        dialog = null;
        key = null;
    }

    @Override public void onDialogDismissed(int dialogId) {
        // no-op
    }

    /**
     * Set the new color
     *
     * @param color
     *     The newly selected color
     */
    public void saveValue(@ColorInt int color) {
        this.color = color;
        persistInt(this.color);
        notifyChanged();
        callChangeListener(color);
    }

    /**
     * Set the colors shown in the {@link ColorPickerDialog}.
     *
     * @param presets An array of color ints
     */
    public void setPresets(@NonNull int[] presets) {
        this.presets = presets;
    }

    /**
     * Get the colors that will be shown in the {@link ColorPickerDialog}.
     *
     * @return An array of color ints
     */
    public int[] getPresets() {
        return presets;
    }

    /**
     * The listener used for showing the {@link ColorPickerDialog}.
     * Call {@link #saveValue(int)} after the user chooses a color.
     * If this is set then it is up to you to show the dialog.
     *
     * @param listener
     *     The listener to show the dialog
     */
    public void setOnShowDialogListener(OnShowDialogListener listener) {
        onShowDialogListener = listener;
    }

    /**
     * The tag used for the {@link ColorPickerDialog}.
     *
     * @return The tag
     */
    public String getFragmentTag() {
        return "color_" + getKey();
    }

    public interface OnShowDialogListener {

        void onShowColorPickerDialog(String title, int currentColor);
    }

    public boolean isShowDialog() {
        return showDialog;
    }

    public void setShowDialog(boolean showDialog) {
        this.showDialog = showDialog;
    }

    public int getDialogType() {
        return dialogType;
    }

    public void setDialogType(int dialogType) {
        this.dialogType = dialogType;
    }

    public int getColorShape() {
        return colorShape;
    }

    public void setColorShape(int colorShape) {
        this.colorShape = colorShape;
    }

    public boolean isAllowPresets() {
        return allowPresets;
    }

    public void setAllowPresets(boolean allowPresets) {
        this.allowPresets = allowPresets;
    }

    public boolean isAllowCustom() {
        return allowCustom;
    }

    public void setAllowCustom(boolean allowCustom) {
        this.allowCustom = allowCustom;
    }

    public boolean isShowAlphaSlider() {
        return showAlphaSlider;
    }

    public void setShowAlphaSlider(boolean showAlphaSlider) {
        this.showAlphaSlider = showAlphaSlider;
    }

    public boolean isShowColorShades() {
        return showColorShades;
    }

    public void setShowColorShades(boolean showColorShades) {
        this.showColorShades = showColorShades;
    }

    public int getPreviewSize() {
        return previewSize;
    }

    public void setPreviewSize(int previewSize) {
        this.previewSize = previewSize;
    }

    public int getDialogTitle() {
        return dialogTitle;
    }

    public void setDialogTitle(int dialogTitle) {
        this.dialogTitle = dialogTitle;
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
    }
}