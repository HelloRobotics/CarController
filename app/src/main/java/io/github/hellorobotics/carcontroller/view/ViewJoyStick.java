package io.github.hellorobotics.carcontroller.view;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import io.github.hellorobotics.carcontroller.R;

/**
 * Author: towdium
 * Date:   23/02/17.
 */

public class ViewJoyStick extends View {
    final int colorActive = ContextCompat.getColor(getContext(), R.color.colorAccent);
    final int colorInactive = ContextCompat.getColor(getContext(), R.color.colorAccentInactive);
    final double halfPi = Math.PI / 2;
    final double twoThirdPi = Math.PI / 3 * 2;
    final double oneOverPi = 1 / Math.PI;
    Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    float radiusButton;
    float radiusBase;
    float radiusRange;
    float center;
    float circleX;
    float circleY;
    int color = colorInactive;
    JoyStickListener listener;

    public ViewJoyStick(Context context) {
        super(context);
    }


    public ViewJoyStick(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ViewJoyStick(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setListener(JoyStickListener listener) {
        this.listener = listener;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed) {
            circleX = getWidth() / 2;
            circleY = getWidth() / 2;
            radiusButton = getWidth() / 9;
            radiusBase = getWidth() / 4;
            radiusRange = radiusBase - radiusButton / 3;
            center = getWidth() / 2;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            ObjectAnimator.ofFloat(ViewJoyStick.this, "circleX", circleX, getWidth() / 2).start();
            ObjectAnimator.ofFloat(ViewJoyStick.this, "circleY", circleY, getWidth() / 2).start();
            ObjectAnimator.ofArgb(ViewJoyStick.this, "color", colorActive, colorInactive).start();
            notifyListener(0, 0);
        } else if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (Math.pow(event.getY() - center, 2)
                    + Math.pow(event.getX() - center, 2) < radiusRange * radiusRange) {
                circleX = event.getX();
                circleY = event.getY();
                ObjectAnimator.ofArgb(ViewJoyStick.this, "color", colorInactive, colorActive)
                        .start();
                notifyListener(circleX - center, circleY - center);
            } else {
                return false;
            }
        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            double dist = Math.pow(Math.pow(event.getY() - center, 2)
                    + Math.pow(event.getX() - center, 2), 0.5);
            if (dist < radiusRange) {
                circleX = event.getX();
                circleY = event.getY();
            } else {
                double ratio = radiusRange / dist;
                circleX = (float) (center + (event.getX() - center) * ratio);
                circleY = (float) (center + (event.getY() - center) * ratio);
            }
            notifyListener(circleX - center, circleY - center);
        }
        return true;
    }

    private void notifyListener(float x, float y) {
        double angle = Math.atan2(-y, x);
        double dist = Math.pow(x * x + y * y, 0.5) / radiusRange;
        if (listener != null) {
            listener.onDataChange((int) Math.round(dist * myFunc(angle, true)),
                    (int) Math.round(dist * myFunc(angle, false)));
        }
    }

    private double modifiedCos(double x) {
        return x < twoThirdPi ? (1 - x * 1.5 * oneOverPi) : (-x * 3 * oneOverPi + 2);
    }

    private double myFunc(double x) {
        if (x > halfPi)
            return modifiedCos(2 * (x - halfPi));
        else if (x > 0)
            return 1;
        else if (x > -halfPi)
            return -modifiedCos(2 * (x + halfPi));
        else
            return -1;
    }

    private double myFunc(double x, boolean left) {
        if (left)
            return myFunc(x) * 127;
        else
            return -myFunc(-x) * 127;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        p.setColor(ContextCompat.getColor(getContext(), R.color.colorPrimaryDark));
        canvas.drawCircle(center, center, radiusBase, p);
        p.setColor(color);
        canvas.drawCircle(circleX, circleY, radiusButton, p);
        invalidate();
    }

    public float getCircleX() {
        return circleX;
    }

    public void setCircleX(float circleX) {
        this.circleX = circleX;
    }

    public float getCircleY() {
        return circleY;
    }

    public void setCircleY(float circleY) {
        this.circleY = circleY;
    }

    public interface JoyStickListener {
        void onDataChange(int left, int right);
    }
}
