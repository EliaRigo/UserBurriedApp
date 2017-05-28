package com.findrone.userburiedapp;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "Debug";
    private final int PERMISSIONS_LOCATION = 0;
    private final float meters = 0.01f;
    //Milliseconds Constant
    private final int MIN_IN_MILLISECONDS = 60000;
    private final int SEC_IN_MILLISECONDS = 1000;
    //Socket
    Socket socket = null;
    PrintWriter out = null;
    //Timer
    Timer timer1 = null;
    Timer timer2 = null;
    Timer timer3 = null;
    private TextView txtGpsLastUpdate = null;
    private TextView txtLatitude = null;
    private TextView txtLongitude = null;
    private TextView txtRawLocation = null;
    private TextView txtAdvise = null;
    private TextView txtWiFiLastUpdate = null;
    private ToggleButton tbService = null;
    private Switch swWiFiConn = null;
    //WiFi
    private String networkSSID = "-";
    private String networkPass = "-";
    private WifiManager wifiManager = null;
    private WifiConfiguration conf = new WifiConfiguration();
    //Location
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
        txtWiFiLastUpdate = (TextView) findViewById(R.id.txtWiFiLastUpdate);
        tbService = (ToggleButton) findViewById(R.id.tbService);
        swWiFiConn = (Switch) findViewById(R.id.swWiFiConn);
        if (swWiFiConn != null) {
            swWiFiConn.setClickable(false);
        }
        locationListener = new MyLocationListener();
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        tbService.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    doThings();
                    doThings2();
                    doThings3();
                } else {
                    locationManager.removeUpdates(locationListener);
                    if (timer1 != null) {
                        timer1.cancel();
                    }
                    if (timer2 != null) {
                        timer2.cancel();
                    }
                    if (timer3 != null) {
                        timer3.cancel();
                    }
                }
            }
        });
    }

    public void doThings() {
        timer1 = new Timer();
        timer1.schedule(new TimerTask() {
            @Override
            public void run() { //Timer to start GPS every 120 seconds
                final int stopAfter = 30 * SEC_IN_MILLISECONDS;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() { //LocationUpdates con be perform only on UiThread
                        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                                ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSIONS_LOCATION);
                        } else {
                            tbService.setChecked(true);
                            //locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, meters, locationListener);
                            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, meters, locationListener);
                        }
                    }
                });
                Timer timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() { //Timer to stop GPS after 15 seconds (if acc <= 10m)
                        cntCheck++;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() { //You can upload Ui only in UiThread
                                txtAdvise.setText("Accuracy: " + accuracy + "\nChecks : " +
                                        cntCheck + "\nStopAfter: " + stopAfter);
                            }
                        });

                        if (accuracy <= 10.0) {
                            locationManager.removeUpdates(locationListener);
                            accuracy = Float.MAX_VALUE;
                            cntCheck = 0;
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() { //You can upload Ui only in UiThread
                                    tbService.setChecked(false);
                                }
                            });
                            this.cancel();
                        }
                    }
                }, 0, stopAfter);
            }
        }, 0, 2 * MIN_IN_MILLISECONDS);
    }

    public void doThings2() {
        timer2 = new Timer();
        timer2.schedule(new TimerTask() {
            @Override
            public void run() { //Timer to start and connect via WiFi every 60 seconds
                // Please note the quotes. String should contain ssid in quotes
                conf.SSID = "\"" + networkSSID + "\"";
                conf.preSharedKey = "\"" + networkPass + "\"";
                //conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE); //For Open Network
                wifiManager.setWifiEnabled(true); //With peace and love we drain your battery life <3

                List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
                if (list != null) {
                    for (WifiConfiguration i : list) {
                        if (i.SSID != null && i.SSID.equals("\"" + networkSSID + "\"")) {
                            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                            if (wifiInfo.getSupplicantState() != SupplicantState.COMPLETED) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        swWiFiConn.setChecked(false);
                                    }
                                });
                                wifiManager.disconnect();
                                wifiManager.enableNetwork(i.networkId, true);
                                wifiManager.reconnect();
                            }
                        }
                    }
                    //Network not present Add
                    wifiManager.addNetwork(conf);
                } else {
                    //Toast.makeText(getApplicationContext(), "Please enable WiFi", Toast.LENGTH_LONG);
                }
            }
        }, 0, 1 * MIN_IN_MILLISECONDS);
    }

    public void doThings3() {
        timer3 = new Timer();
        timer3.schedule(new TimerTask() {
            @Override
            public void run() { //Timer to send data about WiFi power every 100 ms
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                if (wifiInfo.getSupplicantState() == SupplicantState.COMPLETED) {
                    String ssid = wifiInfo.getSSID();
                    if (Objects.equals(ssid, "\"" + networkSSID + "\"")) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                swWiFiConn.setChecked(true);
                                txtWiFiLastUpdate.setText(getDateTime());
                            }
                        });
                        //Send data
                        try {
                            if (socket == null || out == null) {
                                //Initialize socket
                                socket = new Socket("192.168.1.200", 9119);
                                out = new PrintWriter(socket.getOutputStream(), true);
                            }
                            sendToPI("WiFi Power: " + wifiInfo.getRssi() + " dB");
                        } catch (IOException e) {
                            e.printStackTrace();
                            //Force reconnect
                            try {
                                socket = new Socket("192.168.1.200", 9119);
                                out = new PrintWriter(socket.getOutputStream(), true);
                            } catch (IOException e2) {
                                e2.printStackTrace();
                            }
                            //Toast.makeText(getApplicationContext(), "Too fast ?", Toast.LENGTH_SHORT);
                        }
                    }
                }
            }
        }, 0, 100);
    }

    private String getDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss:SSSS dd-MM-yyyy");
        return sdf.format(new Date());
    }

    private void sendToPI(String message) {
        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        if (mWifi.isConnected() && socket != null && out != null) {
            out.println(message);
        }
    }

    /*----------Listener class to get coordinates ------------- */
    private class MyLocationListener implements LocationListener {
        @Override
        public void onLocationChanged(Location loc) {
            accuracy = loc.getAccuracy();

            String latitude = Double.toString(loc.getLatitude());
            String longitude = Double.toString(loc.getLongitude());
            String timestamp = getDateTime();

            String sSend = String.format("Timestamp: %1$s, Latitude: %2$s, Longitude: %3$s, Accuracy: %4$s",
                    timestamp, latitude, longitude, accuracy);
            Log.v(TAG, sSend);

            txtLatitude.setText("Latitude: " + loc.getLatitude());
            txtLongitude.setText("Longitude: " + loc.getLongitude());
            txtGpsLastUpdate.setText("GPS Last Update: " + timestamp);
            txtRawLocation.setText(loc.toString());

            sendToPI(sSend);
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


