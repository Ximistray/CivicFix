package avik.hakobyan.civicfix.ui.report;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import avik.hakobyan.civicfix.LocaleHelper;
import avik.hakobyan.civicfix.R;
import avik.hakobyan.civicfix.model.Problem;
import avik.hakobyan.civicfix.model.ReportsAdapter;
import avik.hakobyan.civicfix.ui.main.MainActivity;
import avik.hakobyan.civicfix.ui.main.ProfileActivity;
import avik.hakobyan.civicfix.ui.map.MapActivity;

public class MyReportsActivity extends AppCompatActivity {

    private static final String TAG = "MyReportsActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private RecyclerView recyclerView;
    private ReportsAdapter adapter;
    private List<Problem> allReports;
    private List<Problem> filteredReports;
    private ProgressBar progressBar;
    private LinearLayout emptyState;
    private EditText etSearch;
    private ImageButton btnFilterMenu;
    
    private DatabaseReference databaseReference;
    private String currentUserId;
    private String userRegion = "Unknown";
    
    // Filter State
    private String selectedCategory;
    private int selectedStatusTab = 0; // 0: Unsolved, 1: Solved
    private int selectedRegionTab = 1; // 0: My Region, 1: All Regions (Default to All for My Reports)
    private String searchQuery = "";

    private FusedLocationProviderClient locationClient;
    private final Map<String, Problem> reportCache = new HashMap<>();

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_reports);

        selectedCategory = getString(R.string.type_all);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.nav_reports);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        currentUserId = FirebaseAuth.getInstance().getUid();
        if (currentUserId == null) {
            finish();
            return;
        }

        locationClient = LocationServices.getFusedLocationProviderClient(this);
        initViews();
        databaseReference = FirebaseDatabase.getInstance().getReference("problems");
        setupRecyclerView();
        setupSearch();
        setupBottomNavigation();
        
        btnFilterMenu.setOnClickListener(v -> showFilterDialog());

        checkLocationPermissionAndGetRegion();
        loadReports();

        FloatingActionButton fabAdd = findViewById(R.id.fabAddReport);
        if (fabAdd != null) {
            fabAdd.setOnClickListener(v -> startActivity(new Intent(this, ReportProblemActivity.class)));
        }
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        emptyState = findViewById(R.id.emptyState);
        etSearch = findViewById(R.id.etSearch);
        btnFilterMenu = findViewById(R.id.btnFilterMenu);
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString().toLowerCase().trim();
                applyFilter();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void showFilterDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_filter_reports, null);
        TabLayout regionTabs = view.findViewById(R.id.dialogRegionTabs);
        TabLayout statusTabs = view.findViewById(R.id.dialogStatusTabs);
        ChipGroup categoryChips = view.findViewById(R.id.dialogCategoryChips);

        if (regionTabs != null) {
            TabLayout.Tab regionTab = regionTabs.getTabAt(selectedRegionTab);
            if (regionTab != null) regionTab.select();
        }
        if (statusTabs != null) {
            TabLayout.Tab statusTab = statusTabs.getTabAt(selectedStatusTab);
            if (statusTab != null) statusTab.select();
        }
        
        if (categoryChips != null) {
            for (int i = 0; i < categoryChips.getChildCount(); i++) {
                Chip chip = (Chip) categoryChips.getChildAt(i);
                if (chip.getText().toString().equals(selectedCategory)) {
                    chip.setChecked(true);
                    break;
                }
            }
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this)
                .setView(view);
        
        androidx.appcompat.app.AlertDialog dialog = builder.create();

        view.findViewById(R.id.btnApplyFilters).setOnClickListener(v -> {
            if (regionTabs != null) selectedRegionTab = regionTabs.getSelectedTabPosition();
            if (statusTabs != null) selectedStatusTab = statusTabs.getSelectedTabPosition();
            
            if (categoryChips != null) {
                int checkedId = categoryChips.getCheckedChipId();
                if (checkedId != View.NO_ID) {
                    selectedCategory = ((Chip) view.findViewById(checkedId)).getText().toString();
                }
            }
            
            applyFilter();
            dialog.dismiss();
        });

        view.findViewById(R.id.btnResetFilters).setOnClickListener(v -> {
            selectedRegionTab = 1; 
            selectedStatusTab = 0;
            selectedCategory = getString(R.string.type_all);
            applyFilter();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void checkLocationPermissionAndGetRegion() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            getUserRegion();
        }
    }

    private void getUserRegion() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;
        
        locationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                Geocoder geocoder = new Geocoder(this, Locale.ENGLISH);
                try {
                    List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        userRegion = addresses.get(0).getAdminArea();
                        if (userRegion == null) userRegion = "Unknown";
                        applyFilter();
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Geocoder error", e);
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getUserRegion();
        }
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_reports);
            bottomNav.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_home) {
                    startActivity(new Intent(this, MainActivity.class));
                    return true;
                } else if (itemId == R.id.nav_map) {
                    startActivity(new Intent(this, MapActivity.class));
                    return true;
                } else if (itemId == R.id.nav_profile) {
                    startActivity(new Intent(this, ProfileActivity.class));
                    return true;
                }
                return itemId == R.id.nav_reports;
            });
        }
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        allReports = new ArrayList<>();
        filteredReports = new ArrayList<>();
        adapter = new ReportsAdapter(this, filteredReports);
        recyclerView.setAdapter(adapter);
    }

    private void applyFilter() {
        boolean showSolved = (selectedStatusTab == 1);
        boolean onlyMyRegion = (selectedRegionTab == 0);

        filteredReports.clear();
        for (Problem p : allReports) {
            boolean matchesCategory = selectedCategory.equals(getString(R.string.type_all)) || isTypeMatch(p.getType(), selectedCategory);
            boolean isSolved = "solved".equalsIgnoreCase(p.getStatus());
            boolean matchesStatus = (showSolved == isSolved);
            boolean matchesRegion = !onlyMyRegion || (p.getRegion() != null && p.getRegion().equalsIgnoreCase(userRegion));
            
            boolean matchesSearch = searchQuery.isEmpty() || 
                    (p.getType() != null && p.getType().toLowerCase().contains(searchQuery)) ||
                    (p.getDescription() != null && p.getDescription().toLowerCase().contains(searchQuery));

            if (matchesCategory && matchesStatus && matchesRegion && matchesSearch) {
                filteredReports.add(p);
            }
        }
        
        sortReportsByVotes();
        adapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void sortReportsByVotes() {
        filteredReports.sort((p1, p2) -> Integer.compare(p2.getVoteCount(), p1.getVoteCount()));
    }

    private boolean isTypeMatch(String firebaseType, String uiType) {
        if (firebaseType == null || uiType == null) return false;
        return uiType.equals(getString(getResIdForType(firebaseType)));
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

    private void loadReports() {
        progressBar.setVisibility(View.VISIBLE);
        Query userReportsQuery = databaseReference.orderByChild("userId").equalTo(currentUserId);
        
        userReportsQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Problem> tempList = new ArrayList<>();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Problem report = data.getValue(Problem.class);
                    if (report != null) {
                        String id = data.getKey();
                        report.setId(id);
                        
                        // Consistency Cache Logic
                        if (report.getDescription() == null && reportCache.containsKey(id)) {
                            Problem cached = reportCache.get(id);
                            if (cached != null) {
                                report.setDescription(cached.getDescription());
                                report.setType(cached.getType());
                                report.imageUrl = cached.imageUrl;
                                report.setVerified(cached.isVerified());
                                report.setUserId(cached.getUserId());
                                report.setTimestamp(cached.getTimestamp());
                                report.setStatus(cached.getStatus());
                                report.setRegion(cached.getRegion());
                                report.latitude = cached.latitude;
                                report.longitude = cached.longitude;
                            }
                        } else if (report.getDescription() != null) {
                            reportCache.put(id, report);
                        }
                        
                        tempList.add(report);
                    }
                }
                allReports.clear();
                allReports.addAll(tempList);
                progressBar.setVisibility(View.GONE);
                applyFilter();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void updateEmptyState() {
        if (filteredReports.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }
}
