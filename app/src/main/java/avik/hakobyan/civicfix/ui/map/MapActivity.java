package avik.hakobyan.civicfix.ui.map;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import avik.hakobyan.civicfix.R;

public class MapActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final String TAG = "MapActivity";
    private GoogleMap mMap;
    private DatabaseReference problemsRef;

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
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }

        try {

            boolean success = googleMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style)
            );
            if (!success) {
                Log.e(TAG, "Style parsing failed.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Can't find style. Error: ", e);
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

                title.setText(marker.getTitle());
                desc.setText(marker.getSnippet());
                return view;
            }
        });

        loadProblems();
    }

    private void loadProblems() {
        problemsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (mMap == null) return;
                mMap.clear();

                for (DataSnapshot problem : snapshot.getChildren()) {
                    Double lat = problem.child("latitude").getValue(Double.class);
                    Double lon = problem.child("longitude").getValue(Double.class);
                    String type = problem.child("type").getValue(String.class);
                    String description = problem.child("description").getValue(String.class);

                    if (lat != null && lon != null) {
                        LatLng location = new LatLng(lat, lon);
                        float color = getMarkerColor(type);

                        mMap.addMarker(new MarkerOptions()
                                .position(location)
                                .title(type != null ? type : "Problem")
                                .snippet(description != null ? description : "")
                                .icon(BitmapDescriptorFactory.defaultMarker(color)));
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Database error: " + error.getMessage());
            }
        });
    }

    private float getMarkerColor(String type) {
        if (type == null) return BitmapDescriptorFactory.HUE_RED;
        switch (type) {
            case "Trash": return BitmapDescriptorFactory.HUE_GREEN;
            case "Broken streetlight": return BitmapDescriptorFactory.HUE_YELLOW;
            case "Pothole": return BitmapDescriptorFactory.HUE_BLUE;
            case "Damaged road": return BitmapDescriptorFactory.HUE_ORANGE;
            default: return BitmapDescriptorFactory.HUE_RED;
        }
    }
}