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
 * Date:   01/03/17.
 */

public abstract class ViewJoystickSquare extends View {
    int centerX, centerY, xLeft, xRight, yTop, yBottom, boundary, radius, ref;
    float pointX, pointY;
    Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    int colorCircle = ContextCompat.getColor(getContext(), R.color.colorAccentInactive);
    int colorPrimary = ContextCompat.getColor(getContext(), R.color.colorPrimary);
    ControlListener controlListener;

    public ViewJoystickSquare(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed) {
            centerX = (right - left) / 2;
            centerY = (bottom - top) / 2;
            ref = (int) (Math.min(right - left, bottom - top) * 0.325);
            pointX = centerX;
            pointY = centerY;
            boundary = ref / 4;
            xLeft = centerX - ref;
            xRight = centerX + ref;
            yTop = centerY - ref;
            yBottom = centerY + ref;
            radius = Math.min(getWidth(), getHeight()) / 7;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        p.setColor(colorPrimary);
        canvas.drawRect(xLeft - boundary, yTop, xRight + boundary, yBottom, p);
        canvas.drawRect(xLeft, yTop - boundary, xRight, yBottom + boundary, p);
        canvas.drawCircle(xLeft, yTop, boundary, p);
        canvas.drawCircle(xLeft, yBottom, boundary, p);
        canvas.drawCircle(xRight, yTop, boundary, p);
        canvas.drawCircle(xRight, yBottom, boundary, p);
        p.setColor(colorCircle);
        canvas.drawCircle(pointX, pointY, radius, p);
        invalidate();
    }

    public void setControlListener(ControlListener controlListener) {
        this.controlListener = controlListener;
    }

    public void setPoint(float x, float y) {
        pointX = centerX + x * ref;
        pointY = centerY - y * ref;
        notifyListener(x, y);
    }

    void notifyListener(float x, float y) {
        if (controlListener != null) {
            controlListener.onDataChange((int) (myFunc(x, true) * y * 127), (int) (myFunc(x, false) * y * 127));
        }
    }

    float myFunc(float x, boolean left) {
        return left ? myFunc(x) : myFunc(-x);
    }

    float myFunc(float x) {
        return x < 0 ? 1 + x : 1;
    }

    public float getPointX() {
        return pointX;
    }

    public void setPointX(float pointX) {
        this.pointX = pointX;
    }

    public float getPointY() {
        return pointY;
    }

    public void setPointY(float pointY) {
        this.pointY = pointY;
    }

    public int getColorCircle() {
        return colorCircle;
    }

    public void setColorCircle(int colorCircle) {
        this.colorCircle = colorCircle;
    }

    public interface ControlListener {
        void onDataChange(int left, int right);
    }

    public static class JoystickTouch extends ViewJoystickSquare {
        int colorInactive = ContextCompat.getColor(getContext(), R.color.colorAccentInactive);
        int colorActive = ContextCompat.getColor(getContext(), R.color.colorAccent);
        StateListener stateListener;

        public JoystickTouch(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                ObjectAnimator.ofFloat(this, "pointX", centerX).start();
                ObjectAnimator.ofFloat(this, "pointY", centerY).start();
                ObjectAnimator.ofArgb(this, "colorCircle", colorInactive).start();
                notifyListener(0, 0);
                if (stateListener != null)
                    stateListener.onStateChange(false);
            } else if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (event.getX() > xLeft && event.getX() < xRight && event.getY() > yTop && event.getY() < yBottom) {
                    pointX = event.getX();
                    pointY = event.getY();
                    ObjectAnimator.ofArgb(this, "colorCircle", colorInactive, colorActive).start();
                    notifyListener((pointX - centerX) / ref, (centerY - pointY) / ref);
                    if (stateListener != null)
                        stateListener.onStateChange(true);
                } else {
                    return false;
                }
            } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                float diffX = event.getX() - centerX;
                float diffY = event.getY() - centerY;
                float diffM = Math.max(Math.abs(diffX), Math.abs(diffY));
                if (diffM > ref) {
                    diffX /= diffM / ref;
                    diffY /= diffM / ref;
                    pointX = centerX + diffX;
                    pointY = centerY + diffY;
                } else {
                    pointX = event.getX();
                    pointY = event.getY();
                }
                notifyListener((pointX - centerX) / ref, (centerY - pointY) / ref);
            }
            return true;
        }

        public void setStateListener(StateListener stateListener) {
            this.stateListener = stateListener;
        }

        public interface StateListener {
            void onStateChange(boolean state);
        }
    }

    public static class JoystickGravity extends ViewJoystickSquare {
        boolean active = false;
        int colorInactive = ContextCompat.getColor(getContext(), R.color.colorAccentInactive);
        int colorActive = ContextCompat.getColor(getContext(), R.color.colorAccent);
        ObjectAnimator animator;

        public JoystickGravity(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        @Override
        void notifyListener(float x, float y) {
            if (active)
                super.notifyListener(x, y);
        }

        public void setActive(boolean a) {
            active = a;
            if (a) {
                if (animator != null)
                    animator.end();
                animator = ObjectAnimator.ofArgb(this, "colorCircle", colorActive);
                animator.start();
            } else {
                if (animator != null)
                    animator.end();
                animator = ObjectAnimator.ofArgb(this, "colorCircle", colorInactive);
                animator.start();
                notifyListener(0, 0);
            }
        }
    }
}
