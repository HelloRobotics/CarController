package io.github.hellorobotics.carcontroller;

import android.bluetooth.le.ScanResult;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;

import java.nio.charset.Charset;

import io.github.hellorobotics.carcontroller.utils.Constants;
import io.github.hellorobotics.carcontroller.utils.HelperBle;
import io.github.hellorobotics.carcontroller.utils.Instruction;

/**
 * Author: towdium
 * Date:   15/02/17.
 */

public class ActivityControl extends AppCompatActivity implements HelperBle.InstructionListener, HelperBle.DataReceiveListener {
    HelperBle ble;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ScanResult result = getIntent().getParcelableExtra("result");
        ble = new HelperBle(this);
        ble.setInstListener(this);
        ble.setDataReceiveListener(this);
        ble.connect(result.getDevice());
    }

    public void onDestroy() {
        super.onDestroy();
        ble.disconnect();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        ble.sendData(new Instruction(Instruction.enumInstruction.MOSI_TEXT, "hello", true));
        return super.onTouchEvent(event);
    }

    @Override
    public void onInstruction(Instruction instruction) {
        Log.i(Constants.TAG, instruction.getString());
    }

    @Override
    public void onDataReceive(byte[] data) {
        Log.i(Constants.TAG, new String(data, Charset.forName("UTF-8")));
    }
}