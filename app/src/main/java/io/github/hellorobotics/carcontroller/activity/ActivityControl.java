package io.github.hellorobotics.carcontroller.activity;

import android.bluetooth.le.ScanResult;
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
import io.github.hellorobotics.carcontroller.view.ViewJoyStickCircle;
import io.github.hellorobotics.carcontroller.view.ViewPagerCustom;

/**
 * Author: towdium
 * Date:   15/02/17.
 */

public class ActivityControl extends AppCompatActivity implements HelperBle.InstructionListener, ViewJoyStickCircle.StateListener {
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
        viewPager = (ViewPagerCustom) findViewById(R.id.view_pager);
        viewTab = (TabLayout) findViewById(R.id.view_tab);
        viewPager.setAdapter(new MyFragmentStatePagerAdapter(getSupportFragmentManager()));
        viewTab.setupWithViewPager(viewPager);
    }

    public void onDestroy() {
        super.onDestroy();
        ble.disconnect();
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

    @Override
    public void onStateChange(boolean state) {
        viewPager.setPagingEnabled(!state);
    }

    public static class FragJoystick extends Fragment {
        ViewJoyStickCircle.ControlListener l;
        ViewJoyStickCircle.StateListener sl;

        public void setControlListener(ViewJoyStickCircle.ControlListener l) {
            this.l = l;
        }

        public void setStateListener(ViewJoyStickCircle.StateListener l) {
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
            ((ViewJoyStickCircle) view.findViewById(R.id.view3)).setControlListener(l);
            ((ViewJoyStickCircle) view.findViewById(R.id.view3)).setStateListener(sl);
        }
    }

    class MyFragmentStatePagerAdapter extends FragmentStatePagerAdapter {

        public MyFragmentStatePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            FragJoystick f = new FragJoystick();
            f.setControlListener(new ViewJoyStickCircle.ControlListener() {
                @Override
                public void onDataChange(int left, int right) {
                    ActivityControl.this.onDataChange(left, right);
                }
            });
            f.setStateListener(ActivityControl.this);
            return f;
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getString(R.string.control_joystick);
                case 1:
                    return getString(R.string.control_gravity);
                default:
                    return null;
            }
        }
    }
}