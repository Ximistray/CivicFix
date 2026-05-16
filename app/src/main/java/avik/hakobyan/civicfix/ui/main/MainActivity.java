package avik.hakobyan.civicfix.ui.main;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;

import avik.hakobyan.civicfix.LocaleHelper;
import avik.hakobyan.civicfix.R;
import avik.hakobyan.civicfix.ui.map.MapActivity;
import avik.hakobyan.civicfix.ui.report.AllReportsActivity;
import avik.hakobyan.civicfix.ui.report.MyReportsActivity;
import avik.hakobyan.civicfix.ui.report.ReportProblemActivity;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();

        // 1. Initialize Navigation Cards
        View btnReport = findViewById(R.id.btnReport);
        View btnMap = findViewById(R.id.btnMap);
        View btnHistory = findViewById(R.id.btnHistory);
        View btnAllReports = findViewById(R.id.btnAllReports);

        // 2. Setup Card Click Listeners
        if (btnReport != null) {
            btnReport.setOnClickListener(v -> animateAndStart(v, ReportProblemActivity.class));
        }

        if (btnMap != null) {
            btnMap.setOnClickListener(v -> animateAndStart(v, MapActivity.class));
        }

        if (btnHistory != null) {
            btnHistory.setOnClickListener(v -> animateAndStart(v, MyReportsActivity.class));
        }

        if (btnAllReports != null) {
            btnAllReports.setOnClickListener(v -> animateAndStart(v, AllReportsActivity.class));
        }

        // 3. Setup Floating Action Button
        FloatingActionButton fabAdd = findViewById(R.id.fabAddReport);
        if (fabAdd != null) {
            fabAdd.setOnClickListener(v -> startActivity(new Intent(this, ReportProblemActivity.class)));
        }

        // 4. Setup Bottom Navigation
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_home);
            bottomNav.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_map) {
                    startActivity(new Intent(this, MapActivity.class));
                    return true;
                } else if (itemId == R.id.nav_reports) {
                    startActivity(new Intent(this, MyReportsActivity.class));
                    return true;
                } else if (itemId == R.id.nav_profile) {
                    startActivity(new Intent(this, ProfileActivity.class));
                    return true;
                }
                return itemId == R.id.nav_home;
            });
        }
    }

    private void animateAndStart(View v, Class<?> cls) {
        v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100)
                .withEndAction(() -> {
                    v.animate().scaleX(1f).scaleY(1f);
                    startActivity(new Intent(MainActivity.this, cls));
                });
    }
}
