package com.mafaz.ble;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;

import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static android.bluetooth.BluetoothProfile.STATE_DISCONNECTED;

public class BluetoothLEService extends Service {

    public final static String ACTION_GATT_CONNECTED =
            "com.app.androidkt.heartratemonitor.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.app.androidkt.heartratemonitor.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.app.androidkt.heartratemonitor.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.app.androidkt.heartratemonitor.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.app.androidkt.heartratemonitor.le.EXTRA_DATA";
    File folder = new File(Environment.getExternalStorageDirectory()+"/sensordata.csv");



    public final static UUID UUID_BATTERY_LEVEL =
            UUID.fromString(SampleGattAttributes.UUID_BATTERY_LEVEL_UUID);

    private static final String TAG = "BluetoothLEService";
    private static final int STATE_DISCONNECT = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;
    IBinder mBinder = new LocalBinder();
    private int mConnectionState = STATE_DISCONNECT;
    private BluetoothAdapter mBluetoothAdapter;

    private BluetoothGatt mBluetoothGatt;
    private String bluetoothAddress;

    private BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            Log.d(TAG, "onConnectionStateChange " + newState);

            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                Log.i(TAG, "Connected to GATT server.");

                Log.i(TAG, "Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());

            } else if (newState == STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
                broadcastUpdate(intentAction);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            Log.d(TAG, "onServicesDiscovered " + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
               // mBluetoothGatt.setCharacteristicNotification,true);
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
                System.out.println("oooooooooooooooooooooooooooooooooooooooooooooo");
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }

        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            Log.d(TAG, "onCharacteristicRead " + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
                System.out.println(characteristic+"hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh");

            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            Log.d(TAG, "onCharacteristicWrite " + status);

        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            Log.d(TAG, "onCharacteristicChanged");
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            generateCsvFile(folder.toString(),characteristic);


            System.out.println(characteristic+"hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh");

        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
            Log.d(TAG, "onDescriptorRead " + status);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            Log.d(TAG, "onDescriptorWrite " + status);

        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            super.onReliableWriteCompleted(gatt, status);
            Log.d(TAG, "onReliableWriteCompleted " + status);

        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            super.onReadRemoteRssi(gatt, rssi, status);
            Log.d(TAG, "onReadRemoteRssi " + status);

        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
            Log.d(TAG, "onMtuChanged " + status);
        }
    };
    private Timestamp timestamp;


    public BluetoothLEService() {
    }

    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();
    }

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);
        System.out.println(characteristic.getUuid()+"hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh");

        if (UUID_BATTERY_LEVEL.equals(characteristic.getUuid())) {
            int format = BluetoothGattCharacteristic.FORMAT_SFLOAT;
            System.out.println( characteristic.getValue()[1]+"line17222222222222222222222222222222222222222222222222222222");
           // ArrayList<byte[]> dat=characteristic.get
            //System.out.println(dat[2]);

            final String battery_level = String.valueOf(characteristic.getValue()[6]);
            intent.putExtra(EXTRA_DATA, battery_level);
        }
        sendBroadcast(intent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        close();
        return super.onUnbind(intent);
    }

    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.d(TAG, "Bluetooth adapter not initialize");
            return;
        }
        mBluetoothGatt.disconnect();
    }


    public boolean initialize() {
        mBluetoothAdapter = BluetoothUtils.getBluetoothAdapter(this);
        return true;
    }

    public boolean connect(@NonNull String address) {
        //Try to use existing connection
        if (mBluetoothAdapter != null && address.equals(bluetoothAddress) && mBluetoothGatt != null) {
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }
        final BluetoothDevice bluetoothDevice = mBluetoothAdapter.getRemoteDevice(address);
        if (bluetoothDevice == null) {
            Log.w(TAG, "Device not found");
            return false;
        }

        mBluetoothGatt = bluetoothDevice.connectGatt(this, false, bluetoothGattCallback);
        bluetoothAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }


    public void readCharacteristic(@NonNull BluetoothGattCharacteristic bluetoothGattCharacteristic) {
        mBluetoothGatt.readCharacteristic(bluetoothGattCharacteristic);
    }

    public void setCharacteristicNotification(@NonNull BluetoothGattCharacteristic characteristic, boolean enabled) {
        System.out.println(characteristic.getUuid()+"setttttttttttttttttttttt");
       // if(characteristic.getUuid().toString()=="6e400003-b5a3-f393-e0a9-e50e24dcca9e")
        mBluetoothGatt.setCharacteristicNotification(characteristic, true);
        //BluetoothGattDescriptor descrip=descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        //mBluetoothGatt.writeDescriptor(descriptor);
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        mBluetoothGatt.writeDescriptor(descriptor);

    }

    public class LocalBinder extends Binder {
        BluetoothLEService getService() {
            return BluetoothLEService.this;
        }
    }

    public void generateCsvFile(String fileName, BluetoothGattCharacteristic characteristic) {

        FileWriter writer = null;

        try {

            writer = new FileWriter(fileName,true);
            writer.append("TempValue");
            writer.append(',');
            writer.append("time");
            writer.append('\n');

            writer.append(characteristic.getStringValue(0));
            writer.append(',');
            writer.append((timestamp = new Timestamp(System.currentTimeMillis())).toString());
            writer.append('\n');

           // writer.append("13C");
           // writer.append(',');
            //writer.append();
            //writer.append('\n');

            System.out.println("CSV file is created...");

            uploadFile(folder.toString());

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                writer.flush();
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public boolean uploadFile(String folder){

        File file=new File(folder);
        try{
            RequestBody requestBody=new MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart("files",file.getName(),RequestBody.create(MediaType.parse("image/*"),file))
                    .addFormDataPart("some_key","some_value")
                    .addFormDataPart("submit","submit")
                    .build();

            Request request=new Request.Builder()
                    .url("http://"+"10.57.55.18"+"//project/upload.php")
                    .post(requestBody)
                    .build();

            OkHttpClient client = new OkHttpClient();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {

                }
            });
            return true;
        }
        catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }






}