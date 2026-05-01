package avik.hakobyan.civicfix.ui.main;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.switchmaterial.SwitchMaterial;

import avik.hakobyan.civicfix.R;

public class NotificationSettingsActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "CivicFixPrefs";
    private static final String KEY_REPORT_UPDATES = "report_updates";
    private static final String KEY_NEARBY_ALERTS = "nearby_alerts";

    private SwitchMaterial switchReportUpdates, switchNearbyAlerts;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_settings);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        switchReportUpdates = findViewById(R.id.switchReportUpdates);
        switchNearbyAlerts = findViewById(R.id.switchNearbyAlerts);

        // Load saved preferences
        switchReportUpdates.setChecked(prefs.getBoolean(KEY_REPORT_UPDATES, true));
        switchNearbyAlerts.setChecked(prefs.getBoolean(KEY_NEARBY_ALERTS, false));

        // Save preferences on change
        switchReportUpdates.setOnCheckedChangeListener((buttonView, isChecked) -> 
                prefs.edit().putBoolean(KEY_REPORT_UPDATES, isChecked).apply());

        switchNearbyAlerts.setOnCheckedChangeListener((buttonView, isChecked) -> 
                prefs.edit().putBoolean(KEY_NEARBY_ALERTS, isChecked).apply());
    }
}
