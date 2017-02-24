package io.github.hellorobotics.carcontroller.utils;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.os.ParcelUuid;
import android.util.Log;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

/**
 * Author: towdium
 * Date:   15/02/17.
 */

public class HelperBle {
    public static final UUID UUID_SERVICE = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID UUID_RX = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID UUID_TX = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    public static final int kTxMaxCharacters = 20;

    private Context context;
    private BluetoothAdapter adapter;
    private BluetoothGatt gatt;
    private BluetoothGattService service;
    private BluetoothDevice device;

    private byte[] unfinishedInstruction;

    private DataReceiveListener dataListener;
    private InstructionListener instListener;
    private ScanCallback callbackScan;

    public HelperBle(Context context) {
        this.context = context;
        BluetoothManager btManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (btManager != null)
            adapter = btManager.getAdapter();
    }

    public static boolean isUART(ScanResult result) {
        ScanRecord record = result.getScanRecord();
        if (record == null)
            return false;
        for (ParcelUuid uuid : result.getScanRecord().getServiceUuids()) {
            if (uuid.getUuid().equals(UUID_SERVICE)) {
                return true;
            }
        }
        return false;
    }

    public enumBleState getState() {
        if (adapter == null) {
            return enumBleState.UNAVAILABLE;
        } else if (!adapter.isEnabled()) {
            return enumBleState.DISABLED;
        } else {
            return enumBleState.AVAILABLE;
        }
    }

    public void startScan(ScanCallback callback) {
        Objects.requireNonNull(callback);
        if (adapter != null) {
            disconnect();
            stopScan();
            adapter.getBluetoothLeScanner().startScan(callback);
            callbackScan = callback;
        } else
            Log.e(Constants.TAG, "Adapter not initialized, abort.");
    }

    public void stopScan() {
        if (adapter != null && callbackScan != null) {
            adapter.getBluetoothLeScanner().stopScan(callbackScan);
            callbackScan = null;
        }
    }

    public void disconnect() {
        if (gatt != null) {
            device = null;
            gatt.disconnect();
            gatt.close();
        }
    }

    public void connect(BluetoothDevice device) {
        gatt = device.connectGatt(context, false, new MyBluetoothGattCallback());
        this.device = device;
    }

    public void sendData(byte[] data) {
        if (service != null) {
            for (int i = 0; i < data.length; i += kTxMaxCharacters) {
                final byte[] chunk = Arrays.copyOfRange(data, i, Math.min(i + kTxMaxCharacters, data.length));
                BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID_TX);
                if (characteristic != null) {
                    characteristic.setValue(chunk);
                    gatt.writeCharacteristic(characteristic);
                }
            }
        } else {
            Log.w(Constants.TAG, "Uart Service not discovered. Unable to send data");
        }
    }

    public void sendData(String text) {
        final byte[] value = text.getBytes(Charset.forName("UTF-8"));
        sendData(value);
    }

    public void sendData(Instruction inst) {
        sendData(inst.toByteArray());
    }

    public void setDataReceiveListener(DataReceiveListener l) {
        dataListener = l;
    }

    public void setInstListener(InstructionListener l) {
        instListener = l;
    }

    public enum enumBleState {UNAVAILABLE, DISABLED, AVAILABLE}

    public interface DataReceiveListener {
        void onDataReceive(byte[] data);
    }

    public interface InstructionListener {
        void onInstruction(Instruction instruction);
    }

    private class MyBluetoothGattCallback extends BluetoothGattCallback {
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            service = gatt.getService(UUID_SERVICE);
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID_RX);
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CHARACTERISTIC_CONFIG);
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            gatt.writeDescriptor(descriptor);
            gatt.setCharacteristicNotification(characteristic, true);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            if (characteristic.getService().getUuid().equals(UUID_SERVICE)) {
                if (characteristic.getUuid().equals(UUID_RX)) {
                    final byte[] bytes = characteristic.getValue();
                    if (dataListener != null)
                        dataListener.onDataReceive(bytes);
                    if (unfinishedInstruction != null) {
                        byte[] buffer = new byte[unfinishedInstruction.length + bytes.length];
                        System.arraycopy(unfinishedInstruction, 0, buffer, 0, unfinishedInstruction.length);
                        System.arraycopy(bytes, 0, buffer, unfinishedInstruction.length, bytes.length);
                        if (buffer[buffer.length - 1] != Instruction.ETB) {
                            unfinishedInstruction = buffer;
                        } else {
                            unfinishedInstruction = null;
                            if (instListener != null)
                                try {
                                    instListener.onInstruction(Instruction.fromByteArray(buffer));
                                } catch (Exception e) {
                                    new IOException(genError(buffer), e).printStackTrace();
                                }

                        }
                    } else if (bytes[bytes.length - 1] != Instruction.ETB) {
                        unfinishedInstruction = bytes;
                    } else if (instListener != null) {
                        try {
                            instListener.onInstruction(Instruction.fromByteArray(bytes));
                        } catch (Exception e) {
                            new IOException(genError(bytes), e).printStackTrace();
                        }
                    }
                }
            }
        }

        private String genError(byte[] data) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Data: ");
            for (byte b : data) {
                stringBuilder.append(String.format("0x%02x ", b));
            }
            stringBuilder.append(", Buffered: ");
            if (unfinishedInstruction == null)
                stringBuilder.append("None.");
            else {
                for (byte b : unfinishedInstruction) {
                    stringBuilder.append(String.format("0x%02x ", b));
                }
            }
            return stringBuilder.toString();
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                if (device != null) {
                    connect(device);
                } else {
                    Log.i(Constants.TAG, "Disconnected.");
                }
            }
        }
    }
}
