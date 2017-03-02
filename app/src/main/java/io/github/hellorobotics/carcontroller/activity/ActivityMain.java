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

package io.github.hellorobotics.carcontroller.activity;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;

import io.github.hellorobotics.carcontroller.Constants;
import io.github.hellorobotics.carcontroller.R;
import io.github.hellorobotics.carcontroller.utils.HelperBle;

/**
 * Author: towdium
 * Date:   15/02/17.
 */

public class ActivityMain extends AppCompatActivity {

    private static final String keyDeviceName = "device_name";
    private HelperBle ble;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ble = new HelperBle(this);
        setContentView(R.layout.activity_main);
        ((EditText) findViewById(R.id.editText2))
                .setText(getSharedPreferences(Constants.TAG, MODE_PRIVATE)
                        .getString(keyDeviceName, null));
        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tryConnectDevice();
                getSharedPreferences(Constants.TAG, MODE_PRIVATE).edit().
                        putString(keyDeviceName, getNameToSearch()).apply();
            }
        });
    }

    private void tryConnectDevice() {
        switch (ble.getState()) {
            case UNAVAILABLE:
                new AlertDialog.Builder(this)
                        .setMessage(R.string.dialog_error_no_bluetooth)
                        .setPositiveButton(android.R.string.ok,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        finishAffinity();
                                    }
                                })
                        .show();
                break;
            case DISABLED:
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, Activity.RESULT_OK);
                break;
            default:
                ble.startScan(new MyScanCallback());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK)
            finishAffinity();
        else
            ble.startScan(new MyScanCallback());
    }

    private String getNameToSearch() {
        return ((EditText) findViewById(R.id.editText2)).getText().toString();
    }

    class MyScanCallback extends ScanCallback {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            if (result.getDevice().getName() != null &&
                    result.getDevice().getName().toLowerCase().
                            contains(getNameToSearch().toLowerCase())
                    && HelperBle.isUART(result)) {
                Intent intent = new Intent(ActivityMain.this, ActivityControl.class);
                intent.putExtra("result", result);
                ble.stopScan();
                startActivity(intent);
            }
        }
    }
}
