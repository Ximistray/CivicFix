package avik.hakobyan.civicfix.ui.main;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.switchmaterial.SwitchMaterial;

import avik.hakobyan.civicfix.R;

public class NotificationSettingsActivity extends AppCompatActivity {

    public static final String PREFS_NAME = "CivicFixPrefs";
    public static final String KEY_REPORT_UPDATES = "report_updates";
    public static final String KEY_NEARBY_ALERTS = "nearby_alerts";
    public static final String KEY_COMMENT_NOTIFICATIONS = "comment_notifications";

    private SwitchMaterial switchReportUpdates, switchNearbyAlerts, switchCommentNotifications;
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
        switchCommentNotifications = findViewById(R.id.switchCommentNotifications);

        // Load saved preferences
        switchReportUpdates.setChecked(prefs.getBoolean(KEY_REPORT_UPDATES, true));
        switchNearbyAlerts.setChecked(prefs.getBoolean(KEY_NEARBY_ALERTS, false));
        switchCommentNotifications.setChecked(prefs.getBoolean(KEY_COMMENT_NOTIFICATIONS, true));

        // Save preferences on change
        switchReportUpdates.setOnCheckedChangeListener((buttonView, isChecked) -> 
                prefs.edit().putBoolean(KEY_REPORT_UPDATES, isChecked).apply());

        switchNearbyAlerts.setOnCheckedChangeListener((buttonView, isChecked) -> 
                prefs.edit().putBoolean(KEY_NEARBY_ALERTS, isChecked).apply());
                
        switchCommentNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> 
                prefs.edit().putBoolean(KEY_COMMENT_NOTIFICATIONS, isChecked).apply());
    }
}
