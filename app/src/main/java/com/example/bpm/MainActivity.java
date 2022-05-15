package com.example.bpm;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    // startActivityForResult deprecated so don't need this anymore
    // private static final int ENABLE_BLUETOOTH_REQUEST_CODE = 1;

    // private BluetoothManager mBtManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
    // private BluetoothAdapter mBtAdapter = mBtManager.getAdapter();

    private BluetoothLeScanner scanner;
    public Boolean isScanning = false;
    BluetoothAdapter mBtAdapter = BluetoothAdapter.getDefaultAdapter();
    ScanCallback scanCallBack;
    public ScanResultAdapter adapter;
    public RecyclerView recyclerView;
    public List<ScanResult> scanResults = new ArrayList<>();
    private BluetoothGatt mGatt;
    private int properties;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.i("onCreate", "get default adapter");

        Log.i("onCreate", "set onclick listener for button");
        Button button = (Button) findViewById(R.id.scan_button);
        button.setOnClickListener(
                view -> {
                    if (isScanning) {
                        stopBleScan();
                        button.setText("scan");
                    } else {
                        startBleScan();
                        button.setText("stop scan");
                    }
                }
        );

        setupRecyclerView();
        // Register for broadcasts when a device is discovered.
        // IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        // registerReceiver(receiver, filter);

    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i("onResume", "resume");
        if (!mBtAdapter.isEnabled()) {
            promptEnableBluetooth();
        }
    }

    boolean hasPermission(String permissionType) {
        Log.i("hasPermission", "checking if hasPermission");
        return ContextCompat.checkSelfPermission(this, permissionType) == PackageManager.PERMISSION_GRANTED;
    }

    private void promptEnableBluetooth() {
        Log.i("promptEnableBluetooth", "start func");
        // request bluetooth to be enabled if it's not and be annoying about it (inf loop if deny)
        if (!mBtAdapter.isEnabled()) {
            Log.i("promptEnableBluetooth", "intent");
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            Log.i("promptEnableBluetooth", "prompt");
            activityResultLauncher.launch(enableBtIntent);
        }
    }

    ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Log.i("activityResultLauncher", "start lambda");
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Log.i("activityResultLauncher", "OK");
                    // Intent data = result.getData();
                } else {
                    Log.i("activityResultLauncher", "BAD");
                }
            }
    );

    ActivityResultLauncher<String> locationPermissionRequest =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    result -> {
                        if (!result) {
                            Log.i("locationPermissionRequest", "result false");
                        } else {
                            Log.i("locationPermissionRequest", "result true");
                        }
                    }
            );

    private void startBleScan() {
        Log.i("startBleScan", "------------------------------");
        Log.i("startBleScan", "invoked");
        if (hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            Log.i("startBleScan", "has location");

            // clear list of results on each scan
            // int scanResultSize = scanResults.size() - 1;
            scanResults.clear();
            adapter.notifyDataSetChanged();
            // adapter.notifyItemRangeRemoved(0, scanResultSize);
            Log.i("startBleScan", "cleared existing results");

            scanner = mBtAdapter.getBluetoothLeScanner();
            ScanSettings settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                    // .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
                    // .setNumOfMatches(ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT)
                    // .setReportDelay(0L)
                    .build();
            scanCallBack = createDeviceCallback();

            UUID heartRateServiceUuid = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb");
            UUID heartRateCharUuid = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb");

            // devices UUID service
            ParcelUuid parcelUuid = new ParcelUuid(UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb"));
            // devices UUID service mask
            ParcelUuid parcelUuidMask = new ParcelUuid(UUID.fromString("FFFFFFFF-FFFF-FFFF-FFFF-FFFFFFFFFFFF"));
            ScanFilter scanFilter = new ScanFilter.Builder().setServiceUuid(parcelUuid, parcelUuidMask).build();
            ArrayList<ScanFilter> scanFilterList = new ArrayList<>();
            scanFilterList.add(scanFilter);

            // scanner.startScan(null, settings, scanCallBack);
            scanner.startScan(scanFilterList, settings, scanCallBack);
            isScanning = true;
            Log.i("startBleScan", "set isScanning true");
            Log.i("startBleScan", "done");
        } else {
            locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            Log.i("startBleScan", "no location");
        }

    }

    @SuppressLint("MissingPermission")
    private void stopBleScan() {
        Log.i("stopBleScan", "invoked");
        scanner.stopScan(scanCallBack);
        isScanning = false;
        Log.i("startBleScan", "set isScanning false");
    }

    private int getFirstScanResult(List<ScanResult> scanResults, ScanResult result) {
        // for (ScanResult curr : scanResults) {
        //     if (curr.getDevice().getAddress().equals(result.getDevice().getAddress())) {
        //         return curr;
        //     }
        // }
        for (int i = 0; i < scanResults.size(); i++) {
            if (scanResults.get(i).getDevice().getAddress().equals(result.getDevice().getAddress())) {
                return i;
            }
        }
        return -1;
    }

    // private final Map<String, BluetoothDevice> devices = new HashMap<>();
    private ScanCallback createDeviceCallback() {
        return new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                // Log.i("createDeviceCallbac", "onScanResult callbacktype = " + callbackType);
                BluetoothDevice device = result.getDevice();
                // ParcelUuid[] a = device.getUuids();
                Log.i("createDeviceCallback", "Name: " + device.getName() + " Addr:" + device.getAddress() + ", class" + device.getBluetoothClass() + ", type" + device.getType());

                int indexQuery = getFirstScanResult(scanResults, result);
                Log.i("onScanResult", "indexQuery is " + Integer.toString(indexQuery));
                if (indexQuery != -1) { // scan result already exists
                    scanResults.set(indexQuery, result);
                    // scanResultAdapter.notifyItemChanged(indexQuery);
                    adapter.notifyItemChanged(indexQuery);
                    Log.i("scanResult", "BLE device already exists");
                } else {
                    Log.i("scanResult", "new BLE device found");
                    scanResults.add(result);
                    // Log.i("scanResult", "current num of scanResults is " + (scanResults.size() - 1));
                    adapter.notifyItemInserted(scanResults.size() - 1);
                    // scanResultAdapter.notifyItemInserted(scanResults.size - 1);
                    Log.i("scanResult", "end");
                }

            }

            @Override
            public void onScanFailed(int errorCode) {
                Log.e("createDeviceCallback", "onScanFailed: code " + errorCode);
            }
        };
    }

    public void foo() {
        Log.i("FOO", "hello");
    }

    private void setupRecyclerView() {
        Log.i("setupReyclerView", "invoke");
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.scan_results_recycler_view);

        Button button = (Button) findViewById(R.id.scan_button);

        // RecyclerView.Adapter<ScanResultAdapter.ViewHolder> adapter = new ScanResultAdapter(MainActivity.this);
        View.OnClickListener onClickListener = new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // Log.d("test onclicklistener input", "Element " + getAdapterPosition() + " clicked.");
                Log.d("onClick select item", "------------------------");
                Log.d("onClick select item", "viewholder onclick");
                Log.d("onClick select itemonClick select item", isScanning.toString());
                if (isScanning) {
                    stopBleScan();
                    button.setText("scan");
                }
                int itemPosition = recyclerView.getChildAdapterPosition(v);
                Log.d("onClick select item", "item position is " + itemPosition);
                ScanResult result = scanResults.get(itemPosition);
                connectToDevice(result.getDevice());
                mGatt.discoverServices();
            }

        };
        adapter = new ScanResultAdapter(this, scanResults, onClickListener);
        recyclerView.setAdapter(adapter);

        // LinearLayoutManager layoutManager = new LinearLayoutManager(this, RecyclerView.VERTICAL, false);
        LinearLayoutManager layoutManager = new LinearLayoutManager(MainActivity.this, RecyclerView.VERTICAL, false);
        recyclerView.setLayoutManager(layoutManager);
    }

    public void connectToDevice(BluetoothDevice device) {
        Log.d("connectToDevice", "invoked");
        device.connectGatt(this, false, gattCallback);
        // mGatt = device.connectGatt(this, false, gattCallback);
        if (mGatt == null) {
            Log.d("connectToDevice", "connect to gatt");
            mGatt = device.connectGatt(this, false, gattCallback);
        }
    }

    // GATT STUFF ----------------------------------------------------
    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.d("onConnectionStateChange", "Status: " + status);
            String deviceAddress = gatt.getDevice().getAddress();

            if (status == BluetoothGatt.GATT_SUCCESS) { // if gatt success
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.d("BluetoothGattCallback", "Successfully connected to " +deviceAddress);
                    // mGatt = gatt;
                    Log.d("BluetoothGattCallback", "before call discover service");
                    mGatt.discoverServices();
                    Log.d("BluetoothGattCallback", "after call discover service");
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.d("BluetoothGattCallback", "Successfully disconnected from $deviceAddress");
                    gatt.close();
                }
            } else { // if gatt not success
                Log.d("BluetoothGattCallback", "Error $status encountered for $deviceAddress! Disconnecting...");
                gatt.close();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            List<BluetoothGattService> services = gatt.getServices();
            Log.i("onServicesDiscovered", services.toString());
            // gatt.readCharacteristic(services.get(1).getCharacteristics().get(0));
            if (services.isEmpty()) {
                Log.i("printGattTable", "No service and characteristic available, call discoverServices() first?");
                return;
            }

            for (BluetoothGattService service : services) {
                Log.d("onServicesDiscovered", "---------------------------------------");
                Log.d("onServicesDiscovered", "service: " + service.toString());
                Log.d("onServicesDiscovered", "uuid: " + service.getUuid().toString());

                // String chrs = (String) service.getCharacteristics().stream().collect(joining(","));
                List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();

                for (BluetoothGattCharacteristic characteristic : characteristics) {
                    properties = characteristic.getProperties();
                    if (BluetoothGattCharacteristicIsReadable(properties)) {
                        // Log.d("onServicesDiscovered", "characteristic: " + characteristic.toString());
                        Log.d("onServicesDiscovered", "readable characteristic uuid: " + characteristic.getUuid().toString());
                    }
                    if (BluetoothGattCharacteristicIsNotifiable(properties)) {
                        Log.d("onServicesDiscovered", "notifiable characteristic uuid: " + characteristic.getUuid().toString());
                    }
                }
            }

            readHeartRate();
            readBatteryLevel();
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.i("onCharacteristicRead", "status: " + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i("onCharacteristicRead", "success");
                Log.i("onCharacteristicRead", "value: " + Arrays.toString(characteristic.getValue()));
                Log.i("onCharacteristicRead", "len: " + characteristic.getValue().length);
                // Log.i("onCharacteristicRead", "first: " + characteristic.getValue()[0]);
            } else if (status == BluetoothGatt.GATT_READ_NOT_PERMITTED) {
                Log.i("onCharacteristicRead", "read not permitted");
            } else {
                Log.i("onCharacteristicRead", "read failed");
            }
        }

        private double extractHeartRate(BluetoothGattCharacteristic characteristic) {
            int flag = characteristic.getProperties();
            int format = -1;
            // Heart rate bit number format
            if ((flag & 0x01) != 0) {
                format = BluetoothGattCharacteristic.FORMAT_UINT16;
            } else {
                format = BluetoothGattCharacteristic.FORMAT_UINT8;
            }
            final int heartRate = characteristic.getIntValue(format, 1);
            return heartRate;
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            Log.i("onCharacteristicChanged", "invoked");
            Log.i("onCharacteristicChanged", "properties" + characteristic.getProperties());
            Log.i("onCharacteristicChanged", "value: " + Arrays.toString(characteristic.getValue()));
            Log.i("onCharacteristicChanged", "value2: " + characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0));
            Log.i("onCharacteristicChanged", "value3: " + extractHeartRate(characteristic));
            int currHeartRate = (int) extractHeartRate(characteristic);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    TextView hr = (TextView) findViewById(R.id.heart_rate_text_view);
                    hr.setText(Integer.toString(currHeartRate));
                }
            });
            Log.i("onCharacteristicChanged", "len: " + characteristic.getValue().length);
            // if (status == BluetoothGatt.GATT_SUCCESS) {
            //     Log.i("onCharacteristicRead", "success");
            //     Log.i("onCharacteristicRead", "value: " + Arrays.toString(characteristic.getValue()));
            //     Log.i("onCharacteristicRead", "len: " + characteristic.getValue().length);
            //     // Log.i("onCharacteristicRead", "first: " + characteristic.getValue()[0]);
            // } else if (status == BluetoothGatt.GATT_READ_NOT_PERMITTED) {
            //     Log.i("onCharacteristicRead", "read not permitted");
            // } else {
            //     Log.i("onCharacteristicRead", "read failed");
            // }
        }
        private boolean BluetoothGattCharacteristicContainsProperty(int properties, int property) {
            return (properties & property) != 0;
        }

        private boolean BluetoothGattCharacteristicIsReadable(int properties) {
            return BluetoothGattCharacteristicContainsProperty(properties, BluetoothGattCharacteristic.PROPERTY_READ);
        }

        private boolean BluetoothGattCharacteristicIsWritable(int properties) {
            return BluetoothGattCharacteristicContainsProperty(properties, BluetoothGattCharacteristic.PROPERTY_WRITE);
        }

        private boolean BluetoothGattCharacteristicIsWritableWithoutResponse(int properties) {
            return BluetoothGattCharacteristicContainsProperty(properties, BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE);
        }

        private boolean BluetoothGattCharacteristicIsIndicatable(int properties) {
            return BluetoothGattCharacteristicContainsProperty(properties, BluetoothGattCharacteristic.PROPERTY_INDICATE);
        }

        private boolean BluetoothGattCharacteristicIsNotifiable(int properties) {
            return BluetoothGattCharacteristicContainsProperty(properties, BluetoothGattCharacteristic.PROPERTY_NOTIFY);
        }

        private void readBatteryLevel() {
            UUID batteryServiceUuid = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb");
            UUID batteryLevelCharUuid = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb");
            BluetoothGattService batteryLevelService = mGatt.getService(batteryServiceUuid);
            BluetoothGattCharacteristic batteryLevelChar = null;
            if (batteryLevelService != null) {
                batteryLevelChar = batteryLevelService.getCharacteristic(batteryLevelCharUuid);
            }
            if (batteryLevelChar != null && BluetoothGattCharacteristicIsReadable(batteryLevelChar.getProperties()))
                mGatt.readCharacteristic(batteryLevelChar);
        }

        private void readHeartRate() {
            Log.v("test", "reading heart rate");
            UUID heartRateServiceUuid = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb");
            UUID heartRateCharUuid = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb");
            Log.v("test", "reading heart rate 2");
            BluetoothGattService heartRateService = mGatt.getService(heartRateServiceUuid);
            BluetoothGattCharacteristic heartRateChar = null;
            Log.v("test", "reading heart rate 3");
            if (heartRateService != null) {
                heartRateChar = heartRateService.getCharacteristic(heartRateCharUuid);
                Log.v("test", "reading heart rate 4");
            }
            // if (heartRateChar != null && BluetoothGattCharacteristicIsReadable(heartRateChar.getProperties())) {
            //     mGatt.readCharacteristic(heartRateChar);
            //     Log.v("test", "reading heart rate 5");
            // }

            if (mGatt.setCharacteristicNotification(heartRateChar, true) == false) {
                Log.e("set characteristic notification", "failed");
            }
        }


    };




}