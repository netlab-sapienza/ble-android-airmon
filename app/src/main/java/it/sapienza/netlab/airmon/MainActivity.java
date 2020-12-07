package it.sapienza.netlab.airmon;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import it.sapienza.netlab.airmon.common.Utility;
import it.sapienza.netlab.airmon.listeners.CustomScanCallback;

public class MainActivity extends AppCompatActivity {

    public static final int REQUEST_ENABLE_BT = 322;
    private static final long SCAN_PERIOD = 5000;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 456;
    private static final String TAG = MainActivity.class.getSimpleName();
    BluetoothManager mBluetoothManager;
    BluetoothAdapter mBluetoothAdapter;
    BluetoothLeScanner mBluetoothLeScanner;
    private Button startScanButton;
    private boolean isMultipleAdvertisementSupported;
    private boolean isScanning;
    private CustomScanCallback mScanCallback;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startScanButton = findViewById(R.id.button_start_scan);
        startScanButton.setOnClickListener(v -> startScan());

        askPermissions(savedInstanceState);
    }


    private void startScan() {
        if (mScanCallback == null) {


            new Handler().postDelayed(this::stopScan, SCAN_PERIOD);

            // Kick off a new scan.
            mScanCallback = new CustomScanCallback();


            mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

            isScanning = true;
            mBluetoothLeScanner.startScan(Utility.buildScanFilters(), Utility.buildScanSettings(), mScanCallback);

        } else {
            Log.d(TAG, "OUD: startScanning: Scanning already started ");
        }
    }

    private void stopScan() {
        mBluetoothLeScanner.stopScan(mScanCallback);

        for (ScanResult scanResult : mScanCallback.getResults()) {
            Log.d(TAG, "scan result" + scanResult.toString());
        }

        mScanCallback = null;
        isScanning = false;
    }


    private void askPermissions(Bundle savedInstanceState) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            checkBluetoothAvailability(savedInstanceState);
        } else {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
        }
    }

    /**
     * Cattura la risposta asincrona di richiesta dei permessi e se è tutto ok passa a controllare il bluetooth
     *
     * @param requestCode  codice richiesta ( per coarse location = PERMISSION_REQUEST_COARSE_LOCATION )
     * @param permissions  permessi richiesti. NB If request is cancelled, the result arrays are empty.
     * @param grantResults int [] rappresentati gli esiti delle richieste
     */

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    checkBluetoothAvailability();
                } else {
                    Log.e(TAG, "onRequestPermissionsResult: Permission denied");
                }
                break;
            default:
                Log.e(TAG, "case not found");
        }
    }

    /**
     * Controlla che il cellulare supporti l'app e il multiple advertisement. Maschera per onActivityResult e onRequestPermissionsResult
     */
    private void checkBluetoothAvailability() {
        checkBluetoothAvailability(null);
    }


    /**
     * Controlla che il cellulare supporti l'app e il multiple advertisement.
     *
     * @param savedInstanceState se l'app era già attiva non devo reinizializzare tutto
     */
    private void checkBluetoothAvailability(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager != null) {
                mBluetoothAdapter = mBluetoothManager.getAdapter();

                // Is Bluetooth turned on?
                if (mBluetoothAdapter.isEnabled()) {
                    // Are Bluetooth Advertisements supported on this device?
                    if (mBluetoothAdapter.isMultipleAdvertisementSupported()) {
                        Log.d(TAG, "Everything is supported and enabled");
                        isMultipleAdvertisementSupported = true;
                    } else {
                        Log.d(TAG, "Your device does not support multiple advertisement, you can be only client");
                        isMultipleAdvertisementSupported = false;
                    }
                } else {
                    // Prompt user to turn on Bluetooth (logic continues in onActivityResult()).
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                }
            } else {
                // Bluetooth is not supported.
                Log.e(TAG, "Bluetooth is not supported");
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            switch (resultCode) {
                case Activity.RESULT_OK:
                    checkBluetoothAvailability();
                    break;
                case Activity.RESULT_CANCELED:
                    Log.e(TAG, "Bluetooth is not enabled. Please reboot application.");
                    break;
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

}