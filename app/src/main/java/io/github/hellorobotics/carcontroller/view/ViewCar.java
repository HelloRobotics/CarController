/*
 *  Copyright 2017 HelloRobotics.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 *     either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package io.github.hellorobotics.carcontroller.view;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import io.github.hellorobotics.carcontroller.R;

/**
 * Author: towdium
 * Date:   26/02/17.
 */

public class ViewCar extends View {
    int left;
    int top;
    int bottom;
    int right;
    int xArrow;
    int yArrow;
    int tArrow;
    int direction;

    Drawable body;
    Drawable wheel;
    Drawable far;
    Drawable mid;
    Drawable close;
    Drawable arrow;
    int opacFar = 0;
    int opacMid = 0;
    int opacClose = 0;
    int opacFarSet = 0;
    int opacMidSet = 0;
    int opacCloseSet = 0;

    public ViewCar(Context context) {
        super(context);
        init();
    }

    public ViewCar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ViewCar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public int getOpacFar() {
        return opacFar;
    }

    public void setOpacFar(int opacFar) {
        this.opacFar = opacFar;
    }

    public int getOpacMid() {
        return opacMid;
    }

    public void setOpacMid(int opacMid) {
        this.opacMid = opacMid;
    }

    public int getOpacClose() {
        return opacClose;
    }

    public void setOpacClose(int opacClose) {
        this.opacClose = opacClose;
    }

    private void init() {
        body = ContextCompat.getDrawable(getContext(), R.drawable.viewcar_body);
        wheel = ContextCompat.getDrawable(getContext(), R.drawable.viewcar_wheels);
        far = ContextCompat.getDrawable(getContext(), R.drawable.viewcar_distfar);
        mid = ContextCompat.getDrawable(getContext(), R.drawable.viewcar_distmid);
        close = ContextCompat.getDrawable(getContext(), R.drawable.viewcar_distclose);
        arrow = ContextCompat.getDrawable(getContext(), R.drawable.viewcar_direction);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed) {
            int width = getRight() - getLeft();
            int height = getBottom() - getTop();
            int hCenter = (getRight() + getLeft()) / 2;
            int vCenter = (getBottom() - getTop()) / 2;
            int ref = Math.min(width / 2, height / 3);
            this.left = hCenter - ref;
            this.right = hCenter + ref;
            this.top = (int) (vCenter - 1.5 * ref);
            this.bottom = (int) (vCenter + 1.5 * ref) - 1;
            xArrow = hCenter;
            yArrow = (int) (vCenter + 0.5 * ref);
            tArrow = (int) (vCenter - 0.5 * ref);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawPart(body, R.color.colorPrimaryDark, canvas, 255);
        drawPart(wheel, android.R.color.black, canvas, 255);
        drawPart(far, R.color.colorAccentThird, canvas, opacFar);
        drawPart(mid, R.color.colorAccentSecond, canvas, opacMid);
        drawPart(close, R.color.colorAccent, canvas, opacClose);
        canvas.rotate(direction, xArrow, yArrow);
        arrow.setBounds(left, tArrow, right, bottom);
        DrawableCompat.setTint(arrow, ContextCompat.getColor(getContext(), R.color.colorAccent));
        arrow.draw(canvas);
        canvas.restore();
        invalidate();
    }

    private void drawPart(Drawable d, int color, Canvas canvas, int opacity) {
        d.setBounds(left, top, right, bottom);
        d.setAlpha(opacity);
        DrawableCompat.setTint(d, ContextCompat.getColor(getContext(), color));
        d.draw(canvas);
    }

    public void setDistance(int distance) {
        int far = 0, mid = 0, clo = 0;
        if (distance < 60) {
            if (distance > 40)
                far = 255;
            else if (distance > 20)
                mid = 255;
            else
                clo = 255;
        }
        if (far != opacFarSet) {
            ObjectAnimator.ofInt(this, "opacFar", far).setDuration(500).start();
            opacFarSet = far;
        }
        if (mid != opacMidSet) {
            ObjectAnimator.ofInt(this, "opacMid", mid).setDuration(500).start();
            opacMidSet = mid;
        }
        if (clo != opacCloseSet) {
            ObjectAnimator.ofInt(this, "opacClose", clo).setDuration(500).start();
            opacCloseSet = clo;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        setDistance(10);
        return super.onTouchEvent(event);
    }

    public void setDirection(int d) {
        direction = d;
    }
}
