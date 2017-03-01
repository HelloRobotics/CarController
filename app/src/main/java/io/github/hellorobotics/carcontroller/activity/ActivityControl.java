package io.github.hellorobotics.carcontroller.activity;

import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import io.github.hellorobotics.carcontroller.Constants;
import io.github.hellorobotics.carcontroller.R;
import io.github.hellorobotics.carcontroller.core.Instruction;
import io.github.hellorobotics.carcontroller.core.Instruction.enumInstruction;
import io.github.hellorobotics.carcontroller.utils.HelperBle;
import io.github.hellorobotics.carcontroller.utils.Utilities;
import io.github.hellorobotics.carcontroller.view.ViewCar;
import io.github.hellorobotics.carcontroller.view.ViewJoystickCircle;
import io.github.hellorobotics.carcontroller.view.ViewJoystickSquare;
import io.github.hellorobotics.carcontroller.view.ViewPagerCustom;

/**
 * Author: towdium
 * Date:   15/02/17.
 */

public class ActivityControl extends AppCompatActivity implements HelperBle.InstructionListener {
    HelperBle ble;

    long time;
    ViewCar viewCar;
    ViewPagerCustom viewPager;
    TabLayout viewTab;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ScanResult result = getIntent().getParcelableExtra("result");
        ble = new HelperBle(this);
        ble.setInstListener(this);
        ble.connect(result.getDevice());
        setContentView(R.layout.activity_control);
        viewCar = (ViewCar) findViewById(R.id.view_car);
        viewCar.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return viewPager.onTouchEvent(event);
            }
        });
        viewPager = (ViewPagerCustom) findViewById(R.id.view_pager);
        viewTab = (TabLayout) findViewById(R.id.view_tab);
        viewPager.setAdapter(new MyFragmentStatePagerAdapter(getSupportFragmentManager()));
        viewPager.setOffscreenPageLimit(5);
        viewTab.setupWithViewPager(viewPager);
    }

    public void onDestroy() {
        super.onDestroy();
        ble.disconnect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        ble.sendData(new Instruction(enumInstruction.MOSI_SPEED, (byte) 0x00, (byte) 0x00));
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return super.onTouchEvent(event);
    }

    @Override
    public void onInstruction(final Instruction instruction) {
        if (instruction.getCode() == enumInstruction.MISO_DISTANCE) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    viewCar.setDistance(Utilities.toUnsigned(instruction.getData(0)));
                }
            });
        } else if (instruction.getCode() == enumInstruction.MISO_DIRECTION) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    viewCar.setDirection((int) (instruction.getData(0) / 256.0 * 360));
                }
            });
        }
    }

    public void onDataChange(final int left, final int right) {
        if (System.currentTimeMillis() - time > 20) {
            ble.sendData(new Instruction(enumInstruction.MOSI_SPEED, (byte) left, (byte) right));
            Log.i(Constants.TAG, left + ", " + right);
            time = System.currentTimeMillis();
        } else if (left == 0 || right == 0) {
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    ble.sendData(new Instruction(enumInstruction.MOSI_SPEED,
                            (byte) left, (byte) right));
                }
            }, 20 - (System.currentTimeMillis() - time));
            time = time + 20;
        }
    }

    public void onStateChange(boolean state) {
        viewPager.setPagingEnabled(!state);
    }

    public static class FragJoystickCircle extends Fragment {
        ViewJoystickCircle.ControlListener l;
        ViewJoystickCircle.StateListener sl;

        public void setControlListener(ViewJoystickCircle.ControlListener l) {
            this.l = l;
        }

        public void setStateListener(ViewJoystickCircle.StateListener l) {
            sl = l;
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.frag_circle, container, false);
        }

        @Override
        public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            ((ViewJoystickCircle) view.findViewById(R.id.view)).setControlListener(l);
            ((ViewJoystickCircle) view.findViewById(R.id.view)).setStateListener(sl);
        }
    }

    public static class FragJoystickSquareTouch extends Fragment {
        ViewJoystickSquare.ControlListener l;
        ViewJoystickSquare.JoystickTouch.StateListener sl;

        public void setControlListener(ViewJoystickSquare.ControlListener l) {
            this.l = l;
        }

        public void setStateListener(ViewJoystickSquare.JoystickTouch.StateListener l) {
            sl = l;
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.frag_square_touch, container, false);
        }

        @Override
        public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            ((ViewJoystickSquare) view.findViewById(R.id.view)).setControlListener(l);
            ((ViewJoystickSquare.JoystickTouch) view.findViewById(R.id.view)).setStateListener(sl);
        }
    }

    public static class FragJoystickSquareGravity extends Fragment implements SensorEventListener {
        ViewJoystickSquare.ControlListener l;
        ViewJoystickSquare.JoystickGravity view;
        SensorManager sensorManager;
        Sensor sensorAccelerometer;


        public void setControlListener(ViewJoystickSquare.ControlListener l) {
            this.l = l;
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.frag_square_gravity, container, false);
        }

        @Override
        public void setUserVisibleHint(boolean isVisibleToUser) {
            super.setUserVisibleHint(isVisibleToUser);
            if (view != null)
                view.setActive(isVisibleToUser);
        }

        @Override
        public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            this.view = (ViewJoystickSquare.JoystickGravity) view.findViewById(R.id.view);
            this.view.setControlListener(l);
        }

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            sensorManager = (SensorManager) getContext().getSystemService(Context.SENSOR_SERVICE);
            sensorAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        @Override
        public void onResume() {
            super.onResume();
            sensorManager.registerListener(this, sensorAccelerometer,
                    SensorManager.SENSOR_DELAY_GAME);
        }

        @Override
        public void onPause() {
            super.onPause();
            sensorManager.unregisterListener(this);
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            Point p = myFunc(event.values[0], event.values[1]);
            view.setPoint(p.x, p.y);
        }

        Point myFunc(float x, float y) {
            float x1 = myFunc(-x);
            float y1 = myFunc((float) Math.sqrt(x * x + y * y));

            x1 = (float) Math.sqrt(Math.abs(x1));
            y1 = (float) Math.sqrt(Math.abs(y1));
            if (y > 0)
                y1 = -y1;
            if (x > 0)
                x1 = -x1;

            return new Point(x1, y1);
        }

        float myFunc(float x) {
            if (x > 5)
                return 1;
            else if (x < -5)
                return -1;
            else
                return x / 5;
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    }

    static class Point {
        float x;
        float y;

        public Point(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }

    class MyFragmentStatePagerAdapter extends FragmentStatePagerAdapter {

        public MyFragmentStatePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            if (position == 0) {
                FragJoystickCircle f = new FragJoystickCircle();
                f.setControlListener(new ViewJoystickCircle.ControlListener() {
                    @Override
                    public void onDataChange(int left, int right) {
                        ActivityControl.this.onDataChange(left, right);
                    }
                });
                f.setStateListener(new ViewJoystickCircle.StateListener() {
                    @Override
                    public void onStateChange(boolean state) {
                        ActivityControl.this.onStateChange(state);
                    }
                });
                return f;
            } else if (position == 1) {
                FragJoystickSquareTouch f = new FragJoystickSquareTouch();
                f.setControlListener(new ViewJoystickSquare.ControlListener() {
                    @Override
                    public void onDataChange(int left, int right) {
                        ActivityControl.this.onDataChange(left, right);
                    }
                });
                f.setStateListener(new ViewJoystickSquare.JoystickTouch.StateListener() {
                    @Override
                    public void onStateChange(boolean state) {
                        ActivityControl.this.onStateChange(state);
                    }
                });
                return f;
            } else {
                FragJoystickSquareGravity f = new FragJoystickSquareGravity();
                f.setControlListener(new ViewJoystickSquare.ControlListener() {
                    @Override
                    public void onDataChange(int left, int right) {
                        ActivityControl.this.onDataChange(left, right);
                    }
                });
                return f;
            }
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getString(R.string.control_joystick_a);
                case 1:
                    return getString(R.string.control_joystick_b);
                case 2:
                    return getString(R.string.control_gravity);
                default:
                    return null;
            }
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            super.setPrimaryItem(container, position, object);
        }
    }
}