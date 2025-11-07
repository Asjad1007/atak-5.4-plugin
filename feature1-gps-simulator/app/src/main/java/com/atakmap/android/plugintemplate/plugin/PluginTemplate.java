
package com.atakmap.android.feature1gpssimulator.plugin;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.atak.plugins.impl.PluginContextProvider;
import com.atak.plugins.impl.PluginLayoutInflater;

import gov.tak.api.plugin.IPlugin;
import gov.tak.api.plugin.IServiceController;
import gov.tak.api.ui.IHostUIService;
import gov.tak.api.ui.Pane;
import gov.tak.api.ui.PaneBuilder;
import gov.tak.api.ui.ToolbarItem;
import gov.tak.api.ui.ToolbarItemAdapter;
import gov.tak.platform.marshal.MarshalManager;

public class PluginTemplate implements IPlugin {

    private static final String TAG = "PluginTemplate";
    IServiceController serviceController;
    Context pluginContext;
    IHostUIService uiService;
    ToolbarItem toolbarItem;
    Pane templatePane;
    
    // UI elements for live data display
    private android.widget.TextView txtLatitude;
    private android.widget.TextView txtLongitude;
    private android.widget.TextView txtSpeed;
    private android.widget.TextView txtHeading;
    private android.widget.TextView txtStatus;
    
    // BroadcastReceiver for position updates
    private BroadcastReceiver positionReceiver;

    public PluginTemplate(IServiceController serviceController) {
        this.serviceController = serviceController;
        final PluginContextProvider ctxProvider = serviceController
                .getService(PluginContextProvider.class);
        if (ctxProvider != null) {
            pluginContext = ctxProvider.getPluginContext();
            pluginContext.setTheme(R.style.ATAKPluginTheme);
        }

        // obtain the UI service
        uiService = serviceController.getService(IHostUIService.class);

        // initialize the toolbar button for the plugin

        // create the button
        toolbarItem = new ToolbarItem.Builder(
                pluginContext.getString(R.string.app_name),
                MarshalManager.marshal(
                        pluginContext.getResources().getDrawable(R.drawable.ic_launcher),
                        android.graphics.drawable.Drawable.class,
                        gov.tak.api.commons.graphics.Bitmap.class))
                .setListener(new ToolbarItemAdapter() {
                    @Override
                    public void onClick(ToolbarItem item) {
                        showPane();
                    }
                })
                .build();
    }

    @Override
    public void onStart() {
        // the plugin is starting, add the button to the toolbar
        if (uiService == null)
            return;

        uiService.addToolbarItem(toolbarItem);
    }

    @Override
    public void onStop() {
        // the plugin is stopping, remove the button from the toolbar
        if (uiService == null)
            return;

        uiService.removeToolbarItem(toolbarItem);
        unregisterPositionReceiver();
    }

    private void showPane() {
        Log.d(TAG, "showPane() called");
        if (templatePane == null) {
            Log.d(TAG, "Creating new pane");

            // Inflate your pane layout
            // We'll add Start/Stop buttons directly inside the pane
            android.view.View view = PluginLayoutInflater.inflate(pluginContext, R.layout.main_layout, null);
            Log.d(TAG, "Layout inflated");

            // --- Get references to UI elements ---
            android.widget.Button start = view.findViewById(R.id.btnStart);
            android.widget.Button stop  = view.findViewById(R.id.btnStop);
            txtLatitude = view.findViewById(R.id.txtLatitude);
            txtLongitude = view.findViewById(R.id.txtLongitude);
            txtSpeed = view.findViewById(R.id.txtSpeed);
            txtHeading = view.findViewById(R.id.txtHeading);
            txtStatus = view.findViewById(R.id.txtStatus);

            if (start == null) {
                Log.e(TAG, "Start button is NULL!");
            }
            if (stop == null) {
                Log.e(TAG, "Stop button is NULL!");
            }

            start.setOnClickListener(v -> {
                try {
                    Log.d(TAG, "Start button clicked");
                    android.widget.Toast.makeText(pluginContext, "Starting GPS Simulator...", android.widget.Toast.LENGTH_SHORT).show();
                    
                    android.content.Intent i = new android.content.Intent(pluginContext, GpsSimService.class);
                    if (android.os.Build.VERSION.SDK_INT >= 26) {
                        Log.d(TAG, "Starting foreground service");
                        pluginContext.startForegroundService(i);
                    } else {
                        Log.d(TAG, "Starting service");
                        pluginContext.startService(i);
                    }
                    Log.d(TAG, "Service start command sent");
                    android.widget.Toast.makeText(pluginContext, "GPS Simulator started", android.widget.Toast.LENGTH_SHORT).show();
                    if (txtStatus != null) txtStatus.setText("Status: Running");
                    registerPositionReceiver();
                } catch (Exception e) {
                    Log.e(TAG, "Error starting service: " + e.getMessage(), e);
                    android.widget.Toast.makeText(pluginContext, "Error: " + e.getMessage(), android.widget.Toast.LENGTH_LONG).show();
                }
            });

            stop.setOnClickListener(v -> {
                try {
                    Log.d(TAG, "Stop button clicked");
                    android.widget.Toast.makeText(pluginContext, "Stopping GPS Simulator...", android.widget.Toast.LENGTH_SHORT).show();
                    android.content.Intent i = new android.content.Intent(pluginContext, GpsSimService.class);
                    pluginContext.stopService(i);
                    Log.d(TAG, "Service stop command sent");
                    android.widget.Toast.makeText(pluginContext, "GPS Simulator stopped", android.widget.Toast.LENGTH_SHORT).show();
                    if (txtStatus != null) txtStatus.setText("Status: Stopped");
                    unregisterPositionReceiver();
                    resetDisplayValues();
                } catch (Exception e) {
                    Log.e(TAG, "Error stopping service: " + e.getMessage(), e);
                    android.widget.Toast.makeText(pluginContext, "Error: " + e.getMessage(), android.widget.Toast.LENGTH_LONG).show();
                }
            });
            // --- End button logic ---

            // Build the plugin pane
            templatePane = new PaneBuilder(view)
                    .setMetaValue(Pane.RELATIVE_LOCATION, Pane.Location.Default)
                    .setMetaValue(Pane.PREFERRED_WIDTH_RATIO, 0.5D)
                    .setMetaValue(Pane.PREFERRED_HEIGHT_RATIO, 0.5D)
                    .build();
            Log.d(TAG, "Pane built successfully");
        }

        // If the pane isn’t visible, show it
        if (!uiService.isPaneVisible(templatePane)) {
            Log.d(TAG, "Showing pane");
            uiService.showPane(templatePane, null);
        } else {
            Log.d(TAG, "Pane already visible");
        }
    }
    
    private void registerPositionReceiver() {
        if (positionReceiver != null) {
            return; // Already registered
        }
        
        positionReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "Broadcast received: " + intent.getAction());
                if (GpsSimService.ACTION_POSITION_UPDATE.equals(intent.getAction())) {
                    double lat = intent.getDoubleExtra(GpsSimService.EXTRA_LATITUDE, 0.0);
                    double lon = intent.getDoubleExtra(GpsSimService.EXTRA_LONGITUDE, 0.0);
                    double speed = intent.getDoubleExtra(GpsSimService.EXTRA_SPEED, 0.0);
                    double heading = intent.getDoubleExtra(GpsSimService.EXTRA_HEADING, 0.0);
                    
                    Log.d(TAG, String.format(java.util.Locale.US, "Position update: lat=%.6f, lon=%.6f, speed=%.2f, heading=%.1f", 
                            lat, lon, speed, heading));
                    updatePositionDisplay(lat, lon, speed, heading);
                }
            }
        };
        
        IntentFilter filter = new IntentFilter(GpsSimService.ACTION_POSITION_UPDATE);
        
        // Register receiver with RECEIVER_EXPORTED flag for Android 13+ to receive from service in different process
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            pluginContext.registerReceiver(positionReceiver, filter, Context.RECEIVER_EXPORTED);
            Log.d(TAG, "Position receiver registered with RECEIVER_EXPORTED");
        } else {
            pluginContext.registerReceiver(positionReceiver, filter);
            Log.d(TAG, "Position receiver registered (legacy)");
        }
        Log.d(TAG, "Listening for action: " + GpsSimService.ACTION_POSITION_UPDATE);
    }
    
    private void unregisterPositionReceiver() {
        if (positionReceiver != null) {
            try {
                pluginContext.unregisterReceiver(positionReceiver);
                positionReceiver = null;
                Log.d(TAG, "Position receiver unregistered");
            } catch (Exception e) {
                Log.e(TAG, "Error unregistering receiver: " + e.getMessage());
            }
        }
    }
    
    private void updatePositionDisplay(double lat, double lon, double speed, double heading) {
        Log.d(TAG, "updatePositionDisplay called");
        if (txtLatitude != null) {
            txtLatitude.setText(String.format(java.util.Locale.US, "Latitude: %.6f°", lat));
            Log.d(TAG, "Updated latitude TextView");
        } else {
            Log.e(TAG, "txtLatitude is NULL!");
        }
        if (txtLongitude != null) {
            txtLongitude.setText(String.format(java.util.Locale.US, "Longitude: %.6f°", lon));
            Log.d(TAG, "Updated longitude TextView");
        } else {
            Log.e(TAG, "txtLongitude is NULL!");
        }
        if (txtSpeed != null) {
            txtSpeed.setText(String.format(java.util.Locale.US, "Speed: %.2f m/s", speed));
            Log.d(TAG, "Updated speed TextView");
        } else {
            Log.e(TAG, "txtSpeed is NULL!");
        }
        if (txtHeading != null) {
            txtHeading.setText(String.format(java.util.Locale.US, "Heading: %.1f°", heading));
            Log.d(TAG, "Updated heading TextView");
        } else {
            Log.e(TAG, "txtHeading is NULL!");
        }
    }
    
    private void resetDisplayValues() {
        if (txtLatitude != null) txtLatitude.setText("Latitude: --");
        if (txtLongitude != null) txtLongitude.setText("Longitude: --");
        if (txtSpeed != null) txtSpeed.setText("Speed: -- m/s");
        if (txtHeading != null) txtHeading.setText("Heading: --°");
    }

}
