package com.plesba.ioio_logger;

import ioio.lib.api.DigitalInput;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.plesba.ioio_logger.GPS_ListenerService.GPSBinder;

public class MainActivity extends IOIOActivity {
    private SharedPreferences settings;
    private SharedPreferences.Editor editor;
    private TextView clockView;
    private TextView leftHeightView;
    private TextView rightHeightView;
    private TextView speedView;
    private TextView maxSpeedView;
    private TextView frontRPMview;
    private TextView rearRPMview;
    private TextView deltaRPMview;
    private TextView latitudeView;
    private TextView longitudeView;
    private FileWriter write;
    private PowerManager.WakeLock wakeLock;
    private ServiceConnection gpsSvcConn;
    private GPS_ListenerService gpsService;
    private boolean isGPSserviceBound;
    private RideHeightReader height;
    private String normalHeightLeft = "";
    private String normalHeightRight = "";
    private String maxHeightLeft = "";
    private String maxHeightRight = "";
    private String csvHeader = "SYSTIME,LH,RH,GPSTIME,LAT,LONG,DIST,SPEED,F.RPM,D.RPM,R.RPM";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        write = FileWriter.getInstance();
        initializeSettings();
        startGPSService();
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        // what burns more power, the GPS or a dimmed screen? If the screen,
        // perhaps that
        // should be PARTIAL_WAKE_LOCK... but be aware that PARTIAL ignores
        // everything
        // including the power button. :)
        wakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK,
                "android-ioio");
        initializeGui(); // this method actually acquires the wakelock
        // start out the Data file
        write.data(csvHeader);
        write.syslog("onCreate complete");
    }

    // start of stuff to bind to GPS service so we can get values
    private void startGPSService() {
        startService(new Intent(this, GPS_ListenerService.class));
        gpsSvcConn = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder binder) {
                GPSBinder gpsBinder = (GPSBinder) binder;
                gpsService = gpsBinder.getService();
                isGPSserviceBound = true;
                write.syslog("GPS service bound");
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                isGPSserviceBound = false;
                write.syslog("GPS service came unbound?");
            }
        };
        Intent intent = new Intent(this, GPS_ListenerService.class);
        bindService(intent, gpsSvcConn, Context.BIND_AUTO_CREATE);
        write.syslog("Started to bind to GPS service");
    }
    // end of stuff to bind to GPS service

    private class Looper extends BaseIOIOLooper {
        private int zeroButtonPin = 34;
        private DigitalInput zeroButton;
        private WheelSensorReader frontReader;
        private WheelSensorReader rearReader;
        private double frontRPMs, rearRPMs, deltaRPMs;
        private String frontRevs = "fR";
        private String rearRevs = "rR";
        private String deltaRevs = "dR";
        private String gpsTime = "";
        private String lastGPStime = "";
        private String latitude = "lat";
        private String lastLat = "";
        private String longitude = "long";
        private String lastLong = "";
        private String speed = "spd";
        private String lastSpeed = "";
        private String maxSpeed = "mx";
        private String distFromStart = "";
        private String leftReading = "lH";
        private String rightReading = "rH";
        private String updateTime = "12:00:00";
        @SuppressLint("SimpleDateFormat")
        private SimpleDateFormat clockFormat = new SimpleDateFormat("HH:mm:ss");

        private String getRevs(double rpms) {
            String rpm = Double.toString(rpms);
            int point = rpm.indexOf('.');
            if (point > 0) {
                return rpm.substring(0, point);
            }
            return rpm;
        }

        @Override
        public void setup() throws ConnectionLostException {
            zeroButton = ioio_.openDigitalInput(zeroButtonPin, DigitalInput.Spec.Mode.PULL_UP);
            height = new RideHeightReader(ioio_);
            height.start();
            frontReader = new WheelSensorReader(ioio_, WheelSensorReader.frontInput);
            //frontReader.start();
            rearReader = new WheelSensorReader(ioio_, WheelSensorReader.rearInput);
            //rearReader.start();
            write.syslog("Looper setup complete");
        }

        @Override
        public void loop() throws ConnectionLostException, InterruptedException {
            updateTime = clockFormat.format(new Date());
            setDisplayText(clockView, updateTime);
            // imagine a pushbutton switching pin 34 to GRND.
            if (zeroButton.read()) {
                gpsService.setStartingPosition();
                FileWriter.getInstance().rollFiles();
            }
            // the GPS service needs to be bound before these will work...
            if (isGPSserviceBound) {
                gpsTime = gpsService.getTime();
                if (!gpsTime.equals(lastGPStime)) {
                    latitude = gpsService.getLat();
                    longitude = gpsService.getLong();
                    speed = gpsService.getSpeed();
                    maxSpeed = gpsService.getMaxSpeed();
                    distFromStart = gpsService.getMilesFromStart();
                }
            }
            setDisplayText(speedView, speed);
            setDisplayText(maxSpeedView, maxSpeed);
            setDisplayText(latitudeView, latitude);
            setDisplayText(longitudeView, longitude);

            // pull current height strings
            leftReading = height.getLeftReading();
            setDisplayText(leftHeightView, leftReading);
            rightReading = height.getRightReading();
            setDisplayText(rightHeightView, rightReading);

            // get rpms as doubles, then turn 'em into String form
            frontRPMs = frontReader.getRPM();
            frontRevs = getRevs(frontRPMs);
            setDisplayText(frontRPMview, frontRevs);

            rearRPMs = rearReader.getRPM();
            rearRevs = getRevs(rearRPMs);
            setDisplayText(rearRPMview, rearRevs);

            deltaRPMs = frontRPMs - rearRPMs;
            deltaRevs = getRevs(deltaRPMs);
            setDisplayText(deltaRPMview, deltaRevs);

            // only log if we're moving
            if (!lastLat.equals(latitude) ||
                    !lastLong.equals(longitude) ||
                    !lastSpeed.equals(speed)) {
                // log the data
                // "SYSTIME,LH,RH,GPSTIME,LAT,LONG,DIST,SPEED,F.RPM,D.RPM,R.RPM";
                write.data(updateTime + "," + leftReading + "," + rightReading
                        + "," + gpsTime + "," + latitude + "," + longitude
                        + "," + distFromStart + "," + speed + "," + frontRevs
                        + "," + deltaRevs + "," + rearRevs);

            }

            // and set the values for next time
            lastLat = latitude;
            lastLong = longitude;
            lastSpeed = speed;
            lastGPStime = gpsTime;

            Thread.sleep(498);
        }

    }

    @Override
    protected IOIOLooper createIOIOLooper() {
        return new Looper();
    }

    private void initializeSettings() {
        settings = PreferenceManager.getDefaultSharedPreferences(this);
        normalHeightLeft = settings.getString("LH_NORMAL", "0");
        maxHeightLeft = settings.getString("LH_MAX", "99");
        normalHeightRight = settings.getString("RH_NORMAL", "0");
        maxHeightRight = settings.getString("RH_MAX", "99");
        write.syslog("read settings from preferences");
        write.syslog("LH NORM: " + normalHeightLeft + " LH MAX: "
                + maxHeightLeft + " RH NORM: " + normalHeightRight
                + " RH MAX: " + maxHeightRight);
    }

    private void initializeGui() {
        setContentView(R.layout.activity_main);
        clockView = (TextView) findViewById(R.id.clockView);
        leftHeightView = (TextView) findViewById(R.id.leftHeighDisplay);
        rightHeightView = (TextView) findViewById(R.id.rightHeightDisplay);
        speedView = (TextView) findViewById(R.id.SpeedDisplay);
        maxSpeedView = (TextView) findViewById(R.id.maxSpeedDisplay);
        frontRPMview = (TextView) findViewById(R.id.frontRpm);
        rearRPMview = (TextView) findViewById(R.id.rearRpm);
        deltaRPMview = (TextView) findViewById(R.id.deltaRpm);
        latitudeView = (TextView) findViewById(R.id.lat_value);
        longitudeView = (TextView) findViewById(R.id.long_value);
        wakeLock.acquire();
        write.syslog("gui initialized");
    }

    private void setDisplayText(final TextView view, final String str) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                view.setText(str);
            }
        });
    }

    public void calibrateNormal(View v) {
        // get the latest service output
        normalHeightLeft = height.getLeftReading();
        normalHeightRight = height.getRightReading();
        editor = settings.edit();
        editor.putString("LH_NORMAL", normalHeightLeft);
        editor.putString("RH_NORMAL", normalHeightRight);
        editor.apply();
        write.syslog("calibrated normal: LH_NORM " + normalHeightLeft + " RH_NORM "
                + normalHeightRight);
    }

    public void calibrateMax(View v) {
        // get the latest service output
        maxHeightLeft = height.getLeftReading();
        maxHeightRight = height.getRightReading();
        editor = settings.edit();
        editor.putString("LH_MAX", maxHeightLeft);
        editor.putString("RH_MAX", maxHeightRight);
        editor.apply();
        write.syslog("calibrated max: LH_MAX " + maxHeightLeft + " RH_MAX "
                + maxHeightRight);
    }

    @Override
    protected void onPause() {
        super.onPause();
        write.syslog("MainActivity paused");
    }

    @Override
    protected void onResume() {
        super.onResume();
        write.syslog("MainActivity resumed");
    }

    @Override
    protected void onStop() {
        // The activity is no longer visible (it is now "stopped")
        super.onStop();
        // stop GPS service
        unbindService(gpsSvcConn);
        stopService(new Intent(this, GPS_ListenerService.class));
        // release wake lock
        wakeLock.release();
        // close log files... but write this first.
        write.syslog("MainActivity stopped");
        write.finish();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.CalNormItem:
                calibrateNormal(null);
                return true;
            case R.id.CalMaxItem:
                calibrateMax(null);
                return true;
            case R.id.rollFilesItem:
                // start new files somehow
                write.rollFiles();
                gpsService.zeroMaxSpeed();
                write.syslog("LH NORM: " + normalHeightLeft + " LH MAX: "
                        + maxHeightLeft + " RH NORM: " + normalHeightRight
                        + " RH MAX: " + maxHeightRight);
                write.data("SYSTIME,LH,RH,GPSTIME,LAT,LONG,SPEED,F.RPM,R.RPM");
                return true;
            case R.id.setStartPosItem:
                gpsService.setStartingPosition();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}
