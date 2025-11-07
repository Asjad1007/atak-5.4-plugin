
package com.atakmap.android.feature4boundaryalert.plugin;

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

import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.MapEvent;
import com.atakmap.android.maps.MapEventDispatcher;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.Marker;
import com.atakmap.android.maps.PointMapItem;
import com.atakmap.android.maps.Shape;
import com.atakmap.android.maps.Polyline;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.coremap.maps.coords.GeoPointMetaData;
import com.atakmap.android.editableShapes.EditablePolyline;
import com.atakmap.android.maps.MapTouchController;
import android.graphics.Color;
import android.view.MotionEvent;

public class PluginTemplate implements IPlugin {

    private static final String TAG = "BoundaryAlertPlugin";
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
    
    // Boundary UI elements
    private android.widget.TextView txtPolygonStatus;
    private android.widget.TextView txtBoundaryStatus;
    private android.widget.TextView txtLastEvent;
    private android.widget.Button btnStartDrawing;
    private android.widget.Button btnCompletePolygon;
    private android.widget.Button btnClearPolygon;
    
    // BroadcastReceiver for position updates
    private BroadcastReceiver positionReceiver;
    
    // Boundary management
    private BoundaryManager boundaryManager;
    private MapView mapView;
    private boolean isDrawingMode = false;
    private Polyline boundaryPolygon;
    private Marker robotMarker;
    private android.view.View.OnTouchListener mapTouchListener;

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
        
        // Initialize boundary manager
        boundaryManager = new BoundaryManager();
        
        // Get MapView instance
        try {
            mapView = MapView.getMapView();
            Log.d(TAG, "MapView obtained successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error getting MapView: " + e.getMessage(), e);
        }

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
            
            // Boundary UI elements
            btnStartDrawing = view.findViewById(R.id.btnStartDrawing);
            btnCompletePolygon = view.findViewById(R.id.btnCompletePolygon);
            btnClearPolygon = view.findViewById(R.id.btnClearPolygon);
            txtPolygonStatus = view.findViewById(R.id.txtPolygonStatus);
            txtBoundaryStatus = view.findViewById(R.id.txtBoundaryStatus);
            txtLastEvent = view.findViewById(R.id.txtLastEvent);

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
            
            // Boundary control button listeners
            btnStartDrawing.setOnClickListener(v -> startDrawingMode());
            btnCompletePolygon.setOnClickListener(v -> completePolygon());
            btnClearPolygon.setOnClickListener(v -> clearPolygon());
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
                    
                    // Check boundary crossing
                    GeoPoint currentPos = new GeoPoint(lat, lon);
                    checkBoundary(currentPos);
                    updateRobotMarker(currentPos);
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
    
    // ========== Boundary Management Methods ==========
    
    private void startDrawingMode() {
        if (mapView == null) {
            android.widget.Toast.makeText(pluginContext, "MapView not available", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        
        isDrawingMode = true;
        
        // Create touch listener for drawing
        mapTouchListener = new android.view.View.OnTouchListener() {
            @Override
            public boolean onTouch(android.view.View v, MotionEvent event) {
                if (isDrawingMode && event.getAction() == MotionEvent.ACTION_UP) {
                    // Convert screen coordinates to GeoPoint
                    android.graphics.PointF screenPoint = new android.graphics.PointF(event.getX(), event.getY());
                    GeoPoint geoPoint = mapView.inverse(screenPoint.x, screenPoint.y).get();
                    
                    if (geoPoint != null) {
                        addPolygonVertex(geoPoint);
                        return true;
                    }
                }
                return false;
            }
        };
        
        mapView.setOnTouchListener(mapTouchListener);
        
        if (btnStartDrawing != null) btnStartDrawing.setEnabled(false);
        if (btnCompletePolygon != null) btnCompletePolygon.setEnabled(true);
        
        android.widget.Toast.makeText(pluginContext, "Tap on map to add polygon vertices (min 3)", android.widget.Toast.LENGTH_LONG).show();
        Log.d(TAG, "Drawing mode started - tap to add vertices");
    }
    
    private void addPolygonVertex(GeoPoint point) {
        boundaryManager.addVertex(point);
        int count = boundaryManager.getVertexCount();
        
        if (txtPolygonStatus != null) {
            txtPolygonStatus.setText("Polygon: " + count + " vertices added");
        }
        
        // Draw or update the polygon on the map
        updatePolygonOnMap();
        
        android.widget.Toast.makeText(pluginContext, 
            "Vertex " + count + " added", android.widget.Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Vertex added: " + point.getLatitude() + ", " + point.getLongitude());
    }
    
    private void updatePolygonOnMap() {
        if (mapView == null) return;
        
        java.util.List<GeoPoint> vertices = boundaryManager.getVertices();
        if (vertices.size() < 2) return;
        
        // Remove old polygon if exists
        if (boundaryPolygon != null) {
            mapView.getRootGroup().removeItem(boundaryPolygon);
        }
        
        // Create closed polygon by adding first point at the end
        java.util.List<GeoPoint> closedVertices = new java.util.ArrayList<>(vertices);
        if (boundaryManager.isComplete() && vertices.size() >= 3) {
            closedVertices.add(vertices.get(0)); // Close the polygon
        }
        
        // Create new polygon
        GeoPoint[] points = closedVertices.toArray(new GeoPoint[0]);
        boundaryPolygon = new Polyline(java.util.UUID.randomUUID().toString());
        boundaryPolygon.setPoints(points);
        boundaryPolygon.setStrokeColor(Color.BLUE);
        boundaryPolygon.setStrokeWeight(3.0);
        
        // Set fill color if polygon is complete
        if (boundaryManager.isComplete()) {
            boundaryPolygon.setFillColor(Color.argb(50, 0, 0, 255)); // Semi-transparent blue
        }
        
        boundaryPolygon.setTitle("Boundary Polygon");
        boundaryPolygon.setMetaString("callsign", "Boundary");
        
        mapView.getRootGroup().addItem(boundaryPolygon);
        Log.d(TAG, "Polygon updated on map with " + vertices.size() + " vertices");
    }
    
    private void completePolygon() {
        if (boundaryManager.completePolygon()) {
            isDrawingMode = false;
            
            // Remove touch listener
            if (mapView != null) {
                mapView.setOnTouchListener(null);
                mapTouchListener = null;
            }
            
            if (btnStartDrawing != null) btnStartDrawing.setEnabled(false);
            if (btnCompletePolygon != null) btnCompletePolygon.setEnabled(false);
            if (txtPolygonStatus != null) {
                txtPolygonStatus.setText("Polygon: Complete (" + boundaryManager.getVertexCount() + " vertices)");
            }
            
            // Update polygon to be closed
            updatePolygonOnMap();
            
            android.widget.Toast.makeText(pluginContext, "Polygon completed! Boundary detection active.", android.widget.Toast.LENGTH_LONG).show();
            Log.d(TAG, "Polygon completed");
        } else {
            android.widget.Toast.makeText(pluginContext, "Need at least 3 vertices to complete polygon", android.widget.Toast.LENGTH_SHORT).show();
        }
    }
    
    private void clearPolygon() {
        boundaryManager.clearPolygon();
        isDrawingMode = false;
        
        // Remove polygon from map
        if (boundaryPolygon != null && mapView != null) {
            mapView.getRootGroup().removeItem(boundaryPolygon);
            boundaryPolygon = null;
        }
        
        // Remove touch listener
        if (mapView != null) {
            mapView.setOnTouchListener(null);
            mapTouchListener = null;
        }
        
        if (btnStartDrawing != null) btnStartDrawing.setEnabled(true);
        if (btnCompletePolygon != null) btnCompletePolygon.setEnabled(false);
        if (txtPolygonStatus != null) txtPolygonStatus.setText("Polygon: Not drawn");
        if (txtBoundaryStatus != null) txtBoundaryStatus.setText("Position: Outside boundary");
        if (txtLastEvent != null) txtLastEvent.setText("Last Event: None");
        
        android.widget.Toast.makeText(pluginContext, "Polygon cleared", android.widget.Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Polygon cleared");
    }
    
    private void checkBoundary(GeoPoint position) {
        if (!boundaryManager.isComplete()) {
            return;
        }
        
        String event = boundaryManager.checkBoundaryCrossing(position);
        boolean isInside = boundaryManager.isCurrentlyInside();
        
        // Update UI
        if (txtBoundaryStatus != null) {
            String status = isInside ? "Position: INSIDE boundary" : "Position: OUTSIDE boundary";
            txtBoundaryStatus.setText(status);
            txtBoundaryStatus.setTextColor(isInside ? Color.GREEN : Color.rgb(255, 102, 0));
        }
        
        // Handle boundary crossing events
        if (event != null) {
            if (txtLastEvent != null) {
                String timestamp = new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(new java.util.Date());
                txtLastEvent.setText("Last Event: " + event + " at " + timestamp);
            }
            
            // Show alert
            String message = event.equals("ENTERED") ? 
                "⚠️ ENTERED boundary zone!" : "⚠️ EXITED boundary zone!";
            android.widget.Toast.makeText(pluginContext, message, android.widget.Toast.LENGTH_LONG).show();
            
            // Change marker color based on event
            updateRobotMarkerColor(isInside);
            
            Log.i(TAG, "Boundary event: " + event);
        }
    }
    
    private void updateRobotMarker(GeoPoint position) {
        if (mapView == null) return;
        
        if (robotMarker == null) {
            // Create robot marker
            robotMarker = new Marker(position, java.util.UUID.randomUUID().toString());
            robotMarker.setTitle("Simulated Robot");
            robotMarker.setType("b-m-p-s-p-loc");
            robotMarker.setMetaString("callsign", "ROBOT-SIM");
            
            // Set initial color
            boolean isInside = boundaryManager.isCurrentlyInside();
            robotMarker.setColor(isInside ? Color.GREEN : Color.RED);
            
            mapView.getRootGroup().addItem(robotMarker);
            Log.d(TAG, "Robot marker created");
        } else {
            // Update position
            robotMarker.setPoint(position);
        }
    }
    
    private void updateRobotMarkerColor(boolean isInside) {
        if (robotMarker != null) {
            robotMarker.setColor(isInside ? Color.GREEN : Color.RED);
        }
    }

}

