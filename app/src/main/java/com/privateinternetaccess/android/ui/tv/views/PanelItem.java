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

package com.privateinternetaccess.android.ui.tv.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.privateinternetaccess.android.R;
import com.privateinternetaccess.android.pia.utils.DLog;

import java.util.ArrayList;
import java.util.List;

public class PanelItem extends FrameLayout {

    private View backgroundView;

    private boolean shouldUpdateColors = false;

    private int focusedTextColor;
    private int unfocusedTextColor;

    private List<TextView> childTextViews;

    private boolean firstLayout = true;

    @Nullable
    private PanelClickListener clickListener;

    public PanelItem(Context context) {
        super(context);
        init(context, null);
    }

    public PanelItem(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public PanelItem(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        if (firstLayout) {
            childTextViews = new ArrayList<>();
            findAllTextViews(this);
            firstLayout = true;
        }

        updateTextColors(isFocused());
    }

    public void init(Context context, AttributeSet attrs) {
        this.setClipChildren(false);
        this.setClipToPadding(false);

        if (attrs != null) {
            TypedArray a = context.getTheme().obtainStyledAttributes(
                    attrs,
                    R.styleable.PanelItem,
                    0, 0);

            try {
                focusedTextColor = a.getColor(R.styleable.PanelItem_focusedTextColor,
                        getResources().getColor(R.color.tv_grey_15));
                unfocusedTextColor = a.getColor(R.styleable.PanelItem_unfocusedTextColor,
                        getResources().getColor(R.color.white));
            } finally {
                a.recycle();
            }
        }

        backgroundView = new View(context);
        addView(backgroundView);

        backgroundView.setBackgroundColor(getResources().getColor(R.color.tv_grey_20));

        setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                float yScale = 1f, xScale = 1f, elevation = 0f;

                if (b) {
                    yScale = 1.2f;
                    xScale = 1.05f;
                    elevation = 1f;

                    backgroundView.setBackgroundColor(getResources().getColor(R.color.white));
                }
                else {
                    backgroundView.setBackgroundColor(getResources().getColor(R.color.tv_grey_20));
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    PanelItem.this.animate().setDuration(200).translationZ(elevation);
                }
                else {
                    ViewCompat.setElevation(PanelItem.this, elevation);
                }

                backgroundView.animate().setDuration(200).scaleX(xScale);
                backgroundView.animate().setDuration(200).scaleY(yScale);

                updateTextColors(b);
            }
        });

        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (clickListener != null) {
                    clickListener.onPanelClicked(view);
                }
            }
        });
    }

    public void setPanelClickListener(@Nullable PanelClickListener listener) {
        this.clickListener = listener;
    }

    private void findAllTextViews(ViewGroup viewGroup) {
        int count = viewGroup.getChildCount();

        for (int i = 0; i < count; i++) {
            View view = viewGroup.getChildAt(i);
            if (view instanceof ViewGroup)
                findAllTextViews((ViewGroup) view);
            else if (view instanceof TextView) {
                childTextViews.add((TextView)view);
            }
        }

        updateTextColors(false);
    }

    private void updateTextColors(boolean isFocused) {
        if (childTextViews == null || childTextViews.size() == 0) {
            return;
        }

        for (TextView view : childTextViews) {
            DLog.d("PanelItem", "View: " + view);
            DLog.d("PanelItem", "Focused: " + Integer.toString(focusedTextColor));
            DLog.d("PanelItem", "Unfocused: " + Integer.toString(unfocusedTextColor));
            if (isFocused) {
                view.setTextColor(focusedTextColor);
            }
            else {
                view.setTextColor(unfocusedTextColor);
            }
        }
    }

    public interface PanelClickListener {
        void onPanelClicked(View v);
    }

}
