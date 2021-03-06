package it.sapienza.netlab.airmon;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.tasks.Task;

import java.util.Arrays;

import it.sapienza.netlab.airmon.listeners.ServerScanCallback;

import static it.sapienza.netlab.airmon.common.Utility.isBLESupported;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CHECK_SETTINGS = 0x1;
    public static final int REQUEST_ENABLE_BT = 322;
    private static final long SCAN_PERIOD = 5000;
    private static final int PERMISSION_REQUEST_FINE_LOCATION = 456;
    private static final String TAG = MainActivity.class.getSimpleName();

    BluetoothManager mBluetoothManager;
    BluetoothAdapter mBluetoothAdapter;
    BluetoothLeScanner bluetoothLeScanner;
    private Button startScanButton, sendMessageButton;
    private boolean isMultipleAdvertisementSupported;
    private boolean mScanning = false;
    private TextView debugger;
//    private BLEClient client;
//    private FusedLocationProviderClient fusedLocationClient;
//    private String latitude;
//    private String longitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        debugger = findViewById(R.id.debugger);
        startScanButton = findViewById(R.id.startService);
        sendMessageButton = findViewById(R.id.sendMessage);

        startScanButton.setOnClickListener(v -> startService());
        sendMessageButton.setOnClickListener(view -> sendMessage());
        cleanDebug();
        askPermissions(savedInstanceState);
//        this.client = BLEClient.getInstance(this.getApplicationContext());

        bluetoothLeScanner = BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();

        //fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    }

    private void sendMessage() {
//        if (this.client.IsDeviceConnected()) {
//            this.client.sendMessage("ciao", "ciao2", "ciao3");
//        } else {
//            writeErrorDebug("Client not initialized");
//        }
    }


    private void startService() {
        writeDebug("Start scan");
//        this.client.addOnClientOnlineListener(() -> {
//            writeDebug("New node found");
//        });
//        this.client.startClient();
        ServerScanCallback cb = new ServerScanCallback(new ServerScanCallback.OnServerFoundMessageListener() {
            @Override
            public void OnServerFound(String message) {
                writeDebug(message);
            }

            @Override
            public void OnErrorScan(String message, int errorCodeCallback) {
                writeErrorDebug(message);
            }
        });

        if (!mScanning) {
            // Stops scanning after a pre-defined scan period.
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    bluetoothLeScanner.stopScan(cb);
                    writeDebug("Scan Stop");
                    for (ScanResult result : cb.getResults()) {
                        writeDebug(result.getDevice().getAddress());
                    }
                }
            }, SCAN_PERIOD);

            mScanning = true;
//            bluetoothLeScanner.startScan(Utility.buildScanFilters(), Utility.buildScanSettings(), cb);
            bluetoothLeScanner.startScan(cb);
        } else {
            mScanning = false;
            bluetoothLeScanner.stopScan(cb);
        }

    }

    private void stopScan() {
//        this.client.stopClient();
    }


    private void askPermissions(Bundle savedInstanceState) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            checkBluetoothAvailability(savedInstanceState);
        } else {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_FINE_LOCATION);
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
            case PERMISSION_REQUEST_FINE_LOCATION:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    checkBluetoothAvailability();
                } else {
                    writeErrorDebug("onRequestPermissionsResult: Permission denied");
                }
                break;
            default:
                writeErrorDebug("Case not found.");
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
                if (mBluetoothAdapter.isEnabled() && isBLESupported(this)) {
                    // Are Bluetooth Advertisements supported on this device?
                    if (mBluetoothAdapter.isMultipleAdvertisementSupported()) {
                        writeDebug("Everything is supported and enabled");
                        isMultipleAdvertisementSupported = true;
                    } else {
                        isMultipleAdvertisementSupported = false;
                        writeDebug("Your device does not support multiple advertisement, you can be only client");
                    }
                } else {
                    // Prompt user to turn on Bluetooth (logic continues in onActivityResult()).
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                }
            } else {
                // Bluetooth is not supported.
                writeErrorDebug("Bluetooth is not supported");
            }
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CHECK_SETTINGS) {

            switch (resultCode) {
                case Activity.RESULT_OK:
                    writeDebug("GPS OK");
                    break;
                case Activity.RESULT_CANCELED:
                    writeErrorDebug("GPS request was cancelled.");
                    setGPSOn();
                    break;
            }

        } else if (requestCode == REQUEST_ENABLE_BT) {
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

    /**
     * Makes request to enable GPS
     */
    protected void setGPSOn() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(30 * 1000);
        locationRequest.setFastestInterval(5 * 1000);
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);
        builder.setAlwaysShow(true);

        Task<LocationSettingsResponse> task = LocationServices.getSettingsClient(this).checkLocationSettings(builder.build());
        task.addOnCompleteListener(task1 -> {
            try {
                task1.getResult(ApiException.class);
            } catch (ApiException exception) {
                switch (exception.getStatusCode()) {
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        // Location settings are not satisfied. But could be fixed by showing the
                        // user a dialog.
                        try {
                            // Cast to a resolvable exception.
                            ResolvableApiException resolvable = (ResolvableApiException) exception;
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            resolvable.startResolutionForResult(
                                    MainActivity.this,
                                    REQUEST_CHECK_SETTINGS);
                        } catch (IntentSender.SendIntentException | ClassCastException e) {
                            // Ignore the error.
                            writeErrorDebug("GPS: " + e.getMessage());
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        writeErrorDebug("Location settings are not satisfied. However, we have no way to fix the settings so we won't show the dialog.");
                        break;
                }
            }
        });
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            // TODO: Consider calling
//            //    ActivityCompat#requestPermissions
//            // here to request the missing permissions, and then overriding
//            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//            //                                          int[] grantResults)
//            // to handle the case where the user grants the permission. See the documentation
//            // for ActivityCompat#requestPermissions for more details.
//            return;
    }
//        fusedLocationClient.getLastLocation()
//                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
//                    @Override
//                    public void onSuccess(Location location) {
//                        // Got last known location. In some rare situations this can be null.
//                        latitude = Double.toString(location.getLatitude());
//                        longitude = Double.toString(location.getLongitude());
//                    }
//                });
//    }
//
//    public String catchLatitude() {
//        return latitude;
//    }
//
//    public String catchLongitude() {
//        return longitude;
//    }


    /**
     * Clean the field debugger
     */
    private void cleanDebug() {
        runOnUiThread(() -> debugger.setText(""));
    }

    /**
     * Write a message debug into log and text debugger. The message will be logged into the debug logger.
     *
     * @param message message to be written
     */
    private void writeDebug(final String message) {
        runOnUiThread(() -> {
            if (debugger.getLineCount() == debugger.getMaxLines())
                debugger.setText(String.format("%s\n", message));
            else
                debugger.setText(String.format("%s%s\n", debugger.getText(), message));
        });
        Log.d(TAG, message);
    }

    /**
     * Write a message debug into log and text debugger. The message will be logged into the error logger.
     *
     * @param message message to be written
     */
    private void writeErrorDebug(final String message) {
        runOnUiThread(() -> {
            if (debugger.getLineCount() == debugger.getMaxLines())
                debugger.setText(String.format("%s\n", message));
            else
                debugger.setText(String.format("%s%s\n", debugger.getText(), message));
        });
        Log.e(TAG, message);
    }

}