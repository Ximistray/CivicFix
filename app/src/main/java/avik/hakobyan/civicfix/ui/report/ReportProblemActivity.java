package avik.hakobyan.civicfix.ui.report;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import avik.hakobyan.civicfix.R;

public class ReportProblemActivity extends AppCompatActivity {

    private static final String TAG = "ReportProblemActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 1002;
    private static final int CAMERA_REQUEST = 2001;

    private EditText etDescription;
    private Spinner spinnerType;
    private Button btnLocation, btnSendReport, btnSelectPhoto;
    private TextView tvLocation;
    private ImageView imagePreview;

    private Uri imageUri;
    private double latitude = 0, longitude = 0;

    private FusedLocationProviderClient locationClient;
    private FirebaseAuth mAuth;
    private DatabaseReference problemsRef;
    private FirebaseStorage storage;
    private StorageReference storageRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_problem);

        initViews();
        
        mAuth = FirebaseAuth.getInstance();
        problemsRef = FirebaseDatabase.getInstance().getReference("problems");
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();
        locationClient = LocationServices.getFusedLocationProviderClient(this);

        if (savedInstanceState != null) {
            String uriString = savedInstanceState.getString("imageUriString");
            if (uriString != null) {
                imageUri = Uri.parse(uriString);
                imagePreview.setImageURI(imageUri);
            }
        }

        setupSpinner();

        btnLocation.setOnClickListener(v -> checkLocationPermissionAndGetLocation());
        btnSelectPhoto.setOnClickListener(v -> checkCameraPermissionAndOpen());
        btnSendReport.setOnClickListener(v -> sendReport());
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (imageUri != null) {
            outState.putString("imageUriString", imageUri.toString());
        }
    }

    private void initViews() {
        etDescription = findViewById(R.id.etDescription);
        spinnerType = findViewById(R.id.spinnerType);
        btnLocation = findViewById(R.id.btnLocation);
        btnSendReport = findViewById(R.id.btnSendReport);
        btnSelectPhoto = findViewById(R.id.btnSelectPhoto);
        imagePreview = findViewById(R.id.imagePreview);
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

    private void checkCameraPermissionAndOpen() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
        } else {
            openCamera();
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

    private void openCamera() {
        try {
            // Using getExternalCacheDir so the camera app can write to the file
            File storageDir = getExternalCacheDir();
            File imageFile = File.createTempFile("report_", ".jpg", storageDir);
            imageUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", imageFile);
            
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            startActivityForResult(intent, CAMERA_REQUEST);
        } catch (IOException e) {
            Log.e(TAG, "Camera error", e);
            Toast.makeText(this, "Could not create image file", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getLocation();
        } else if (requestCode == CAMERA_PERMISSION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_REQUEST && resultCode == RESULT_OK) {
            if (imageUri != null) {
                imagePreview.setImageURI(null); 
                imagePreview.setImageURI(imageUri);
                Log.d(TAG, "Photo URI: " + imageUri);
            }
        }
    }

    private void sendReport() {
        String description = etDescription.getText().toString().trim();
        String type = spinnerType.getSelectedItem().toString();
        
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show();
            return;
        }
        String userId = mAuth.getCurrentUser().getUid();

        if (description.isEmpty()) { etDescription.setError("Description required"); return; }
        if (latitude == 0 && longitude == 0) { Toast.makeText(this, "Get location first", Toast.LENGTH_SHORT).show(); return; }

        btnSendReport.setEnabled(false);
        String reportId = problemsRef.push().getKey();
        if (reportId == null) { btnSendReport.setEnabled(true); return; }

        Map<String, Object> reportData = new HashMap<>();
        reportData.put("description", description);
        reportData.put("type", type);
        reportData.put("latitude", latitude);
        reportData.put("longitude", longitude);
        reportData.put("userId", userId);
        reportData.put("status", "OPEN");
        reportData.put("timestamp", System.currentTimeMillis());

        if (imageUri != null) {
            StorageReference fileRef = storageRef.child("report_images/" + reportId + ".jpg");
            Log.d(TAG, "Uploading image to: " + fileRef.getPath());
            
            fileRef.putFile(imageUri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    return fileRef.getDownloadUrl();
                })
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        reportData.put("imageUrl", task.getResult().toString());
                        saveReport(reportId, reportData);
                    } else {
                        Log.e(TAG, "Upload failed", task.getException());
                        Toast.makeText(this, "Upload failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        btnSendReport.setEnabled(true);
                    }
                });
        } else {
            saveReport(reportId, reportData);
        }
    }

    private void saveReport(String reportId, Map<String, Object> reportData) {
        problemsRef.child(reportId).setValue(reportData)
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Report sent successfully!", Toast.LENGTH_SHORT).show();
                finish();
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Database error", e);
                Toast.makeText(this, "Database error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                btnSendReport.setEnabled(true);
            });
    }
}