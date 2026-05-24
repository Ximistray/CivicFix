package avik.hakobyan.civicfix.ui.report;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import avik.hakobyan.civicfix.LocaleHelper;
import avik.hakobyan.civicfix.R;

public class ReportProblemActivity extends AppCompatActivity {

    private static final String TAG = "ReportProblemActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 1002;
    private static final int CAMERA_REQUEST = 2001;
    private static final int GALLERY_REQUEST = 2002;

    private TextInputEditText etDescription;
    private AutoCompleteTextView spinnerType;
    private Button btnLocation, btnSendReport;
    private View btnSelectPhoto;
    private TextView tvLocation;
    private ImageView imagePreview;
    private View photoPlaceholder;

    private Uri imageUri;
    private double latitude = 0, longitude = 0;
    private String region = "Unknown";

    private FusedLocationProviderClient locationClient;
    private FirebaseAuth mAuth;
    private DatabaseReference problemsRef;
    private FirebaseStorage storage;
    private StorageReference storageRef;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_problem);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

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
                photoPlaceholder.setVisibility(View.GONE);
            }
        }

        setupSpinner();

        btnLocation.setOnClickListener(v -> checkLocationPermissionAndGetLocation());
        btnSelectPhoto.setOnClickListener(v -> showPhotoOptions());
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
        photoPlaceholder = findViewById(R.id.photoPlaceholder);
    }

    private void setupSpinner() {
        String[] types = {
                getString(R.string.type_pothole),
                getString(R.string.type_trash),
                getString(R.string.type_streetlight),
                getString(R.string.type_road),
                getString(R.string.type_graffiti),
                getString(R.string.type_water),
                getString(R.string.type_parking),
                getString(R.string.type_sidewalk),
                getString(R.string.type_sign),
                getString(R.string.type_other)
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, types);
        spinnerType.setAdapter(adapter);
        spinnerType.setText(types[0], false);
    }

    private void showPhotoOptions() {
        String[] options = {getString(R.string.camera), getString(R.string.gallery)};
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.choose_photo_source)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        checkCameraPermissionAndOpen();
                    } else {
                        openGallery();
                    }
                })
                .show();
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

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, GALLERY_REQUEST);
    }

    private void getLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;
        locationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                latitude = location.getLatitude();
                longitude = location.getLongitude();
                updateRegionName(latitude, longitude);
                tvLocation.setText(getString(R.string.location_lat_lon, latitude, longitude));
            } else {
                Toast.makeText(this, getString(R.string.gps_error), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateRegionName(double lat, double lon) {
        Geocoder geocoder = new Geocoder(this, Locale.ENGLISH);
        try {
            List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address adr = addresses.get(0);
                region = adr.getAdminArea() != null ? adr.getAdminArea() : "Unknown";
                Log.d(TAG, "Detected Region: " + region);
            }
        } catch (IOException e) {
            Log.e(TAG, "Geocoder error", e);
        }
    }

    private void openCamera() {
        try {
            File storageDir = getExternalCacheDir();
            if (storageDir == null) {
                Toast.makeText(this, "Storage not available", Toast.LENGTH_SHORT).show();
                return;
            }
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
        if (resultCode == RESULT_OK) {
            if (requestCode == CAMERA_REQUEST) {
                if (imageUri != null) {
                    displaySelectedImage();
                }
            } else if (requestCode == GALLERY_REQUEST) {
                if (data != null && data.getData() != null) {
                    imageUri = data.getData();
                    displaySelectedImage();
                }
            }
        }
    }

    private void displaySelectedImage() {
        imagePreview.setImageURI(null); 
        imagePreview.setImageURI(imageUri);
        photoPlaceholder.setVisibility(View.GONE);
        Log.d(TAG, "Photo URI: " + imageUri);
    }

    private void sendReport() {
        if (etDescription.getText() == null) return;
        String description = etDescription.getText().toString().trim();
        String type = spinnerType.getText().toString();
        
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show();
            return;
        }
        String userId = mAuth.getCurrentUser().getUid();

        if (description.isEmpty()) { etDescription.setError(getString(R.string.description)); return; }
        if (latitude == 0 && longitude == 0) { Toast.makeText(this, getString(R.string.get_location), Toast.LENGTH_SHORT).show(); return; }

        btnSendReport.setEnabled(false);
        String reportId = problemsRef.push().getKey();
        if (reportId == null) { btnSendReport.setEnabled(true); return; }

        Map<String, Object> reportData = new HashMap<>();
        reportData.put("description", description);
        
        String dbType = getEnglishType(type);
        reportData.put("type", dbType);
        
        reportData.put("latitude", latitude);
        reportData.put("longitude", longitude);
        reportData.put("region", region);
        reportData.put("userId", userId);
        reportData.put("status", "pending");
        reportData.put("timestamp", System.currentTimeMillis());
        reportData.put("verified", false); 
        reportData.put("voteCount", 0);

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

    private String getEnglishType(String localizedType) {
        if (localizedType.equals(getString(R.string.type_pothole))) return "Pothole";
        if (localizedType.equals(getString(R.string.type_trash))) return "Trash";
        if (localizedType.equals(getString(R.string.type_streetlight))) return "Broken streetlight";
        if (localizedType.equals(getString(R.string.type_road))) return "Damaged road";
        if (localizedType.equals(getString(R.string.type_graffiti))) return "Graffiti";
        if (localizedType.equals(getString(R.string.type_water))) return "Water Leak";
        if (localizedType.equals(getString(R.string.type_parking))) return "Illegal Parking";
        if (localizedType.equals(getString(R.string.type_sidewalk))) return "Sidewalk Damage";
        if (localizedType.equals(getString(R.string.type_sign))) return "Damaged Sign";
        return "Other";
    }

    private void saveReport(String reportId, Map<String, Object> reportData) {
        problemsRef.child(reportId).setValue(reportData)
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(this, getString(R.string.submit_report) + " Success!", Toast.LENGTH_SHORT).show();
                finish();
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Database error", e);
                Toast.makeText(this, "Database error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                btnSendReport.setEnabled(true);
            });
    }
}
