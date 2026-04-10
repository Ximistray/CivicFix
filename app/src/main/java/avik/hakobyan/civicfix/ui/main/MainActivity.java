package avik.hakobyan.civicfix.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

import avik.hakobyan.civicfix.R;
import avik.hakobyan.civicfix.ui.auth.LoginActivity;
import avik.hakobyan.civicfix.ui.map.MapActivity;
import avik.hakobyan.civicfix.ui.report.ReportProblemActivity;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();

        // Report Button
        LinearLayout btnReport = findViewById(R.id.btnReport);
        if (btnReport != null) {
            btnReport.setOnClickListener(v -> {
                v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100)
                        .withEndAction(() -> {
                            v.animate().scaleX(1f).scaleY(1f);
                            startActivity(new Intent(this, ReportProblemActivity.class));
                        });
            });
        }

        // Map Button
        LinearLayout btnMap = findViewById(R.id.btnMap);
        if (btnMap != null) {

            btnMap.setOnClickListener(v -> {
                v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100)
                        .withEndAction(() -> {
                            v.animate().scaleX(1f).scaleY(1f);
                            startActivity(new Intent(this, MapActivity.class));
                        });
            });
        }

        // Logout Button
        Button btnLogout = findViewById(R.id.btnLogout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> logout());
        }
    }

    private void logout() {
        mAuth.signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}