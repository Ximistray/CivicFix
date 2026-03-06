package avik.hakobyan.civicfix.ui.report;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

import avik.hakobyan.civicfix.R;

public class ReportProblemActivity extends AppCompatActivity {

    private static final String TAG = "ReportProblemActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private EditText etDescription;
    private Spinner spinnerType;
    private Button btnLocation;
    private Button btnSendReport;
    private TextView tvLocation;

    private double latitude = 0;
    private double longitude = 0;

    private FusedLocationProviderClient locationClient;
    private DatabaseReference problemsRef;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_problem);

        initViews();
        
        mAuth = FirebaseAuth.getInstance();
        locationClient = LocationServices.getFusedLocationProviderClient(this);
        
        // Ensure database is initialized properly. 
        // NOTE: If you have multiple databases, use FirebaseDatabase.getInstance("URL")
        problemsRef = FirebaseDatabase.getInstance().getReference("problems");

        setupSpinner();

        btnLocation.setOnClickListener(v -> checkLocationPermissionAndGetLocation());
        btnSendReport.setOnClickListener(v -> sendReport());
    }

    private void initViews() {
        etDescription = findViewById(R.id.etDescription);
        spinnerType = findViewById(R.id.spinnerType);
        btnLocation = findViewById(R.id.btnLocation);
        btnSendReport = findViewById(R.id.btnSendReport);
        tvLocation = findViewById(R.id.tvLocation);
    }

    private void setupSpinner() {
        String[] types = {"Pothole", "Trash", "Broken streetlight", "Damaged road", "Other"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, types);
        spinnerType.setAdapter(adapter);
    }

    private void checkLocationPermissionAndGetLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            getLocation();
        }
    }

    private void getLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;

        locationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                latitude = location.getLatitude();
                longitude = location.getLongitude();
                tvLocation.setText(String.format("Lat: %.6f, Lon: %.6f", latitude, longitude));
            } else {
                Toast.makeText(this, "Turn on GPS and try again", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendReport() {
        String description = etDescription.getText().toString().trim();
        String type = spinnerType.getSelectedItem().toString();
        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "anonymous";

        if (description.isEmpty()) {
            etDescription.setError("Description required");
            return;
        }

        if (latitude == 0 && longitude == 0) {
            Toast.makeText(this, "Get location first", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSendReport.setEnabled(false);
        String reportId = problemsRef.push().getKey();

        Map<String, Object> reportData = new HashMap<>();
        reportData.put("description", description);
        reportData.put("type", type);
        reportData.put("latitude", latitude);
        reportData.put("longitude", longitude);
        reportData.put("userId", userId);
        reportData.put("status", "OPEN");
        reportData.put("timestamp", System.currentTimeMillis());

        if (reportId != null) {
            problemsRef.child(reportId).setValue(reportData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Report sent successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Write failed", e);
                    Toast.makeText(this, "Database error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    btnSendReport.setEnabled(true);
                });
        }
    }
}