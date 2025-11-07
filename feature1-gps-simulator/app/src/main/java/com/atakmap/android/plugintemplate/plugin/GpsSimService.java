package com.atakmap.android.feature1gpssimulator.plugin;
import android.app.*;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.*;
public class GpsSimService extends Service {
    private static final String TAG = "GpsSimService";
    private ScheduledExecutorService exec;
    // Center near GooglePlex (change to anything you like)
    private double lat0 = 37.4219999, lon0 = -122.0840575;
    private double radiusMeters = 120; // circle radius
    private int t = 0; // seconds

    private static final String CH_ID = "gps_sim_ch";
    private static final String UID = "SIM-ROBOT-1";
    private static final String CALLSIGN = "SIMBOT";
    private static final String MULTI_ADDR = "239.2.3.1";
    private static final int MULTI_PORT = 6969;
    
    // Broadcast action for position updates
    public static final String ACTION_POSITION_UPDATE = "com.atakmap.android.feature1gpssimulator.POSITION_UPDATE";
    public static final String EXTRA_LATITUDE = "latitude";
    public static final String EXTRA_LONGITUDE = "longitude";
    public static final String EXTRA_SPEED = "speed";
    public static final String EXTRA_HEADING = "heading";

    @Override public void onCreate() {
        super.onCreate();
        Log.d(TAG, "GpsSimService onCreate()");
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                NotificationChannel ch = new NotificationChannel(CH_ID, "GPS Sim", NotificationManager.IMPORTANCE_LOW);
                NotificationManager nm = getSystemService(NotificationManager.class);
                if (nm != null) {
                    nm.createNotificationChannel(ch);
                }
                Notification n = new Notification.Builder(this, CH_ID)
                        .setContentTitle("GPS Simulator running")
                        .setContentText("Simulating GPS movement")
                        .setSmallIcon(android.R.drawable.ic_media_play)
                        .build();
                startForeground(1, n);
                Log.d(TAG, "Foreground service started successfully");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage(), e);
        }
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "GpsSimService onStartCommand()");
        try {
            if (exec == null || exec.isShutdown()) {
                exec = Executors.newSingleThreadScheduledExecutor();
                exec.scheduleAtFixedRate(this::tick, 0, 1, TimeUnit.SECONDS);
                Log.d(TAG, "GPS simulation task scheduled");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onStartCommand: " + e.getMessage(), e);
        }
        return START_STICKY;
    }

    private void tick() {
        try {
            Log.d(TAG, "Tick " + t);
            t++;
            double omega = 2 * Math.PI / 60.0; // 1 lap per 60s
            double bearing = t * omega;        // radians

            // point on a circle around (lat0, lon0)
            double dRad = radiusMeters / 6378137.0;
            double latR = Math.toRadians(lat0);
            double lonR = Math.toRadians(lon0);
            double lat = Math.asin(Math.sin(latR) * Math.cos(dRad) +
                    Math.cos(latR) * Math.sin(dRad) * Math.cos(bearing));
            double lon = lonR + Math.atan2(Math.sin(bearing) * Math.sin(dRad) * Math.cos(latR),
                    Math.cos(dRad) - Math.sin(latR) * Math.sin(lat));
            double latDeg = Math.toDegrees(lat);
            double lonDeg = Math.toDegrees(lon);

            // constant speed (m/s)
            double speed = (2 * Math.PI * radiusMeters) / 60.0;
            double course = (bearing * 180.0 / Math.PI + 90) % 360; // approximate course

            String cot = buildCot(latDeg, lonDeg, speed, course);
            sendUdp(cot);
            
            // Broadcast position update to UI
            broadcastPositionUpdate(latDeg, lonDeg, speed, course);
        } catch (Throwable e) {
            Log.e(TAG, "Error in tick: " + e.getMessage(), e);
        }
    }
    
    private void broadcastPositionUpdate(double lat, double lon, double speed, double heading) {
        Intent intent = new Intent(ACTION_POSITION_UPDATE);
        // Set package to ATAK's package since plugin runs in ATAK's process
        intent.setPackage("com.atakmap.app.civ");
        intent.putExtra(EXTRA_LATITUDE, lat);
        intent.putExtra(EXTRA_LONGITUDE, lon);
        intent.putExtra(EXTRA_SPEED, speed);
        intent.putExtra(EXTRA_HEADING, heading);
        
        sendBroadcast(intent);
        Log.d(TAG, "Broadcast sent to: com.atakmap.app.civ with action: " + ACTION_POSITION_UPDATE);
    }

    private static String isoNowPlus(int seconds) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(new Date(System.currentTimeMillis() + seconds * 1000L));
    }

    private String buildCot(double lat, double lon, double speed, double course) {
        // Minimal CoT “track” event; ATAK will render it as a moving icon
        String time  = isoNowPlus(0);
        String start = time;
        String stale = isoNowPlus(20);
        return ""
                + "<event version=\"2.0\" uid=\"" + UID + "\" type=\"a-f-A-M-F\" how=\"m-g\" "
                + "time=\"" + time + "\" start=\"" + start + "\" stale=\"" + stale + "\">"
                + "<point lat=\"" + lat + "\" lon=\"" + lon + "\" hae=\"0\" ce=\"5\" le=\"5\"/>"
                + "<detail>"
                + "<contact callsign=\"" + CALLSIGN + "\"/>"
                + "<track speed=\"" + String.format(Locale.US, "%.2f", speed) + "\" "
                + "course=\"" + String.format(Locale.US, "%.1f", course) + "\"/>"
                + "</detail>"
                + "</event>";
    }

    private void sendUdp(String payload) {
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket();
            byte[] data = payload.getBytes();
            InetAddress addr = InetAddress.getByName(MULTI_ADDR);
            DatagramPacket pkt = new DatagramPacket(data, data.length, addr, MULTI_PORT);
            socket.send(pkt);
            Log.d(TAG, "UDP packet sent successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error sending UDP: " + e.getMessage(), e);
        } finally {
            if (socket != null) socket.close();
        }
    }

    @Override public void onDestroy() {
        Log.d(TAG, "GpsSimService onDestroy()");
        if (exec != null) exec.shutdownNow();
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
