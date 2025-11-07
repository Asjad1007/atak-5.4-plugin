package com.atakmap.android.feature1gpssimulator.plugin;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

// If Android Studio can’t find R, add this import:
// import com.atakmap.android.feature1gpssimulator.plugin.R;

public class SimControllerActivity extends Activity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_sim_controller);

        Button start = findViewById(R.id.btnStart);
        Button stop  = findViewById(R.id.btnStop);

        start.setOnClickListener(v ->
                startForegroundService(new Intent(SimControllerActivity.this, GpsSimService.class)));

        stop.setOnClickListener(v ->
                stopService(new Intent(SimControllerActivity.this, GpsSimService.class)));
    }
}
