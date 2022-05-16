package com.example.bpm;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
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
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Looper;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private BluetoothLeScanner scanner;
    public Boolean isScanning = false;
    public BluetoothAdapter mBtAdapter = BluetoothAdapter.getDefaultAdapter();
    public ScanCallback scanCallBack;
    public ScanResultAdapter adapter;
    public RecyclerView recyclerView;
    public List<ScanResult> scanResults = new ArrayList<>();
    private BluetoothGatt mGatt;
    private Button button;

    private final int lowerBound = 120;
    private final int upperBound = 150;
    // private int newHeartRate;
    private int currHeartRate = 50;
    private float direction = 0;
    private ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);

    private final UUID heartRateServiceUuid = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb");
    private final UUID heartRateCharUuid = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb");

    // devices UUID service
    private final ParcelUuid parcelUuid = new ParcelUuid(heartRateServiceUuid);
    // devices UUID service mask
    private final ParcelUuid parcelUuidMask = new ParcelUuid(UUID.fromString("FFFFFFFF-FFFF-FFFF-FFFF-FFFFFFFFFFFF"));



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        button = (Button) findViewById(R.id.scan_button);
        button.setOnClickListener(
                view -> {
                    if (isScanning) {
                        stopBleScan();
                    } else {
                        startBleScan();
                    }
                }
        );
        setupRecyclerView();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!mBtAdapter.isEnabled()) {
            promptEnableBluetooth();
        }
    }

    private void promptEnableBluetooth() {
        // request bluetooth to be enabled. if it's not and be annoying about it (inf loop if deny)
        if (!mBtAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activityResultLauncher.launch(enableBtIntent);
        }
    }

    private boolean hasPermission(String permissionType) {
        return ContextCompat.checkSelfPermission(this, permissionType) == PackageManager.PERMISSION_GRANTED;
    }

    private ActivityResultLauncher<Intent> activityResultLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() != Activity.RESULT_OK) {
                            // do smth
                            Log.i("activityResultLauncher", "BAD");
                        }
                    }
            );

    private ActivityResultLauncher<String> permissionRequestLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    result -> {
                        if (!result) {
                            // do smth
                            Log.i("permissionRequestLauncher", "BAD");
                        }
                    }
            );

    private void startBleScan() {
        if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            permissionRequestLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        // clear list of results on each scan
        scanResults.clear();
        adapter.notifyDataSetChanged();

        scanner = mBtAdapter.getBluetoothLeScanner();
        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                // .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
                // .setNumOfMatches(ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT)
                // .setReportDelay(0L)
                .build();
        scanCallBack = createDeviceCallback();

        ScanFilter scanFilter = new ScanFilter.Builder()
                .setServiceUuid(parcelUuid, parcelUuidMask)
                .build();

        ArrayList<ScanFilter> scanFilterList = new ArrayList<>();
        scanFilterList.add(scanFilter);

        scanner.startScan(scanFilterList, settings, scanCallBack);

        isScanning = true;
        button.setText("Stop Scan");
    }


    private void stopBleScan() {
        Log.i("stopBleScan", "invoked");
        scanner.stopScan(scanCallBack);
        isScanning = false;
        button.setText("Scan");
    }

    private boolean scanResultsAreEqual(ScanResult r1, ScanResult r2) {
        return r1.getDevice().getAddress().equals(r2.getDevice().getAddress());
    }

    private int getFirstScanResult(ScanResult result, List<ScanResult> scanResults) {
        for (int i = 0; i < scanResults.size(); i++) {
            if (scanResultsAreEqual(scanResults.get(i), result)) {
                return i;
            }
        }
        return -1;
    }

    private ScanCallback createDeviceCallback() {
        return new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                BluetoothDevice device = result.getDevice();
                Log.i("createDeviceCallback", "Name: " + device.getName() + " Addr:" + device.getAddress());

                int indexQuery = getFirstScanResult(result, scanResults);
                if (indexQuery != -1) { // scan result already exists
                    scanResults.set(indexQuery, result);
                    adapter.notifyItemChanged(indexQuery);
                } else {
                    Log.i("scanResult", "new BLE device found");
                    scanResults.add(result);
                    adapter.notifyItemInserted(scanResults.size() - 1);
                }
            }

            @Override
            public void onScanFailed(int errorCode) {
                Log.e("createDeviceCallback", "onScanFailed: code " + errorCode);
            }
        };
    }

    private void setupRecyclerView() {
        recyclerView = (RecyclerView) findViewById(R.id.scan_results_recycler_view);

        // should already exist
        // button = (Button) findViewById(R.id.scan_button);

        View.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isScanning) {
                    stopBleScan();
                }
                int itemPosition = recyclerView.getChildAdapterPosition(v);
                ScanResult result = scanResults.get(itemPosition);
                connectToDevice(result.getDevice());
                mGatt.discoverServices();
            }

        };
        adapter = new ScanResultAdapter(this, scanResults, onClickListener);
        recyclerView.setAdapter(adapter);

        LinearLayoutManager layoutManager = new LinearLayoutManager(
                MainActivity.this, RecyclerView.VERTICAL, false
        );
        recyclerView.setLayoutManager(layoutManager);
    }

    public void connectToDevice(BluetoothDevice device) {
        device.connectGatt(this, false, gattCallback);
        if (mGatt == null) {
            mGatt = device.connectGatt(this, false, gattCallback);
        }
    }

    // GATT STUFF ----------------------------------------------------
    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String deviceAddress = gatt.getDevice().getAddress();

            if (status == BluetoothGatt.GATT_SUCCESS) { // if gatt success
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.d("BluetoothGattCallback", "Successfully connected to " + deviceAddress);
                    // mGatt = gatt;
                    mGatt.discoverServices();
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
            if (services.isEmpty()) {
                Log.i("printGattTable", "No service and characteristic available, call discoverServices() first?");
                return;
            }

            for (BluetoothGattService service : services) {
                List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();

                for (BluetoothGattCharacteristic characteristic : characteristics) {
                    int properties = characteristic.getProperties();
                    if (BluetoothGattCharacteristicIsReadable(properties)) {
                        Log.d("onServicesDiscovered", "readable characteristic uuid: " + characteristic.getUuid().toString());
                    }
                    if (BluetoothGattCharacteristicIsNotifiable(properties)) {
                        Log.d("onServicesDiscovered", "notifiable characteristic uuid: " + characteristic.getUuid().toString());
                    }
                }
            }

            readHeartRate();
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i("onCharacteristicRead", "success");
            } else if (status == BluetoothGatt.GATT_READ_NOT_PERMITTED) {
                Log.i("onCharacteristicRead", "read not permitted");
            } else {
                Log.i("onCharacteristicRead", "read failed");
            }
        }

        private double extractHeartRate(BluetoothGattCharacteristic characteristic) {
            int flag = characteristic.getProperties();
            int format = -1;
            if ((flag & 0x01) != 0) {
                format = BluetoothGattCharacteristic.FORMAT_UINT16;
            } else {
                format = BluetoothGattCharacteristic.FORMAT_UINT8;
            }
            final int heartRate = characteristic.getIntValue(format, 1);
            return heartRate;
        }

        private void toBeepOrNotToBeep(int newHeartRate) {
            // newHeartRate = (int) extractHeartRate(characteristic);
            direction = currHeartRate - newHeartRate;
            currHeartRate = newHeartRate;
            if (currHeartRate < lowerBound & direction < 0) {
                toneG.startTone(ToneGenerator.TONE_PROP_BEEP,500);
            } else if (currHeartRate > upperBound & direction > 0) {
                toneG.startTone(ToneGenerator.TONE_PROP_BEEP2, 500);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            toBeepOrNotToBeep((int) extractHeartRate(characteristic));
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    TextView hr = (TextView) findViewById(R.id.heart_rate_text_view);
                    hr.setText(Integer.toString(currHeartRate));
                }
            });
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

        private void readHeartRate() {
            BluetoothGattService heartRateService = mGatt.getService(heartRateServiceUuid);
            BluetoothGattCharacteristic heartRateChar = null;
            if (heartRateService != null) {
                heartRateChar = heartRateService.getCharacteristic(heartRateCharUuid);
            }

            if (mGatt.setCharacteristicNotification(heartRateChar, true) == false) {
                Log.e("set characteristic notification", "failed");
            }
        }


    };

}