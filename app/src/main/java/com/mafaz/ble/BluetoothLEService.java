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

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
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
    private byte[] buf = new byte[20];

    private int bufIndex=0;
   // private byte[] buf = new byte[20];


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
                try {
                    broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
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
            try {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
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
    private Integer dat;


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

    private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic) throws InterruptedException {
        final Intent intent = new Intent(action);
        System.out.println(characteristic.getUuid()+"hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh");

        if (UUID_BATTERY_LEVEL.equals(characteristic.getUuid())) {
            int format = BluetoothGattCharacteristic.FORMAT_UINT8;
            System.out.println( characteristic.getValue()[1]+"line17222222222222222222222222222222222222222222222222222222");
            System.out.println(characteristic.getValue()+"byteeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeees");


            byte[] value = characteristic.getValue();
            int i = 0;
            for (byte b : value) {
                byte[] bArr = this.buf;
                bArr[i] = b;
                System.out.println("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"+bArr[i]+"------"+i);



                i++;

                if (i == bArr.length) {
                    //System.out.println("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"+bArr);
                    if(bArr[0]==-86&&bArr[19]==-86 && bArr.length==20){
                        String arr[]={};
                        ArrayList<String> battery_level=new ArrayList<String>(Arrays.asList(arr));
                        String sp02=String.valueOf(bArr[5]);
                        String pr=String.valueOf(bArr[6]);
                        //String un=String.valueOf(bArr[8]/10);
                        float s=bArr[8]/10;
                        String un=String.valueOf(s);

                        System.out.println("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"+bArr);


                        battery_level.add(sp02);

                        battery_level.add((pr));
                        battery_level.add(un);
                        System.out.println(battery_level+"qqqqqqqqqqqqqqqqqqqqqq");

//                        String[] battery_level;
//
//                         battery_level = String.valueOf(bArr[6]);

                        intent.putExtra(EXTRA_DATA, battery_level);
                        sendBroadcast(intent);
                      //  System.out.println("kkkkkkkkkkkkkkk"+bArr[5]);
                    }


                }
//
                }


            }

/*
            //System.out.println(characteristic.getValue() + ;);
            System.out.println(characteristic.getDescriptor(UUID.fromString("00002901-0000-1000-8000-00805f9b34fb")));

            System.out.println("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"+characteristic.getDescriptors().size());




            if(characteristic.getValue()[0]==85 && characteristic.getValue().length>=12 && characteristic.getValue()!=null && characteristic.getValue().length <= 500) {
                //Log.d("WaveData: " + characteristic.getValue()[0]);
                //characteristic.wait(100);
                System.out.println(characteristic.getValue()[3]&255);


                final String battery_level = String.valueOf(characteristic.getIntValue(format,4));
                intent.putExtra(EXTRA_DATA, battery_level);
            }
        }*/
       // sendBroadcast(intent);
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
        String arr[]={};
        ArrayList<String> battery_level=new ArrayList<String>(Arrays.asList(arr));



        byte[] value = characteristic.getValue();
        int i = 0;
        for (byte b : value) {
            byte[] bArr = this.buf;
            bArr[i] = b;
            System.out.println("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"+bArr[i]+"------"+i);
            i++;

            if (i == bArr.length) {
                System.out.println("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"+bArr);
                if(bArr[0]==-86&&bArr[19]==-86 && bArr.length==20){
                    System.out.println("SENSORRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRR" + bArr[8] +"----------------"+bArr[8]/10);
                    System.out.println("hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh" + bArr +"----------------");

//                    String arr[]={};
//                    ArrayList<String> battery_level=new ArrayList<String>(Arrays.asList(arr));
                    String one=String.valueOf(bArr[5]);
                    String two=String.valueOf(bArr[6]);
                    //String three=String.valueOf(bArr[8]);
                    float s=bArr[8]/10;
                    String three=String.valueOf(s);
//                    String four=String.valueOf(bArr[3]);
//                    String five=String.valueOf(bArr[4]);
//                    String sp02=String.valueOf(bArr[5]);
//                    String pr=String.valueOf(bArr[6]);
//                    String eight=String.valueOf(bArr[7]);
//                    String nine=String.valueOf(bArr[8]);
//                    String ten=String.valueOf(bArr[9]);
//                    String elev=String.valueOf(bArr[10]);
//                    String twelve=String.valueOf(bArr[11]);
//                    String thirteen=String.valueOf(bArr[12]);
//                    String fourteen=String.valueOf(bArr[13]);
//
//                    String un=String.valueOf(bArr[14]);

                    battery_level.add(one);
                    battery_level.add(two);
                    battery_level.add(three);
//                    battery_level.add(four);
//                    battery_level.add(five);
//
//
//
//                    battery_level.add(sp02);
//                    battery_level.add((pr));
//                    battery_level.add(eight);
//                    battery_level.add(nine);
//                    battery_level.add(ten);
//                    battery_level.add(elev);
//                    battery_level.add(twelve);
//                    battery_level.add(thirteen);
//                    battery_level.add(fourteen);
//



                    System.out.println(battery_level+"qqqqqqqqqqqqqqqqqqqqqq");
                    //  System.out.println("kkkkkkkkkkkkkkk"+bArr[5]);
                }


            }
//
        }

        FileWriter writer = null;

        try {

            writer = new FileWriter(fileName,true);
           /* writer.append("SPO2");
            writer.append(',');
            writer.append("PulseRate");
            writer.append(',');
            writer.append("TimeStamp");
            writer.append('\n');*/

            writer.append(battery_level.get(0));
            writer.append(',');
            writer.append(battery_level.get(1));
            writer.append(',');
            writer.append(battery_level.get(2));
            writer.append(',');

//            writer.append(battery_level.get(2));
//            writer.append(',');
//            writer.append(battery_level.get(3));
//            writer.append(',');
//            writer.append(battery_level.get(4));
//            writer.append(',');
//            writer.append(battery_level.get(5));
//            writer.append(',');
//            writer.append(battery_level.get(6));
//            writer.append(',');
//            writer.append(battery_level.get(7));
//            writer.append(',');
//            writer.append(battery_level.get(8));
//            writer.append(',');
//            writer.append(battery_level.get(9));
//            writer.append(',');
//            writer.append(battery_level.get(10));
//            writer.append(',');
//            writer.append(battery_level.get(11));
//            writer.append(',');
//            writer.append(battery_level.get(12));
//            writer.append(',');
//            writer.append(battery_level.get(13));
//            writer.append(',');
//            writer.append(battery_level.get(14));
//            writer.append(',');
            writer.append((timestamp = new Timestamp(System.currentTimeMillis())).toString());
            writer.append('\n');

//            writer.append(battery_level.get(1));
//            writer.append(',');
//            writer.append(battery_level.get(1));
//            writer.append(',');


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

            System.out.println("upload hoaaageeeeeeeeeeeeee");

            Request request=new Request.Builder()
                    .url("https://"+"nedncl.com/robodoc/upload.php")
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
                    System.out.println(response.body());

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