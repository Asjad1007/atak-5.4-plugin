# Feature 1: GPS Simulator Plugin

## Overview

The GPS Simulator Plugin is an ATAK (Android Team Awareness Kit) plugin that simulates GPS movement in a circular pattern. This plugin demonstrates background service implementation, real-time position broadcasting, and live UI updates within the ATAK environment.

**Plugin Name**: Feature 1 Gps Simulator  
**Package**: `com.atakmap.android.feature1gpssimulator.plugin`  
**Status**: ✅ Complete and Functional  
**ATAK SDK Version**: 5.4.0.27  
**Minimum Android API**: 21 (Android 5.0)

---

## Purpose and Capabilities

### What It Does

This plugin simulates a GPS-enabled robot or vehicle moving in a circular pattern on the ATAK map. It provides:

1. **Simulated GPS Movement**: Generates realistic GPS coordinates in a circular path
2. **Real-time Position Updates**: Broadcasts position data every second
3. **Live Data Display**: Shows current latitude, longitude, speed, and heading
4. **Map Visualization**: Displays a moving marker on the ATAK map
5. **Background Service**: Runs independently as a foreground service

### Use Cases

- **Testing**: Test ATAK plugins without physical GPS hardware
- **Demonstrations**: Show GPS tracking capabilities in controlled environment
- **Development**: Develop and debug location-based features
- **Training**: Train users on ATAK interface with predictable movement
- **Integration**: Serve as foundation for other GPS-dependent features

---

## Implementation Details

### Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    PluginTemplate                       │
│  - Main plugin entry point                             │
│  - UI management                                        │
│  - BroadcastReceiver for position updates              │
└────────────────┬────────────────────────────────────────┘
                 │
                 │ Controls
                 ▼
┌─────────────────────────────────────────────────────────┐
│                   GpsSimService                         │
│  - Background foreground service                        │
│  - Circular path calculation                            │
│  - Position broadcasting (1 Hz)                         │
│  - Map marker management                                │
└─────────────────────────────────────────────────────────┘
```

### Key Components

#### 1. **PluginTemplate.java**
- Main plugin class implementing `IPlugin` interface
- Creates toolbar button for plugin access
- Manages UI pane with Start/Stop controls
- Displays live GPS data (lat, lon, speed, heading)
- Registers `BroadcastReceiver` to receive position updates
- Updates UI in real-time as position changes

**Key Methods**:
- `onStart()`: Adds plugin to ATAK toolbar
- `showPane()`: Displays control panel with live data
- `registerPositionReceiver()`: Listens for GPS updates
- `updatePositionDisplay()`: Updates UI with new coordinates

#### 2. **GpsSimService.java**
- Extends `Service` to run in background
- Implements foreground service with notification
- Calculates circular path using trigonometry
- Broadcasts position updates via Intent
- Creates and updates map marker

**Key Features**:
- **Circular Path**: 100-meter radius, 10 m/s speed
- **Update Rate**: 1 Hz (every 1000ms)
- **Broadcast Action**: `com.atakmap.android.feature1gpssimulator.POSITION_UPDATE`
- **Map Marker**: Blue robot icon with callsign "SimRobot"

**Position Calculation**:
```java
angle += (speed / radius) * (updateInterval / 1000.0);
lat = centerLat + (radius / 111320.0) * Math.cos(angle);
lon = centerLon + (radius / (111320.0 * Math.cos(Math.toRadians(centerLat)))) * Math.sin(angle);
heading = Math.toDegrees(angle) + 90;
```

#### 3. **UI Layout (main_layout.xml)**
- Start/Stop buttons for service control
- Live data display fields:
  - Latitude (6 decimal places)
  - Longitude (6 decimal places)
  - Speed (m/s)
  - Heading (degrees)
  - Status indicator

---

## How to Use

### Building the Plugin

1. **Open in Android Studio**:
   ```bash
   cd d:\ATAK-CIV-5.4.0.27-SDK\samples\feature1-gps-simulator
   ```

2. **Build the APK**:
   ```bash
   gradlew assembleCivDebug
   ```

3. **Install on Device**:
   ```bash
   gradlew installCivDebug
   ```
   Or manually install:
   ```bash
   adb install app\build\outputs\apk\civ\debug\app-civ-debug.apk
   ```

### Using the Plugin

1. **Launch ATAK** on your Android device

2. **Open Plugin**:
   - Look for "Feature 1 Gps Simulator" icon in ATAK toolbar
   - Tap the icon to open control panel

3. **Start Simulation**:
   - Click **"Start GPS Simulator"** button
   - Service starts and notification appears
   - Robot marker appears on map at current center
   - Live data starts updating

4. **Monitor Movement**:
   - Watch robot marker move in circular pattern
   - Observe live data updates:
     - Latitude/Longitude changing
     - Speed showing ~10 m/s
     - Heading rotating 0-360°
   - Status shows "Running"

5. **Stop Simulation**:
   - Click **"Stop GPS Simulator"** button
   - Service stops
   - Marker remains at last position
   - Status shows "Stopped"

### Expected Behavior

- **Circle Radius**: 100 meters from starting point
- **Speed**: 10 m/s (36 km/h)
- **Update Rate**: 1 second intervals
- **Complete Circle**: ~63 seconds per revolution
- **Marker Color**: Blue
- **Heading**: Tangent to circle (perpendicular to radius)

---

## Technical Specifications

### Broadcast Intent Details

**Action**: `com.atakmap.android.feature1gpssimulator.POSITION_UPDATE`

**Extras**:
- `latitude` (double): Current latitude in degrees
- `longitude` (double): Current longitude in degrees
- `speed` (double): Speed in m/s
- `heading` (double): Heading in degrees (0-360)

### Permissions Required

```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
```

### Service Configuration

- **Type**: Foreground Service
- **Notification Channel**: "GPS Simulator"
- **Notification ID**: 1
- **Sticky**: Returns `START_STICKY` for automatic restart

---

## File Structure

```
feature1-gps-simulator/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/com/atakmap/android/feature1gpssimulator/plugin/
│   │       │   ├── PluginTemplate.java      (Main plugin class)
│   │       │   └── GpsSimService.java       (Background service)
│   │       ├── res/
│   │       │   ├── layout/
│   │       │   │   └── main_layout.xml      (UI layout)
│   │       │   ├── values/
│   │       │   │   └── strings.xml          (App name & strings)
│   │       │   └── drawable/
│   │       │       └── ic_launcher.png      (Plugin icon)
│   │       ├── assets/
│   │       │   └── plugin.xml               (Plugin configuration)
│   │       └── AndroidManifest.xml          (Manifest with service)
│   └── build.gradle                         (Build configuration)
└── README.md                                (This file)
```

---

## Troubleshooting

### Service Won't Start
- **Check**: Android version >= 8.0 requires foreground service
- **Solution**: Plugin automatically handles this with `startForegroundService()`

### No Position Updates
- **Check**: BroadcastReceiver registered correctly
- **Check**: Logs for broadcast messages: `adb logcat | grep GpsSimService`
- **Solution**: Ensure receiver uses `RECEIVER_EXPORTED` flag on Android 13+

### Marker Not Visible
- **Check**: Map is zoomed to appropriate level
- **Check**: Marker UID is unique
- **Solution**: Pan map to starting location

### UI Not Updating
- **Check**: TextViews are not null
- **Check**: Broadcast action matches exactly
- **Solution**: Verify filter in `registerPositionReceiver()`

---

## Development Notes

### Code Highlights

1. **Foreground Service Pattern**:
   ```java
   if (android.os.Build.VERSION.SDK_INT >= 26) {
       pluginContext.startForegroundService(intent);
   } else {
       pluginContext.startService(intent);
   }
   ```

2. **BroadcastReceiver Registration (Android 13+)**:
   ```java
   if (android.os.Build.VERSION.SDK_INT >= 33) {
       context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED);
   }
   ```

3. **Map Marker Creation**:
   ```java
   Marker marker = new Marker(position, UUID.randomUUID().toString());
   marker.setType("a-f-G-E-S");
   marker.setMetaString("callsign", "SimRobot");
   ```

### Extension Ideas

- Add configurable radius and speed
- Support different path patterns (figure-8, waypoints)
- Add pause/resume functionality
- Export position history to file
- Multiple simultaneous robots
- Collision detection

---

## Status

**Current Status**: ✅ Fully Functional  
**Last Updated**: November 2025  
**Testing**: Verified on ATAK SDK 5.4.0.27  
**Known Issues**: None

---

## Point of Contact

Developed as part of ATAK plugin development training project.

---

## Compilation

**Requirements**:
- ATAK SDK 5.4.0.27
- Android Studio
- JDK 11 or higher
- Gradle 7.x

**Build Command**:
```bash
gradlew assembleCivDebug
```

**Output**:
```
app\build\outputs\apk\civ\debug\app-civ-debug.apk
```

---

## License

This plugin is developed for educational and demonstration purposes as part of ATAK SDK training.
