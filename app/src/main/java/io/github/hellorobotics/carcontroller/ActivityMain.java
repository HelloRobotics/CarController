package io.github.hellorobotics.carcontroller;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.widget.EditText;

import io.github.hellorobotics.carcontroller.utils.Constants;
import io.github.hellorobotics.carcontroller.utils.HelperBle;

public class ActivityMain extends AppCompatActivity {

    private static final String keyDeviceName = "device_name";
    private HelperBle ble;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ble = new HelperBle(this);
        setContentView(R.layout.activity_main);
        ((EditText) findViewById(R.id.editText2)).setText(getSharedPreferences(Constants.TAG, MODE_PRIVATE).getString(keyDeviceName, null));
        findViewById(R.id.button).setOnClickListener(v -> {
            tryConnectDevice();
            getSharedPreferences(Constants.TAG, MODE_PRIVATE).edit().
                    putString(keyDeviceName, getNameToSearch()).apply();
        });
    }

    private void tryConnectDevice() {
        switch (ble.getState()) {
            case UNAVAILABLE:
                new AlertDialog.Builder(this).setMessage(R.string.dialog_error_no_bluetooth).
                        setPositiveButton(android.R.string.ok, (dialog, which) -> finishAffinity())
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
