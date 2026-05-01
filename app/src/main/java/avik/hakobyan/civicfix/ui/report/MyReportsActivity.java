package avik.hakobyan.civicfix.ui.report;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import avik.hakobyan.civicfix.LocaleHelper;
import avik.hakobyan.civicfix.R;
import avik.hakobyan.civicfix.model.Problem;
import avik.hakobyan.civicfix.model.ReportsAdapter;
import avik.hakobyan.civicfix.ui.main.MainActivity;
import avik.hakobyan.civicfix.ui.main.ProfileActivity;
import avik.hakobyan.civicfix.ui.map.MapActivity;

public class MyReportsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ReportsAdapter adapter;
    private List<Problem> allReports;
    private List<Problem> filteredReports;
    private ProgressBar progressBar;
    private LinearLayout emptyState;
    private ChipGroup filterChipGroup;
    
    private DatabaseReference databaseReference;
    private String currentUserId;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_reports);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.nav_reports);
        }

        initViews();
        
        currentUserId = FirebaseAuth.getInstance().getUid();
        if (currentUserId == null) {
            finish();
            return;
        }

        databaseReference = FirebaseDatabase.getInstance().getReference("problems");
        setupRecyclerView();
        setupFilters();
        setupBottomNavigation();
        loadReports();
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

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        emptyState = findViewById(R.id.emptyState);
        filterChipGroup = findViewById(R.id.filterChipGroup);
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        allReports = new ArrayList<>();
        filteredReports = new ArrayList<>();
        adapter = new ReportsAdapter(this, filteredReports);
        recyclerView.setAdapter(adapter);
    }

    private void setupFilters() {
        filterChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                applyFilter(getString(R.string.type_all));
            } else {
                Chip chip = findViewById(checkedIds.get(0));
                applyFilter(chip.getText().toString());
            }
        });
    }

    private void applyFilter(String type) {
        filteredReports.clear();
        if (type.equals(getString(R.string.type_all))) {
            filteredReports.addAll(allReports);
        } else {
            for (Problem p : allReports) {
                if (isTypeMatch(p.getType(), type)) {
                    filteredReports.add(p);
                }
            }
        }
        adapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private boolean isTypeMatch(String firebaseType, String uiType) {
        if (firebaseType == null || uiType == null) return false;
        return uiType.equals(getString(getResIdForType(firebaseType)));
    }

    private int getResIdForType(String type) {
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
                allReports.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Problem report = data.getValue(Problem.class);
                    if (report != null) {
                        report.setId(data.getKey());
                        allReports.add(report);
                    }
                }
                Collections.reverse(allReports);
                progressBar.setVisibility(View.GONE);
                applyFilter(getString(R.string.type_all));
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