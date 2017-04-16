package com.findrone.userburiedapp;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "Debug";
    private final int PERMISSIONS_LOCATION = 0;
    private final float meters = 0.01f;
    private TextView txtGpsLastUpdate = null;
    private TextView txtLatitude = null;
    private TextView txtLongitude = null;
    private TextView txtRawLocation = null;
    private TextView txtAdvise = null;
    private ToggleButton tbService = null;
    private Switch swWiFiConn = null;
    private LocationManager locationManager = null;
    private LocationListener locationListener = null;
    private int cntCheck = 0;
    private float accuracy = Float.MAX_VALUE;

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_LOCATION: {
                //If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //Permission was granted
                    doThings();
                } else {
                    //Permission denied
                    txtAdvise.setText("Permission problem, reboot app.");
                    //doThings() //Again ?
                }
            }
            //Other 'case' lines to check for other
            //permissions this app might request
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtLatitude = (TextView) findViewById(R.id.txtLatitude);
        txtLongitude = (TextView) findViewById(R.id.txtLongitude);
        txtRawLocation = (TextView) findViewById(R.id.txtRawLocation);
        txtAdvise = (TextView) findViewById(R.id.txtAdvise);
        txtGpsLastUpdate = (TextView) findViewById(R.id.txtGpsLastUpdate);
        tbService = (ToggleButton) findViewById(R.id.tbService);
        swWiFiConn = (Switch) findViewById(R.id.swWiFiConn);
        if (swWiFiConn != null) {
            swWiFiConn.setClickable(false);
        }
        locationListener = new MyLocationListener();
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        tbService.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    doThings();
                } else {
                    locationManager.removeUpdates(locationListener);
                }
            }
        });
    }

    public void doThings() {
        final int stopAfter = 15000;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSIONS_LOCATION);
        } else {
            //locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, meters, locationListener);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, meters, locationListener);
        }

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                cntCheck++;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        txtAdvise.setText("Accuracy: " + accuracy + "\nChecks : " +
                                cntCheck + "\nStopAfer: " + stopAfter);
                    }
                });

                if (accuracy <= 1.0) {
                    locationManager.removeUpdates(locationListener);
                    accuracy = Float.MAX_VALUE;
                    cntCheck = 0;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tbService.setChecked(false);
                        }
                    });
                    this.cancel();
                }
            }
        }, 0, stopAfter);
    }

    private String getDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss dd-MM-yyyy");
        return sdf.format(new Date());
    }

    /*----------Listener class to get coordinates ------------- */
    private class MyLocationListener implements LocationListener {
        @Override
        public void onLocationChanged(Location loc) {
            //Debug
            String longitude = "Longitude: " + loc.getLongitude();
            Log.v(TAG, longitude);
            String latitude = "Latitude: " + loc.getLatitude();
            accuracy = loc.getAccuracy();
            Log.v(TAG, latitude);

            txtLatitude.setText("Latitude: " + loc.getLatitude());
            txtLongitude.setText("Longitude: " + loc.getLongitude());
            txtGpsLastUpdate.setText("GPS Last Update: " + getDateTime());
            txtRawLocation.setText(loc.toString());
        }

        @Override
        public void onProviderDisabled(String provider) {
            // TODO Auto-generated method stub
        }

        @Override
        public void onProviderEnabled(String provider) {
            // TODO Auto-generated method stub
        }

        @Override
        public void onStatusChanged(String provider,
                                    int status, Bundle extras) {
            // TODO Auto-generated method stub
        }
    }
}


