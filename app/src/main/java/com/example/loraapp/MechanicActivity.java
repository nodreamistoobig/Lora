package com.example.loraapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.blikoon.qrcodescanner.QrCodeActivity;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.List;

public class MechanicActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    SupportMapFragment mapFragment;
    LatLng myPlace;

    private static final int REQUEST_CODE_QR_SCAN = 101;
    private final int REQUEST_CODE_LOCATION = 1001;

    private LocationManager locationManager;
    Location my_location;
    boolean granted;

    Intent intentScan, intentTrans;
    String url = "http://192.168.50.170:5000";

    int mode = 0;
    Button dataButton, deleteButton;
    String chosenDevice;
    String[] devices = {"5"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mechanic);

        dataButton = findViewById(R.id.dataButton);
        deleteButton = findViewById(R.id.deleteButton);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        LocationListener listener = helpLocation();

            // boolean granted - ???????????????????? ?????????????? ???? ?? ?????? ?????? ???????????????????? ?????? ??????????
            // ????????????????????, ?????????????? ???? ?????????????????????? :
            // - ???? ?????????????????????? ????????????????????????????
            // - ???? ???????????????? ????????????
            // - ???? ???????????? ?? ???????????? ?? ???????????? ????????????????????

        if (granted || checkPermission(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,  Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE})) {
            Log.d("mytag", Boolean.toString(granted));
            //locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000 * 10, 20, listener);
            locationManager=(LocationManager) getSystemService(LOCATION_SERVICE);
            if (locationManager != null) {
                //my_location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                my_location  = new Location(locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER));
                myPlace = new LatLng(my_location.getLatitude(), my_location.getLongitude());
            }
        }
    }

    private boolean checkPermission(String[] permissions) {
        // ?????????????? ?????? ???????????????????? ????????????????????, ???????? ?????? ??????, ???????? ???????????????? ?????? ????????????????????, ?????? ???? ?????????????? ?????????? ?? 87 ??????????????

        for (int i=0; i<permissions.length; i++) {
            if (ActivityCompat.checkSelfPermission(this,
                    permissions[i]) != PackageManager.PERMISSION_GRANTED) {
                mapFragment.requestPermissions(
                        permissions, REQUEST_CODE_LOCATION);
            }
        }

        if ((ActivityCompat.checkSelfPermission(this,
                permissions[0]) == PackageManager.PERMISSION_GRANTED)&&(ActivityCompat.checkSelfPermission(this,
                permissions[1]) == PackageManager.PERMISSION_GRANTED)) {
            granted = true;
            return true;
        } else return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public LocationListener helpLocation() {
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        LocationListener listener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                if (location == null) {
                    return;
                } else {
                   myPlace = new LatLng(location.getLatitude(), location.getLongitude());
                }
            }
        };

        return listener;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        // ???????????????????? ??????????????, ???????? ???? ????????????????
        mMap = googleMap;

        if (granted == true) {

            mMap.setMyLocationEnabled(true);
            ServerConnection conn = new ServerConnection(url, this);
            conn.getDevices(setMarkers);

            mMap.moveCamera(CameraUpdateFactory.newLatLng(myPlace));
            CameraPosition cameraPosition = new CameraPosition(myPlace, 23, 45, 15);
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(15));
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        }
    }


    @SuppressLint("MissingPermission")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK) {
            Log.d("SCAN", "COULD NOT GET A GOOD RESULT.");
            if (data == null)
                return;
            String result = data.getStringExtra("com.blikoon.qrcodescanner.error_decoding_image");
            if (result != null) {
                AlertDialog alertDialog = new AlertDialog.Builder(MechanicActivity.this).create();
                alertDialog.setTitle("Scan Error");
                alertDialog.setMessage("QR Code could not be scanned");
                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                alertDialog.show();
            }
            return;

        }
        if (requestCode == REQUEST_CODE_QR_SCAN) {
            if (data == null)
                return;
            String result = data.getStringExtra("com.blikoon.qrcodescanner.got_qr_scan_relult");

            double latitude=0, longitude=0;
            if (locationManager != null) {
                Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (location == null) {
                    location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                }
                if (location != null) {
                    latitude = location.getLongitude();
                    longitude = location.getLatitude();
                    Log.d("DATA", "Have scan result in your app activity :" + result + " " + latitude + " " + longitude);
                }
            }

            ServerConnection conn = new ServerConnection(url, this);
            if(mode == 1){
                conn.sendPos(result, new DeviceCoords(latitude, longitude), "reg");
            }
            else {
                conn.checkPos(result, latitude, longitude);
            }
        }
    }

    public void gotoMain(View v) {
        Intent intent = new Intent(MechanicActivity.this, MainActivity.class);
        startActivity(intent);
    }

    public void gotoQRCheckRegister(View v) {
        intentScan = new Intent(MechanicActivity.this, QrCodeActivity.class);
        startActivityForResult(intentScan, REQUEST_CODE_QR_SCAN);
        mode = 1;
    }

    public void gotoQRCheckTransfer(View v) {
            intentTrans = new Intent(MechanicActivity.this, QrCodeActivity.class);
            startActivityForResult(intentTrans, REQUEST_CODE_QR_SCAN);
            mode = 2;
    }

    @Override
    protected void onDestroy() {
        mMap.clear();
        super.onDestroy();
    }

    public void gotoData(View v) {
        Intent intent = new Intent(MechanicActivity.this, SensorActivity.class);
        intent.putExtra("device_code", chosenDevice);
        startActivity(intent);
    }

    Runnable setMarkers = new Runnable() {
        @Override
        public void run() {
            ServerConnection conn = new ServerConnection(url, MechanicActivity.this);

            Role deviceList = (Role) getApplicationContext();
            List<String> devices = deviceList.getDevices();

            for (String device : devices){
                conn.setMarker(device, mMap);
                mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                    @Override
                    public boolean onMarkerClick(@NonNull Marker marker) {
                        conn.setCurValue(device, marker);
                        if (dataButton.getVisibility() == View.VISIBLE){
                            dataButton.setVisibility(View.INVISIBLE);
                            deleteButton.setVisibility(View.INVISIBLE);
                        }
                        else{
                            dataButton.setVisibility(View.VISIBLE);
                            deleteButton.setVisibility(View.VISIBLE);
                            deleteButton.setOnClickListener(v -> {
                                marker.remove();
                                conn.delete(device);
                            });
                            chosenDevice = device;
                        }
                        return true;
                    }
                });
            }
        }
    };
}