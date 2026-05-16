package avik.hakobyan.civicfix.ui.map;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

import avik.hakobyan.civicfix.LocaleHelper;
import avik.hakobyan.civicfix.R;
import avik.hakobyan.civicfix.model.Account;
import avik.hakobyan.civicfix.model.Problem;
import avik.hakobyan.civicfix.ui.main.MainActivity;
import avik.hakobyan.civicfix.ui.main.ProfileActivity;
import avik.hakobyan.civicfix.ui.report.MyReportsActivity;
import avik.hakobyan.civicfix.ui.report.ReportDetailActivity;
import avik.hakobyan.civicfix.ui.report.ReportProblemActivity;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "MapActivity";
    private GoogleMap mMap;
    private DatabaseReference problemsRef;
    private DatabaseReference usersRef;
    private final Map<String, String> userNames = new HashMap<>();

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        problemsRef = FirebaseDatabase.getInstance().getReference("problems");
        usersRef = FirebaseDatabase.getInstance().getReference("users");

        FloatingActionButton fabAdd = findViewById(R.id.fabAddReport);
        if (fabAdd != null) {
            fabAdd.setOnClickListener(v -> startActivity(new Intent(this, ReportProblemActivity.class)));
        }

        setupBottomNavigation();
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_map);
            bottomNav.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_home) {
                    startActivity(new Intent(this, MainActivity.class));
                    return true;
                } else if (itemId == R.id.nav_reports) {
                    startActivity(new Intent(this, MyReportsActivity.class));
                    return true;
                } else if (itemId == R.id.nav_profile) {
                    startActivity(new Intent(this, ProfileActivity.class));
                    return true;
                }
                return itemId == R.id.nav_map;
            });
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }

        try {
            googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style));
        } catch (Exception e) {
            Log.e(TAG, "Map style error: ", e);
        }

        LatLng yerevan = new LatLng(40.1772, 44.5035);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(yerevan, 12));

        mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Override
            public View getInfoWindow(@NonNull Marker marker) {
                return null;
            }

            @Override
            public View getInfoContents(@NonNull Marker marker) {
                View view = getLayoutInflater().inflate(R.layout.custom_info_window, null);
                TextView title = view.findViewById(R.id.title);
                TextView desc = view.findViewById(R.id.description);
                TextView tvReporterName = view.findViewById(R.id.tvReporterName);
                ImageView ivImage = view.findViewById(R.id.ivInfoImage);

                Problem problem = (Problem) marker.getTag();
                if (problem != null) {
                    title.setText(getString(getResIdForType(problem.getType())));
                    desc.setText(problem.getDescription());
                    
                    String reporterName = userNames.get(problem.getUserId());
                    if (reporterName != null) {
                        tvReporterName.setText(reporterName);
                    } else {
                        tvReporterName.setText("...");
                        fetchUserName(problem.getUserId(), marker);
                    }

                    if (problem.getImageUrl() != null && !problem.getImageUrl().isEmpty()) {
                        ivImage.setVisibility(View.VISIBLE);
                        Glide.with(MapActivity.this)
                                .load(problem.getImageUrl())
                                .into(ivImage);
                    } else {
                        ivImage.setVisibility(View.GONE);
                    }
                }
                return view;
            }
        });

        mMap.setOnInfoWindowClickListener(marker -> {
            Problem problem = (Problem) marker.getTag();
            if (problem != null) {
                Intent intent = new Intent(MapActivity.this, ReportDetailActivity.class);
                intent.putExtra("reportId", problem.getId());
                startActivity(intent);
            }
        });

        loadProblems();
    }

    private void fetchUserName(String userId, Marker marker) {
        if (userId == null) return;
        usersRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Account account = snapshot.getValue(Account.class);
                if (account != null) {
                    userNames.put(userId, account.getName());
                    if (marker.isInfoWindowShown()) {
                        marker.showInfoWindow(); // Refresh info window
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private int getResIdForType(String type) {
        if (type == null) return R.string.type_other;
        switch (type) {
            case "Pothole":
                return R.string.type_pothole;
            case "Trash":
                return R.string.type_trash;
            case "Broken streetlight":
                return R.string.type_streetlight;
            case "Damaged road":
                return R.string.type_road;
            default:
                return R.string.type_other;
        }
    }

    private int getColorForType(String type) {
        if (type == null) return 0xFF6C63FF; // Default Purple
        switch (type) {
            case "Pothole":
                return 0xFFFF5252; // Red
            case "Trash":
                return 0xFF4CAF50; // Green
            case "Broken streetlight":
                return 0xFFFFC107; // Yellow/Amber
            case "Damaged road":
                return 0xFFFF9800; // Orange
            default:
                return 0xFF6C63FF; // Original Purple
        }
    }

    private void loadProblems() {
        problemsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (mMap == null) return;
                mMap.clear();

                for (DataSnapshot problemSnapshot : snapshot.getChildren()) {
                    Problem problem = problemSnapshot.getValue(Problem.class);
                    if (problem != null && problem.isVerified()) { // Only show verified
                        problem.setId(problemSnapshot.getKey());
                        addMarkerWithPhoto(problem);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void addMarkerWithPhoto(Problem problem) {
        LatLng location = new LatLng(problem.getLatitude(), problem.getLongitude());
        
        Marker marker = mMap.addMarker(new MarkerOptions()
                .position(location)
                .title(problem.getType()));
        
        if (marker != null) {
            marker.setTag(problem);
            
            if (problem.getImageUrl() != null && !problem.getImageUrl().isEmpty()) {
                loadMarkerIcon(problem.getImageUrl(), marker, problem.getType());
            }
        }
    }

    private void loadMarkerIcon(String url, Marker marker, String type) {
        Glide.with(this)
                .asBitmap()
                .load(url)
                .override(100, 100)
                .circleCrop()
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        Bitmap customMarker = createCustomMarkerBitmap(resource, getColorForType(type));
                        if (customMarker != null) {
                            marker.setIcon(BitmapDescriptorFactory.fromBitmap(customMarker));
                        }
                    }

                    @Override
                    public void onLoadCleared(@Nullable android.graphics.drawable.Drawable placeholder) {
                    }
                });
    }

    private Bitmap createCustomMarkerBitmap(Bitmap resource, int color) {
        View markerView = ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.marker_layout, null);
        
        ImageView markerBackground = markerView.findViewById(R.id.marker_background);
        markerBackground.setColorFilter(color, PorterDuff.Mode.SRC_IN);
        
        ImageView markerImage = markerView.findViewById(R.id.marker_image);
        markerImage.setImageBitmap(resource);

        markerView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        markerView.layout(0, 0, markerView.getMeasuredWidth(), markerView.getMeasuredHeight());
        markerView.buildDrawingCache();

        Bitmap bitmap = Bitmap.createBitmap(markerView.getMeasuredWidth(), markerView.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        markerView.draw(canvas);

        return bitmap;
    }
}
