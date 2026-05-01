package avik.hakobyan.civicfix.ui.map;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

import avik.hakobyan.civicfix.LocaleHelper;
import avik.hakobyan.civicfix.R;
import avik.hakobyan.civicfix.model.Problem;
import avik.hakobyan.civicfix.ui.main.MainActivity;
import avik.hakobyan.civicfix.ui.main.ProfileActivity;
import avik.hakobyan.civicfix.ui.report.MyReportsActivity;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "MapActivity";
    private GoogleMap mMap;
    private DatabaseReference problemsRef;
    private Map<String, Bitmap> imageCache = new HashMap<>();

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
            public View getInfoWindow(@NonNull Marker marker) { return null; }

            @Override
            public View getInfoContents(@NonNull Marker marker) {
                View view = getLayoutInflater().inflate(R.layout.custom_info_window, null);
                TextView title = view.findViewById(R.id.title);
                TextView desc = view.findViewById(R.id.description);
                ImageView ivImage = view.findViewById(R.id.ivInfoImage);

                Problem problem = (Problem) marker.getTag();
                if (problem != null) {
                    title.setText(getString(getResIdForType(problem.getType())));
                    desc.setText(problem.getDescription());

                    if (problem.getImageUrl() != null && !problem.getImageUrl().isEmpty()) {
                        ivImage.setVisibility(View.VISIBLE);
                        if (imageCache.containsKey(problem.getImageUrl())) {
                            ivImage.setImageBitmap(imageCache.get(problem.getImageUrl()));
                        } else {
                            loadAndCacheImage(marker, problem.getImageUrl());
                        }
                    } else {
                        ivImage.setVisibility(View.GONE);
                    }
                }
                return view;
            }
        });

        loadProblems();
    }

    private void loadAndCacheImage(Marker marker, String url) {
        Glide.with(this)
                .asBitmap()
                .load(url)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        imageCache.put(url, resource);
                        updateMarkerIcon(marker, resource);
                        if (marker.isInfoWindowShown()) {
                            marker.showInfoWindow();
                        }
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {}
                });
    }

    private void updateMarkerIcon(Marker marker, Bitmap bitmap) {
        Problem problem = (Problem) marker.getTag();
        if (problem == null) return;
        
        View markerView = getLayoutInflater().inflate(R.layout.layout_marker_custom, null);
        ImageView markerImage = markerView.findViewById(R.id.markerImage);
        ImageView markerBackground = markerView.findViewById(R.id.markerBackground);

        if (markerImage != null) {
            markerImage.setImageBitmap(bitmap);
        }

        if (markerBackground != null) {
            int color = getMarkerColorValue(problem.getType());
            markerBackground.setColorFilter(color);
        }

        marker.setIcon(BitmapDescriptorFactory.fromBitmap(createDrawableFromView(markerView)));
    }

    private Bitmap createDrawableFromView(View view) {
        view.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        view.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
        Bitmap bitmap = Bitmap.createBitmap(view.getMeasuredWidth(), view.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);
        return bitmap;
    }

    private int getMarkerColorValue(String type) {
        if (type == null) return 0xFFE2E8F0;
        switch (type) {
            case "Trash": return 0xFF4CAF50;
            case "Broken streetlight": return 0xFFFFEB3B;
            case "Pothole": return 0xFF2196F3;
            case "Damaged road": return 0xFFFF9800;
            default: return 0xFFF44336;
        }
    }

    private int getResIdForType(String type) {
        if (type == null) return R.string.type_other;
        switch (type) {
            case "Pothole": return R.string.type_pothole;
            case "Trash": return R.string.type_trash;
            case "Broken streetlight": return R.string.type_streetlight;
            case "Damaged road": return R.string.type_road;
            default: return R.string.type_other;
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
                    if (problem != null) {
                        problem.setId(problemSnapshot.getKey());
                        LatLng location = new LatLng(problem.getLatitude(), problem.getLongitude());

                        Marker marker = mMap.addMarker(new MarkerOptions().position(location));
                        if (marker != null) {
                            marker.setTag(problem);
                            if (problem.getImageUrl() != null && !problem.getImageUrl().isEmpty()) {
                                if (imageCache.containsKey(problem.getImageUrl())) {
                                    updateMarkerIcon(marker, imageCache.get(problem.getImageUrl()));
                                } else {
                                    loadAndCacheImage(marker, problem.getImageUrl());
                                }
                            }
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}
