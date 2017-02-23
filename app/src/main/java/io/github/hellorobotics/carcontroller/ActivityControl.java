package io.github.hellorobotics.carcontroller;

import android.bluetooth.le.ScanResult;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;

import io.github.hellorobotics.carcontroller.utils.Constants;
import io.github.hellorobotics.carcontroller.utils.HelperBle;
import io.github.hellorobotics.carcontroller.utils.Instruction;
import io.github.hellorobotics.carcontroller.utils.Instruction.enumInstruction;
import io.github.hellorobotics.carcontroller.view.ViewJoyStick;

/**
 * Author: towdium
 * Date:   15/02/17.
 */

public class ActivityControl extends AppCompatActivity implements HelperBle.InstructionListener, ViewJoyStick.JoyStickListener {
    HelperBle ble;
    long time;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ScanResult result = getIntent().getParcelableExtra("result");
        ble = new HelperBle(this);
        ble.setInstListener(this);
        ble.connect(result.getDevice());
        setContentView(R.layout.activity_control);
        ((ViewJoyStick) findViewById(R.id.view)).setListener(this);
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
    public void onInstruction(Instruction instruction) {
        Log.i(Constants.TAG, instruction.getString());
    }

    @Override
    public void onDataChange(int left, int right) {
        if (System.currentTimeMillis() - time > 20) {
            ble.sendData(new Instruction(enumInstruction.MOSI_SPEED, (byte) left, (byte) right));
            time = System.currentTimeMillis();
        } else if (left == 0 || right == 0) {
            Handler handler = new Handler();
            handler.postDelayed(() ->
                    ble.sendData(new Instruction(enumInstruction.MOSI_SPEED,
                            (byte) left, (byte) right)), 20 - (System.currentTimeMillis() - time));
            time = time + 20;
        }
    }
}