package avik.hakobyan.civicfix.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

import avik.hakobyan.civicfix.R;
import avik.hakobyan.civicfix.ui.auth.LoginActivity;
import avik.hakobyan.civicfix.ui.report.ReportProblemActivity;

public class MainActivity extends AppCompatActivity {

    private Button btnLogout, btnGoReport;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();

        btnLogout = findViewById(R.id.btnLogout);
        btnGoReport = findViewById(R.id.btnReportProblem);

        btnLogout.setOnClickListener(v -> logout());
        btnGoReport.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ReportProblemActivity.class);
            startActivity(intent);
        });

    }

    private void logout() {
        mAuth.signOut();
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}